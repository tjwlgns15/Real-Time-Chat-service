package com.sjh.realtimechatservice.chat.dto;

import com.sjh.realtimechatservice.domain.message.ChatMessageType;

import java.time.LocalDateTime;

/**
 * Kafka 토픽과 Redis Pub/Sub 채널 양쪽에서 사용하는 메시지 페이로드.
 */
public record ChatMessagePayload(
        Long messageId,
        Long roomId,
        Long senderId,
        String senderNickname,
        String content,
        ChatMessageType type,
        LocalDateTime createdAt
) {
    public static ChatMessagePayload of(
            Long messageId,
            Long roomId,
            Long senderId,
            String senderNickname,
            String content,
            ChatMessageType type,
            LocalDateTime createdAt) {

        return new ChatMessagePayload(messageId, roomId, senderId, senderNickname, content, type, createdAt);
    }
}
