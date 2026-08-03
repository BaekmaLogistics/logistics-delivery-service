package com.sparta.logistics.infrastructure.feign.exception;

import lombok.Getter;

@Getter
public class FeignApiException extends RuntimeException {

    private final String errorCode;
    private final int status;

    public FeignApiException(
            String errorCode,
            String message,
            int status
    ) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
