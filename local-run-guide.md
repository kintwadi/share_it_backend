# Local Run Guide (Default HTTP)

This guide explains how to run the NearShare backend + Angular frontend locally.

## URLs (local)

- Frontend (Angular): `http://localhost:4200/`
- Backend health: `http://localhost:8081/api/v1/health`
- Backend API base: `http://localhost:8081/api/v1`

The default local port is `8081` (can be overridden via `PORT`).

## Backend (Local)

### Option A: quick run (SQLite default)

From the repo root:

```bash
mvn spring-boot:run
```

If `DB_URL` is not set, the backend defaults to a local SQLite file.

### Option B: PostgreSQL (recommended for parity)

1) Start PostgreSQL

- Ensure PostgreSQL is running on `localhost:5432`
- Database: `nearshare`
- User: `postgres`
- Password: `postgres`

2) Run the backend script

From the repo root:

```bat
.\run-local-postgres.bat
```

### Confirm the backend is up

- `http://localhost:8081/api/v1/health`

## Frontend (Angular)

### 1) Install dependencies

From the `frontend` folder:

```powershell
cd .\frontend
npm install
```

### 2) Start the dev server

```powershell
npm start
```

The dev server proxies `/api/*` + `/ws/*` to `http://localhost:8081` via [proxy.conf.json](file:///c:/Users/core101/Desktop/desk/shareit_back/frontend/proxy.conf.json).

See [environment.ts](file:///c:/Users/core101/Desktop/desk/shareit_back/frontend/src/environments/environment.ts) (it uses relative `/api/v1` + `/ws`).

## Seeded data (mock data)

Seeding is enabled in [application.properties](/shareit_back/src/main/resources/application.properties) via:

```properties
seeding.enabled=true
```

On startup, the app seeds:

- Categories
- Pickup locations
- Users, subscriptions, listings, reviews, messages (from [mockdata.json](file:///C:/Users/core101/Desktop/desk/shareit_back/src/main/resources/mockdata.json))

You can also trigger seeding manually:

- `http://localhost:8081/api/v1/seed`

Example seeded login credentials:

- `linda.lender@example.com` / `password123`
- `bob.borrower@example.com` / `password123`
- `sarah.smith@example.com` / `password123`
- `peter.pro@example.com` / `password123`
- `admin@nearshare.local` / `password123`

## Optional: HTTPS local setup

If you choose to enable HTTPS locally, you may need to trust a self-signed certificate to avoid `net::ERR_CERT_AUTHORITY_INVALID`.

See:
- [tls_ssl_configuration_guide.md](file:///c:/Users/core101/Desktop/desk/shareit_back/tls_ssl_configuration_guide.md)

### Option A (fastest): accept the certificate warning once

1. Open `https://localhost/api/v1/health` in your browser
2. Click Advanced → Proceed (wording varies by browser)
3. Reload `https://localhost:4200/` (or your current frontend URL)

### Option B (recommended): install a trusted local CA using mkcert

This makes the certificate trusted by your OS and browser.

1) Install mkcert:

- Windows (Chocolatey):
  - `choco install mkcert`
- Windows (Scoop):
  - `scoop install mkcert`

2) Install the mkcert local CA into your trust store:

```powershell
mkcert -install
```

3) Generate a certificate for localhost:

```powershell
mkcert localhost 127.0.0.1 ::1
```

4) Use that certificate for Spring Boot:

- Convert the generated cert/key into a PKCS12 keystore (example with OpenSSL):

```powershell
openssl pkcs12 -export -in localhost+2.pem -inkey localhost+2-key.pem -out keystore.p12 -name springboot
```

- Point Spring Boot to the generated keystore and password using:
  - `server.ssl.key-store`
  - `server.ssl.key-store-password`
  - `server.ssl.key-store-type=PKCS12`
  - `server.ssl.key-alias=springboot`

### Option C (Chrome/Edge only): allow insecure localhost

1) Open:

- `chrome://flags/#allow-insecure-localhost`

2) Enable “Allow invalid certificates for resources loaded from localhost”
3) Restart the browser

## Common issues

### Port already in use

If the backend fails to start with “Port was already in use”:

- Stop the process using the configured port (default `8081`), then restart the backend.

### No listings show in the UI

The frontend returns an empty list if the API call fails.

Check DevTools → Network:

- If you see `ERR_CERT_AUTHORITY_INVALID`, fix the certificate trust (section above).
- If you see `CORS` errors, confirm backend is running and your frontend origin is allowed.
