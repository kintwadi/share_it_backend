# Vicinity24 Angular Client

## Prerequisites

- Node.js + npm (this repo is set to `npm@11.11.0`)
- A running backend API (Vicinity24 Spring Boot) at `http://localhost:8081/api/v1`

## Install

```bash
cd frontend
npm install
```

## API Routing (Dev)

In development, the app uses relative URLs:
- `apiUrl: /api/v1`
- `wsUrl: /ws`

The dev server proxies `/api/*` + `/ws/*` to `http://localhost:8081` via [proxy.conf.json](file:///c:/Users/core101/Desktop/desk/shareit_back/frontend/proxy.conf.json).

If the backend uses multi-tenant database routing, any direct API calls outside the dev proxy flow should include the tenant header configured by `TENANT_HEADER_NAME` unless the backend falls back to the default database because the header is missing. Valid tenant ids must match configured `tenants.config.*` keys such as `vicinity24_tenant_a`.

## Run (Dev)

```bash
npm start
```

Open:
- http://localhost:4200/

Before `npm start` and `npm run build`, the app generates `frontend/public/env.js` from environment variables. Useful keys include `API_URL`, `UI_LAYOUT`, `TENANT_HEADER_NAME`, and `TENANT_ID`.

## Run Backend (Required for Live Endpoints)

From the backend folder:

```bash
cd shareit_back
mvn spring-boot:run
```

Backend health check:
- http://localhost:8081/api/v1/health

## Build

```bash
npm run build
```

Output:
- `dist/share-it-client`

## Assets

Static assets are served from:
- `src/assets/...` (for example `src/assets/images/logo.png`)

The build includes `src/assets` via [`angular.json`](file:///c:/Users/core101/Desktop/desk/shareit_back/frontend/angular.json).
