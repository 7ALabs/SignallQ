# SignallQ Authentication Layer — OpenAPI Documentation

**Created**: 27.06.2026  
**Based on**: integrations/cloudflare/signallq-admin-worker/src/{auth.ts, index.ts}  
**Issue**: SIG-167

---

## Overview

SignallQ uses **3 distinct authentication schemes** depending on the endpoint group:

1. **Admin Dashboard** (`/admin/*, /auth/*`) — Session-based (httpOnly cookies, PBKDF2)
2. **App Ingest** (`/ingest/*`) — Bearer token (limited scope, app-only)
3. **Health/Legacy** (`/health`) — Bearer token (legacy, ADMIN_SECRET only)

---

## 1. Session Authentication (Admin Dashboard)

### Scheme: `cookieAuth`

**Type**: Cookie-based sessions with httpOnly + Secure flags

**Properties**:
- **Cookie Name**: `session`
- **Storage**: HttpOnly, Secure, SameSite=None
- **Lifetime**: 7 days (604800 seconds)
- **Hash Algorithm**: SHA-256(token) → stored in D1 admin_sessions table
- **Token Generation**: 32-byte random value, base64url encoded
- **Validation**: Token hash + user_id lookup + expiry check + active user flag

### Session Lifecycle

```
1. POST /auth/login
   → Verify email + password (PBKDF2)
   → Create session token
   → Store token_hash in admin_sessions (D1)
   → Return 200 + Set-Cookie: session=...; HttpOnly; Secure; SameSite=None; Max-Age=604800

2. Subsequent requests
   → Extract session cookie from request
   → Validate token_hash + expiry + user.active
   → Update last_seen timestamp in admin_sessions
   → Return 200 (session valid) or 401 (invalid/expired)

3. POST /auth/logout
   → Extract session cookie
   → Delete from admin_sessions (revoke immediately)
   → Return 200 + Set-Cookie: session=; Max-Age=0 (clear cookie)
```

### Password Security

**Algorithm**: PBKDF2-SHA256

**Parameters**:
- Iterations: 100,000
- Salt: Random 16-byte value, generated per password
- Output: 32 bytes (256 bits)
- Pepper: ADMIN_AUTH_PEPPER (from wrangler.toml secrets, appended to password before hash)

**Storage Format**:
```
pbkdf2$100000$<base64(salt)>$<base64(hash)>
```

**Example**:
```
pbkdf2$100000$fPxWXo+4m...base64...==$/GCx7...base64...==
```

**Verification**: Constant-time comparison (timing-attack resistant)

### Database Tables

**admin_sessions** (D1):
```sql
CREATE TABLE admin_sessions (
  token_hash TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  last_seen INTEGER NOT NULL,
  FOREIGN KEY (user_id) REFERENCES admin_users(id) ON DELETE CASCADE
);
```

**admin_users** (D1):
```sql
CREATE TABLE admin_users (
  id TEXT PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL,         -- 'admin' or 'viewer'
  active INTEGER NOT NULL,    -- 1 or 0
  created_at INTEGER NOT NULL,
  last_login INTEGER
);
```

---

## 2. Bearer Token (Ingest)

### Scheme: `bearerAuth`

**Type**: Bearer token in Authorization header

**Token**: INGEST_KEY (from wrangler.toml secrets)

**Scope**: Write-only, limited to `/ingest/*` endpoints

**Why Separate Key**:
- INGEST_KEY embedded in Android APK (public)
- Separate from ADMIN_SECRET (never exposed)
- If INGEST_KEY leaks: attacker can only POST measurements, cannot read admin data
- Reduces blast radius of APK compromise

**Authorization Flow**:
```
Request:
  Authorization: Bearer <INGEST_KEY>

Validation:
  1. Extract Authorization header
  2. Parse "Bearer <token>"
  3. Compare token === INGEST_KEY (constant-time)
  4. Allow POST /ingest/*
  5. Reject other methods/paths
```

**Fallback**: ADMIN_SECRET also accepted (for dev/testing), but not recommended in production

---

## 3. Legacy Bearer Token (Health)

### Scheme: `adminSecretAuth`

**Type**: Bearer token in Authorization header

**Token**: ADMIN_SECRET (from wrangler.toml secrets)

**Endpoint**: GET /health (heartbeat for external monitoring)

**Why Separate**: Retrocompat for monitoring systems that can't support the new schemes

**Status**: Deprecated (consider removing in next major version)

---

## Rate Limiting

### Endpoint: `/auth/login` and `/auth/change-password`

**Algorithm**: IP-based token bucket

**Limits**:
- Max 5 attempts per IP per 15-minute window
- After 5 failures: return 429 Too Many Requests for 15 minutes
- Window resets automatically after 15 minutes

