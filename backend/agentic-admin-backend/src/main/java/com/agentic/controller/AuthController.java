package com.agentic.controller;

import com.agentic.dto.AuthProfileResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @GetMapping("/profile")
    public AuthProfileResponse getProfile(Authentication authentication) {

        JwtAuthenticationToken jwtAuth =
                (JwtAuthenticationToken) authentication;

        Jwt jwt = jwtAuth.getToken();

        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");

        List<String> roles = jwtAuth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.replace("ROLE_", ""))
                .collect(Collectors.toList());

        return new AuthProfileResponse(username, email, roles);
    }
}
