package com.sparta.logistics.common.code;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum GeneralResponseCode implements ApiResponseCode {
    // Common
    OK(HttpStatus.OK, "요청이 성공적으로 처리되었습니다."),
    CREATED(HttpStatus.CREATED, "성공적으로 생성되었습니다.");

    public int getCode() {
        return this.status.value();
    }

    private final HttpStatus status;
    private final String message;
}