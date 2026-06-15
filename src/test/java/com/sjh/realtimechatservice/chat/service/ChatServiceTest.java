package com.sjh.realtimechatservice.chat.service;

import com.sjh.realtimechatservice.chat.dto.ChatMessageRequest;
import com.sjh.realtimechatservice.chat.dto.ChatMessageResponse;
import com.sjh.realtimechatservice.chat.kafka.ChatMessageProducer;
import com.sjh.realtimechatservice.chat.redis.ReadCountCacheManager;
import com.sjh.realtimechatservice.common.exception.BusinessException;
import com.sjh.realtimechatservice.common.exception.ErrorCode;
import com.sjh.realtimechatservice.domain.member.Member;
import com.sjh.realtimechatservice.domain.member.MemberRepository;
import com.sjh.realtimechatservice.domain.message.ChatMessage;
import com.sjh.realtimechatservice.domain.message.ChatMessageRepository;
import com.sjh.realtimechatservice.domain.message.MessageReadStatus;
import com.sjh.realtimechatservice.domain.message.MessageReadStatusRepository;
import com.sjh.realtimechatservice.domain.room.ChatRoom;
import com.sjh.realtimechatservice.domain.room.ChatRoomMember;
import com.sjh.realtimechatservice.domain.room.ChatRoomMemberRepository;
import com.sjh.realtimechatservice.fixture.ChatFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock private MemberRepository memberRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private MessageReadStatusRepository messageReadStatusRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private ChatMessageProducer chatMessageProducer;
    @Mock private ReadCountCacheManager readCountCacheManager;

    private Member member;
    private ChatRoom dmRoom;
    private ChatRoomMember roomMember;

    @BeforeEach
    void setUp() {
        member = ChatFixture.createMember(1L, "user1");
        dmRoom = ChatFixture.createDmRoom();
        roomMember = ChatFixture.createRoomMember(dmRoom, member);
    }

    // ─── sendMessage ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("채팅방 참여자가 메시지를 전송하면 Kafka로 발행된다")
    void sendMessage_성공() {
        ChatMessageRequest request = new ChatMessageRequest(dmRoom.getId(), member.getId(), "안녕하세요");
        given(chatRoomMemberRepository.findByRoomIdAndMemberId(dmRoom.getId(), member.getId()))
                .willReturn(Optional.of(roomMember));

        chatService.sendMessage(request);

        verify(chatMessageProducer).send(any());
    }

    @Test
    @DisplayName("채팅방 참여자가 아니면 CHAT_ROOM_NOT_MEMBER 예외가 발생한다")
    void sendMessage_비참여자_예외() {
        ChatMessageRequest request = new ChatMessageRequest(dmRoom.getId(), 999L, "안녕하세요");
        given(chatRoomMemberRepository.findByRoomIdAndMemberId(any(), any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendMessage(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CHAT_ROOM_NOT_MEMBER));

        verify(chatMessageProducer, never()).send(any());
    }

    // ─── enterRoom ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("방 입장 시 안 읽은 메시지가 읽음 처리되고 Redis 캐시가 업데이트된다")
    void enterRoom_읽음_처리() {
        Member sender = ChatFixture.createMember(2L, "user2");
        ChatMessage unread1 = ChatFixture.createTextMessage(10L, dmRoom, sender, "메시지1");
        ChatMessage unread2 = ChatFixture.createTextMessage(11L, dmRoom, sender, "메시지2");

        given(chatRoomMemberRepository.findByRoomIdAndMemberId(dmRoom.getId(), member.getId()))
                .willReturn(Optional.of(roomMember));
        given(memberRepository.findById(member.getId()))
                .willReturn(Optional.of(member));
        given(chatMessageRepository.findUnreadMessages(any(), any(), anyLong()))
                .willReturn(List.of(unread1, unread2));
        given(messageReadStatusRepository.findReadMessageIds(anyList(), anyLong()))
                .willReturn(Set.of());

        chatService.enterRoom(dmRoom.getId(), member.getId());

        // DB 읽음 처리
        ArgumentCaptor<List<MessageReadStatus>> captor = ArgumentCaptor.forClass(List.class);
        verify(messageReadStatusRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);

        // Redis 캐시 업데이트
        verify(readCountCacheManager).incrementReadCount(any(), anyList());
    }

    @Test
    @DisplayName("이미 읽은 메시지는 중복으로 읽음 처리되지 않는다")
    void enterRoom_중복_읽음_방지() {
        Member sender = ChatFixture.createMember(2L, "user2");
        ChatMessage unread1 = ChatFixture.createTextMessage(10L, dmRoom, sender, "메시지1");
        ChatMessage unread2 = ChatFixture.createTextMessage(11L, dmRoom, sender, "메시지2");

        given(chatRoomMemberRepository.findByRoomIdAndMemberId(dmRoom.getId(), member.getId()))
                .willReturn(Optional.of(roomMember));
        given(memberRepository.findById(member.getId()))
                .willReturn(Optional.of(member));
        given(chatMessageRepository.findUnreadMessages(any(), any(), anyLong()))
                .willReturn(List.of(unread1, unread2));
        // 10L은 이미 읽음 처리됨
        given(messageReadStatusRepository.findReadMessageIds(anyList(), anyLong()))
                .willReturn(Set.of(10L));

        chatService.enterRoom(dmRoom.getId(), member.getId());

        ArgumentCaptor<List<MessageReadStatus>> captor = ArgumentCaptor.forClass(List.class);
        verify(messageReadStatusRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("안 읽은 메시지가 없으면 읽음 처리가 발생하지 않는다")
    void enterRoom_읽을_메시지_없음() {
        given(chatRoomMemberRepository.findByRoomIdAndMemberId(dmRoom.getId(), member.getId()))
                .willReturn(Optional.of(roomMember));
        given(memberRepository.findById(member.getId()))
                .willReturn(Optional.of(member));
        given(chatMessageRepository.findUnreadMessages(any(), any(), anyLong()))
                .willReturn(List.of());

        chatService.enterRoom(dmRoom.getId(), member.getId());

        verify(messageReadStatusRepository, never()).saveAll(any());
        verify(readCountCacheManager, never()).incrementReadCount(any(), any());
    }

    // ─── getMessages ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("메시지 조회 시 Redis 캐시에서 읽음 수를 가져와 unreadCount를 계산한다")
    void getMessages_unreadCount_계산() {
        ChatMessage msg1 = ChatFixture.createTextMessage(10L, dmRoom, member, "메시지1");
        ChatMessage msg2 = ChatFixture.createTextMessage(11L, dmRoom, member, "메시지2");

        given(chatRoomMemberRepository.findByRoomIdAndMemberId(dmRoom.getId(), member.getId()))
                .willReturn(Optional.of(roomMember));
        given(chatRoomMemberRepository.countByRoomId(dmRoom.getId())).willReturn(2);
        given(chatMessageRepository.findByRoomIdBeforeCursor(any(), any(), any(Pageable.class)))
                .willReturn(List.of(msg1, msg2));
        // msg1은 1명이 읽음, msg2는 아무도 안 읽음
        given(readCountCacheManager.getReadCount(dmRoom.getId(), 10L)).willReturn(1);
        given(readCountCacheManager.getReadCount(dmRoom.getId(), 11L)).willReturn(0);

        List<ChatMessageResponse> result = chatService.getMessages(dmRoom.getId(), member.getId(), null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).unreadCount()).isEqualTo(1); // 2 - 1
        assertThat(result.get(1).unreadCount()).isEqualTo(2); // 2 - 0
    }

    @Test
    @DisplayName("채팅방 참여자가 아니면 메시지 조회 시 CHAT_ROOM_NOT_MEMBER 예외가 발생한다")
    void getMessages_비참여자_예외() {
        given(chatRoomMemberRepository.findByRoomIdAndMemberId(any(), any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getMessages(dmRoom.getId(), 999L, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CHAT_ROOM_NOT_MEMBER));
    }
}