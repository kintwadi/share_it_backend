# Execution Plan: NeighborShare / Circula Implementation

## 1. Backend Implementation

### 1.1 Database Schema Updates
- **Transaction Entity**: Add `borrowerPath` (ENUM: DEPOSIT, VERIFIED, FEE, NA) to track revenue streams.
- **AiNudge Entity**: Create `AiNudge` entity to track nudges (dummy implementation for now).
- **Trust Score**: Ensure `User` entity has `trustScore` and `trustTier`.

### 1.2 Service Layer Logic
- **TrustScoreService**: Implement `calculateTrustScoreChange` logic based on `project.md` (Bonuses/Penalties).
- **SubscriptionService**: Implement `upgradeSubscription` with proration logic (German compliance Â§312g BGB).
    - Calculate credit for unused days.
    - Calculate charge for new tier.
    - Return net immediate charge.
- **ListingService**: Ensure "Intent-First" support (GIVE, SELL, LEND).
- **AiNudgeService**: Create dummy service to generate nudges (e.g., "High demand detected").

### 1.3 API Endpoints
- `POST /api/transactions`: Update to accept `borrowerPath`.
- `POST /api/subscriptions/upgrade`: New endpoint for prorated upgrades.
- `POST /api/trust-score/update`: Internal/System endpoint to update score after transaction.
- `GET /api/ai/nudge`: Dummy endpoint to check for nudges.

## 2. Frontend Implementation

### 2.1 New Item Page (Intent-First)
- Redesign `NewItem.tsx` to follow "Intent Selection" flow:
    1.  Photo Upload (existing)
    2.  Intent Selection: "Give Away", "Sell", "Lend" (New Step).
    3.  AI Nudge (Mock): Show "High Demand" alert if item is "Give Away" (randomly or fixed).

### 2.2 Listing Detail (Borrow Flow)
- Implement "Path Selection Modal" in `ListingDetail.tsx` when clicking "Rent" or "Borrow".
    - **Path 1**: Deposit (â‚¬50 refundable).
    - **Path 2**: Verified Subscription (Free Trial / â‚¬2.99).
    - **Path 3**: One-time Fee (8%).
- Default to **Path 2** (Recommended).

### 2.3 Subscription & Upgrade
- Update `SubscriptionCheckout.tsx` or create `UpgradeSubscription.tsx`.
- Show proration breakdown: "Credit for unused X", "Charge for Y", "Net Pay".

### 2.4 Trust Score & Profile
- Visualize Trust Score on `Dashboard.tsx` or `Profile`.
- Show "Trust Tier" benefits.

## 3. Privacy & Compliance
- Ensure "Partner Location" (Concierge, Bakery) is highlighted in handoff flow.
- Add "German Legal Compliance" notices (Impressum, GDPR) in footer or checkout.

## 4. Testing & Verification
- Verify Trust Score updates.
- Verify Prorated Calculation.
- Verify Path Selection records correctly in backend.

## Multi-Tenant Environment Note

This project supports static database-per-tenant routing in the backend configuration layer.

- Main env vars: `SETTING_USE_DEFAULT_DATABASE`, `TENANT_HEADER_NAME`, `TENANT_DEFAULT_ID`, `TENANT_DEFAULT_DB_URL`, `TENANT_DEFAULT_DB_USERNAME`, `TENANT_DEFAULT_DB_PASSWORD`, `TENANT_DEFAULT_DB_DRIVER`
- Optional extra tenant examples: `TENANT_A_*`, `TENANT_B_*`
- Active tenant ids are defined by the keys under `tenants.config.*` in `src/main/resources/application.properties`; the current sample configuration uses `default`, `vicinity24_tenant_a`, and `vicinity24_tenant_b`
- `SETTING_USE_DEFAULT_DATABASE=true` uses the default database only when the tenant header is missing; a valid tenant header still routes to the matching tenant database
- Startup bootstrap initializes or upgrades schema and seed data for the default database and every configured tenant database
- Full setup details live in `DOC/configuration-guide.md` and `.env.template`


