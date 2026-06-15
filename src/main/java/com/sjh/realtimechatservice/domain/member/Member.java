package com.sjh.realtimechatservice.domain.member;

import com.sjh.realtimechatservice.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member")
@NoArgsConstructor
@Getter
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String nickname;


    private Member(String username, String nickname) {
        this.username = username;
        this.nickname = nickname;
    }

    public static Member create(String username, String nickname) {
        return new Member(username, nickname);
    }
}
