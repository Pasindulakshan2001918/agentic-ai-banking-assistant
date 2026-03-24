package com.agentic.controller;

import com.agentic.dto.ApiErrorResponse;
import com.agentic.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * GLOBAL EXCEPTION HANDLER
 * Centralizes exception handling across all controllers
 * Ensures consistent error response format
 * 
 * Maps custom exceptions → HTTP status codes → API Error Response
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle UnauthorizedException
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ApiErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex,
            HttpServletRequest request) {
        
        logger.warn("Unauthorized access attempt: {}", ex.getMessage());
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            HttpStatus.FORBIDDEN.value(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Handle InsufficientFundsException
     */
    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiErrorResponse> handleInsufficientFundsException(
            InsufficientFundsException ex,
            HttpServletRequest request) {
        
        logger.warn("Insufficient funds: {}", ex.getMessage());
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI()
        );
        
        errorResponse.addDetail("available", String.valueOf(ex.getAvailable()));
        errorResponse.addDetail("required", String.valueOf(ex.getRequired()));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle TransactionLimitExceededException
     */
    @ExceptionHandler(TransactionLimitExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiErrorResponse> handleTransactionLimitExceededException(
            TransactionLimitExceededException ex,
            HttpServletRequest request) {
        
        logger.warn("Transaction limit exceeded: {}", ex.getMessage());
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI()
        );
        
        errorResponse.addDetail("limitType", ex.getLimitType());
        errorResponse.addDetail("limit", String.valueOf(ex.getLimit()));
        errorResponse.addDetail("attempted", String.valueOf(ex.getAttempted()));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle EntityNotFoundException
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex,
            HttpServletRequest request) {
        
        logger.warn("Entity not found: {}", ex.getMessage());
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value(),
            request.getRequestURI()
        );
        
        if (ex.getEntityType() != null) {
            errorResponse.addDetail("entityType", ex.getEntityType());
        }
        if (ex.getEntityId() != null) {
            errorResponse.addDetail("entityId", String.valueOf(ex.getEntityId()));
        }
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handle InvalidTransactionException
     */
    @ExceptionHandler(InvalidTransactionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiErrorResponse> handleInvalidTransactionException(
            InvalidTransactionException ex,
            HttpServletRequest request) {
        
        logger.warn("Invalid transaction: {}", ex.getMessage());
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle ConcurrencyException
     */
    @ExceptionHandler(ConcurrencyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ApiErrorResponse> handleConcurrencyException(
            ConcurrencyException ex,
            HttpServletRequest request) {
        
        logger.warn("Concurrency error: {}", ex.getMessage());
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            HttpStatus.CONFLICT.value(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * Handle ValidationException
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            ValidationException ex,
            HttpServletRequest request) {
        
        logger.warn("Validation error: {}", ex.getMessage());
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI()
        );
        
        if (ex.getFieldErrors() != null && !ex.getFieldErrors().isEmpty()) {
            errorResponse.setFieldErrors(ex.getFieldErrors());
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle Spring Security AccessDeniedException
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        
        logger.warn("Access denied: {}", ex.getMessage());
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            "ACCESS_DENIED",
            "Access denied: " + ex.getMessage(),
            HttpStatus.FORBIDDEN.value(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        logger.warn("Method argument validation failed");
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            "VALIDATION_ERROR",
            "Input validation failed",
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI()
        );
        
        errorResponse.setFieldErrors(fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle generic RuntimeException
     * Logs full stack trace for debugging but returns generic message to client
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        logger.error("Unhandled exception occurred", ex);
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An internal server error occurred. Please contact support.",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getRequestURI()
        );
        
        // Only expose exception message in development mode
        String env = System.getProperty("app.environment", "production");
        if ("development".equals(env)) {
            errorResponse.addDetail("debug_message", ex.getMessage());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
