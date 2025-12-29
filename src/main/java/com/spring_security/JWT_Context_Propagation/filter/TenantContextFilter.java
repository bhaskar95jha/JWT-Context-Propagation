package com.spring_security.JWT_Context_Propagation.filter;

import com.spring_security.JWT_Context_Propagation.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that extracts tenant_id from JWT and populates TenantContext.
 * 
 * This filter runs AFTER JWT authentication, extracts the tenant_id claim,
 * and stores it in TenantContext for use throughout the request.
 * 
 * CRITICAL: The finally block ensures TenantContext is cleared after every
 * request to prevent memory leaks.
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TenantContextFilter.class);

    private static final String TENANT_ID_CLAIM = "tenant_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Get the authenticated user from Security Context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Check if it's a JWT authentication (user is authenticated with valid JWT)
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();

                // Extract tenant_id claim from JWT
                String tenantId = jwt.getClaimAsString(TENANT_ID_CLAIM);

                if (tenantId != null && !tenantId.isEmpty()) {
                    TenantContext.setCurrentTenant(tenantId);
                    logger.debug("TenantContext set to: {} for user: {}", 
                            tenantId, jwtAuth.getName());
                } else {
                    logger.warn("No tenant_id claim found in JWT for user: {}", 
                            jwtAuth.getName());
                }
            }

            // Continue with the filter chain (process the request)
            filterChain.doFilter(request, response);

        } finally {
            // CRITICAL: Always clear the context to prevent memory leaks
            // This runs even if an exception occurs
            TenantContext.clear();
            logger.debug("TenantContext cleared after request");
        }
    }
}

