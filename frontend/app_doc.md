# ShareIt Angular Client

## Prerequisites

- Node.js + npm (this repo is set to `npm@11.11.0`)
- A running backend API (ShareIt Spring Boot) at `http://localhost:8081`

## Install

```bash
cd shareit_angular_client
npm install
```

## Configure API Base URL

The Angular app uses [`environment.ts`](file:///c:/Users/core101/Desktop/desk/shareit_angular_client/src/environments/environment.ts) to build API requests.

- `apiUrl` should point to the backend API root (must include `/api`)
- Example:

```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081/api',
  wsUrl: 'ws://localhost:8081/ws'
};
```

## Run (Dev)

```bash
npm start -- --host 127.0.0.1 --port 4201
```

Open:
- http://127.0.0.1:4201/

This app uses hash-based routing, so routes look like:
- `http://127.0.0.1:4201/#/`
- `http://127.0.0.1:4201/#/listing/<listingId>`

## Run Backend (Required for Live Endpoints)

From the backend folder:

```bash
cd shareit_back
mvn spring-boot:run
```

Backend health check:
- http://localhost:8081/api/health

## Build

```bash
npm run build
```

Output:
- `dist/share-it-client`

## Assets

Static assets are served from:
- `src/assets/...` (for example `src/assets/images/logo.png`)

The build includes `src/assets` via [`angular.json`](file:///c:/Users/core101/Desktop/desk/shareit_angular_client/angular.json).

