# Local Run Guide (HTTPS)

This guide explains how to run the NearShare backend + Angular frontend locally over HTTPS, and how to fix `net::ERR_CERT_AUTHORITY_INVALID` when using self-signed certificates.

## URLs (local)

- Frontend (Angular): https://localhost:4200/
- Backend health: https://localhost/shareit/api/health
- Backend API base: https://localhost/shareit/api

The backend runs on port `443` with context path `/shareit`.

## Backend (HTTPS + PostgreSQL)

### 1) Start PostgreSQL

- Ensure PostgreSQL is running on `localhost:5432`
- Database: `nearshare`
- User: `postgres`
- Password: `postgres`

### 2) Run the backend script

From the repo root:

```bat
.\run-local-postgres.bat
```

This script calls [setup.bat](/shareit_back/setup.bat) to set required environment variables (DB, SSL passwords, JWT keystore aliases/passwords, etc.) and then starts Spring Boot.

### 3) Confirm the backend is up

Open in a browser:

- https://localhost/shareit/api/health

Or from a terminal:

```powershell
C:\Windows\System32\curl.exe -k https://localhost/shareit/api/health
```

## Frontend (Angular over HTTPS)

### 1) Install dependencies

From the `frontend` folder:

```powershell
cd .\frontend
npm install
```

### 2) Start the dev server (HTTPS)

```powershell
npm run start -- --ssl true --host 0.0.0.0 --port 4200
```

### 3) Confirm the frontend is using the HTTPS backend

The dev environment is configured to call:

- `apiUrl: https://localhost/shareit/api`
- `wsUrl: wss://localhost/shareit/ws`

See [environment.ts](/shareit_back/frontend/src/environments/environment.ts).

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

- https://localhost/shareit/api/seed

Example seeded login credentials:

- `linda.lender@example.com` / `password123`
- `bob.borrower@example.com` / `password123`
- `sarah.smith@example.com` / `password123`
- `peter.pro@example.com` / `password123`
- `admin@nearshare.local` / `password123`

## Fixing `net::ERR_CERT_AUTHORITY_INVALID` (browser trust)

If the frontend shows no data and DevTools Network shows errors like:

- `/shareit/api/config/settings: net::ERR_CERT_AUTHORITY_INVALID`

it means the browser does not trust the backend’s certificate.

### Option A (fastest): accept the certificate warning once

1. Open https://localhost/shareit/api/health in your browser
2. Click Advanced → Proceed (wording varies by browser)
3. Reload https://localhost:4200/

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

### Port 443 already in use

If the backend fails to start with “Port 443 was already in use”:

- Stop the process using port 443, then restart the backend.

### No listings show in the UI

The frontend returns an empty list if the API call fails.

Check DevTools → Network:

- If you see `ERR_CERT_AUTHORITY_INVALID`, fix the certificate trust (section above).
- If you see `CORS` errors, confirm backend is running and `https://localhost:4200` is allowed.
