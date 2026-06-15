package com.sjh.realtimechatservice.domain.room;

import com.sjh.realtimechatservice.domain.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_room_member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "member_id"})
)
@NoArgsConstructor
@Getter
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    // DM: 나가면 숨김 처리, 새 메시지 오면 다시 노출
    @Column(nullable = false)
    private boolean hidden;

    // 마지막으로 읽은 시각 — 읽지 않은 메시지 수 계산 기준
    @Column
    private LocalDateTime lastReadAt;


    private ChatRoomMember(ChatRoom room, Member member) {
        this.room = room;
        this.member = member;
        this.joinedAt = LocalDateTime.now();
        this.hidden = false;
        this.lastReadAt = LocalDateTime.now();
    }

    public static ChatRoomMember join(ChatRoom room, Member member) {
        return new ChatRoomMember(room, member);
    }

    // DM 방 나가기 — 숨김 처리
    public void hide() {
        this.hidden = true;
    }

    // 새 메시지 수신 시 DM 방 다시 노출
    public void show() {
        this.hidden = false;
    }

    // 방 입장 시 읽음 처리
    public void updateLastReadAt() {
        this.lastReadAt = LocalDateTime.now();
    }
}