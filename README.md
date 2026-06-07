# Vicinity24

Monorepo containing:
- Spring Boot backend (API + auth + admin + partner + payments)
- Angular frontend (in `frontend/`)

## Quick Start (Local)

### Backend

From repo root:

```bash
mvn spring-boot:run
```

Defaults (override via env vars / properties):
- API base: `http://localhost:8081/api/v1`
- DB: SQLite when `DB_URL` is not set
- SSL: disabled by default for local runs

### Frontend

From `frontend/`:

```bash
npm install
npm start
```

The dev server proxies `/api/*` + `/ws/*` to `http://localhost:8081` using [proxy.conf.json](file:///c:/Users/core101/Desktop/desk/shareit_back/frontend/proxy.conf.json).

## Runtime App Settings (Admin)

Admins can edit safe runtime settings at:
- UI: `/admin` → Settings tab
- API: `GET /api/v1/admin/app-settings`, `PUT /api/v1/admin/app-settings`

Edits are stored as overrides in `app_config_overrides` and merged with the base `settings.*` config at runtime.

## Multi-Tenant Backend

The backend now supports static database-per-tenant routing at the infrastructure layer.

- Header: `X-Tenant-ID` by default
- Fallback switch: `SETTING_USE_DEFAULT_DATABASE=true`
- Default tenant id: `TENANT_DEFAULT_ID=default`
- Default tenant database env vars:
  - `TENANT_DEFAULT_DB_URL`
  - `TENANT_DEFAULT_DB_USERNAME`
  - `TENANT_DEFAULT_DB_PASSWORD`
  - `TENANT_DEFAULT_DB_DRIVER`
- Optional extra tenant examples:
  - `TENANT_A_DB_URL`, `TENANT_A_DB_USERNAME`, `TENANT_A_DB_PASSWORD`, `TENANT_A_DB_DRIVER`
  - `TENANT_B_DB_URL`, `TENANT_B_DB_USERNAME`, `TENANT_B_DB_PASSWORD`, `TENANT_B_DB_DRIVER`

If `SETTING_USE_DEFAULT_DATABASE=true`, requests without `X-Tenant-ID` continue to use the default database so existing integrations keep working.

## Service Fee When Subscription Is Disabled

When `settings.enable.subscription=false`, the backend applies a fixed service fee to every `LEND` checkout:
- `settings.service.fee` (default `2.99`)

## Docs

- Local setup: [local-run-guide.md](file:///c:/Users/core101/Desktop/desk/shareit_back/DOC/local-run-guide.md)
- Full configuration: [configuration-guide.md](file:///c:/Users/core101/Desktop/desk/shareit_back/DOC/configuration-guide.md)
- API overview: [api-contract.md](file:///c:/Users/core101/Desktop/desk/shareit_back/DOC/api-contract.md)
- Frontend usage: [user-guide.md](file:///c:/Users/core101/Desktop/desk/shareit_back/DOC/user-guide.md)
