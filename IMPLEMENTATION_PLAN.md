# 🎯 JWT Validation & Tenant Context Propagation - Implementation Plan

## 📋 Objective

Implement the security layer that intercepts incoming API requests, validates the JWT via Keycloak, extracts the tenant_id, and stores it in a thread-safe manner for the duration of the request.

---

## ✅ Acceptance Criteria

| Criteria | Description | Status |
|----------|-------------|--------|
| Auth Enforcement | Requests without a valid Bearer token return HTTP 401 | ⬜ |
| Context Available | `TenantContext.getCurrentTenant()` returns the correct ID from token claims | ⬜ |
| Context Cleanup | The ThreadLocal variable is empty after the response is sent | ⬜ |
| Role Validation | The system recognizes roles defined in the JWT | ⬜ |

---

## 🗺️ Complete System Architecture

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              COMPLETE SYSTEM ARCHITECTURE                              │
└────────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────┐      ┌─────────────────┐      ┌─────────────────────────────────────┐
│                 │      │                 │      │                                     │
│    POSTMAN      │      │    KEYCLOAK     │      │         YOUR SPRING BOOT APP        │
│    (Client)     │      │  (Auth Server)  │      │         (Resource Server)           │
│                 │      │   Port: 8080    │      │           Port: 8081                │
│                 │      │                 │      │                                     │
└────────┬────────┘      └────────┬────────┘      └──────────────────┬──────────────────┘
         │                        │                                  │
         │   STEP A: LOGIN        │                                  │
         │   (Get JWT Token)      │                                  │
         │                        │                                  │
         │  1. POST /token        │                                  │
         │     username/password  │                                  │
         │───────────────────────>│                                  │
         │                        │                                  │
         │  2. Returns JWT with:  │                                  │
         │     - tenant_id        │                                  │
         │     - roles            │                                  │
         │<───────────────────────│                                  │
         │                        │                                  │
         │   STEP B: API CALL     │                                  │
         │   (Use JWT Token)      │                                  │
         │                        │                                  │
         │  3. GET /api/test      │                                  │
         │     Bearer <JWT>       │                                  │
         │─────────────────────────────────────────────────────────>│
         │                        │                                  │
         │                        │  4. Fetch public key             │
         │                        │<─────────────────────────────────│
         │                        │  5. Return public key            │
         │                        │─────────────────────────────────>│
         │                        │                                  │
         │                        │     6. Validate JWT signature    │
         │                        │     7. Extract tenant_id         │
         │                        │     8. Set TenantContext         │
         │                        │     9. Process business logic    │
         │                        │    10. Clear TenantContext       │
         │                        │                                  │
         │  11. Response data     │                                  │
         │<─────────────────────────────────────────────────────────│
```

---

## 📁 Final Project Structure

```
src/main/java/com/spring_security/JWT_Context_Propagation/
├── JwtContextPropagationApplication.java
├── config/
│   └── SecurityConfig.java              ← Security filter chain
├── context/
│   └── TenantContext.java               ← ThreadLocal holder
├── converter/
│   └── KeycloakJwtAuthenticationConverter.java  ← Role mapping
├── filter/
│   └── TenantContextFilter.java         ← Extract tenant, cleanup
└── controller/
    └── TestController.java              ← Test endpoints

src/main/resources/
└── application.yaml                     ← Keycloak config

pom.xml                                  ← Resource server dependency
```

---

# PHASE 1: INFRASTRUCTURE SETUP

## Step 1.1: Start Docker Containers ⬜

**What you achieve:** Keycloak and PostgreSQL running locally

```bash
cd /Users/bj519244/Desktop/JWT-Context-Propagation
docker-compose up -d
```

**Verify:**
- Keycloak Admin: http://localhost:8080
- Login: admin / admin

---

# PHASE 2: KEYCLOAK CONFIGURATION

## Step 2.1: Create Realm ⬜

**What you achieve:** Isolated security domain for your application

1. Click dropdown "master" (top-left)
2. Click "Create Realm"
3. Realm name: `terafina`
4. Click "Create"

---

## Step 2.2: Create Client ⬜

**What you achieve:** Your Spring Boot app registered as an OAuth2 client

**General Settings:**
- Client type: OpenID Connect
- Client ID: `terafina-api`
- Name: Terafina API Client

**Capability Config:**
- Client authentication: ✅ ON
- Standard flow: ✅
- Direct access grants: ✅ (for Postman/testing)

**Login Settings:**
- Valid redirect URIs: `http://localhost:8081/*`
- Web origins: `http://localhost:8081`

**⚠️ IMPORTANT - Get Client Secret:**
- Go to: Clients → terafina-api → Credentials tab
- Copy and save the Client Secret: `___________________________`

---

## Step 2.3: Create Client Scope for tenant_id ⬜

**What you achieve:** Custom `tenant_id` claim will appear in JWT tokens

