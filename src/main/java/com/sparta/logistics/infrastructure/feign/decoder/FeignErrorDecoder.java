package com.sparta.logistics.infrastructure.feign.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.logistics.infrastructure.feign.dto.FeignErrorResponse;
import com.sparta.logistics.infrastructure.feign.exception.FeignApiException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    @Override
    public Exception decode (String methodKey, Response response){
        try{
            FeignErrorResponse error =
                    objectMapper.readValue(
                            response.body().asInputStream(),
                            FeignErrorResponse.class
                    );

            return new FeignApiException(
                    error.errorCode(),
                    error.message(),
                    response.status()
            );
        } catch (Exception e){
            return new FeignApiException(
                    "COMMON_OO03",
                    "Feign Client Error",
                    response.status()
            );
        }
    }
}
