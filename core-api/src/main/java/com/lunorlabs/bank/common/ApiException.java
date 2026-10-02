package com.lunorlabs.bank.common;

import org.springframework.http.HttpStatus;

/** Protocol-level problem (4xx/5xx). Business outcomes are NOT exceptions; they are decisions. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
}