**Implementation**:
```sql
CREATE TABLE auth_rate_limit (
  ip TEXT PRIMARY KEY,
  count INTEGER NOT NULL,
  window_start INTEGER NOT NULL
);
```

**Logic** (in index.ts lines 76–103):
```typescript
1. On failed login:
   → Get client IP from CF-Connecting-IP header
   → Check if (count > 5 AND window < 15min):
     → Return 429 "Muitas tentativas. Tente novamente em 15 minutos."
   → Increment counter or reset if window expired

2. Valid login:
   → Rate limit counter NOT incremented (only failures)
   → Counter eventually expires after 15min of inactivity
```

---

## OpenAPI 3.0 Spec (Security Schemes Section)

```yaml
components:
  securitySchemes:
    # Admin Dashboard Sessions
    cookieAuth:
      type: apiKey
      in: cookie
      name: session
      description: |
        HTTPOnly session cookie (SIG-136).
        
        **Lifetime**: 7 days
        
        **Flow**: 
        1. POST /auth/login with email+password
        2. Response includes Set-Cookie with session token
        3. Subsequent requests: browser automatically sends session cookie
        4. POST /auth/logout to revoke session
        
        **Password Hash**: PBKDF2-SHA256 (100k iterations, pepper from ADMIN_AUTH_PEPPER secret)
        
        **Storage**: Token stored as SHA-256(token_hash) in admin_sessions table (D1)
        
        **Rate Limiting**: Max 5 login attempts per IP per 15 minutes

    # App Ingest (Android/PWA)
    bearerAuth:
      type: http
      scheme: bearer
      description: |
        Bearer token for /ingest/* endpoints (app-only).
        
        **Token**: INGEST_KEY (from wrangler.toml secrets)
        
        **Scope**: Write-only, limited to POST /ingest/diagnostic, /ingest/ai-usage, /ingest/analytics
        
        **Lifetime**: Indefinite (rotate manually if compromised)
        
        **Why Separate**: Reduces blast radius if APK is reverse-engineered.
        If INGEST_KEY leaks, attacker can only POST measurements, cannot read admin data.

    # Legacy Health Check
    adminSecretAuth:
      type: http
      scheme: bearer
      description: |
        Bearer token for GET /health (heartbeat monitoring).
        
        **Token**: ADMIN_SECRET (from wrangler.toml secrets)
        
        **Status**: Deprecated (retrocompat only)

  responses:
    Unauthorized:
      description: Authentication failed or missing
      content:
        application/json:
          schema:
            type: object
            properties:
              error:
                type: string
                example: "Unauthorized"
    
    Forbidden:
      description: Authenticated but insufficient permissions
      content:
        application/json:
          schema:
            type: object
            properties:
              error:
                type: string
                example: "Forbidden"
    
    TooManyRequests:
      description: Rate limited (login/auth endpoints)
      content:
        application/json:
          schema:
            type: object
            properties:
              error:
                type: string
                example: "Muitas tentativas. Tente novamente em 15 minutos."

paths:
  /auth/login:
    post:
      summary: Login with email and password
      operationId: login
      tags:
        - auth
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required:
                - email
                - password
              properties:
                email:
                  type: string
                  format: email
                  example: "admin@signallq.com"
                password:
                  type: string
                  format: password
                  example: "SecureP@ssw0rd"
      responses:
        '200':
          description: Login successful
          headers:
            Set-Cookie:
              schema:
                type: string
                example: "session=<token>; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=604800"
          content:
            application/json:
              schema:
                type: object
                properties:
                  ok:
                    type: boolean
                  role:
                    type: string
                    enum: ["admin", "viewer"]
        '400':
          description: Invalid email or password format
          content:
            application/json:
              schema:
                type: object
                properties:
                  error:
                    type: string
                    example: "email e password obrigatórios"
        '401':
          $ref: '#/components/responses/Unauthorized'
          description: Email or password incorrect (also increments rate limit)
        '409':
          description: Email already registered
          content:
            application/json:
              schema:
                type: object
                properties:
                  error:
                    type: string
                    example: "E-mail já cadastrado"
        '429':
          $ref: '#/components/responses/TooManyRequests'

  /auth/logout:
    post:
      summary: Logout and revoke session
      operationId: logout
      tags:
        - auth
      security:
        - cookieAuth: []
      responses:
        '200':
          description: Logout successful
          headers:
            Set-Cookie:
              schema:
                type: string
                example: "session=; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=0"
          content:
            application/json:
              schema:
                type: object
                properties:
                  ok:
                    type: boolean

  /auth/me:
    get:
      summary: Get current authenticated user
      operationId: getMe
      tags:
        - auth
      security:
        - cookieAuth: []
      responses:
        '200':
          description: Current user info
          content:
            application/json:
              schema:
                type: object
                properties:
                  email:
                    type: string
                    format: email
                  role:
                    type: string
                    enum: ["admin", "viewer"]
        '401':
          $ref: '#/components/responses/Unauthorized'

  /auth/create-user:
    post:
      summary: Create a new admin user (admin-only)
      operationId: createUser
      tags:
        - auth
      security:
        - cookieAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required:
                - email
                - password
              properties:
                email:
                  type: string
                  format: email
                  example: "newadmin@signallq.com"
                password:
                  type: string
                  format: password
      responses:
        '201':
          description: User created
          content:
            application/json:
              schema:
                type: object
                properties:
                  ok:
                    type: boolean
                  id:
                    type: string
                    format: uuid
        '400':
          description: Invalid input
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'
        '409':
          description: Email already registered
          content:
            application/json:
              schema:
                type: object
                properties:
                  error:
                    type: string
                    example: "E-mail já cadastrado"

  /auth/change-password:
    post:
      summary: Change password for current user
      operationId: changePassword
      tags:
        - auth
      security:
        - cookieAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required:
                - currentPassword
                - newPassword
              properties:
                currentPassword:
                  type: string
                  format: password
                newPassword:
                  type: string
                  format: password
      responses:
        '200':
          description: Password changed
          content:
            application/json:
              schema:
                type: object
                properties:
                  ok:
                    type: boolean
        '400':
          description: Missing or invalid passwords
        '401':
          description: Current password incorrect
          content:
            application/json:
              schema:
                type: object
                properties:
                  error:
                    type: string
                    example: "Senha atual incorreta"
        '429':
          $ref: '#/components/responses/TooManyRequests'

  /ingest/diagnostic:
    post:
      summary: Ingest diagnostic measurement (app-only)
      operationId: ingestDiagnostic
      tags:
        - ingest
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              description: "Array of diagnostic records or single record (batch size max 50)"
              properties:
                id:
                  type: string
                created_at:
                  type: string
                  format: date-time
                network_type:
                  type: string
                status:
                  type: string
                  enum: ["excelente", "bom", "regular", "ruim", "critico"]
                download_mbps:
                  type: number
                upload_mbps:
                  type: number
                latency_ms:
                  type: integer
                jitter_ms:
                  type: integer
                packet_loss:
                  type: number
                operator:
                  type: string
                device_model:
                  type: string
                os_version:
                  type: string
                app_version:
                  type: string
                environment:
                  type: string
                  enum: ["production", "staging", "development"]
      responses:
        '200':
          description: Diagnostics ingested
          content:
            application/json:
              schema:
                type: object
                properties:
                  ok:
                    type: boolean
                  ingested:
                    type: integer
        '400':
          description: Invalid payload
        '401':
          $ref: '#/components/responses/Unauthorized'

  /ingest/ai-usage:
    post:
      summary: Ingest AI usage metrics (app-only)
      operationId: ingestAiUsage
      tags:
        - ingest
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                model:
                  type: string
                session_id:
                  type: string
                  format: uuid
                prompt_tokens:
                  type: integer
                completion_tokens:
                  type: integer
                total_tokens:
                  type: integer
                cost_usd:
                  type: number
                environment:
                  type: string
                  enum: ["production", "staging"]
      responses:
        '200':
          description: Usage ingested
          content:
            application/json:
              schema:
                type: object
                properties:
                  ok:
                    type: boolean
        '401':
          $ref: '#/components/responses/Unauthorized'

  /health:
    get:
      summary: Health check (monitoring)
      operationId: health
      tags:
        - health
      security:
        - adminSecretAuth: []
      responses:
        '200':
          description: Service is healthy
          content:
            application/json:
              schema:
                type: object
                properties:
                  ok:
                    type: boolean
        '401':
          $ref: '#/components/responses/Unauthorized'
```

---

## Implementation Checklist

- [x] PBKDF2-SHA256 password hashing (100k iterations, pepper)
- [x] Session token generation (32-byte random, base64url)
- [x] Session storage (SHA-256 hash in D1)
- [x] Session validation (token hash + expiry + user.active)
- [x] Session revocation (logout deletes from D1)
- [x] HttpOnly + Secure + SameSite=None cookies
- [x] Rate limiting (IP-based, 5 attempts / 15min)
- [x] Bearer token for /ingest/* (INGEST_KEY)
- [x] Bearer token for /health (ADMIN_SECRET, legacy)
- [x] /auth/login, /auth/logout, /auth/me, /auth/create-user, /auth/change-password

## Next Steps

1. Add this securitySchemes section to SignallQ Admin/docs/openapi/signallq-admin-api.yaml
2. Update all /admin/* and /auth/* paths with `security: [{ cookieAuth: [] }]`
3. Update all /ingest/* paths with `security: [{ bearerAuth: [] }]`
4. Update /health with `security: [{ adminSecretAuth: [] }]`
5. Run OpenAPI validator (spectacle, swagger-ui, or Postman)
6. Test with Admin Dashboard client
