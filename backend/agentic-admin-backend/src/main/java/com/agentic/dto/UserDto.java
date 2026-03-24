package com.agentic.dto;

import com.agentic.entity.User;

/**
 * USER DTO
 * Safe user response DTO - exposes only necessary fields
 * Hides sensitive data (password, internal identifiers, etc.)
 * 
 * Usage: Map User entity → UserDto for API responses
 * Never expose User entity directly to prevent data leaks
 */
public record UserDto(
        Long id,
        String username,
        String email,
        String fullName,
        String role,
        String status,
        Long createdAt
) {
    
    /**
     * Factory method: Convert User entity → UserDto
     * Ensures sensitive fields are never exposed in API responses
     * 
     * @param user The User entity to map
     * @return UserDto with safe fields only
     */
    public static UserDto fromEntity(User user) {
        if (user == null) {
            return null;
        }
        
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.getRole() != null ? user.getRole().name() : "USER",
            user.getStatus() != null ? user.getStatus().name() : "ACTIVE",
            user.getCreatedAt() != null ? user.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) : null
        );
    }
    
    /**
     * Check if user is active
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
    
    /**
     * Check if user has admin role
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }
}
