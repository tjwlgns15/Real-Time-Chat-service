package com.sjh.realtimechatservice.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {

    private final String name;
    private final String code;
    private final String message;

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getErrorCode(), errorCode.getMessage());
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse("INTERNAL_SERVER_ERROR", "S500", message);
    }
}
