package com.agentic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard API Error Response
 * All exceptions are mapped to this format for consistent client handling
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    
    private String errorCode;
    private String message;
    private String path;
    private int statusCode;
    private LocalDateTime timestamp;
    private Map<String, String> details;
    private Map<String, String> fieldErrors;
    
    public ApiErrorResponse() {
        this.timestamp = LocalDateTime.now();
        this.details = new HashMap<>();
    }
    
    public ApiErrorResponse(String errorCode, String message, int statusCode) {
        this();
        this.errorCode = errorCode;
        this.message = message;
        this.statusCode = statusCode;
    }
    
    public ApiErrorResponse(String errorCode, String message, int statusCode, String path) {
        this();
        this.errorCode = errorCode;
        this.message = message;
        this.statusCode = statusCode;
        this.path = path;
    }
    
    // Getters and Setters
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, String> getDetails() {
        return details;
    }
    
    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
    
    public void addDetail(String key, String value) {
        if (this.details == null) {
            this.details = new HashMap<>();
        }
        this.details.put(key, value);
    }
    
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
    
    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
}
