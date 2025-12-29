package com.spring_security.JWT_Context_Propagation.controller;

import com.spring_security.JWT_Context_Propagation.context.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller to verify JWT authentication and TenantContext functionality.
 * 
 * Provides endpoints to test:
 * - Public access (no auth required)
 * - Authenticated access (valid JWT required)
 * - Role-based access (ROLE_USER, ROLE_ADMIN)
 * - TenantContext functionality
 */
@RestController
@RequestMapping("/api")
public class TestController {

    /**
     * Public endpoint - no authentication required.
     * Used to verify the app is running.
     */
    @GetMapping("/public/health")
    public Map<String, String> publicHealth() {
        return Map.of(
            "status", "UP",
            "message", "Public endpoint - no authentication required",
            "timestamp", java.time.Instant.now().toString()
        );
    }

    /**
     * Protected endpoint - requires valid JWT token.
     * Tests TenantContext.getCurrentTenant() functionality.
     */
    @GetMapping("/test/tenant")
    public Map<String, Object> getTenantInfo(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();

        // From TenantContext (ThreadLocal) - this is what your business logic will use
        response.put("tenantFromContext", TenantContext.getCurrentTenant());

        // Directly from JWT for comparison/verification
        response.put("tenantFromJwt", jwt.getClaimAsString("tenant_id"));

        // User information
        response.put("username", jwt.getClaimAsString("preferred_username"));
        response.put("email", jwt.getClaimAsString("email"));
        response.put("subject", jwt.getSubject());

        // Token info
        response.put("tokenIssuer", jwt.getIssuer() != null ? jwt.getIssuer().toString() : "N/A");
        response.put("tokenExpiry", jwt.getExpiresAt() != null ? jwt.getExpiresAt().toString() : "N/A");

        return response;
    }

    /**
     * User endpoint - requires ROLE_USER.
     * Tests role-based access control.
     */
    @GetMapping("/user/profile")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public Map<String, Object> userProfile(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        
        response.put("message", "Welcome to your profile!");
        response.put("tenant", TenantContext.getCurrentTenant());
        response.put("username", jwt.getClaimAsString("preferred_username"));
        response.put("email", jwt.getClaimAsString("email"));
        response.put("name", jwt.getClaimAsString("name"));
        response.put("accessLevel", "USER");

        return response;
    }

    /**
     * Admin endpoint - requires ROLE_ADMIN.
     * Tests admin-only access control.
     */
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Map<String, Object> adminDashboard(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();

        response.put("message", "Welcome to Admin Dashboard!");
        response.put("tenant", TenantContext.getCurrentTenant());
        response.put("username", jwt.getClaimAsString("preferred_username"));
        response.put("accessLevel", "ADMIN");
        response.put("adminNote", "You have full administrative access for tenant: " 
                + TenantContext.getCurrentTenant());

        return response;
    }

    /**
     * Endpoint to demonstrate business logic accessing tenant context.
     * Shows how any service can get the current tenant without passing it as parameter.
     */
    @GetMapping("/test/business-logic")
    public Map<String, Object> businessLogicExample() {
        Map<String, Object> response = new HashMap<>();

        // In real code, this would be in a Service class
        String currentTenant = TenantContext.getCurrentTenant();
        
        response.put("message", "Business logic executed successfully");
        response.put("tenant", currentTenant);
        response.put("databaseSchema", "tenant_" + currentTenant + "_schema");
        response.put("note", "TenantContext.getCurrentTenant() can be called from anywhere in the code");

        return response;
    }
}

