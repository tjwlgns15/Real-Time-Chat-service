package com.sjh.realtimechatservice.chat.dto;

import com.sjh.realtimechatservice.domain.message.ChatMessage;
import com.sjh.realtimechatservice.domain.message.ChatMessageType;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long senderId,
        String senderNickname,
        String content,
        ChatMessageType type,
        LocalDateTime createdAt,
        int unreadCount  // 방 참여자 수 - 읽은 인원 수
) {
    public static ChatMessageResponse of(ChatMessage message, int unreadCount) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSender() != null ? message.getSender().getId() : null,
                message.getSender() != null ? message.getSender().getNickname() : null,
                message.getContent(),
                message.getType(),
                message.getCreatedAt(),
                unreadCount
        );
    }
}