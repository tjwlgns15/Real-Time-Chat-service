package com.sjh.realtimechatservice.chat.kafka;

import com.sjh.realtimechatservice.chat.dto.ChatMessagePayload;
import com.sjh.realtimechatservice.domain.member.Member;
import com.sjh.realtimechatservice.domain.member.MemberRepository;
import com.sjh.realtimechatservice.domain.message.ChatMessage;
import com.sjh.realtimechatservice.domain.message.ChatMessageRepository;
import com.sjh.realtimechatservice.domain.message.ChatMessageType;
import com.sjh.realtimechatservice.domain.message.MessageReadStatusRepository;
import com.sjh.realtimechatservice.domain.room.ChatRoom;
import com.sjh.realtimechatservice.domain.room.ChatRoomMember;
import com.sjh.realtimechatservice.domain.room.ChatRoomMemberRepository;
import com.sjh.realtimechatservice.domain.room.ChatRoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 실제 Kafka, MySQL, Redis 컨테이너 필요.

 * Awaitility로 Consumer 비동기 처리를 대기.
 */
@Slf4j
@SpringBootTest
class ChatMessageKafkaIntegrationTest {

    @Autowired private ChatMessageProducer chatMessageProducer;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private ChatRoomMemberRepository chatRoomMemberRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MessageReadStatusRepository messageReadStatusRepository;
    @Autowired private RedisTemplate<String, String> redisTemplate;

    private ChatRoom room;
    private Member sender;
    private Member receiver;

    @BeforeEach
    void setUp() {
        sender = memberRepository.save(Member.create("sender", "발신자"));
        receiver = memberRepository.save(Member.create("receiver", "수신자"));
        room = chatRoomRepository.save(ChatRoom.createDm());
        chatRoomMemberRepository.save(ChatRoomMember.join(room, sender));
        chatRoomMemberRepository.save(ChatRoomMember.join(room, receiver));
    }

    @AfterEach
    void tearDown() {
        messageReadStatusRepository.deleteAll();
        chatRoomMemberRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        memberRepository.deleteAll();
        redisTemplate.delete("read_count:" + room.getId());
    }

    @Test
    @DisplayName("Producer가 발행한 TEXT 메시지가 Consumer에 의해 DB에 저장된다")
    void 메시지_발행_후_DB_저장() {
        ChatMessagePayload payload = ChatMessagePayload.of(
                null,
                room.getId(),
                sender.getId(),
                sender.getNickname(),
                "안녕하세요",
                ChatMessageType.TEXT,
                LocalDateTime.now()
        );

        chatMessageProducer.send(payload);

        // Consumer가 비동기로 처리하므로 최대 10초 대기
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<ChatMessage> messages = chatMessageRepository.findAll();
            assertThat(messages).hasSize(1);
            assertThat(messages.get(0).getContent()).isEqualTo("안녕하세요");
            assertThat(messages.get(0).getType()).isEqualTo(ChatMessageType.TEXT);
            assertThat(messages.get(0).getSender().getId()).isEqualTo(sender.getId());
        });
    }

    @Test
    @DisplayName("Producer가 발행한 SYSTEM 메시지가 Consumer에 의해 DB에 저장된다")
    void 시스템_메시지_발행_후_DB_저장() {
        ChatMessagePayload payload = ChatMessagePayload.of(
                null,
                room.getId(),
                null,
                null,
                "테스트 채팅방이 생성되었습니다.",
                ChatMessageType.SYSTEM,
                LocalDateTime.now()
        );

        chatMessageProducer.send(payload);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<ChatMessage> messages = chatMessageRepository.findAll();
            assertThat(messages).hasSize(1);
            assertThat(messages.get(0).getType()).isEqualTo(ChatMessageType.SYSTEM);
            assertThat(messages.get(0).getSender()).isNull();
        });
    }

    @Test
    @DisplayName("같은 방의 메시지는 파티션 키(roomId)로 인해 순서가 보장된다")
    void 메시지_순서_보장() {
        for (int i = 1; i <= 5; i++) {
            chatMessageProducer.send(ChatMessagePayload.of(
                    null, room.getId(), sender.getId(), sender.getNickname(),
                    "메시지" + i, ChatMessageType.TEXT, LocalDateTime.now()
            ));
        }

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            List<ChatMessage> messages = chatMessageRepository
                    .findByRoomIdBeforeCursor(room.getId(), null,
                            org.springframework.data.domain.PageRequest.of(0, 10));

            assertThat(messages).hasSize(5);
            // DESC 정렬이므로 역순으로 확인
            assertThat(messages.get(0).getContent()).isEqualTo("메시지5");
            assertThat(messages.get(4).getContent()).isEqualTo("메시지1");
        });
    }

    @Test
    @DisplayName("DM 방에서 메시지 발행 시 숨김 처리된 참여자가 다시 노출된다")
    void DM_숨김_참여자_노출() {
        // sender가 방을 나가서 숨김 처리
        chatRoomMemberRepository.findByRoomIdAndMemberId(room.getId(), sender.getId())
                .ifPresent(crm -> {
                    crm.hide();
                    chatRoomMemberRepository.save(crm);
                });

        chatMessageProducer.send(ChatMessagePayload.of(
                null, room.getId(), receiver.getId(), receiver.getNickname(),
                "안녕하세요", ChatMessageType.TEXT, LocalDateTime.now()
        ));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            chatRoomMemberRepository.findByRoomIdAndMemberId(room.getId(), sender.getId())
                    .ifPresent(crm -> assertThat(crm.isHidden()).isFalse());
        });
    }
}