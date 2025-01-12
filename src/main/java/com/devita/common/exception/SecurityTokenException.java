package com.devita.common.exception;


import lombok.Getter;

@Getter
public class SecurityTokenException extends RuntimeException {

    private final ErrorCode errorCode;

    public SecurityTokenException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}