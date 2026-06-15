package com.sjh.realtimechatservice.chat.kafka;

import com.sjh.realtimechatservice.common.config.KafkaConfig;
import com.sjh.realtimechatservice.chat.dto.ChatMessagePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageProducer {

    private final KafkaTemplate<String, ChatMessagePayload> kafkaTemplate;


    /**
     * roomId를 파티션 키로 사용.
     * 같은 방의 메시지는 항상 같은 파티션으로 라우팅되어 순서가 보장됨.
     */
    public void send(ChatMessagePayload payload) {
        String partitionKey = String.valueOf(payload.roomId());

        kafkaTemplate.send(KafkaConfig.CHAT_MESSAGE_TOPIC, partitionKey, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] 메시지 전송 실패 roomId={} messageId={}",
                                payload.roomId(), payload.messageId(), ex);
                    } else {
                        log.debug("[Kafka] 메시지 전송 성공 roomId={} partition={}",
                                payload.roomId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
