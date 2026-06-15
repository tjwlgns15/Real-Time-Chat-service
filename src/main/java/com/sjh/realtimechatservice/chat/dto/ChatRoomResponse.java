package com.sjh.realtimechatservice.chat.dto;

import com.sjh.realtimechatservice.domain.room.ChatRoom;
import com.sjh.realtimechatservice.domain.room.ChatRoomType;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long roomId,
        ChatRoomType type,
        String name,
        LocalDateTime createdAt
) {
    public static ChatRoomResponse from(ChatRoom room) {
        return new ChatRoomResponse(
                room.getId(),
                room.getType(),
                room.getName(),
                room.getCreatedAt()
        );
    }
}