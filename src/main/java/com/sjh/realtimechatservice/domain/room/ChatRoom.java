package com.sjh.realtimechatservice.domain.room;

import com.sjh.realtimechatservice.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_room")
@NoArgsConstructor
@Getter
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type;

    // GROUP 방만 사용, DM은 null
    @Column
    private String name;


    private ChatRoom(ChatRoomType type, String name) {
        this.type = type;
        this.name = name;
    }

    public static ChatRoom createDm() {
        return new ChatRoom(ChatRoomType.DM, null);
    }

    public static ChatRoom createGroup(String name) {
        return new ChatRoom(ChatRoomType.GROUP, name);
    }

    public boolean isDm() {
        return this.type == ChatRoomType.DM;
    }

    public boolean isGroup() {
        return this.type == ChatRoomType.GROUP;
    }
}



