package com.spring_security.JWT_Context_Propagation.context;

/**
 * TenantContext - Thread-safe storage for tenant_id during request lifecycle.
 * 
 * Uses ThreadLocal to store the current tenant ID, ensuring each request
 * has its own isolated tenant context.
 * 
 * IMPORTANT: Always call clear() after the request completes to prevent memory leaks.
 */
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * Set the current tenant ID for this thread/request.
     * 
     * @param tenantId The tenant identifier extracted from JWT
     */
    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Get the current tenant ID.
     * 
     * @return The tenant ID for the current request, or null if not set
     */
    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    /**
     * Clear the tenant context.
     * 
     * MUST be called after request completes to prevent memory leaks.
     * This is typically done in a filter's finally block.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}

