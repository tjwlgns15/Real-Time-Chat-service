package com.sjh.realtimechatservice.domain.room;

import com.sjh.realtimechatservice.fixture.ChatFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomTest {

    @Test
    @DisplayName("createDm으로 생성된 방의 타입은 DM이다")
    void createDm_타입_DM() {
        ChatRoom room = ChatFixture.createDmRoom();

        assertThat(room.getType()).isEqualTo(ChatRoomType.DM);
        assertThat(room.isDm()).isTrue();
        assertThat(room.isGroup()).isFalse();
        assertThat(room.getName()).isNull();
    }

    @Test
    @DisplayName("createGroup으로 생성된 방의 타입은 GROUP이고 이름이 있다")
    void createGroup_타입_GROUP() {
        ChatRoom room = ChatFixture.createGroupRoom("개발팀");

        assertThat(room.getType()).isEqualTo(ChatRoomType.GROUP);
        assertThat(room.isGroup()).isTrue();
        assertThat(room.isDm()).isFalse();
        assertThat(room.getName()).isEqualTo("개발팀");
    }
}
