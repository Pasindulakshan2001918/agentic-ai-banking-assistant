package com.agentic.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.*;
import java.util.stream.Collectors;

public class JwtAuthConverter {

    public static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }

    static class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null || realmAccess.isEmpty()) {
                return Collections.emptyList();
            }

            Object rolesObj = realmAccess.get("roles");
            if (!(rolesObj instanceof List<?>)) {
                return Collections.emptyList();
            }

            // Safely cast each element to String
            List<String> roles = ((List<?>) rolesObj).stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());

            List<GrantedAuthority> authorities = roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // Optional: print for debugging
            System.out.println("Granted authorities: " + authorities);

            return authorities;
        }
    }
}
