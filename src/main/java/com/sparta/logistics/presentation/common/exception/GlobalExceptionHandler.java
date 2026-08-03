package com.sparta.logistics.presentation.common.exception;


import com.sparta.logistics.infrastructure.feign.exception.FeignApiException;
import com.sparta.logistics.presentation.common.dto.response.ErrorResponse;
import com.sparta.logistics.presentation.common.dto.response.ErrorResponseCode;
import feign.RetryableException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request
    ) {
        BindingResult bindingResult = e.getBindingResult();

        log.error("uri : {}, message : {}",
                request.getRequestURI(), e.getMessage(), e);

        HashMap<String, String> errors = new HashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ErrorResponse.toResponseEntity(ErrorResponseCode.INVALID_REQUEST, errors);
    }


    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
            ApiException e, HttpServletRequest request
    ) {
        log.error("errorCode : {}, uri : {}, message : {}",
                e.getResponseCode().getErrorCode(), request.getRequestURI(), e.getMessage());

        return ErrorResponse.toResponseEntity(e.getResponseCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e, HttpServletRequest request
    ) {
        log.error("uri : {}, message : {}",
                request.getRequestURI(), e.getMessage(), e);

        return ErrorResponse.toResponseEntity(ErrorResponseCode.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(FeignApiException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(
            FeignApiException e,
            HttpServletRequest request
    ) {
        log.error("Feign Error : {}", e.getMessage());

        return ResponseEntity
                .status(e.getStatus())
                .body(new ErrorResponse(
                        e.getErrorCode(),
                        e.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<ErrorResponse> handleRetryableException(
            RetryableException e,
            HttpServletRequest request
    ) {
        log.error("Feign Timeout : {}", e.getMessage());

        return ErrorResponse.toResponseEntity(
                ErrorResponseCode.FEIGN_CLIENT_ERROR
        );
    }
}
