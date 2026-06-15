package com.sjh.realtimechatservice.domain.message;

import com.sjh.realtimechatservice.domain.member.Member;
import com.sjh.realtimechatservice.domain.room.ChatRoom;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_message",
        indexes = @Index(name = "idx_room_created", columnList = "room_id, created_at")
)
@NoArgsConstructor
@Getter
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    // SYSTEM 메시지는 sender가 없음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Member sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatMessageType type;

    @Column(nullable = false)
    private LocalDateTime createdAt;


    private ChatMessage(ChatRoom room, Member sender, String content, ChatMessageType type) {
        this.room = room;
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    public static ChatMessage text(ChatRoom room, Member sender, String content) {
        return new ChatMessage(room, sender, content, ChatMessageType.TEXT);
    }

    // 입장/퇴장 시스템 메시지
    public static ChatMessage system(ChatRoom room, String content) {
        return new ChatMessage(room, null, content, ChatMessageType.SYSTEM);
    }
}