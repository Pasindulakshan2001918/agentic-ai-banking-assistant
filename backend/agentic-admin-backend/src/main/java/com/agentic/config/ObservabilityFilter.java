package com.agentic.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

/**
 * OBSERVABILITY FILTER
 * 
 * PHASE 6.4 — Observability
 * 
 * Handles:
 * 1. Correlation ID generation and tracking
 * 2. Request logging (method, path, params)
 * 3. Response logging (status, duration)
 * 4. Error tracking and logging
 * 5. MDC setup for structured logging
 */
@Component
public class ObservabilityFilter implements Filter {
    
    private static final Logger log = LoggerFactory.getLogger(ObservabilityFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC = "correlationId";
    private static final String REQUEST_ID_MDC = "requestId";
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("🔗 ObservabilityFilter initialized");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Wrap request/response to capture bodies
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);
        
        // Generate or retrieve Correlation ID
        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        String requestId = UUID.randomUUID().toString();
        
        // Setup MDC (Mapped Diagnostic Context) for structured logging
        MDC.put(CORRELATION_ID_MDC, correlationId);
        MDC.put(REQUEST_ID_MDC, requestId);
        
        // Add correlation ID to response headers
        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Log request
            logRequest(httpRequest, correlationId, requestId);
            
            // Process request
            chain.doFilter(wrappedRequest, wrappedResponse);
            
            // Log response
            long duration = System.currentTimeMillis() - startTime;
            logResponse(httpRequest, wrappedResponse, duration, correlationId, requestId);
            
            // Write response back to client
            wrappedResponse.copyBodyToResponse();
            
        } catch (Exception e) {
            // Log error
            long duration = System.currentTimeMillis() - startTime;
            logError(httpRequest, e, duration, correlationId, requestId);
            
            // Re-throw exception so Spring can handle it
            throw e;
            
        } finally {
            // Clear MDC
            MDC.remove(CORRELATION_ID_MDC);
            MDC.remove(REQUEST_ID_MDC);
        }
    }
    
    /**
     * Log incoming request
     */
    private void logRequest(HttpServletRequest request, String correlationId, String requestId) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        String contentType = request.getContentType();
        
        log.info("📥 REQUEST STARTED | Method: {} | Path: {} | CorrelationId: {} | RequestId: {}",
            method, path, correlationId, requestId);
        
        if (queryString != null && !queryString.isEmpty()) {
            log.debug("  Query: {}", queryString);
        }
        
        if (contentType != null && contentType.contains("application/json")) {
    try {
        // Use ContentCachingRequestWrapper — does NOT consume the stream
        // The wrapper buffers the body after chain.doFilter reads it
        // For pre-read logging, we skip body logging here to avoid consuming the stream
        log.debug("  Content-Type: {} (body logged after processing)", contentType);
    } catch (Exception e) {
        // Silent fail
    }
}
    }
    
    /**
     * Log response
     */
    private void logResponse(HttpServletRequest request, ContentCachingResponseWrapper response, 
                            long duration, String correlationId, String requestId) {
        int status = response.getStatus();
        String method = request.getMethod();
        String path = request.getRequestURI();
        
        String statusEmoji = status >= 200 && status < 300 ? "✅" : 
                            status >= 300 && status < 400 ? "🔄" : 
                            status >= 400 && status < 500 ? "⚠️" : "❌";
        
        log.info("📤 REQUEST COMPLETED {} | Status: {} | Duration: {}ms | Method: {} | Path: {} | CorrelationId: {}",
            statusEmoji, status, duration, method, path, correlationId);
        
        // Log response body if JSON content-type
        String contentType = response.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            try {
                byte[] bodyBytes = response.getContentAsByteArray();
                if (bodyBytes.length > 0) {
                    String body = new String(bodyBytes);
                    log.debug("  Response body (truncated): {}", body.substring(0, Math.min(500, body.length())));
                }
            } catch (Exception e) {
                // Silent fail
            }
        }
    }
    
    /**
     * Log errors
     */
    private void logError(HttpServletRequest request, Exception exception, long duration,
                         String correlationId, String requestId) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        String exceptionType = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        
        log.error("❌ REQUEST FAILED | Method: {} | Path: {} | Duration: {}ms | " +
                 "Exception: {} | Message: {} | CorrelationId: {} | RequestId: {}",
            method, path, duration, exceptionType, message, correlationId, requestId, exception);
    }
    
    @Override
    public void destroy() {
        log.info("🔗 ObservabilityFilter destroyed");
    }
}
