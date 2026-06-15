package com.sjh.realtimechatservice.domain.room;

import com.sjh.realtimechatservice.fixture.ChatFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomMemberTest {

    private ChatRoom dmRoom;
    private ChatRoom groupRoom;

    @BeforeEach
    void setUp() {
        dmRoom = ChatFixture.createDmRoom();
        groupRoom = ChatFixture.createGroupRoom("테스트 그룹");
    }

    @Test
    @DisplayName("join 시 초기 상태는 hidden=false이다")
    void join_초기상태_hidden_false() {
        ChatRoomMember member = ChatRoomMember.join(dmRoom, ChatFixture.createMember(1L, "user1"));

        assertThat(member.isHidden()).isFalse();
        assertThat(member.getLastReadAt()).isNotNull();
    }

    @Test
    @DisplayName("hide 호출 시 hidden이 true로 전환된다")
    void hide_성공() {
        ChatRoomMember member = ChatRoomMember.join(dmRoom, ChatFixture.createMember(1L, "user1"));

        member.hide();

        assertThat(member.isHidden()).isTrue();
    }

    @Test
    @DisplayName("hide 후 show 호출 시 hidden이 false로 복구된다")
    void show_숨김_복구() {
        ChatRoomMember member = ChatRoomMember.join(dmRoom, ChatFixture.createMember(1L, "user1"));
        member.hide();

        member.show();

        assertThat(member.isHidden()).isFalse();
    }

    @Test
    @DisplayName("updateLastReadAt 호출 시 lastReadAt이 갱신된다")
    void updateLastReadAt_갱신() throws InterruptedException {
        ChatRoomMember member = ChatRoomMember.join(dmRoom, ChatFixture.createMember(1L, "user1"));
        var before = member.getLastReadAt();

        Thread.sleep(10);
        member.updateLastReadAt();

        assertThat(member.getLastReadAt()).isAfter(before);
    }
}