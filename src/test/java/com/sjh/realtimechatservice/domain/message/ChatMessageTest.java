package com.sjh.realtimechatservice.domain.message;

import com.sjh.realtimechatservice.fixture.ChatFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ChatMessageTest {

    @Test
    @DisplayName("text 메시지 생성 시 type은 TEXT이고 sender가 있다")
    void text_메시지_생성() {
        var room = ChatFixture.createDmRoom();
        var sender = ChatFixture.createMember(1L, "user1");

        ChatMessage message = ChatMessage.text(room, sender, "안녕하세요");

        assertThat(message.getType()).isEqualTo(ChatMessageType.TEXT);
        assertThat(message.getSender()).isEqualTo(sender);
        assertThat(message.getContent()).isEqualTo("안녕하세요");
        assertThat(message.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("system 메시지 생성 시 type은 SYSTEM이고 sender가 null이다")
    void system_메시지_생성() {
        var room = ChatFixture.createGroupRoom("개발팀");

        ChatMessage message = ChatMessage.system(room, "개발팀 채팅방이 생성되었습니다.");

        assertThat(message.getType()).isEqualTo(ChatMessageType.SYSTEM);
        assertThat(message.getSender()).isNull();
        assertThat(message.getContent()).isEqualTo("개발팀 채팅방이 생성되었습니다.");
    }
}
