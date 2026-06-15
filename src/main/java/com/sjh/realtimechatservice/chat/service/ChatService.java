package com.sjh.realtimechatservice.chat.service;

import com.sjh.realtimechatservice.chat.dto.ChatMessagePayload;
import com.sjh.realtimechatservice.chat.dto.ChatMessageRequest;
import com.sjh.realtimechatservice.chat.dto.ChatMessageResponse;
import com.sjh.realtimechatservice.chat.kafka.ChatMessageProducer;
import com.sjh.realtimechatservice.chat.redis.ReadCountCacheManager;
import com.sjh.realtimechatservice.common.exception.BusinessException;
import com.sjh.realtimechatservice.common.exception.ErrorCode;
import com.sjh.realtimechatservice.domain.member.Member;
import com.sjh.realtimechatservice.domain.member.MemberRepository;
import com.sjh.realtimechatservice.domain.message.*;
import com.sjh.realtimechatservice.domain.room.ChatRoomMember;
import com.sjh.realtimechatservice.domain.room.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private static final int MESSAGE_PAGE_SIZE = 50;

    private final MemberRepository memberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MessageReadStatusRepository messageReadStatusRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageProducer chatMessageProducer;
    private final ReadCountCacheManager readCountCacheManager;

    /**
     * 메시지 전송 — Kafka Producer로 발행.
     * 실제 DB 저장과 WebSocket 브로드캐스트는 Consumer에서 처리.
     */
    @Transactional
    public void sendMessage(ChatMessageRequest request) {
        ChatRoomMember sender = chatRoomMemberRepository
                .findByRoomIdAndMemberId(request.roomId(), request.senderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_MEMBER));

        ChatMessagePayload payload = ChatMessagePayload.of(
                null,
                request.roomId(),
                request.senderId(),
                sender.getMember().getNickname(),
                request.content(),
                ChatMessageType.TEXT,
                LocalDateTime.now()
        );

        chatMessageProducer.send(payload);
    }

    /**
     * 방 입장 처리:
     * 1. lastReadAt 이후 안 읽은 메시지 일괄 조회
     * 2. DB MessageReadStatus INSERT (중복 제외)
     * 3. Redis 읽음 수 캐시 증가
     * 4. lastReadAt 갱신
     */
    @Transactional
    public void enterRoom(Long roomId, Long memberId) {
        ChatRoomMember roomMember = chatRoomMemberRepository
                .findByRoomIdAndMemberId(roomId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_MEMBER));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        LocalDateTime lastReadAt = roomMember.getLastReadAt();

        List<ChatMessage> unreadMessages = chatMessageRepository
                .findUnreadMessages(roomId, lastReadAt, memberId);

        if (!unreadMessages.isEmpty()) {
            List<Long> unreadIds = unreadMessages.stream().map(ChatMessage::getId).toList();
            Set<Long> alreadyRead = messageReadStatusRepository.findReadMessageIds(unreadIds, memberId);

            List<ChatMessage> toRead = unreadMessages.stream()
                    .filter(msg -> !alreadyRead.contains(msg.getId()))
                    .toList();

            if (!toRead.isEmpty()) {
                List<MessageReadStatus> readStatuses = toRead.stream()
                        .map(msg -> MessageReadStatus.of(msg, member))
                        .toList();
                messageReadStatusRepository.saveAll(readStatuses);

                // Redis 읽음 수 캐시 업데이트
                List<Long> newlyReadIds = toRead.stream().map(ChatMessage::getId).toList();
                readCountCacheManager.incrementReadCount(roomId, newlyReadIds);
            }
        }

        roomMember.updateLastReadAt();
    }

    /**
     * 이전 메시지 조회 — 커서 기반 페이지네이션.
     * cursorId null이면 최신 메시지부터.

     * unreadCount = 방 참여자 수 - Redis 캐시에서 조회한 읽은 수.
     * 캐시 미스(0) 시 DB fallback 없이 0으로 처리 — 정확도보다 성능 우선.
     */
    public List<ChatMessageResponse> getMessages(Long roomId, Long memberId, Long cursorId) {
        chatRoomMemberRepository.findByRoomIdAndMemberId(roomId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_MEMBER));

        int memberCount = chatRoomMemberRepository.countByRoomId(roomId);

        List<ChatMessage> messages = chatMessageRepository.findByRoomIdBeforeCursor(
                roomId, cursorId, PageRequest.of(0, MESSAGE_PAGE_SIZE));

        return messages.stream()
                .map(msg -> {
                    int readCount = readCountCacheManager.getReadCount(roomId, msg.getId());
                    int unreadCount = Math.max(0, memberCount - readCount);
                    return ChatMessageResponse.of(msg, unreadCount);
                })
                .toList();
    }
}