package com.lunorlabs.bank.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Unchecked wrappers so services stay readable. */
public final class Json {
    private Json() {}

    public static String write(ObjectMapper om, Object value) {
        try {
            return om.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    public static JsonNode read(ObjectMapper om, String json) {
        try {
            return om.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON parse failed", e);
        }
    }
}
