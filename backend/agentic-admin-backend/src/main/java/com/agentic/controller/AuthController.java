package com.agentic.controller;

import com.agentic.config.JwtTokenProvider;
import com.agentic.dto.AuthProfileResponse;
import com.agentic.dto.CreateUserRequest;
import com.agentic.dto.LoginRequest;
import com.agentic.dto.UserDto;
import com.agentic.entity.User;
import com.agentic.exception.UnauthorizedException;
import com.agentic.exception.ValidationException;
import com.agentic.repository.UserRepository;
import com.agentic.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AUTH CONTROLLER
 *
 * Handles TWO completely separate authentication flows:
 *
 * CUSTOMER AUTH (Flutter mobile app — database-backed):
 *   POST /api/auth/register  — customer self-registration (public)
 *   POST /api/auth/login     — customer login, returns custom HS256 JWT (public)
 *
 * ADMIN AUTH (React admin portal — Keycloak-backed):
 *   GET  /auth/profile       — read profile from Keycloak JWT (protected)
 *
 * Customers are stored in PostgreSQL.
 * Admins exist only in Keycloak — they never touch this register/login flow.
 */
@RestController
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider,
                          AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditService = auditService;
    }

    // ═══════════════════════════════════════════════════════════
    // CUSTOMER AUTH — database-backed, custom JWT
    // ═══════════════════════════════════════════════════════════

    /**
     * Customer self-registration.
     * Any person can register as a CUSTOMER.
     * Password is BCrypt-hashed before storage — never plain text.
     * Role is always forced to CUSTOMER regardless of what is sent.
     */
    @PostMapping("/api/auth/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody CreateUserRequest request) {

        // Check uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already registered: " + request.getEmail());
        }

        // Create customer — role is always VIEWER on self-registration
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(User.UserRole.VIEWER);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setCreatedBy("SELF");
        user.setUpdatedBy("SELF");

        User saved = userRepository.save(user);

        // Log registration in audit trail
        auditService.logAction("User", saved.getId(), "REGISTER", "SELF",
                null, "Customer registered: " + saved.getUsername(),
                "Customer self-registration");

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Account created successfully. You can now log in.");
        response.put("userId", saved.getId());
        response.put("username", saved.getUsername());
        response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Customer login.
     * Validates username + password against the PostgreSQL users table.
     * Returns a custom HS256 JWT signed by JwtTokenProvider.
     * This JWT is used for all /api/customer/** and /api/ai/** endpoints.
     */
    @PostMapping("/api/auth/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request) {

        // Find user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException(
                        "Invalid username or password"));

        // Verify password against BCrypt hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Same error message as above — never reveal which field was wrong
            throw new UnauthorizedException("Invalid username or password");
        }

        // Check account is not suspended or locked
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new UnauthorizedException(
                    "Your account is " + user.getStatus().name().toLowerCase() +
                    ". Please contact your branch.");
        }

        // Generate our own JWT (not Keycloak)
        String token = jwtTokenProvider.generateToken(user);

        // Log successful login
        auditService.logAction("User", user.getId(), "LOGIN", user.getUsername(),
                null, "Customer login successful", "Customer authentication");

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("tokenType", "Bearer");
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("fullName", user.getFullName());
        response.put("role", user.getRole().name());
        response.put("expiresIn", 86400);
        response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════
    // ADMIN AUTH — Keycloak-backed, reads from Keycloak JWT
    // ═══════════════════════════════════════════════════════════

    /**
     * Admin profile endpoint.
     * Called by the React admin portal after Keycloak login.
     * Reads username, email, and roles directly from the Keycloak JWT.
     * No database lookup needed — all data is in the token.
     */
    @GetMapping("/auth/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthProfileResponse> getProfile(
            Authentication authentication) {

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuth.getToken();

        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");

        List<String> roles = jwtAuth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.replace("ROLE_", ""))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new AuthProfileResponse(username, email, roles));
    }
}
