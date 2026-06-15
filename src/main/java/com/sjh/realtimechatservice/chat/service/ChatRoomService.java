package com.sjh.realtimechatservice.chat.service;

import com.sjh.realtimechatservice.chat.dto.ChatMessagePayload;
import com.sjh.realtimechatservice.chat.dto.ChatRoomSummaryResponse;
import com.sjh.realtimechatservice.chat.kafka.ChatMessageProducer;
import com.sjh.realtimechatservice.chat.redis.ReadCountCacheManager;
import com.sjh.realtimechatservice.common.exception.BusinessException;
import com.sjh.realtimechatservice.common.exception.ErrorCode;
import com.sjh.realtimechatservice.domain.member.Member;
import com.sjh.realtimechatservice.domain.member.MemberRepository;
import com.sjh.realtimechatservice.domain.message.ChatMessage;
import com.sjh.realtimechatservice.domain.message.ChatMessageRepository;
import com.sjh.realtimechatservice.domain.message.ChatMessageType;
import com.sjh.realtimechatservice.domain.room.ChatRoom;
import com.sjh.realtimechatservice.domain.room.ChatRoomMember;
import com.sjh.realtimechatservice.domain.room.ChatRoomMemberRepository;
import com.sjh.realtimechatservice.domain.room.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageProducer chatMessageProducer;
    private final ReadCountCacheManager readCountCacheManager;

    /**
     * DM 방 생성 또는 기존 방 반환.
     * 기존 방이 있으면 숨김 상태를 해제하고 반환.
     */
    @Transactional
    public ChatRoom getOrCreateDmRoom(Long requesterId, Long targetId) {
        if (requesterId.equals(targetId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_INVALID_DM);
        }

        Member requester = memberRepository.findById(requesterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Member target = memberRepository.findById(targetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return chatRoomRepository.findDmRoom(requesterId, targetId)
                .map(room -> {
                    chatRoomMemberRepository.findByRoomIdAndMemberId(room.getId(), requesterId)
                            .ifPresent(ChatRoomMember::show);
                    return room;
                })
                .orElseGet(() -> createDmRoom(requester, target));
    }

    private ChatRoom createDmRoom(Member requester, Member target) {
        ChatRoom room = chatRoomRepository.save(ChatRoom.createDm());
        chatRoomMemberRepository.save(ChatRoomMember.join(room, requester));
        chatRoomMemberRepository.save(ChatRoomMember.join(room, target));
        return room;
    }

    /**
     * 그룹 방 생성.
     * 생성자 포함 memberIds 전원을 참여자로 등록하고 시스템 메시지 발행.
     */
    @Transactional
    public ChatRoom createGroupRoom(String name, List<Long> memberIds) {
        List<Member> members = memberRepository.findAllById(memberIds);
        if (members.size() != memberIds.size()) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        ChatRoom room = chatRoomRepository.save(ChatRoom.createGroup(name));
        members.forEach(member ->
                chatRoomMemberRepository.save(ChatRoomMember.join(room, member)));

        sendSystemMessage(room.getId(), name + " 채팅방이 생성되었습니다.");
        return room;
    }

    /**
     * 채팅방 목록 조회 — 숨김 처리된 방 제외, 마지막 메시지 + 안 읽은 수 포함.
     */
    public List<ChatRoomSummaryResponse> getRooms(Long memberId) {
        List<ChatRoomMember> roomMembers =
                chatRoomMemberRepository.findVisibleRoomsByMemberId(memberId);

        return roomMembers.stream()
                .map(crm -> {
                    ChatRoom room = crm.getRoom();
                    ChatMessage lastMessage = chatMessageRepository
                            .findLastMessageByRoomId(room.getId())
                            .orElse(null);

                    int unreadCount = chatMessageRepository.countUnreadMessages(
                            room.getId(), crm.getLastReadAt(), memberId);

                    return ChatRoomSummaryResponse.of(
                            room,
                            lastMessage != null ? lastMessage.getContent() : null,
                            lastMessage != null ? lastMessage.getCreatedAt() : null,
                            unreadCount
                    );
                })
                .toList();
    }

    /**
     * 퇴장 — DM은 숨김 처리, 그룹은 행 삭제 후 시스템 메시지.
     */
    @Transactional
    public void leaveRoom(Long roomId, Long memberId) {
        ChatRoomMember roomMember = chatRoomMemberRepository
                .findByRoomIdAndMemberId(roomId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_MEMBER));

        ChatRoom room = roomMember.getRoom();
        if (room.isDm()) {
            roomMember.hide();
            return;
        }

        String nickname = roomMember.getMember().getNickname();
        chatRoomMemberRepository.delete(roomMember);

        // 참여자 수 변경으로 기존 읽음 수 캐시가 무의미해지므로 삭제
        readCountCacheManager.deleteRoomCache(roomId);
        sendSystemMessage(roomId, nickname + "님이 퇴장했습니다.");
    }

    private void sendSystemMessage(Long roomId, String content) {
        ChatMessagePayload systemMessage = ChatMessagePayload.of(
                null, roomId, null, null, content, ChatMessageType.SYSTEM, LocalDateTime.now());
        chatMessageProducer.send(systemMessage);
    }
}