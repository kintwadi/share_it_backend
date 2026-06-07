# Vicinity24 Client

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 17.3.17.

## Development server

Run `npm start` for a dev server. Navigate to `http://localhost:4200/`.

Notes:
- The app uses relative API paths (`/api/v1`, `/ws`) in dev.
- The dev server proxies `/api/*` + `/ws/*` to `http://localhost:8081` via `proxy.conf.json`.
- If the backend is running in multi-tenant mode, direct API tools or custom clients should send `X-Tenant-ID` unless the backend is configured with `SETTING_USE_DEFAULT_DATABASE=true` and the request should fall back only when the header is missing.
- The Angular app can send the tenant header automatically when runtime env includes values that match a backend tenant id:
  - `TENANT_ID=<tenant-id>`
  - optional `TENANT_HEADER_NAME=X-Tenant-ID`

## Frontend .env

The frontend runtime generator now reads:

- `frontend/.env`
- `frontend/.env.local`

Create your local file by copying [`.env.template`](file:///c:/Users/core101/Desktop/desk/shareit_back/frontend/.env.template):

```bash
cd frontend
cp .env.template .env
```

Example values:

```bash
API_URL=/api/v1
TENANT_HEADER_NAME=X-Tenant-ID
TENANT_ID=vicinity24_tenant_a
```

Priority order:

- real shell environment variables win first
- then `frontend/.env`
- then `frontend/.env.local`

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via a platform of your choice. To use this command, you need to first add a package that implements end-to-end testing capabilities.

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.

