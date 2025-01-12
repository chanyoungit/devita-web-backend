package com.devita.common.exception;

import lombok.Getter;

@Getter
public class IllegalArgumentException extends java.lang.IllegalArgumentException {

    private final ErrorCode errorCode;

    public IllegalArgumentException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}