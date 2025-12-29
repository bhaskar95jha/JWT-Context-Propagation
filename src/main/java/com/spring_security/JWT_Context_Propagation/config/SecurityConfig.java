package com.spring_security.JWT_Context_Propagation.config;

import com.spring_security.JWT_Context_Propagation.converter.KeycloakJwtAuthenticationConverter;
import com.spring_security.JWT_Context_Propagation.filter.TenantContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for JWT-based authentication with Keycloak.
 * 
 * Configures:
 * - OAuth2 Resource Server with JWT validation
 * - Stateless session management (no server-side sessions)
 * - Custom JWT converter for Keycloak roles
 * - TenantContextFilter for tenant_id extraction
 * - Authorization rules for endpoints
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enables @PreAuthorize, @PostAuthorize, @RolesAllowed
public class SecurityConfig {

    private final KeycloakJwtAuthenticationConverter jwtAuthenticationConverter;
    private final TenantContextFilter tenantContextFilter;

    public SecurityConfig(KeycloakJwtAuthenticationConverter jwtAuthenticationConverter,
                          TenantContextFilter tenantContextFilter) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.tenantContextFilter = tenantContextFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF - not needed for stateless REST APIs
            .csrf(csrf -> csrf.disable())

            // Stateless session - no server-side session storage
            // Each request must include JWT token
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/error").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )

            // Configure as OAuth2 Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    // Use custom converter to extract Keycloak roles
                    .jwtAuthenticationConverter(jwtAuthenticationConverter)
                )
            )

            // Add TenantContextFilter AFTER JWT authentication
            // This ensures the JWT is validated before we extract tenant_id
            .addFilterAfter(tenantContextFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}

