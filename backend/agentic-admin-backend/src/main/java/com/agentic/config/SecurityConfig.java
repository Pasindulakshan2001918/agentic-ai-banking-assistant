package com.agentic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SECURITY CONFIGURATION
 *
 * Two completely separate security filter chains:
 *
 * CHAIN 1 — Customer routes (/api/auth/**, /api/customer/**, /api/ai/**)
 *   - Validated by CustomerJwtFilter using our own HS256 JWT
 *   - Login: POST /api/auth/login with username + password
 *   - Token signed by JwtTokenProvider (not Keycloak)
 *   - Customers are stored in the PostgreSQL users table
 *
 * CHAIN 2 — Admin routes (/api/admin/**, /api/banking/**, /api/users/**)
 *   - Validated by Spring OAuth2 Resource Server (Keycloak)
 *   - Login: Keycloak login page (React admin portal redirect)
 *   - Token issued by Keycloak realm: agentic-bank
 *   - Admins exist ONLY in Keycloak — not in the local users table
 *   - Roles: SUPER_ADMIN (full access), ADMIN (limited access)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomerJwtFilter customerJwtFilter;

    public SecurityConfig(CustomerJwtFilter customerJwtFilter) {
        this.customerJwtFilter = customerJwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CHAIN 1 — Customer security (Flutter mobile app)
     * Order 1 = evaluated first (higher priority)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain customerSecurityChain(HttpSecurity http)
            throws Exception {
        http
            .securityMatcher("/api/auth/**", "/api/customer/**", "/api/ai/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints — no token needed
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/api/ai/health").permitAll()
                // Everything else in these paths needs a valid customer token
                .anyRequest().authenticated()
            )
            // CustomerJwtFilter validates our own HS256 tokens
            .addFilterBefore(customerJwtFilter,
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CHAIN 2 — Admin security (React admin portal)
     * Order 2 = evaluated second for everything not matched by chain 1
     */
    @Bean
    @Order(2)
    public SecurityFilterChain adminSecurityChain(HttpSecurity http)
            throws Exception {
        http
            .securityMatcher(
                "/api/admin/**",
                "/api/banking/**",
                "/api/users/**",
                "/api/superadmin/**",
                "/auth/**",
                "/actuator/**",
                "/api/public/**"
            )
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/superadmin/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/banking/**").hasAnyRole("ADMIN", "SUPER_ADMIN",
                                                               "APPROVER", "CREATOR", "VIEWER")
                .anyRequest().authenticated()
            )
            // Keycloak JWT validation via Spring OAuth2 Resource Server
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(
                        JwtAuthConverter.jwtAuthenticationConverter())
                )
            );

        return http.build();
    }
}