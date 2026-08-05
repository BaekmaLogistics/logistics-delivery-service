package com.sparta.logistics.common.code;

import org.springframework.http.HttpStatus;

public interface ApiResponseCode {
    HttpStatus getStatus();
    String getMessage();
}
