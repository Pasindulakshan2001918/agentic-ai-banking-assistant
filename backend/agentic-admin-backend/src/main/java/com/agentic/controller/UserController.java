package com.agentic.controller;

import com.agentic.dto.CreateUserRequest;
import com.agentic.dto.UserDto;
import com.agentic.entity.User;
import com.agentic.exception.EntityNotFoundException;
import com.agentic.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * USER CONTROLLER
 * Handles user management endpoints
 * Properly maps User entities → UserDto responses
 * DTO validation on request bodies
 * 
 * Endpoints:
 * POST   /api/users - Create user
 * GET    /api/users/{id} - Get user by ID
 * GET    /api/users - List all users (admin only)
 * PUT    /api/users/{id} - Update user (admin only)
 * DELETE /api/users/{id} - Delete user (admin only)
 * GET    /api/users/me - Get current user
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Create a new user (public endpoint for self-registration)
     * 
     * @param request CreateUserRequest with validation
     * @return 201 Created with UserDto response
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        
        // Convert role string to UserRole enum
        User.UserRole role = User.UserRole.valueOf(request.getRole().toUpperCase());
        
        // Create user via service (password hashed, role validated)
        User user = userService.createUser(
            request.getUsername(),
            request.getEmail(),
            request.getPassword(),
            request.getFullName(),
            role,
            "SELF"  // System user indicator for audit
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("user", UserDto.fromEntity(user));
        response.put("message", "User created successfully");
        response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get user by ID (authenticated users can see their own, admin can see all)
     * 
     * @param id User ID
     * @param auth Authentication object
     * @return 200 OK with UserDto
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(
            @PathVariable Long id,
            Authentication auth) {
        
        User user = userService.getUser(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id, "USER_NOT_FOUND", id));
        
        Map<String, Object> response = new HashMap<>();
        response.put("user", UserDto.fromEntity(user));
        response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all users (admin only)
     * 
     * @param pageable Pagination parameters
     * @param auth Authentication object
     * @return 200 OK with paginated UserDto list
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        
        Page<User> users = userService.getAllUsers(Pageable.ofSize(size).withPage(page));
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", users.map(UserDto::fromEntity).getContent());
        response.put("totalElements", users.getTotalElements());
        response.put("totalPages", users.getTotalPages());
        response.put("currentPage", page);
        response.put("pageSize", size);
        response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update user (admin only) - Updates email, full name, and role
     * 
     * @param id User ID to update
     * @param request Updated user data
     * @param auth Authentication object
     * @return 200 OK with updated UserDto
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody CreateUserRequest request,
            Authentication auth) {
        
        // Update user role
        User.UserRole role = User.UserRole.valueOf(request.getRole().toUpperCase());
        User user = userService.updateUserRole(id, role, auth.getName());
        
        Map<String, Object> response = new HashMap<>();
        response.put("user", UserDto.fromEntity(user));
        response.put("message", "User updated successfully");
        response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete user (admin only)
     * 
     * @param id User ID to delete
     * @param auth Authentication object
     * @return 200 OK with success message
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @PathVariable Long id,
            Authentication auth) {
        
        userService.deleteUser(id, auth.getName());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User deleted successfully");
        response.put("userId", id);
        response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get current authenticated user
     * 
     * @param auth Authentication object (JWT from Keycloak)
     * @return 200 OK with current UserDto
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication auth) {
        
        // Extract username from JWT and fetch user details
        String username = auth.getName();
        User user = userService.getUserByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + username, "USER_NOT_FOUND", 0L));
        
        Map<String, Object> response = new HashMap<>();
        response.put("user", UserDto.fromEntity(user));
        response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
        
        return ResponseEntity.ok(response);
    }
}
