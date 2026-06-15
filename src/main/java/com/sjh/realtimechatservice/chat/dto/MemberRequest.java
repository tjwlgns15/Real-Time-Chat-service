package com.sjh.realtimechatservice.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record MemberRequest (
        @NotBlank(message = "username은 필수입니다.")
        String username,

        @NotBlank(message = "nickname은 필수입니다.")
        String nickname
){
}
