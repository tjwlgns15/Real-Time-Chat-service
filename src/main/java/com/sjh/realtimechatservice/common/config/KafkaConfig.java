package com.sjh.realtimechatservice.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String CHAT_MESSAGE_TOPIC = "chat-message";

    /**
     * 파티션 3개 — roomId % 3 으로 파티셔닝.
     * 같은 방의 메시지는 같은 파티션에 들어가므로 순서 보장.
     */
    @Bean
    public NewTopic chatMessageTopic() {
        return TopicBuilder.name(CHAT_MESSAGE_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
