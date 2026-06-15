package com.sjh.realtimechatservice.chat.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sjh.realtimechatservice.chat.redis.ReadCountCacheManager;
import com.sjh.realtimechatservice.common.config.KafkaConfig;
import com.sjh.realtimechatservice.domain.member.Member;
import com.sjh.realtimechatservice.domain.member.MemberRepository;
import com.sjh.realtimechatservice.domain.message.*;
import com.sjh.realtimechatservice.domain.room.ChatRoom;
import com.sjh.realtimechatservice.domain.room.ChatRoomMember;
import com.sjh.realtimechatservice.domain.room.ChatRoomMemberRepository;
import com.sjh.realtimechatservice.domain.room.ChatRoomRepository;
import com.sjh.realtimechatservice.chat.dto.ChatMessagePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageConsumer {

    private static final String CHAT_CHANNEL_PREFIX = "chat:room:";

    private final ChatMessageRepository chatMessageRepository;
    private final MessageReadStatusRepository messageReadStatusRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ReadCountCacheManager readCountCacheManager;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Kafka 컨슈머:
     * 1. MySQL에 메시지 저장
     * 2. 현재 방에 접속 중인 멤버(lastReadAt이 최근인 멤버) 읽음 처리
     * 3. Redis Pub/Sub 브로드캐스트
     * 4. DM 방이면 숨김 참여자 노출
     */
    @KafkaListener(topics = KafkaConfig.CHAT_MESSAGE_TOPIC, groupId = "chatting-service")
    @Transactional
    public void consume(ChatMessagePayload payload) {
        try {
            ChatMessage saved = saveMessage(payload);

            ChatMessagePayload payloadWithId = ChatMessagePayload.of(
                    saved.getId(),
                    payload.roomId(),
                    payload.senderId(),
                    payload.senderNickname(),
                    payload.content(),
                    payload.type(),
                    payload.createdAt()
            );

            // TEXT 메시지만 읽음 처리 대상
            if (saved.getType() == ChatMessageType.TEXT) {
                markAsReadForActiveMembers(saved);
            }

            publishToRedis(payloadWithId);

            if (saved.getRoom().isDm()) {
                showHiddenDmMembers(payload.roomId());
            }

            log.debug("[Consumer] 메시지 처리 완료 roomId={} messageId={}", payload.roomId(), saved.getId());

        } catch (Exception e) {
            log.error("[Consumer] 메시지 처리 실패 roomId={}", payload.roomId(), e);
            throw e;
        }
    }

    /**
     * 현재 방에 접속 중인 멤버 읽음 처리.
     * lastReadAt이 메시지 createdAt 이후인 멤버 = 방에 접속 중인 멤버로 판단.
     * 발신자 본인은 제외.
     */
    private void markAsReadForActiveMembers(ChatMessage message) {
        List<ChatRoomMember> roomMembers =
                chatRoomMemberRepository.findAllByRoomId(message.getRoom().getId());

        List<ChatRoomMember> activeMembers = roomMembers.stream()
                .filter(crm -> crm.getMember().getId() != null
                        && !crm.getMember().getId().equals(message.getSender().getId())
                        && crm.getLastReadAt() != null
                        && !crm.getLastReadAt().isBefore(message.getCreatedAt().minusMinutes(1)))
                .toList();

        if (activeMembers.isEmpty()) return;

        List<MessageReadStatus> readStatuses = activeMembers.stream()
                .map(crm -> MessageReadStatus.of(message, crm.getMember()))
                .toList();

        messageReadStatusRepository.saveAll(readStatuses);

        // Redis 읽음 수 캐시 업데이트
        readCountCacheManager.incrementReadCount(
                message.getRoom().getId(), List.of(message.getId()));

        log.debug("[Consumer] 접속자 읽음 처리 roomId={} messageId={} count={}",
                message.getRoom().getId(), message.getId(), activeMembers.size());
    }

    private ChatMessage saveMessage(ChatMessagePayload payload) {
        ChatRoom room = chatRoomRepository.findById(payload.roomId())
                .orElseThrow(() -> new IllegalStateException("채팅방 없음: " + payload.roomId()));

        if (payload.type() == ChatMessageType.SYSTEM) {
            return chatMessageRepository.save(ChatMessage.system(room, payload.content()));
        }

        Member sender = memberRepository.findById(payload.senderId())
                .orElseThrow(() -> new IllegalStateException("회원 없음: " + payload.senderId()));

        return chatMessageRepository.save(ChatMessage.text(room, sender, payload.content()));
    }

    private void publishToRedis(ChatMessagePayload payload) {
        try {
            String channel = CHAT_CHANNEL_PREFIX + payload.roomId();
            String message = objectMapper.writeValueAsString(payload);
            redisTemplate.convertAndSend(channel, message);
        } catch (Exception e) {
            log.error("[Consumer] Redis 발행 실패 roomId={}", payload.roomId(), e);
        }
    }

    private void showHiddenDmMembers(Long roomId) {
        chatRoomMemberRepository.findAllByRoomId(roomId).stream()
                .filter(ChatRoomMember::isHidden)
                .forEach(ChatRoomMember::show);
    }
}