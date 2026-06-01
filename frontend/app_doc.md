# ShareIt Angular Client

## Prerequisites

- Node.js + npm (this repo is set to `npm@11.11.0`)
- A running backend API (ShareIt Spring Boot) at `http://localhost:8081/api/v1`

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

## Run (Dev)

```bash
npm start
```

Open:
- http://localhost:4200/

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
