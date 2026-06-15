package com.sjh.realtimechatservice.chat.controller;

import com.sjh.realtimechatservice.chat.dto.ChatMessagePayload;
import com.sjh.realtimechatservice.chat.dto.ChatMessageRequest;
import com.sjh.realtimechatservice.chat.dto.ChatMessageResponse;
import com.sjh.realtimechatservice.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatMessageController {

    private final ChatService chatService;

    /**
     * STOMP 메시지 수신.
     * 클라이언트: stompClient.send("/app/chat/message", {}, JSON.stringify(request))
     * 흐름: Controller → Kafka Producer → Consumer → Redis Pub/Sub → WebSocket
     */
    @MessageMapping("/chat/message")
    public void sendMessage(ChatMessageRequest request) {
        chatService.sendMessage(request);
    }

    /**
     * 방 입장 — 안 읽은 메시지 일괄 읽음 처리.
     * 클라이언트: stompClient.send("/app/chat/enter", {}, JSON.stringify({roomId, memberId}))
     */
    @MessageMapping("/chat/enter")
    public void enterRoom(ChatMessageRequest request) {
        chatService.enterRoom(request.roomId(), request.senderId());
    }

    /**
     * 이전 메시지 조회 REST API — 커서 기반 페이지네이션.
     * GET /api/chat/rooms/{roomId}/messages?memberId=1&cursorId=100
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable Long roomId,
            @RequestParam Long memberId,
            @RequestParam(required = false) Long cursorId) {

        return ResponseEntity.ok(chatService.getMessages(roomId, memberId, cursorId));
    }
}