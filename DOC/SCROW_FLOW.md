# Escrow Flow (Borrow â†’ Return â†’ Release)

This document explains the current escrow-like implementation for borrow checkout payments.

## Goal

- Borrower pays at checkout using Stripe.
- Funds are held by the platform while the item is borrowed.
- If the return completes without dispute, the lender is paid out and any deposit is refunded.
- If there is a dispute, funds are not released automatically.

The implementation uses Stripe Connect transfers to pay lenders after return.

## High-level overview

1. Borrower initiates checkout and pays with Stripe (PaymentIntent is captured immediately).
2. The backend stores a `Transaction` with status `ESCROWED` and the amount breakdown:
   - `rentalAmount` (what should be paid out to lender)
   - `serviceFeeAmount` (platform fee if applicable)
   - `depositAmount` (refundable deposit if applicable)
3. On successful return (no dispute), the backend:
   - creates a Stripe `Transfer` for `rentalAmount` to the lenderâ€™s connected account
   - creates a Stripe `Refund` for `depositAmount` back to the borrower (if deposit was used)
   - marks the transaction `RELEASED`
4. On dispute (manual or automatic), the backend marks the transaction `DISPUTED` and does not transfer/refund.

## Components

### Frontend (client)

- Checkout UI creates a PaymentIntent and confirms it in the browser.
  - [ListingDetail.tsx](file:///c:/Users/core101/Desktop/desk/shareit_client/share_it_client/pages/ListingDetail.tsx)
  - [CheckoutForm.tsx](file:///c:/Users/core101/Desktop/desk/shareit_client/share_it_client/components/CheckoutForm.tsx)

- Lender payout setup is exposed in the Payment Settings screen and opens Stripe Connect onboarding.
  - [PaymentSettings.tsx](file:///c:/Users/core101/Desktop/desk/shareit_client/share_it_client/components/PaymentSettings.tsx)
  - API calls: [mockApi.ts](file:///c:/Users/core101/Desktop/desk/shareit_client/share_it_client/services/mockApi.ts)

### Backend (server)

- PaymentIntent creation and Stripe webhooks:
  - [PaymentController.java](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/controller/PaymentController.java)

- Stripe integration and Connect helpers:
  - [StripePayment.java](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/payment/StripePayment.java)

- Borrow flow and transaction persistence:
  - [ListingService.java](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/service/ListingService.java)

- Return flow and escrow release:
  - [ReturnService.java](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/service/ReturnService.java)
  - [EscrowService.java](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/service/EscrowService.java)

## Configuration

- `app.frontend.baseUrl` is used to build Stripe Connect return/refresh URLs for onboarding links.
  - Defined in [application.properties](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/resources/application.properties)
  - Default: `http://localhost:3001`

## Stripe Dashboard requirements

This flow depends on Stripe Connect, webhooks, and correct key configuration.

### 1) Enable Stripe Connect

- Stripe Connect must be enabled on your Stripe account.
- The code creates Express connected accounts for lenders.
- In live mode, your platform Stripe account may need to complete verification before transfers/payouts work reliably.

#### Step-by-step: how Express connected accounts are created

1. In the Stripe Dashboard, enable **Stripe Connect** for your platform account.
2. In the app, the lender signs in and opens the payments/settings area.
3. The frontend calls `POST /api/payments/connect/onboard`.
   - Implemented in [PaymentController.java](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/controller/PaymentController.java#L146-L163)
4. The backend checks whether the user already has a stored Stripe Connect account ID in `User.stripeConnectAccountId`.
5. If the user has no connected account yet, the backend creates a new Stripe **Express** account using:
   - `stripePayment.createExpressConnectAccount(user.getEmail(), user.getName())`
   - Implemented in [StripePayment.java](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/payment/StripePayment.java#L273-L297)
6. The new Stripe account ID is saved on the user record as `stripeConnectAccountId`.
7. The backend builds a Stripe onboarding link with:
   - `refreshUrl = <app.frontend.baseUrl>/settings?tab=payments&connect=refresh`
   - `returnUrl = <app.frontend.baseUrl>/settings?tab=payments&connect=return`
8. The backend returns the onboarding URL to the frontend.
9. The lender is redirected to Stripe and completes the Express onboarding flow.
10. The frontend or backend can then call `GET /api/payments/connect/status` to verify:
   - whether account details were submitted
   - whether charges/payouts are enabled
   - whether additional requirements are still due
11. When escrow release happens, the backend checks the saved `stripeConnectAccountId` and verifies `detailsSubmitted` before creating transfers to the lender.
   - If no account exists, release fails with `missing_stripe_connect_account`
   - If onboarding is incomplete, release fails with `connect_onboarding_incomplete`
   - Release logic is in [EscrowService.java](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/service/EscrowService.java#L161-L195)

#### Application configuration required for Connect onboarding

These values must be configured in the application for Stripe Connect onboarding to work:

- `STRIPE_SECRET_KEY`
  - Required by the backend to call Stripe APIs, create Express accounts, create onboarding links, and later create transfers/refunds.
  - Defined in [application.properties](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/resources/application.properties#L158)
- `STRIPE_WEBHOOK_SECRET`
  - Required for Stripe webhook verification.
  - Not strictly required just to create onboarding links, but required for the full payment/escrow flow.
  - Defined in [application.properties](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/resources/application.properties#L160)
- `STRIPE_PUBLIC_KEY`
  - Required by the frontend for Stripe client-side payment flows.
  - Defined in [application.properties](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/resources/application.properties#L159)
- `app.frontend.baseUrl`
  - Required so the backend can build valid Stripe Connect `refreshUrl` and `returnUrl`.
  - Used in [PaymentController.java](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/controller/PaymentController.java#L60-L61) and [PaymentController.java](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/controller/PaymentController.java#L156-L158)
  - Example local value: `https://localhost:4200`

Optional but commonly needed for the overall Stripe integration:

- `subscription.plus.stripe_price_id`
- `subscription.pro.stripe_price_id`
  - These are used for subscription checkout, not for Connect account onboarding itself.
  - They no longer need to be supplied manually through environment variables in the default setup.
  - They can be created and stored at runtime through the admin endpoint:
    - `POST /api/admin/stripe/provision-subscriptions`
  - They can also be auto-created during the first paid subscription checkout when no stored price ID exists yet.
  - You can inspect the currently active Stripe account, mode, and stored subscription price IDs through:
    - `GET /api/admin/stripe/diagnostics`

#### Stripe-side requirements to verify

- Your Stripe platform account must have Connect enabled.
- The platform must use keys from the same environment as the dashboard you are testing in:
  - `sk_test_...` with Stripe test mode
  - `sk_live_...` with Stripe live mode
- The lender must complete the Stripe Express onboarding screens fully enough for `detailsSubmitted` to become `true`.
- For actual payouts/transfers in live mode, Stripe may also require:
  - business verification
  - identity details
  - bank account / payout destination setup
  - additional information listed under account requirements

### 2) Configure webhooks

You must create a webhook endpoint in the Stripe Dashboard pointing to:

- `POST https://<your-backend>/api/payments/webhook`

At minimum, subscribe to:

- `payment_intent.succeeded` (used to finalize/record escrowed borrow transactions)

If you also use subscription billing features, additionally subscribe to:

- `checkout.session.completed`
- `invoice.payment_succeeded`
- `customer.subscription.created`
- `customer.subscription.updated`
- `customer.subscription.deleted`

Then copy the webhook signing secret into:

- `STRIPE_WEBHOOK_SECRET`

### 3) Set API keys (test vs live)

Make sure keys match the environment you are testing:

- Backend: `STRIPE_SECRET_KEY`
- Frontend: `STRIPE_PUBLIC_KEY`

Test mode and live mode have separate keys, webhook endpoints, and connected accounts.

### 4) Payout readiness for lenders

For transfers to succeed on return:

- Each lender must complete Stripe Express onboarding (KYC).
- If onboarding is incomplete, escrow release will mark the transaction `RELEASE_FAILED` with `connect_onboarding_incomplete`.
- `app.frontend.baseUrl` must be a reachable URL in production, otherwise Stripe onboarding return/refresh links will redirect incorrectly.

### 5) Operational notes

- Transfers and refunds are funded from the platform Stripe balance. If the platform balance is insufficient, the release step may fail and the transaction will remain `RELEASE_FAILED` until retried.

## Payment flow (Borrow checkout)

### 1) Create PaymentIntent

Backend endpoint:

- `POST /api/payments/create-payment-intent`

Implementation:

- Calculates total amount based on listing hourly rate, duration and borrower path.
- Calls Stripe to create a PaymentIntent.
- Metadata includes `listingId`, `borrowerId`, `durationHours`, `borrowerPath`.

Code:

- [PaymentController.createPaymentIntent](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/controller/PaymentController.java#L57-L115)
- [StripePayment.createPaymentIntent](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/payment/StripePayment.java#L93-L116)

Important behavior:

- PaymentIntents are created with automatic payment methods enabled.
- The PaymentIntent is captured immediately when confirmed successfully.

### 2) Confirm PaymentIntent in the browser

Frontend:

- Uses Stripe Elements `PaymentElement`.
- Calls `stripe.confirmPayment` with `redirect: 'if_required'`.
- On `paymentIntent.status === "succeeded"`, it calls the backend borrow endpoint with `paymentToken = paymentIntent.id`.

Code:

- [CheckoutForm.handleSubmit](file:///c:/Users/core101/Desktop/desk/shareit_client/share_it_client/components/CheckoutForm.tsx#L18-L58)
- [ListingDetail.handleStripeSuccess](file:///c:/Users/core101/Desktop/desk/shareit_client/share_it_client/pages/ListingDetail.tsx#L192-L224)

### 3) Persist escrowed transaction

Backend:

- `ListingService.borrow(...)` verifies Stripe payment success via `PaymentManager.processPayment(...)`.
- The transaction is stored with:
  - `status="ESCROWED"`
  - `paymentToken=PaymentIntentId`
  - `rentalAmount/serviceFeeAmount/depositAmount`

Code:

- [ListingService.borrow](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/service/ListingService.java#L205-L312)
- Model fields: [Transaction.java](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/model/Transaction.java)

### 4) Webhook finalization (safety)

The backend also listens to Stripe webhooks:

- `POST /api/payments/webhook`
- On `payment_intent.succeeded`, it calls `ListingService.completeTransaction(...)` if that payment token is not already stored.
- This path also writes the transaction as `ESCROWED`.

Code:

- [PaymentController.webhook](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/controller/PaymentController.java#L117-L145)
- [ListingService.completeTransaction](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/service/ListingService.java#L467-L506)

## Stripe Connect onboarding (Lender payouts)

To pay out lenders, each lender needs a Stripe Connect account.

Backend endpoints:

- `POST /api/payments/connect/onboard`
  - Creates an Express Connect account if missing.
  - Returns an onboarding URL.
- `GET /api/payments/connect/status`
  - Returns if an account exists and whether onboarding details are submitted.

Code:

- [PaymentController connect endpoints](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/controller/PaymentController.java)
- Account creation/link:
  - [StripePayment.createExpressConnectAccount](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/payment/StripePayment.java)
  - [StripePayment.createAccountOnboardingLink](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/payment/StripePayment.java)

User storage:

- `User.stripeConnectAccountId`
- `User.stripeConnectDetailsSubmitted`

Code:

- [User.java](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/model/User.java)

## Return flow (Release vs Dispute)

### 1) Dispute

If a dispute is started (or return session expires), the system marks the transaction as disputed and does not release funds.

Code:

- [ReturnService.initiateDispute](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/service/ReturnService.java)
- [EscrowService.markDisputed](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/service/EscrowService.java)

Transaction status:

- `DISPUTED`

### 2) Successful return

When both parties confirm the return (scan or manual with concierge witness), `ReturnService` sets:

- `ReturnSession.status = COMPLETED`
- `Listing.status = AVAILABLE`
- `Listing.borrower = null`

Then escrow release runs:

- Finds the latest `Transaction` for that listing with status `ESCROWED`.
- Verifies lender has a Stripe Connect account and onboarding is completed (`detailsSubmitted`).
- Creates:
  - a `Transfer` for `rentalAmount` to lender
  - a `Refund` for `depositAmount` back to borrower (if deposit was used)
- Marks transaction `RELEASED` and stores `stripeTransferId` / `stripeRefundId`.

Code:

- [ReturnService.checkAndCompleteSession](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/service/ReturnService.java#L168-L191)
- [EscrowService.releaseOnSuccessfulReturn](file:///c:/Users/core101/Desktop/desk/shareit_back/src/main/java/com/vicinity24/api/service/EscrowService.java#L44-L134)

Transaction statuses:

- `ESCROWED` â†’ `RELEASED`
- If payout/refund fails (e.g. lender not onboarded): `ESCROWED` â†’ `RELEASE_FAILED`

## Notes and current limitations

- PaymentIntents are captured immediately; this is not a card â€œauthorization holdâ€. The â€œescrowâ€ is implemented by delaying the payout transfer to the lender.
- `serviceFeeAmount` is currently kept by the platform by simply transferring only `rentalAmount` to the lender.
- There is no admin dispute resolution workflow yet (e.g., partial payout, partial refund, evidence upload, deadlines).
- Database schema migrations are handled via `spring.jpa.hibernate.ddl-auto=update`. New fields will be added automatically by Hibernate in dev environments.

## Multi-Tenant Environment Note

This project supports static database-per-tenant routing in the backend configuration layer.

- Main env vars: `SETTING_USE_DEFAULT_DATABASE`, `TENANT_HEADER_NAME`, `TENANT_DEFAULT_ID`, `TENANT_DEFAULT_DB_URL`, `TENANT_DEFAULT_DB_USERNAME`, `TENANT_DEFAULT_DB_PASSWORD`, `TENANT_DEFAULT_DB_DRIVER`
- Optional extra tenant examples: `TENANT_A_*`, `TENANT_B_*`
- Legacy `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_DRIVER` variables are not required in the multi-tenant setup
- Active tenant ids are defined by the keys under `tenants.config.*` in `src/main/resources/application.properties`; the current sample configuration uses `default`, `vicinity24_tenant_a`, and `vicinity24_tenant_b`
- `SETTING_USE_DEFAULT_DATABASE=true` uses the default database only when the tenant header is missing; a valid tenant header still routes to the matching tenant database
- Startup bootstrap initializes or upgrades schema and seed data for the default database and every configured tenant database
- Full setup details live in `DOC/configuration-guide.md` and `.env.template`


