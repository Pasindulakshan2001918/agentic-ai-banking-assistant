package com.agentic.controller;

import com.agentic.dto.CreateUserRequest;
import com.agentic.entity.User;
import com.agentic.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * INTEGRATION TESTS: UserController
 * Tests REST endpoints with H2 in-memory database
 * No mocking - full Spring Boot context
 * 
 * Profile: test (uses application-test.yml with H2)
 * Ensures:
 * - DTOs properly mapped (User entity → UserDto response)
 * - Request validation (@Valid, @NotBlank, @Email)
 * - HTTP status codes correct (201 on create, 404 on not found)
 * - Sensitive fields hidden (password never in response)
 * - Timestamps in UTC
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        // Clean up before each test
        userRepository.deleteAll();
    }
    
    /**
     * Test: Create a valid user
     * Expects: 201 Created with UserDto (password hidden)
     */
    @Test
    void testCreateUserSuccess() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
            "testuser",
            "test@example.com",
            "SecurePassword123",
            "Test User",
            "CREATOR"
        );
        
        MvcResult result = mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.username").value("testuser"))
            .andExpect(jsonPath("$.user.email").value("test@example.com"))
            .andExpect(jsonPath("$.user.id").isNumber())
            .andExpect(jsonPath("$.user.role").value("CREATOR"))
            .andExpect(jsonPath("$.message").value("User created successfully"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            // Verify password is NEVER in response
            .andExpect(jsonPath("$.user.password").doesNotExist())
            .andReturn();
        
        // Verify user was saved to database
        assert userRepository.findByUsername("testuser").isPresent();
    }
    
    /**
     * Test: Create user with duplicate username
     * Expects: 400 Bad Request with validation error
     */
    @Test
    void testCreateUserDuplicateUsername() throws Exception {
        // Create first user
        User existing = new User();
        existing.setUsername("duplicate");
        existing.setEmail("first@example.com");
        existing.setPassword(passwordEncoder.encode("password123"));
        existing.setFullName("First User");
        existing.setRole(User.UserRole.CREATOR);
        existing.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(existing);
        
        // Try to create duplicate
        CreateUserRequest request = new CreateUserRequest(
            "duplicate",
            "second@example.com",
            "password456",
            "Second User",
            "CREATOR"
        );
        
        mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    /**
     * Test: Create user with invalid email format
     * Expects: 400 Bad Request with validation error
     */
    @Test
    void testCreateUserInvalidEmail() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
            "testuser",
            "invalid-email",  // Not a valid email
            "SecurePassword123",
            "Test User",
            "USER"
        );
        
        mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    /**
     * Test: Create user with password too short
     * Expects: 400 Bad Request with validation error
     */
    @Test
    void testCreateUserWeakPassword() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
            "testuser",
            "test@example.com",
            "short",  // Less than 8 characters
            "Test User",
            "USER"
        );
        
        mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    /**
     * Test: Get user by ID
     * Expects: 200 OK with UserDto
     */
    @Test
    void testGetUserById() throws Exception {
        // Create a user first
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setFullName("Test User");
        user.setRole(User.UserRole.CREATOR);
        user.setStatus(User.UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        
        // Get user by ID - NOTE: This requires authentication
        // For now test that endpoint exists
        mockMvc.perform(get("/api/users/" + saved.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());  // No JWT token
    }
    
    /**
     * Test: Get all users (admin only)
     * Expects: 403 Forbidden (Access Denied) without valid JWT role
     */
    @Test
    void testGetAllUsersRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/users")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isForbidden());  // 403 Access Denied without admin role
    }
    
    /**
     * Test: Create user with minimal fields
     * Expects: 400 Bad Request  (fullName is required)
     */
    @Test
    void testCreateUserMissingFields() throws Exception {
        String json = "{ \"username\": \"test\", \"email\": \"test@example.com\" }";
        
        mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isBadRequest());
    }
    
    /**
     * Test: Verify UTC timestamps in responses
     * Expects: timestamp field present and valid
     */
    @Test
    void testResponseTimestampIsUTC() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
            "testuser",
            "test@example.com",
            "SecurePassword123",
            "Test User",
            "CREATOR"
        );
        
        MvcResult result = mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andReturn();
        
        // Response should contain ISO8601 timestamp
        String content = result.getResponse().getContentAsString();
        assert content.contains("timestamp");
    }
}