1. Go to: Client scopes → Create client scope
2. Name: `tenant-scope`
3. Description: Adds tenant_id to token
4. Type: Default
5. Protocol: OpenID Connect
6. Click "Save"

---

## Step 2.4: Add Token Mapper for tenant_id ⬜

**What you achieve:** User's tenant_id attribute → JWT claim mapping

1. Go to: Client scopes → tenant-scope → Mappers → Configure new mapper
2. Select: "User Attribute"
3. Configure:
   - Name: `tenant_id`
   - User Attribute: `tenant_id`
   - Token Claim Name: `tenant_id`
   - Claim JSON Type: String
   - Add to ID token: ✅
   - Add to access token: ✅ (IMPORTANT!)
   - Add to userinfo: ✅
4. Click "Save"

---

## Step 2.5: Assign Scope to Client ⬜

**What you achieve:** Your client (terafina-api) uses the tenant-scope

1. Go to: Clients → terafina-api → Client scopes tab
2. Click "Add client scope"
3. Select: tenant-scope
4. Assigned type: Default
5. Click "Add"

---

## Step 2.6: Create Realm Roles ⬜

**What you achieve:** Roles that can be assigned to users

1. Go to: Realm roles → Create role
2. Create: `ROLE_USER` → Save
3. Create: `ROLE_ADMIN` → Save

---

## Step 2.7: Create Test Users ⬜

**What you achieve:** Users with different tenant_id values for testing

### User 1 (Tenant 1):
1. Go to: Users → Add user
   - Username: `user1`
   - Email: `user1@tenant1.com`
   - Email verified: ✅
   - Click "Create"

2. Go to: Credentials tab
   - Set password: `user1pass`
   - Temporary: ❌ OFF
   - Click "Save"

3. Go to: Attributes tab
   - Key: `tenant_id`, Value: `tenant_1`
   - Click "Save"

4. Go to: Role mapping tab
   - Click "Assign role"
   - Select: ROLE_USER
   - Click "Assign"

### User 2 (Tenant 2):
- Username: `user2`
- Password: `user2pass`
- Attribute tenant_id: `tenant_2`
- Role: ROLE_USER

### Admin User:
- Username: `admin1`
- Password: `admin1pass`
- Attribute tenant_id: `tenant_1`
- Roles: ROLE_USER, ROLE_ADMIN

---

## Step 2.8: Test JWT Token Generation ⬜

**What you achieve:** Confirm Keycloak generates JWT with tenant_id claim

```bash
curl -X POST http://localhost:8080/realms/terafina/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=terafina-api" \
  -d "client_secret=YOUR_CLIENT_SECRET_HERE" \
  -d "username=user1" \
  -d "password=user1pass"
```

**Expected Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "expires_in": 300,
  "token_type": "Bearer"
}
```

**Decode at https://jwt.io - Expected claims:**
```json
{
  "iss": "http://localhost:8080/realms/terafina",
  "tenant_id": "tenant_1",
  "realm_access": {
    "roles": ["ROLE_USER"]
  }
}
```

---

# PHASE 3: SPRING BOOT IMPLEMENTATION

## Step 3.1: Update pom.xml ⬜

**What you achieve:** Spring Boot can validate JWT tokens

Add this dependency:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

---

## Step 3.2: Create TenantContext Class ⬜

**What you achieve:** Thread-safe storage for tenant_id during request lifecycle

**File:** `src/main/java/com/spring_security/JWT_Context_Propagation/context/TenantContext.java`

```java
package com.spring_security.JWT_Context_Propagation.context;

public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
```

---

## Step 3.3: Update application.yaml ⬜

**What you achieve:** Spring Security knows where to validate JWTs

**File:** `src/main/resources/application.yaml`

```yaml
server:
  port: 8081

spring:
  application:
    name: JWT-Context-Propagation
    
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/terafina
          jwk-set-uri: http://localhost:8080/realms/terafina/protocol/openid-connect/certs
```

---

## Step 3.4: Create Keycloak JWT Authentication Converter ⬜

**What you achieve:** Keycloak roles mapped to Spring Security authorities

**File:** `src/main/java/com/spring_security/JWT_Context_Propagation/converter/KeycloakJwtAuthenticationConverter.java`

```java
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

@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("preferred_username"));
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role))
                .collect(Collectors.toList());
    }
}
```

---

## Step 3.5: Create Tenant Context Filter ⬜

**What you achieve:** Extract tenant_id from JWT and populate TenantContext

**File:** `src/main/java/com/spring_security/JWT_Context_Propagation/filter/TenantContextFilter.java`

```java
package com.spring_security.JWT_Context_Propagation.filter;

