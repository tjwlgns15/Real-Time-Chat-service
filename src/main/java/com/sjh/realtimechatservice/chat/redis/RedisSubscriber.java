package com.sjh.realtimechatservice.chat.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sjh.realtimechatservice.chat.dto.ChatMessagePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis Pub/Sub 수신 → WebSocket 브로드캐스트.
     * 채널명: chat:room:{roomId}
     * 구독 경로: /topic/chat/{roomId}

     * 멀티 서버 환경에서도 Redis가 모든 인스턴스에 메시지를 발행하므로
     * 어느 서버에 연결된 클라이언트든 메시지를 수신할 수 있음.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            ChatMessagePayload payload = objectMapper.readValue(body, ChatMessagePayload.class);

            String destination = "/topic/chat/" + payload.roomId();
            messagingTemplate.convertAndSend(destination, payload);

            log.debug("[Redis→WS] roomId={} messageId={}", payload.roomId(), payload.messageId());
        } catch (Exception e) {
            log.error("[Redis→WS] 브로드캐스트 실패", e);
        }
    }
}