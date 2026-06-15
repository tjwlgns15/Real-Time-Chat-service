package com.sjh.realtimechatservice.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "존재하지 않는 회원입니다."),
    MEMBER_DUPLICATE_USERNAME(HttpStatus.CONFLICT, "M002","이미 사용 중인 아이디입니다."),

    // ChatRoom
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "존재하지 않는 채팅방입니다."),
    CHAT_ROOM_NOT_MEMBER(HttpStatus.FORBIDDEN, "R002", "채팅방 참여자가 아닙니다."),
    CHAT_ROOM_ALREADY_EXISTS(HttpStatus.CONFLICT, "R003", "이미 존재하는 DM 채팅방입니다."),
    CHAT_ROOM_INVALID_DM(HttpStatus.BAD_REQUEST, "R004", "DM은 자기 자신과 할 수 없습니다."),

    // ChatMessage
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "C001", "존재하지 않는 메시지입니다.");

    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String message;
}

