package com.sjh.realtimechatservice.domain.message;

import com.sjh.realtimechatservice.domain.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "message_read_status",
        uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "member_id"})
)
@NoArgsConstructor
@Getter
public class MessageReadStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private LocalDateTime readAt;


    private MessageReadStatus(ChatMessage message, Member member) {
        this.message = message;
        this.member = member;
        this.readAt = LocalDateTime.now();
    }

    public static MessageReadStatus of(ChatMessage message, Member member) {
        return new MessageReadStatus(message, member);
    }
}