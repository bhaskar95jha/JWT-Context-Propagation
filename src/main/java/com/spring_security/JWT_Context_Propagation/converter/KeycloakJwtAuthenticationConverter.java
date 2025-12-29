package com.spring_security.JWT_Context_Propagation.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts Keycloak JWT tokens to Spring Security Authentication tokens.
 * 
 * Keycloak stores roles in 'realm_access.roles', but Spring Security
 * expects them as GrantedAuthority objects. This converter bridges the gap.
 */
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        String principalName = extractPrincipalName(jwt);
        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    /**
     * Extracts roles from Keycloak's realm_access.roles claim and converts
     * them to Spring Security GrantedAuthority objects.
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Keycloak stores realm roles in: realm_access.roles
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");

        return roles.stream()
                .map(SimpleGrantedAuthority::new)  // Role already has ROLE_ prefix from Keycloak
                .collect(Collectors.toList());
    }

    /**
     * Extracts the principal name from the JWT.
     * Tries preferred_username first, falls back to subject.
     */
    private String extractPrincipalName(Jwt jwt) {
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isEmpty()) {
            return preferredUsername;
        }
        return jwt.getSubject();
    }
}

