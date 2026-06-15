package com.sjh.realtimechatservice.chat.dto;

public record ChatMessageRequest(
        Long roomId,
        Long senderId,
        String content
) {}