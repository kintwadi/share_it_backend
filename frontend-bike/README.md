# Frontend Bike

`frontend-bike` is a standalone Angular application for the bicycle vertical.

It is intentionally isolated from the main `frontend` app and owns its own:

- routing
- auth pages
- password recovery flow
- subscription pages
- checkout page
- bike discovery and detail pages
- listing submission flow
- header, footer, layout, and services

## Structure

- `src/app/bike`: migrated bike catalog, bike detail, rent-to-own, and checklist UI
- `src/app/features/auth`: login, signup, and password recovery
- `src/app/features/subscription`: subscription plan entry point
- `src/app/features/checkout`: checkout launcher for Stripe subscription sessions
- `src/app/features/list-bike`: bike listing submission and current listing view
- `src/app/core`: runtime env, auth/session storage, API client, guards, and services
- `src/app/shared`: bike-only shell components

## Run

Install dependencies:

```bash
npm install
```

Start the isolated bike frontend:

```bash
npm start
```

Default local URL:

```text
http://localhost:4300/
```

Build production assets:

```bash
npm run build
```

## Browser Smoke Test

Run the headed Playwright smoke test:

```bash
npm run test:bike:headed
```

Install Chromium first on a fresh machine:

```bash
npm run test:bike:headed:install
```

Artifacts are written to:

```text
frontend-bike/playwright-artifacts/
```

## Backend Expectations

This frontend talks to the existing backend endpoints and expects the backend to be running locally.

Primary APIs used:

- `/api/v1/auth/*`
- `/api/v1/subscriptions/*`
- `/api/v1/partner/listings`
- `/api/v1/bikes`

Runtime API configuration is loaded from:

- `public/env.js`

## Current Notes

- Login currently supports standard auth flow; 2FA verification is not yet implemented in this isolated app.
- Checkout creates a subscription checkout session and redirects to the returned Stripe URL.
- Bike listing submission uses the existing partner listing backend, so the signed-in account must be allowed to create partner listings.
