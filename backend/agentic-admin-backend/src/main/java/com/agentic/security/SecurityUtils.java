package com.agentic.security;

import com.agentic.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * SECURITY UTILITY
 * 
 * Extracts authenticated user identity from JWT token (Keycloak "sub" claim).
 * Uses UUID string directly for secure, stable identity across all systems.
 * 
 * ✅ Why UUID strings (not numeric IDs):
 * - No collision risk
 * - Stable across systems
 * - Cryptographically unique
 * - No hashing/truncation that loses precision
 * 
 * 🔐 CRITICAL: All user identity operations must use this utility
 */
@Component
public class SecurityUtils {

    /**
     * Extract user UUID from JWT token.
     * 
     * @param auth Spring Security Authentication object with JWT principal
     * @return User ID (UUID string from "sub" claim)
     * @throws UnauthorizedException if authentication is missing or invalid
     */
    public String getUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        try {
            Jwt jwt = (Jwt) auth.getPrincipal();
            String userId = jwt.getClaimAsString("sub");
            
            if (userId == null || userId.isBlank()) {
                throw new UnauthorizedException("Missing 'sub' claim in JWT token");
            }
            
            return userId;
        } catch (ClassCastException e) {
            throw new UnauthorizedException("Invalid token format: expected JWT");
        }
    }

    /**
     * Validate that a given user ID matches the authenticated user.
     * Used for authorization checks (user can only access their own data).
     * 
     * @param auth Spring Security Authentication object
     * @param targetUserId The user ID being accessed
     * @throws UnauthorizedException if mismatch
     */
    public void validateUserIdMatch(Authentication auth, String targetUserId) {
        String authenticatedUserId = getUserId(auth);
        
        if (!authenticatedUserId.equals(targetUserId)) {
            throw new UnauthorizedException(
                "Access denied: you cannot access another user's data"
            );
        }
    }
}
