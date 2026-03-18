package com.agentic.service;

import com.agentic.entity.User;
import com.agentic.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final BCryptPasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }
    
    /* 🔒 Password is automatically hashed with BCrypt
     */
    @Transactional
    public User createUser(String username, String email, String password, String fullName,
                          User.UserRole role, String createdBy) {
        
        // Validate uniqueness
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists: " + email);
        }
        
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));  // 🔒 HASH WITH BCRYPT
        user.setEmail(email);
        user.setPassword(password); // TODO: Hash password with BCrypt
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
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Get user by email
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * Get user by ID
     */
    public Optional<User> getUser(Long userId) {
        return userRepository.findById(userId);
    }
    
    /**
     * Get all users (paginated)
     */
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    /**
     * Update user role (ADMIN only)
     */
    @Transactional
    public User updateUserRole(Long userId, User.UserRole newRole, String updatedBy) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
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
     * Update user status
     */
    @Transactional
    public User updateUserStatus(Long userId, User.UserStatus newStatus, String updatedBy) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
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
     */
    @Transactional
    public void deleteUser(Long userId, String deletedBy) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        String oldValue = toJsonString(user);
        
        // Log audit before deletion
        auditService.logAction("User", userId, "DELETE", deletedBy,
            oldValue, null, "User deleted: " + user.getUsername());
        
        userRepository.delete(user);
    }
    
    /**
     * Count total users
     */
    public long getUserCount() {
        return userRepository.count();
    }
    
    // ====== PRIVATE HELPER METHODS ======
    
    private String toJsonString(User user) {
        return String.format("{\"id\":%d,\"username\":\"%s\",\"email\":\"%s\",\"role\":\"%s\",\"status\":\"%s\"}",
            user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getStatus());
    }
}
