package com.agentic.service;

import com.agentic.entity.User;
import com.agentic.exception.ValidationException;
import com.agentic.exception.EntityNotFoundException;
import com.agentic.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * USER SERVICE
 * Handles user lifecycle: creation, updates, deletion
 * 
 * 🔒 SECURITY:
 * - All methods have @PreAuthorize role checks
 * - Password ALWAYS hashed via injected PasswordEncoder
 * - NEVER instantiate BCryptPasswordEncoder manually
 * - Role-based access control on sensitive operations
 */
@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, AuditService auditService, 
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * 🔒 Create a new user with hashed password
     * Uses injected PasswordEncoder bean from SecurityConfig
     * 
     * SECURITY: Public endpoint - public registration (handled at URL level by SecurityConfig)
     */
    @Transactional
    public User createUser(String username, String email, String password, String fullName,
                          User.UserRole role, String createdBy) {
        
        // Validate uniqueness
        if (userRepository.existsByUsername(username)) {
            throw new ValidationException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException("Email already exists: " + email);
        }
        
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password)); // 🔒 Hash the password
        user.setFullName(fullName);
        user.setRole(role);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setCreatedBy(createdBy);
        user.setUpdatedBy(createdBy);
        
        User saved = userRepository.save(user);
        
        // Log audit
        auditService.logAction("User", saved.getId(), "CREATE", createdBy,
            null, toJsonString(saved), 
            "User created with role: " + role);
        
        return saved;
    }
    
    /**
     * Get user by username
     * 
     * SECURITY: Requires authentication
     */
    @PreAuthorize("isAuthenticated()")
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Get user by email
     * 
     * SECURITY: Requires authentication
     */
    @PreAuthorize("isAuthenticated()")
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * Get user by ID
     * 
     * SECURITY: Requires authentication
     */
    @PreAuthorize("isAuthenticated()")
    public Optional<User> getUser(Long userId) {
        return userRepository.findById(userId);
    }
    
    /**
     * Get all users (paginated)
     * 
     * SECURITY: ADMIN only
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    /**
     * Update user role (SUPER_ADMIN only)
     * 
     * SECURITY: Only SUPER_ADMIN can change roles
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public User updateUserRole(Long userId, User.UserRole newRole, String updatedBy) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException(
                "User not found", "User", userId));
        
        String oldValue = toJsonString(user);
        
        user.setRole(newRole);
        user.setUpdatedBy(updatedBy);
        
        User updated = userRepository.save(user);
        
        // Log audit
        auditService.logAction("User", userId, "UPDATE_ROLE", updatedBy,
            oldValue, toJsonString(updated), 
            "User role changed to " + newRole);
        
        return updated;
    }
    
    /**
     * Update user status (SUPER_ADMIN only)
     * 
     * SECURITY: Only SUPER_ADMIN can disable/suspend users
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public User updateUserStatus(Long userId, User.UserStatus newStatus, String updatedBy) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException(
                "User not found", "User", userId));
        
        String oldValue = toJsonString(user);
        
        user.setStatus(newStatus);
        user.setUpdatedBy(updatedBy);
        
        User updated = userRepository.save(user);
        
        // Log audit
        auditService.logAction("User", userId, "UPDATE_STATUS", updatedBy,
            oldValue, toJsonString(updated), 
            "User status changed to " + newStatus);
        
        return updated;
    }
    
    /**
     * Delete user (SUPER_ADMIN only)
     * 
     * SECURITY: Only SUPER_ADMIN can delete users
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public void deleteUser(Long userId, String deletedBy) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException(
                "User not found", "User", userId));
        
        String oldValue = toJsonString(user);
        
        // Log audit before deletion
        auditService.logAction("User", userId, "DELETE", deletedBy,
            oldValue, null, "User deleted: " + user.getUsername());
        
        userRepository.delete(user);
    }
    
    /**
     * Count total users
     * 
     * SECURITY: Admin only
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public long getUserCount() {
        return userRepository.count();
    }
    
    // ====== PRIVATE HELPER METHODS ======
    
    private String toJsonString(User user) {
        return String.format("{\"id\":%d,\"username\":\"%s\",\"email\":\"%s\",\"role\":\"%s\",\"status\":\"%s\"}",
            user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getStatus());
    }
}
