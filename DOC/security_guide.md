# Vicinity24 Backend â€” Production Security Guide

This document is a practical, production-focused security checklist for the Vicinity24 Spring Boot backend.

## Scope

- Backend: Spring Boot (embedded Tomcat), JWT-based auth, REST + WebSocket
- Deployment: Docker / VM / managed platform
- Note: This repo includes local/dev scripts and sample configs; production should not ship with embedded secrets or debug endpoints.

## High-Risk Findings (Fix First)

### 1) Secrets are present in repo configs/scripts

The following files include hardcoded secrets or secret defaults and must be cleaned before production:

- [application.properties](file:///C:/Users/core101/Desktop/desk/shareit_back/src/main/resources/application.properties)
- [run-local-postgres.bat](file:///C:/Users/core101/Desktop/desk/shareit_back/SCRIPTS/run-local-postgres.bat)
- [setup.bat](file:///C:/Users/core101/Desktop/desk/shareit_back/SCRIPTS/setup.bat)

Required actions:

- Remove any real keys from git history and rotate those credentials immediately.
- Ensure production configs use environment variables only (no secret defaults).
- Prefer `.env` files or secret managers for local convenience; never commit `.env` with real values.

### 2) Debug/seed endpoint is publicly accessible

- Seeding endpoint: [SeedingController.java](file:///C:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/controller/SeedingController.java)
- It is currently permitted without auth in: [SecurityConfig.java](file:///C:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/config/SecurityConfig.java)

Required actions:

- Disable this endpoint in production (feature flag) or require admin-only access.
- Ensure seed data cannot be triggered remotely in any production environment.

### 3) Unlimited upload size configuration

- Multipart upload limits are currently set to unlimited in: [application.properties](file:///C:/Users/core101/Desktop/desk/shareit_back/src/main/resources/application.properties)

Required actions:

- Set strict upload size caps suitable for your application.
- Enforce content-type allowlist and server-side validation on uploaded files.

## Transport Security (TLS/SSL)

### Required

- Terminate TLS at embedded Tomcat for production using an external keystore path (`file:/...`).
- Prioritize TLS 1.3 (keep TLS 1.2 only if required by clients).
- Redirect all HTTP (80) to HTTPS (443) using embedded Tomcat connector:
  - [HttpToHttpsConfig.java](file:///C:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/config/HttpToHttpsConfig.java)
- Enforce HSTS:
  - [HstsFilter.java](file:///C:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/filter/HstsFilter.java)

### Recommended hardening

- Set `Strict-Transport-Security` only on HTTPS responses.
- Consider adding additional security headers (CSP, X-Content-Type-Options, Referrer-Policy, X-Frame-Options or frame-ancestors via CSP).
- Use CA-issued certs in production; self-signed certs are for local testing only.

## Authentication & Authorization

### JWT validation

- Review JWT filter behavior, especially error handling and logging:
  - [JwtAuthenticationFilter.java](file:///C:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/security/JwtAuthenticationFilter.java)

Required actions:

- Do not swallow authentication exceptions silently in production. Log securely (no tokens), with rate-limited/error-sampled logging.
- Ensure token expiry, signature verification, and algorithm restrictions are enforced.
- Ensure role checks are enforced for write endpoints and for sensitive reads.

### Route access policy

- Review all `permitAll()` endpoints and confirm they are safe:
  - [SecurityConfig.java](file:///C:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/config/SecurityConfig.java)

Required actions:

- Restrict Swagger/OpenAPI endpoints in production (`/v3/api-docs/**`, `/swagger-ui/**`).
- Ensure internal/admin endpoints require admin role and cannot be accessed via alternate routes.

## CORS

Current state includes both property-based CORS and SecurityConfig CORS configuration.

Required actions:

- Use a single source of truth for CORS configuration.
- Use a strict allowlist for production origins (no wildcards like `http://localhost:*`).
- Ensure `allowCredentials=true` is used only when required; if enabled, origins must be explicit and not `*`.

## Error Handling & Logging

- Global exception handling currently prints stack traces and returns exception details:
  - [GlobalExceptionHandler.java](file:///C:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/exception/GlobalExceptionHandler.java)

Required actions:

- Do not return internal exception class names/messages to clients in production.
- Use structured logging and correlation IDs.
- Ensure logs do not include secrets (JWT, Stripe webhook signatures, passwords, access keys).
- Reduce debug logging levels in production (`logging.level.*=DEBUG` should not be enabled).

## Webhooks & Payments (Stripe)

Required actions:

- Verify Stripe webhook signatures using `STRIPE_WEBHOOK_SECRET` on every webhook request.
- Apply request replay protection and store event IDs to ensure idempotency.
- Ensure webhook endpoints do not allow unsafe methods and have strict content-type checks.

## Database Security

Required actions:

- Use least-privilege DB credentials (separate migration/admin vs runtime users if possible).
- Enforce TLS to database when using remote Postgres.
- Verify indices and constraints for integrity; avoid relying solely on application logic.
- Use backups, PITR, and tested restore procedures.

## Data Privacy

Required actions:

- Avoid exposing full addresses/PII to unauthorized users.
- Ensure any â€œmaskedâ€/coarse location strategy is enforced server-side for unauthorized users.
- Minimize data returned in DTOs; do not rely only on frontend hiding data.

## Dependency & Build Security

- Dependencies are defined in: [pom.xml](file:///C:/Users/core101/Desktop/desk/shareit_back/pom.xml)

Required actions:

- Add CI dependency scanning (OWASP Dependency-Check or Snyk).
- Add SAST (Semgrep) and secret scanning (gitleaks).
- Keep Spring Boot and libraries patched (scheduled upgrades).

## Docker & Runtime Hardening

Current Dockerfile installs PostgreSQL into the application image:

- [Dockerfile](file:///C:/Users/core101/Desktop/desk/shareit_back/Dockerfile)

Required actions:

- Do not run Postgres inside the app container for production. Use managed Postgres or a separate container (compose/k8s).
- Run as non-root (already done in Dockerfile).
- Use read-only filesystem where possible; drop Linux capabilities; set seccomp/apparmor profiles.
- Provide secrets through the runtime secret mechanism, not `ENV` in the Dockerfile.

## Operational Security

Required actions:

- Enable monitoring/alerting (5xx rate, auth failures, webhook failures, DB errors).
- Add rate limiting and abuse prevention (login, password reset, OTP/2FA, webhook).
- Ensure secure backups and rotation policies for secrets and certificates.
- Define incident response steps: key rotation, credential revoke, rollback procedures.

## Production Readiness Checklist (Quick)

- [ ] No secrets in repo or container images; all secrets injected at runtime
- [ ] Seed/debug endpoints disabled in production
- [ ] TLS 1.3 preferred, CA certs, HSTS enabled on HTTPS only
- [ ] Strict CORS allowlist for production origin(s)
- [ ] AuthZ verified for all endpoints; least privilege by role and ownership
- [ ] Upload limits and file validation enabled
- [ ] Swagger/OpenAPI restricted in production
- [ ] Safe error responses; no stack traces or exception details returned
- [ ] Dependency + secret scanning in CI/CD
- [ ] Separate database instance/service; encrypted connections; least privilege credentials
- [ ] Monitoring, logging, rate limiting, and alerting in place

## Multi-Tenant Environment Note

This project supports static database-per-tenant routing in the backend configuration layer.

- Main env vars: `SETTING_USE_DEFAULT_DATABASE`, `TENANT_HEADER_NAME`, `TENANT_DEFAULT_ID`, `TENANT_DEFAULT_DB_URL`, `TENANT_DEFAULT_DB_USERNAME`, `TENANT_DEFAULT_DB_PASSWORD`, `TENANT_DEFAULT_DB_DRIVER`
- Optional extra tenant examples: `TENANT_A_*`, `TENANT_B_*`
- Active tenant ids are defined by the keys under `tenants.config.*` in `src/main/resources/application.properties`; the current sample configuration uses `default`, `vicinity24_tenant_a`, and `vicinity24_tenant_b`
- `SETTING_USE_DEFAULT_DATABASE=true` uses the default database only when the tenant header is missing; a valid tenant header still routes to the matching tenant database
- Startup bootstrap initializes or upgrades schema and seed data for the default database and every configured tenant database
- Full setup details live in `DOC/configuration-guide.md` and `.env.template`


