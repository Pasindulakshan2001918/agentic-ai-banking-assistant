package com.agentic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Critical: Enables @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // Disable CSRF for stateless API (JWT-based)
            .csrf(csrf -> csrf.disable())
            
            // Session management: Stateless (no sessions, only JWT)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (no authentication required)
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                
                // Auth profile endpoint (any authenticated user)
                .requestMatchers("/auth/profile").authenticated()
                
                // Role-based endpoint access (URL-level security)
                .requestMatchers("/api/superadmin/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/approver/**").hasRole("APPROVER")
                .requestMatchers("/api/creator/**").hasRole("CREATOR")
                .requestMatchers("/api/viewer/**").hasRole("VIEWER")
                
                // Common endpoints accessible by any authenticated user
                .requestMatchers("/api/common/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // OAuth2 Resource Server configuration (JWT validation)
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(JwtAuthConverter.jwtAuthenticationConverter())
                )
            );

        return http.build();
    }
}