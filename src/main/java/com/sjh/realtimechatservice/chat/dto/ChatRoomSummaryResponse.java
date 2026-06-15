package com.sjh.realtimechatservice.chat.dto;

import com.sjh.realtimechatservice.domain.room.ChatRoom;
import com.sjh.realtimechatservice.domain.room.ChatRoomType;

import java.time.LocalDateTime;

/**
 * 채팅방 목록 화면에서 사용하는 DTO.
 * - lastMessage: 가장 최근 메시지 내용 (미리보기)
 * - unreadMessageCount: 내가 읽지 않은 메시지 수
 */
public record ChatRoomSummaryResponse(
        Long roomId,
        ChatRoomType type,
        String name,
        String lastMessage,
        LocalDateTime lastMessageAt,
        int unreadMessageCount
) {
    public static ChatRoomSummaryResponse of(
            ChatRoom room,
            String lastMessage,
            LocalDateTime lastMessageAt,
            int unreadMessageCount) {

        return new ChatRoomSummaryResponse(
                room.getId(),
                room.getType(),
                room.getName(),
                lastMessage,
                lastMessageAt,
                unreadMessageCount
        );
    }
}