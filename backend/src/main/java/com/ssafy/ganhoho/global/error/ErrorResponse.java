package com.ssafy.ganhoho.global.error;

import com.ssafy.ganhoho.global.constant.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {
    private final int status;
    private final String message;
    private final String code;

    public ErrorResponse(ErrorCode errorCode) {
        this.status = errorCode.getHttpStatus().value();
        this.message = errorCode.getMessage();
        this.code = errorCode.name();
    }

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.code = "UNKNOWN";
    }

    public ErrorResponse(HttpStatus httpStatus, String message) {
        this.status = httpStatus.value();
        this.message = message;
        this.code = "UNKNOWN";
    }
}
