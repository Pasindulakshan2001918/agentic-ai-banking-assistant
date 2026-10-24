package com.lunorlabs.bank.common;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Errors {
    private Errors() {}

    public static Map<String, Object> body(String code, String message, boolean retryable) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("code", code);
        err.put("message", message);
        err.put("retryable", retryable);
        err.put("details", Map.of());
        return Map.of("error", err);
    }
}
