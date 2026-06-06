# Configuration Guide

This document explains, step by step, what the technical team needs to configure so Vicinity24 can run locally and in hosted environments.

It covers:

- backend setup
- frontend setup
- environment variables
- database setup
- Stripe payments
- Stripe Connect onboarding
- Stripe webhooks
- storage configuration
- email configuration
- production readiness checks

## 1. What You Are Setting Up

Vicinity24 is a monorepo with:

- Spring Boot backend
- Angular frontend in `frontend/`

Primary local URLs:

- Frontend: `http://localhost:4200/`
- Backend base API: `http://localhost:8081/api/v1`
- Backend health: `http://localhost:8081/api/v1/health`

Useful references:

- [README.md](file:///c:/Users/core101/Desktop/desk/shareit_back/README.md)
- [local-run-guide.md](file:///c:/Users/core101/Desktop/desk/shareit_back/DOC/local-run-guide.md)
- [SCROW_FLOW.md](file:///c:/Users/core101/Desktop/desk/shareit_back/DOC/SCROW_FLOW.md)
- [application.properties](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/resources/application.properties)
- [.env.template](file:///c:/Users/core101/Desktop/desk/shareit_back/.env.template)

## 2. Prerequisites

Before setup, make sure the team has:

1. Java 17
2. Maven 3.9+
3. Node.js and npm
4. PostgreSQL if using local Postgres
5. A Stripe account
6. A LocationIQ API key if address lookup/geocoding is required
7. SMTP credentials if email flows must work
8. Cloudflare R2 or S3 credentials if real file storage is required

## 3. Choose Your Runtime Mode

The backend supports multiple database modes.

### Option A: PostgreSQL

Recommended for team development and production parity.

Local script:

```bat
.\SCRIPTS\run-local-postgres.bat
```

Expected local database for this script:

- Host: `localhost`
- Port: `5432`
- Database: `Vicinity24`
- Username: `postgres`
- Password: `postgres`

Reference:

- [run-local-postgres.bat](file:///c:/Users/core101/Desktop/desk/shareit_back/SCRIPTS/run-local-postgres.bat)

### Option B: SQLite

Useful for fast local runs with fewer dependencies.

```bat
.\SCRIPTS\run-sqlite-local.bat
```

Default SQLite file:

- `./vicinity24.sqlite`

Reference:

- [run-sqlite-local.bat](file:///c:/Users/core101/Desktop/desk/shareit_back/SCRIPTS/run-sqlite-local.bat)

### Option C: H2

Useful for lightweight development/testing only.

```bat
.\SCRIPTS\run-local-h2.bat
```

Reference:

- [run-local-h2.bat](file:///c:/Users/core101/Desktop/desk/shareit_back/SCRIPTS/run-local-h2.bat)

## 4. Create the Environment File

Copy the template:

```powershell
Copy-Item .env.template .env
```

Or on Bash:

```bash
cp .env.template .env
```

The setup scripts load values from `.env`.

Reference:

- [.env.template](file:///c:/Users/core101/Desktop/desk/shareit_back/.env.template)
- [setup.bat](file:///c:/Users/core101/Desktop/desk/shareit_back/SCRIPTS/setup.bat)

## 5. Minimum Environment Variables

For a real usable environment, these are the main values the team should define.

### Core application

- `PORT`
- `SSL_ENABLED`
- `SETTINGS_HTTP_ENABLED`
- `FRONTEND_BASE_URL`

### Database

- `DB_TYPE`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_DRIVER` when needed

### Security

- `JWT_SECRET` or keystore settings
- `ENCRYPTION_KEY`
- `ADMIN_SIGNUP_SECRET` if admin signup is enabled

### Stripe

- `STRIPE_PUBLIC_KEY`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- Subscription Stripe price IDs are no longer required as environment variables by default.
- They can now be provisioned automatically and stored in the application runtime settings database.

### Geolocation / address lookup

- `GEOLOCATION_BASE_URL`
- `GEOLOCATION_ENABLED`
- `LOCATION_IQ_API_KEY`
- `LOCATION_IQ_BASE_URL`
- `LOCATION_IQ_COUNTRYCODES`

### Email

- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`
- `MAIL_SMTP_STARTTLS_ENABLE`
- `MAIL_SMTP_SSL_ENABLE`

### Storage

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `R2_ACCOUNT_ID`
- `R2_ACCESS_KEY_ID`
- `R2_SECRET_ACCESS_KEY`
- `R2_BUCKET_NAME`
- `R2_ENDPOINT`
- `R2_PUBLIC_URL`

## 6. Database Setup

### Local PostgreSQL setup

Create a local database:

```sql
CREATE DATABASE "Vicinity24";
```

If using the provided Postgres script, the expected credentials are:

- user: `postgres`
- password: `postgres`

The script forces:

- `DB_TYPE=postgres`
- `DB_URL=jdbc:postgresql://localhost:5432/Vicinity24`
- `DB_DRIVER=org.postgresql.Driver`

Reference:

- [run-local-postgres.bat](file:///c:/Users/core101/Desktop/desk/shareit_back/SCRIPTS/run-local-postgres.bat)

### Production database

For hosted deployments, configure:

```text
DB_TYPE=postgres
DB_URL=jdbc:postgresql://<host>:5432/<database>?sslmode=require
DB_USERNAME=<username>
DB_PASSWORD=<password>
```

If the DB provider has IPv6-only connectivity issues, set:

```text
JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false
```

## 7. Security and JWT Setup

The application supports JWT signing via:

- `JWT_SECRET`
- or keystore-based signing with `JWT_KEYSTORE_*`

### Recommended minimum

Use at least:

```text
JWT_SECRET=<long-random-secret>
ENCRYPTION_KEY=<16+-char-secret>
```

### Keystore-based signing

If you prefer keystore signing, configure:

- `JWT_KEYSTORE_LOCATION`
- `JWT_KEYSTORE_PASSWORD`
- `JWT_KEYSTORE_TYPE`
- `KEYSTORE_ACCESS_TOKEN_ALIAS`
- `KEYSTORE_ACCESS_TOKEN_PW`
- `KEYSTORE_REFRESH_TOKEN_ALIAS`
- `KEYSTORE_REFRESH_TOKEN_PW`

The setup script validates that either `JWT_SECRET` or keystore configuration is present.

Reference:

- [setup.bat](file:///c:/Users/core101/Desktop/desk/shareit_back/SCRIPTS/setup.bat)

## 8. Backend Startup

### Recommended local start

From repo root:

```bat
.\SCRIPTS\run-local-postgres.bat
```

Alternative direct Maven run:

```bash
mvn -DfrontendSkip=true spring-boot:run
```

Verify backend:

- `http://localhost:8081/api/v1/health`

## 9. Frontend Setup

From `frontend/`:

```powershell
npm install
npm start
```

The Angular dev server runs on:

- `http://localhost:4200/`

In development, the frontend uses proxying for:

- `/api/*`
- `/ws/*`

Proxy target:

- `http://localhost:8081`

Reference:

- [frontend/README.md](file:///c:/Users/core101/Desktop/desk/shareit_back/frontend/README.md)
- [proxy.conf.json](file:///c:/Users/core101/Desktop/desk/shareit_back/frontend/proxy.conf.json)

## 10. Stripe Setup Overview

Vicinity24 uses Stripe for:

- checkout payments
- escrow-like transaction handling
- refunds
- subscription billing
- lender payouts through Stripe Connect Express accounts

Stripe is initialized in:

- [StripePayment.java](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/payment/StripePayment.java)

Core properties:

- `STRIPE_PUBLIC_KEY`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`

## 11. Stripe Payments Configuration

### Required values

Set:

```text
STRIPE_PUBLIC_KEY=pk_test_...
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

For production, use live keys:

```text
STRIPE_PUBLIC_KEY=pk_live_...
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

Important:

- test and live environments use different keys
- test and live environments use different webhooks
- test and live environments use different connected accounts

## 12. Stripe Webhook Setup

Create a Stripe webhook endpoint for:

```text
POST https://<your-backend>/api/payments/webhook
```

Minimum event required for escrow checkout flow:

- `payment_intent.succeeded`

If subscription features are enabled, also subscribe to:

- `checkout.session.completed`
- `invoice.payment_succeeded`
- `customer.subscription.created`
- `customer.subscription.updated`
- `customer.subscription.deleted`

Then copy the signing secret to:

```text
STRIPE_WEBHOOK_SECRET=whsec_...
```

Reference:

- [SCROW_FLOW.md](file:///c:/Users/core101/Desktop/desk/shareit_back/DOC/SCROW_FLOW.md)
- [PaymentController.java](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/controller/PaymentController.java)

## 13. Stripe Connect Express Setup

Stripe Connect is required so lenders can receive payouts.

### Stripe dashboard requirements

1. Enable **Stripe Connect** on the platform account
2. Make sure the account is in the correct environment: test or live
3. Complete any platform verification Stripe requires

### How the app creates connected accounts

1. The lender signs in
2. The client calls `POST /api/payments/connect/onboard`
3. The backend checks `User.stripeConnectAccountId`
4. If missing, the backend creates a Stripe Express account
5. The backend stores the new account ID on the user
6. The backend generates an onboarding link
7. The user completes onboarding on Stripe
8. The app later checks account readiness with `GET /api/payments/connect/status`

Implementation:

- [PaymentController.java](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/controller/PaymentController.java#L146-L205)
- [StripePayment.java](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/payment/StripePayment.java#L273-L319)

### Required application configuration for Connect

- `STRIPE_SECRET_KEY`
- `app.frontend.baseUrl`

`app.frontend.baseUrl` is used to build:

- onboarding refresh URL
- onboarding return URL

Recommended local value:

```text
FRONTEND_BASE_URL=https://localhost:4200
```

### What must be true before payouts work

- the lender has a `stripeConnectAccountId`
- `detailsSubmitted=true`
- Stripe account requirements are complete
- the platform balance is sufficient for transfer/refund operations

If onboarding is incomplete, release may fail with:

- `connect_onboarding_incomplete`

If no connected account exists, release may fail with:

- `missing_stripe_connect_account`

## 14. Subscription Stripe Configuration

If subscription features are used, configure:

- `STRIPE_PUBLIC_KEY`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`

Then provision the recurring subscription catalog from the admin API:

- `POST /api/admin/stripe/provision-subscriptions`

The application can also auto-provision the missing Stripe subscription price during the first paid subscription checkout if no price ID is stored yet.

Optional request body:

```json
{
  "currency": "EUR",
  "plusAmountCents": 499,
  "proAmountCents": 799,
  "plusTrialDays": 14,
  "proTrialDays": 14
}
```

What this does:

- creates the Stripe `Product` and recurring monthly `Price` for `plus`
- creates the Stripe `Product` and recurring monthly `Price` for `pro`
- stores the generated price IDs in the runtime settings database
- allows the checkout flow to use those stored values immediately

Diagnostics endpoint:

- `GET /api/admin/stripe/diagnostics`

Important notes:

- the provisioning endpoint is admin-only
- it reuses the currently stored Stripe price ID when it already matches the requested amount, currency, and interval
- it creates a new Stripe price only when the stored one is missing or no longer matches
- the subscription checkout flow can trigger the same provisioning logic automatically when a paid user starts checkout and no price ID exists yet
- Stripe Connect onboarding is separate from subscription catalog provisioning

## 15. Storage Configuration

The app supports object storage through Cloudflare R2 / S3-style configuration.

Configure:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `R2_ACCOUNT_ID`
- `R2_ACCESS_KEY_ID`
- `R2_SECRET_ACCESS_KEY`
- `R2_BUCKET_NAME`
- `R2_ENDPOINT`
- `R2_PUBLIC_URL`

If local development does not need real uploads, placeholder values may be used in some local modes, but production must use valid credentials.

## 16. Email Configuration

For password recovery and mail flows, set:

- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`

TLS mode:

- Port `587`: `MAIL_SMTP_STARTTLS_ENABLE=true`, `MAIL_SMTP_SSL_ENABLE=false`
- Port `465`: `MAIL_SMTP_STARTTLS_ENABLE=false`, `MAIL_SMTP_SSL_ENABLE=true`

## 17. Location and Geolocation Configuration

### FreeIPAPI

Used for IP-based geolocation support.

Configure:

- `GEOLOCATION_ENABLED`
- `GEOLOCATION_BASE_URL`

### LocationIQ

Used for address lookup / parsing.

Configure:

- `LOCATION_IQ_API_KEY`
- `LOCATION_IQ_BASE_URL`
- `LOCATION_IQ_COUNTRYCODES`

If address search is part of the production flow, `LOCATION_IQ_API_KEY` should be treated as required.

## 18. Seed Data

Seed data can be enabled with:

```text
SEEDING_ENABLED=true
```

Startup seeding loads users, listings, categories, reviews, messages, and supporting data.

Seed source:

- [mockdata.json](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/resources/mockdata.json)

## 19. CORS and Frontend/Backend URL Alignment

Make sure these align:

- backend port
- frontend port
- `FRONTEND_BASE_URL`
- allowed CORS origins in [application.properties](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/resources/application.properties)

For local development, current defaults include localhost origins for Angular and other dev ports.

## 20. Recommended Local Setup Checklist

For the fastest successful local setup:

1. Install Java, Maven, Node.js, PostgreSQL
2. Create DB `Vicinity24`
3. Copy `.env.template` to `.env`
4. Set at minimum:
   - `JWT_SECRET`
   - `ENCRYPTION_KEY`
   - `FRONTEND_BASE_URL`
   - Stripe keys if payments must work
   - `LOCATION_IQ_API_KEY` if address search must work
5. Start backend with:
   - `.\SCRIPTS\run-local-postgres.bat`
6. Start frontend with:
   - `cd frontend`
   - `npm install`
   - `npm start`
7. Verify:
   - backend health endpoint works
   - frontend loads on `http://localhost:4200/`
   - login works
   - create listing works
   - payment flow works
   - Stripe webhook can reach the backend when testing payment completion

## 21. Recommended Production Setup Checklist

1. Provision PostgreSQL
2. Configure all required environment variables
3. Set `FRONTEND_BASE_URL` to the real frontend URL
4. Set real Stripe live keys
5. Enable Stripe Connect
6. Configure Stripe webhook endpoint
7. Configure SMTP credentials
8. Configure LocationIQ API key
9. Configure R2/S3 storage credentials
10. Confirm TLS/HTTPS strategy
11. Confirm CORS origins
12. Disable seed data unless intentionally required
13. Test:
   - registration
   - login
   - password reset
   - listing creation
   - image upload
   - checkout
   - webhook processing
   - Connect onboarding
   - payout release after return

## 22. Troubleshooting

### Backend starts but frontend cannot call the API

Check:

- backend is running on `8081`
- frontend proxy is active
- browser console/network for CORS or TLS issues

### Stripe payments fail

Check:

- `STRIPE_SECRET_KEY`
- `STRIPE_PUBLIC_KEY`
- matching test/live mode
- webhook endpoint and `STRIPE_WEBHOOK_SECRET`

### Connect onboarding opens but payout still fails

Check:

- lender completed onboarding
- `detailsSubmitted=true`
- Stripe account requirements are complete
- platform balance is sufficient

### Address lookup fails

Check:

- `LOCATION_IQ_API_KEY`
- `LOCATION_IQ_BASE_URL`

### Password reset / email verification fails

Check:

- SMTP credentials
- TLS mode flags
- sender address configuration

## 23. Final Verification

A setup should be considered ready when the team can successfully do all of the following:

1. start backend
2. start frontend
3. authenticate a user
4. create a listing
5. upload images
6. create a Stripe payment
7. receive webhook events
8. onboard a lender into Stripe Connect
9. complete a borrow flow
10. complete a return flow and observe escrow release behavior
