package com.sparta.logistics.presentation.common.exception;

import com.sparta.logistics.presentation.common.dto.response.ErrorResponseCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final ErrorResponseCode responseCode;
    private final HttpStatus status;
    private final String message;

    public ApiException(ErrorResponseCode responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
        this.status = responseCode.getStatus();
        this.message = responseCode.getMessage();
    }

    public ApiException(ErrorResponseCode responseCode, String message) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
        this.status = responseCode.getStatus();
        this.message = message;
    }
}
