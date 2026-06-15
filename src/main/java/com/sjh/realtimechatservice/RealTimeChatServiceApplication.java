package com.sjh.realtimechatservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class RealTimeChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealTimeChatServiceApplication.class, args);
    }

}
