package com.agentic.config;

import com.agentic.entity.User;
import com.agentic.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * CUSTOMER JWT FILTER
 *
 * Intercepts every request to /api/customer/**, /api/ai/**, /api/auth/**
 * and validates the Bearer token using JwtTokenProvider (our own HS256 key).
 *
 * This filter is ONLY for customer routes.
 * Admin routes (/api/admin/**, /api/banking/**, /api/users/**)
 * are validated separately by Spring OAuth2 Resource Server (Keycloak).
 *
 * If the token is valid and the user is ACTIVE, it populates the
 * Spring SecurityContext with the User entity as the principal.
 * This means downstream code can do:
 *   (User) auth.getPrincipal()
 * to get the full user object without a second database query.
 */
@Component
public class CustomerJwtFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public CustomerJwtFilter(JwtTokenProvider jwtTokenProvider,
                              UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);

                userRepository.findById(userId).ifPresent(user -> {
                    // Only authenticate if account is ACTIVE
                    if (user.getStatus() == User.UserStatus.ACTIVE) {
                        String roleName = user.getRole() != null
                                ? user.getRole().name()
                                : "CUSTOMER";

                        List<SimpleGrantedAuthority> authorities = List.of(
                                new SimpleGrantedAuthority("ROLE_" + roleName)
                        );

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        user,        // principal = User entity
                                        null,        // credentials = null (token already validated)
                                        authorities
                                );
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                });
            } catch (Exception e) {
                // Token is malformed or user not found — clear context and continue
                // The request will fail authorization downstream if endpoint is protected
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Skip this filter entirely for admin routes.
     * Admin routes are handled by the Keycloak OAuth2 chain in SecurityConfig.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Run filter ONLY for customer, AI, and auth paths
        return !path.startsWith("/api/customer") &&
               !path.startsWith("/api/ai")       &&
               !path.startsWith("/api/auth");
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
