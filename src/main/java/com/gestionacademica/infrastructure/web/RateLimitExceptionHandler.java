package com.gestionacademica.infrastructure.web;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RateLimitExceptionHandler {
    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<Map<String, String>> tooManyRequests(RateLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
                .body(Map.of("error", "rate_limit_exceeded",
                        "message", "Se excedió el límite de solicitudes. Intenta de nuevo más tarde."));
    }
}
