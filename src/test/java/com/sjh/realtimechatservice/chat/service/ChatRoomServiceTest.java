package com.sjh.realtimechatservice.chat.service;

import com.sjh.realtimechatservice.chat.kafka.ChatMessageProducer;
import com.sjh.realtimechatservice.chat.redis.ReadCountCacheManager;
import com.sjh.realtimechatservice.common.exception.BusinessException;
import com.sjh.realtimechatservice.common.exception.ErrorCode;
import com.sjh.realtimechatservice.domain.member.Member;
import com.sjh.realtimechatservice.domain.member.MemberRepository;
import com.sjh.realtimechatservice.domain.message.ChatMessageRepository;
import com.sjh.realtimechatservice.domain.room.ChatRoom;
import com.sjh.realtimechatservice.domain.room.ChatRoomMember;
import com.sjh.realtimechatservice.domain.room.ChatRoomMemberRepository;
import com.sjh.realtimechatservice.domain.room.ChatRoomRepository;
import com.sjh.realtimechatservice.fixture.ChatFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Mock private MemberRepository memberRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatMessageProducer chatMessageProducer;
    @Mock private ReadCountCacheManager readCountCacheManager;

    private Member member1;
    private Member member2;

    @BeforeEach
    void setUp() {
        member1 = ChatFixture.createMember(1L, "user1");
        member2 = ChatFixture.createMember(2L, "user2");
    }

    // ─── getOrCreateDmRoom ───────────────────────────────────────────────────────

    @Test
    @DisplayName("기존 DM 방이 없으면 새로 생성된다")
    void getOrCreateDmRoom_신규_생성() {
        ChatRoom newRoom = ChatFixture.createDmRoom();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member1));
        given(memberRepository.findById(2L)).willReturn(Optional.of(member2));
        given(chatRoomRepository.findDmRoom(1L, 2L)).willReturn(Optional.empty());
        given(chatRoomRepository.save(any())).willReturn(newRoom);

        ChatRoom result = chatRoomService.getOrCreateDmRoom(1L, 2L);

        assertThat(result.isDm()).isTrue();
        // 두 멤버 모두 ChatRoomMember로 등록
        verify(chatRoomMemberRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("기존 DM 방이 있으면 새로 만들지 않고 반환한다")
    void getOrCreateDmRoom_기존_방_반환() {
        ChatRoom existingRoom = ChatFixture.createDmRoom();
        ChatRoomMember crm = ChatFixture.createRoomMember(existingRoom, member1);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member1));
        given(memberRepository.findById(2L)).willReturn(Optional.of(member2));
        given(chatRoomRepository.findDmRoom(1L, 2L)).willReturn(Optional.of(existingRoom));
        given(chatRoomMemberRepository.findByRoomIdAndMemberId(existingRoom.getId(), 1L))
                .willReturn(Optional.of(crm));

        ChatRoom result = chatRoomService.getOrCreateDmRoom(1L, 2L);

        assertThat(result).isEqualTo(existingRoom);
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("기존 DM 방이 숨김 처리되어 있으면 다시 노출된다")
    void getOrCreateDmRoom_숨김_방_노출() {
        ChatRoom existingRoom = ChatFixture.createDmRoom();
        ChatRoomMember crm = ChatFixture.createRoomMember(existingRoom, member1);
        crm.hide();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member1));
        given(memberRepository.findById(2L)).willReturn(Optional.of(member2));
        given(chatRoomRepository.findDmRoom(1L, 2L)).willReturn(Optional.of(existingRoom));
        given(chatRoomMemberRepository.findByRoomIdAndMemberId(existingRoom.getId(), 1L))
                .willReturn(Optional.of(crm));

        chatRoomService.getOrCreateDmRoom(1L, 2L);

        assertThat(crm.isHidden()).isFalse();
    }

    @Test
    @DisplayName("자기 자신과 DM을 시도하면 CHAT_ROOM_INVALID_DM 예외가 발생한다")
    void getOrCreateDmRoom_자기자신_예외() {
        assertThatThrownBy(() -> chatRoomService.getOrCreateDmRoom(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CHAT_ROOM_INVALID_DM));
    }

    // ─── createGroupRoom ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("그룹 방 생성 시 참여자가 등록되고 시스템 메시지가 발행된다")
    void createGroupRoom_성공() {
        ChatRoom groupRoom = ChatFixture.createGroupRoom("개발팀");

        given(memberRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(member1, member2));
        given(chatRoomRepository.save(any())).willReturn(groupRoom);

        ChatRoom result = chatRoomService.createGroupRoom("개발팀", List.of(1L, 2L));

        assertThat(result.isGroup()).isTrue();
        verify(chatRoomMemberRepository, times(2)).save(any());
        // 시스템 메시지 발행
        verify(chatMessageProducer).send(any());
    }

    @Test
    @DisplayName("존재하지 않는 멤버가 포함되면 MEMBER_NOT_FOUND 예외가 발생한다")
    void createGroupRoom_존재하지_않는_멤버_예외() {
        given(memberRepository.findAllById(List.of(1L, 999L))).willReturn(List.of(member1));

        assertThatThrownBy(() -> chatRoomService.createGroupRoom("개발팀", List.of(1L, 999L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    // ─── leaveRoom ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DM 방 퇴장 시 행 삭제 없이 숨김 처리된다")
    void leaveRoom_DM_숨김처리() {
        ChatRoom dmRoom = ChatFixture.createDmRoom();
        ChatRoomMember crm = ChatFixture.createRoomMember(dmRoom, member1);

        given(chatRoomMemberRepository.findByRoomIdAndMemberId(dmRoom.getId(), member1.getId()))
                .willReturn(Optional.of(crm));

        chatRoomService.leaveRoom(dmRoom.getId(), member1.getId());

        assertThat(crm.isHidden()).isTrue();
        verify(chatRoomMemberRepository, never()).delete(any());
    }

    @Test
    @DisplayName("그룹 방 퇴장 시 행이 삭제되고 시스템 메시지가 발행되며 Redis 캐시가 삭제된다")
    void leaveRoom_그룹_삭제_및_시스템메시지() {
        ChatRoom groupRoom = ChatFixture.createGroupRoom("개발팀");
        ChatRoomMember crm = ChatFixture.createRoomMember(groupRoom, member1);

        given(chatRoomMemberRepository.findByRoomIdAndMemberId(groupRoom.getId(), member1.getId()))
                .willReturn(Optional.of(crm));

        chatRoomService.leaveRoom(groupRoom.getId(), member1.getId());

        verify(chatRoomMemberRepository).delete(crm);
        verify(chatMessageProducer).send(any());
        verify(readCountCacheManager).deleteRoomCache(groupRoom.getId());
    }

    @Test
    @DisplayName("채팅방 참여자가 아니면 CHAT_ROOM_NOT_MEMBER 예외가 발생한다")
    void leaveRoom_비참여자_예외() {
        given(chatRoomMemberRepository.findByRoomIdAndMemberId(any(), any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> chatRoomService.leaveRoom(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CHAT_ROOM_NOT_MEMBER));
    }
}