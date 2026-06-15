package com.sjh.realtimechatservice.fixture;

import com.sjh.realtimechatservice.domain.member.Member;
import com.sjh.realtimechatservice.domain.message.ChatMessage;
import com.sjh.realtimechatservice.domain.room.ChatRoom;
import com.sjh.realtimechatservice.domain.room.ChatRoomMember;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.LongStream;

public class ChatFixture {

    public static Member createMember(Long id, String username) {
        Member member = Member.create(username, username + "_닉네임");
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    public static ChatRoom createDmRoom() {
        ChatRoom room = ChatRoom.createDm();
        ReflectionTestUtils.setField(room, "id", 1L);
        return room;
    }

    public static ChatRoom createGroupRoom(String name) {
        ChatRoom room = ChatRoom.createGroup(name);
        ReflectionTestUtils.setField(room, "id", 2L);
        return room;
    }

    public static ChatRoomMember createRoomMember(ChatRoom room, Member member) {
        ChatRoomMember crm = ChatRoomMember.join(room, member);
        ReflectionTestUtils.setField(crm, "id", member.getId());
        return crm;
    }

    public static ChatMessage createTextMessage(Long id, ChatRoom room, Member sender, String content) {
        ChatMessage message = ChatMessage.text(room, sender, content);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    public static ChatMessage createSystemMessage(Long id, ChatRoom room, String content) {
        ChatMessage message = ChatMessage.system(room, content);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    public static List<Member> createMembers(int count) {
        return LongStream.rangeClosed(1, count)
                .mapToObj(i -> createMember(i, "user" + i))
                .toList();
    }
}