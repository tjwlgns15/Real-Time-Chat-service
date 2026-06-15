package com.sjh.realtimechatservice.chat.dto;

import com.sjh.realtimechatservice.domain.member.Member;

public record MemberResponse (
        Long memberId,
        String username,
        String nickname
){
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getUsername(),
                member.getNickname()
        );
    }
}