import com.spring_security.JWT_Context_Propagation.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                String tenantId = jwt.getClaimAsString("tenant_id");
                
                if (tenantId != null) {
                    TenantContext.setCurrentTenant(tenantId);
                    logger.debug("TenantContext set to: " + tenantId);
                } else {
                    logger.warn("No tenant_id claim found in JWT");
                }
            }

            filterChain.doFilter(request, response);
            
        } finally {
            // CRITICAL: Always clear to prevent memory leaks
            TenantContext.clear();
            logger.debug("TenantContext cleared");
        }
    }
}
```

---

## Step 3.6: Create Security Configuration ⬜

**What you achieve:** Security filter chain configured for JWT validation

**File:** `src/main/java/com/spring_security/JWT_Context_Propagation/config/SecurityConfig.java`

```java
package com.spring_security.JWT_Context_Propagation.config;

import com.spring_security.JWT_Context_Propagation.converter.KeycloakJwtAuthenticationConverter;
import com.spring_security.JWT_Context_Propagation.filter.TenantContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter)
                )
            )
            .addFilterAfter(tenantContextFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}
```

---

## Step 3.7: Create Test Controller ⬜

**What you achieve:** Endpoints to test your implementation

**File:** `src/main/java/com/spring_security/JWT_Context_Propagation/controller/TestController.java`

```java
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

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/public/health")
    public Map<String, String> publicHealth() {
        return Map.of(
            "status", "UP",
            "message", "Public endpoint - no auth required"
        );
    }

    @GetMapping("/test/tenant")
    public Map<String, Object> getTenantInfo(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("tenantFromContext", TenantContext.getCurrentTenant());
        response.put("tenantFromJwt", jwt.getClaimAsString("tenant_id"));
        response.put("username", jwt.getClaimAsString("preferred_username"));
        response.put("subject", jwt.getSubject());
        return response;
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Map<String, Object> adminDashboard() {
        return Map.of(
            "message", "Welcome to admin dashboard!",
            "tenant", TenantContext.getCurrentTenant(),
            "accessLevel", "ADMIN"
        );
    }

    @GetMapping("/user/profile")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public Map<String, Object> userProfile(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "message", "User profile",
            "tenant", TenantContext.getCurrentTenant(),
            "email", jwt.getClaimAsString("email")
        );
    }
}
```

---

# PHASE 4: TESTING

## Step 4.1: Start Everything ⬜

```bash
# Terminal 1: Docker
docker-compose up -d

# Terminal 2: Spring Boot
./mvnw spring-boot:run
```

---

## Step 4.2: Get JWT Token ⬜

```bash
curl -X POST http://localhost:8080/realms/terafina/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=terafina-api" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "username=user1" \
  -d "password=user1pass"
```

---

## Step 4.3: Run Test Cases ⬜

### Test 1: Public Endpoint (No Auth)
```bash
curl http://localhost:8081/api/public/health
```
**Expected:** 200 OK ✅

### Test 2: Protected Endpoint WITHOUT Token
```bash
curl http://localhost:8081/api/test/tenant
```
**Expected:** 401 Unauthorized ✅

### Test 3: Protected Endpoint WITH Token
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  http://localhost:8081/api/test/tenant
```
**Expected:** 
```json
{
  "tenantFromContext": "tenant_1",
  "tenantFromJwt": "tenant_1"
}
```
✅ TenantContext works!

### Test 4: Admin Endpoint with Regular User
```bash
curl -H "Authorization: Bearer USER1_TOKEN" \
  http://localhost:8081/api/admin/dashboard
```
**Expected:** 403 Forbidden ✅

### Test 5: Admin Endpoint with Admin User
```bash
curl -H "Authorization: Bearer ADMIN1_TOKEN" \
  http://localhost:8081/api/admin/dashboard
```
**Expected:** 200 OK ✅

---

# ✅ Final Checklist

| # | Task | Status |
|---|------|--------|
| 1.1 | Start Docker containers | ⬜ |
| 2.1 | Create Keycloak Realm | ⬜ |
| 2.2 | Create Client | ⬜ |
| 2.3 | Create Client Scope | ⬜ |
| 2.4 | Add Token Mapper | ⬜ |
| 2.5 | Assign Scope to Client | ⬜ |
| 2.6 | Create Roles | ⬜ |
| 2.7 | Create Test Users | ⬜ |
| 2.8 | Test JWT Generation | ⬜ |
| 3.1 | Update pom.xml | ⬜ |
| 3.2 | Create TenantContext | ⬜ |
| 3.3 | Update application.yaml | ⬜ |
| 3.4 | Create JWT Converter | ⬜ |
| 3.5 | Create Tenant Filter | ⬜ |
| 3.6 | Create Security Config | ⬜ |
| 3.7 | Create Test Controller | ⬜ |
| 4.1 | Start services | ⬜ |
| 4.2 | Get JWT Token | ⬜ |
| 4.3 | Run all test cases | ⬜ |

---

# 📝 Notes

**Client Secret:** `_________________________________`

**Test User Credentials:**
- user1 / user1pass (tenant_1, ROLE_USER)
- user2 / user2pass (tenant_2, ROLE_USER)
- admin1 / admin1pass (tenant_1, ROLE_USER + ROLE_ADMIN)

