# API Contract (Vicinity24)

This document describes the REST API exposed by the Vicinity24 backend.

## Base URL

- API prefix: `/api/v1`
- Example (local): `http://localhost:8081/api/v1` (HTTPS optional)

## Authentication

- Auth scheme: Bearer JWT
- Header:
  - `Authorization: Bearer <token>`
- Unless stated otherwise, endpoints require a valid JWT.

### Token Response Shape

Most login endpoints return a `TokenResponse`:

```json
{
  "token": "eyJhbGciOi...",
  "user": {
    "id": "uuid",
    "name": "string",
    "email": "string",
    "role": "ADMIN|MEMBER|LENDER|BORROWER|...",
    "avatarUrl": "string",
    "trustScore": 50,
    "vouchCount": 0,
    "verificationStatus": "UNVERIFIED|VERIFIED|...",
    "location": { "x": 0, "y": 0 },
    "joinedDate": "YYYY-MM-DD",
    "phone": "string",
    "address": "string",
    "twoFactorEnabled": false,
    "profileVisible": true,
    "showRatings": true
  }
}
```

If MFA/2FA is required, login may return:

```json
{
  "mfaRequired": true,
  "token": "temporary-token"
}
```

Then call `verify-2fa-login` with `Authorization: Bearer <temporary-token>`.

## Error Handling (Observed)

The API returns a mix of error shapes depending on the endpoint:

- JSON objects: `{ "error": "some_code_or_message" }`
- Plain strings (some 2FA endpoints)
- Spring `ResponseStatusException` messages

Clients should treat non-2xx responses as failures and prefer the `error` field when present.

## Public Endpoints

### Health

- `GET /api/health`
  - Response:
    - `200 OK` `{ "status": "UP" }`

### Public Config

- `GET /api/config/public`
  - Response includes:
    - `stripePublicKey: string`
    - `connect: object` (feature flags / UI config)
    - `home: object`
    - `borrowing: object`
    - `subscription: { plusTrialDays, plusMonthlyAmountCents, currency }` (supports runtime overrides)

- `GET /api/config/settings`
  - Returns effective `SettingsProperties` (feature flags / sections) after applying runtime overrides.

### Listings (Read)

- `GET /api/listings/`
  - Query params:
    - `search?: string`
    - `category?: string`
    - `type?: string`
    - `minPrice?: number`
    - `page: number` (default `0`)
    - `size: number` (default `10`)
  - Response: Spring `Page<ListingDTO>`

- `GET /api/listings/{id}`
  - Response: `ListingDTO`

- `POST /api/listings/evaluate`
  - Body: `EvaluateItemRequest` (recommendation engine)
  - Response: `RecommendationResult`

### Insurance (when enabled)

Only active when `settings.insurance.enabled=true`.

- `GET /api/insurance/types`
  - Response: `InsuranceTypeInfoResponse[]`

- `POST /api/insurance/quote`
  - Body: `InsuranceQuoteRequest`
  - Response: `InsuranceQuoteResponse`

- `POST /api/insurance/purchase`
  - Body: `{ "quoteId": "string" }`
  - Response: `201 Created` `InsurancePurchaseResponse`

### Categories

- `GET /api/categories/`
  - Response: `CategoryDTO[]`

### Pickup Locations

- `GET /api/pickup-locations/`
  - Response: `PickupLocationDTO[]`

### Seed (dev/demo)

- `GET /api/seed`
  - Response: `200 OK` text, or `500` text starting with `Failed...`

## Authentication Endpoints

### User Auth (Lender / Borrower / Member)

- `POST /api/auth/register`
  - Body (`RegisterRequest`):
    - `name, email, password, phone, address, avatarUrl, lat, lng`
  - Response: `UserDTO`

- `POST /api/auth/login`
  - Body (`LoginRequest`): `{ "email": "string", "password": "string" }`
  - Response: `TokenResponse` or `{ mfaRequired, token }`

- `POST /api/auth/verify-2fa-login`
  - Auth: `Authorization: Bearer <temporary-token>`
  - Body: `{ "code": "123456" }`
  - Response: `TokenResponse`

- `POST /api/auth/forgot-password`
  - Body: `ForgotPasswordRequest`
  - Response: `200 OK`

- `POST /api/auth/verify-reset-code`
  - Body: `VerifyResetCodeRequest`
  - Response: `{ "valid": true, "token": "reset-token" }`

- `POST /api/auth/reset-password`
  - Body: `ResetPasswordRequest`
  - Response: `200 OK`

### Admin Auth (Dedicated)

Base path: `/api/admin/auth`

- `POST /api/admin/auth/login`
  - Body (`LoginRequest`): `{ "email": "string", "password": "string" }`
  - Response: `TokenResponse` (only if user role is ADMIN) or `{ mfaRequired, token }`

- `POST /api/admin/auth/verify-2fa-login`
  - Auth: `Authorization: Bearer <temporary-token>`
  - Body: `{ "code": "123456" }`
  - Response: `TokenResponse`

- `POST /api/admin/auth/register`
  - Body:
    - `name: string`
    - `email: string`
    - `password: string`
    - `signupSecret: string`
  - Notes:
    - Requires `security.admin.signup.secret` to be configured and to match `signupSecret`.
  - Response: `TokenResponse` (new admin user)

### Partner Auth (Dedicated)

Base path: `/api/partner/auth`

- `POST /api/partner/auth/login`
  - Body (`LoginRequest`): `{ "email": "string", "password": "string" }`
  - Notes:
    - Only succeeds if the user is linked to at least one Partner via `PartnerAdmin`.
  - Response: `TokenResponse` or `{ mfaRequired, token }`

- `POST /api/partner/auth/verify-2fa-login`
  - Auth: `Authorization: Bearer <temporary-token>`
  - Body: `{ "code": "123456" }`
  - Response: `TokenResponse`

- `POST /api/partner/auth/register`
  - Body:
    - `userName: string`
    - `userEmail: string`
    - `userPassword: string`
    - `partner: PartnerRegistrationRequest`
  - Side effects:
    - Creates a `User` (role MEMBER)
    - Creates a `Partner` (status PENDING)
    - Creates a `PartnerAdmin` linking the user to the partner
  - Response: `TokenResponse`

## User / Profile

Base path: `/api/users`

- `GET /api/users/me`
  - Response: `UserDTO`

- `PATCH /api/users/me`
  - Body: `UpdateProfileRequest`
  - Response: `UserDTO`

- `PUT /api/users/me/password`
  - Body: `ChangePasswordRequest` `{ oldPassword, newPassword }`
  - Response: `200 OK`

- `DELETE /api/users/me`
  - Response: `{ "status": "deleted" }`

- `POST /api/users/me/avatar` (multipart)
  - Form field: `file`
  - Response: `UserDTO`

- `GET /api/users/contacts`
  - Response: `UserSummaryDTO[]`

- `GET /api/users/me/activity`
  - Response: `ActivityDTO[]`

- `GET /api/users/online`
  - Response: `UUID[]`

- `POST /api/users/{id}/vouch`
  - Response: `UserDTO`

- `POST /api/users/verification-request`
  - Body: `{ "phone": "string", "address": "string" }`
  - Response: `UserDTO`

### User 2FA Management

Base path: `/api/users/2fa`

- `POST /api/users/2fa/setup`
  - Response: `{ "secret": "string", "qrCode": "data:image/png;base64,..." }`

- `POST /api/users/2fa/verify`
  - Body: `{ "code": "123456" }`
  - Response: `{ "success": true }`

- `POST /api/users/2fa/disable`
  - Response: `{ "success": true }`

## Listings

Base path: `/api/listings`

### ListingDTO

`ListingDTO` includes (common fields):

```json
{
  "id": "uuid",
  "ownerId": "uuid|null",
  "partnerId": "uuid|null",
  "partnerName": "string|null",
  "borrowerId": "uuid|null",
  "title": "string",
  "description": "string",
  "type": "GOODS|SELL|GIVE|...",
  "category": "string",
  "imageUrl": "string",
  "gallery": ["string"],
  "hourlyRate": 0,
  "autoApprove": false,
  "insuranceRequired": false,
  "status": "AVAILABLE|PENDING|APPROVED|READY_FOR_PICKUP|WAITING_FOR_RETURN|BORROWED|...",
  "pickupLocation": { "id": "uuid", "name": "string", "address": "string", "location": { "x": 0, "y": 0 } },
  "pickupLocationCustom": "string|null",
  "pickupLocationStreet": "string|null",
  "pickupLocationHouseNumber": "string|null",
  "pickupLocationCity": "string|null",
  "pickupLocationZip": "string|null"
}
```

### Create / Update

- `POST /api/listings/`
- `PUT /api/listings/{id}`

Body (`CreateListingRequest`):

```json
{
  "title": "string",
  "description": "string",
  "category": "string",
  "type": "GOODS|SELL|GIVE|...",
  "hourlyRate": 10.5,
  "imageUrl": "string",
  "gallery": ["string"],
  "autoApprove": false,
  "insuranceRequired": false,
  "x": 0,
  "y": 0,
  "pickupLocationId": "uuid|null",
  "pickupLocationCustom": "string|null",
  "pickupLocationStreet": "string|null",
  "pickupLocationHouseNumber": "string|null",
  "pickupLocationCity": "string|null",
  "pickupLocationZip": "string|null"
}
```

- `DELETE /api/listings/{id}`
  - Response: `{ "status": "deleted" }`

### Borrow / Approve / Deny

- `POST /api/listings/{id}/borrow`
  - Body (`BorrowRequest`):
    - `paymentMethod?: string`
    - `paymentToken?: string`
    - `durationHours: number`
    - `borrowerPath: string` (example: `VERIFIED|DEPOSIT|FEE`)
  - Notes:
    - Partner listings (`partnerId != null`) are offline: escrow/payment is bypassed and request stays pending for partner approval.

- `POST /api/listings/{id}/approve`
  - Owner action: approves a borrow request
  - For LEND listings, this moves status to `APPROVED` (borrower will be notified once the item is ready).
  - Response: `ListingDTO`

- `POST /api/listings/{id}/deny`
  - Owner action: denies a borrow request
  - Response: `ListingDTO`

- `POST /api/listings/{id}/ready-for-pickup`
  - Owner action: marks an approved request as ready for pickup
  - Side effects:
    - Sends an in-app message to the borrower containing pickup details
    - Sends an email to the borrower
  - Response: `ListingDTO` (status becomes `READY_FOR_PICKUP`)

- `POST /api/listings/{id}/picked-up`
  - Owner or borrower action: confirms the item has been picked up
  - Response: `ListingDTO` (status becomes `WAITING_FOR_RETURN`)

- `POST /api/listings/{id}/return`
  - Response: `ListingDTO`

### Moderation / Safety

- `POST /api/listings/{id}/report`
  - Body: `{ "reason": "string", "details": "string" }`
  - Response: `{ "status": "reported" }`

- `POST /api/listings/{id}/dismiss`
  - Response: `{ "status": "dismissed" }`

- `POST /api/listings/{id}/block` (admin only)
  - Response: `ListingDTO`

## Returns (QR / Manual / Dispute)

Base path: `/api/listings/{id}/return`

- `POST /api/listings/{id}/return/initiate`
  - Response: `ReturnSessionResponse`

- `GET /api/listings/{id}/return`
  - Response: `ReturnSessionResponse`

- `POST /api/listings/{id}/return/scan`
  - Body: `{ "qrCode": "string" }`
  - Response: `ReturnSessionResponse`

- `POST /api/listings/{id}/return/manual`
  - Body: `{ "itemNumber": "string", "conciergeWitnessId": "string" }`
  - Response: `ReturnSessionResponse`

- `POST /api/listings/{id}/return/dispute`
  - Body: `{ "reason": "string", "photoUrl": "string", "conciergeWitnessId": "string" }`
  - Response: `ReturnSessionResponse`

## Payments (Stripe + Escrow)

Base path: `/api/payments`

### Payment Methods

- `GET /api/payments/methods`
  - Response: Stripe `PaymentMethod[]` (raw Stripe objects)

- `POST /api/payments/methods`
  - Body: `{ "paymentMethodId": "pm_..." }`
  - Response: `{ "status": "ok" }`

- `DELETE /api/payments/methods/{id}`
  - Response: `{ "status": "deleted" }`

### Stripe Connect (for payouts)

- `POST /api/payments/connect/onboard`
  - Response: `{ "accountId": "acct_...", "url": "https://connect.stripe.com/..." }`

- `GET /api/payments/connect/status`
  - Response (example): `{ "connected": true, "detailsSubmitted": true, "chargesEnabled": true, ... }`

### Transactions

- `GET /api/payments/transactions`
  - Response: `PaymentTransactionDTO[]`

- `GET /api/payments/transactions/{id}/invoice`
  - Response: `{ "url": "https://..." }`

- `POST /api/payments/release/retry`
  - Response: `{ "attempted": 2 }`

### Card Checkout (PaymentIntent)

- `POST /api/payments/create-payment-intent`
  - Body (observed):
    - `listingId: string`
    - `borrowerPath?: string`
    - `paymentMethodId?: string`
    - `durationHours?: number|string`
  - Response: `{ "clientSecret": "pi_..._secret_...", "amount": 10.0, "currency": "usd" }`
  - Notes:
    - Rejects partner listings: `"partner_listing_offline_payment"`
    - Rejects GIVE/free listings: `"free_listing_no_payment_required"`

### Stripe Webhook

- `POST /api/payments/webhook`
  - Header: `Stripe-Signature: ...`
  - Used for:
    - `payment_intent.succeeded` (finalizes listing transaction)
    - `checkout.session.completed` + subscription events
    - `invoice.payment_succeeded`

## Subscriptions

Base path: `/api/subscriptions`

- `GET /api/subscriptions/config`
  - Response: `{ "starter": true, "plus": true, "pro": true }`

- `POST /api/subscriptions/send-code`
  - Body: `SendSubscriptionCodeRequest` (optional)
  - Response: `{ "status": "ok" }`

- `POST /api/subscriptions/verify-code`
  - Body: `VerifySubscriptionCodeRequest` `{ "code": "string" }`
  - Response: `{ "status": "verified" }`

- `POST /api/subscriptions/starter`
  - Response: `{ "status": "active", "plan": "starter" }`

- `POST /api/subscriptions/create-checkout-session`
  - Body: `{ "planType": "plus|pro", "returnPath": "/dashboard" }` (optional)
  - Response: `{ "sessionId": "cs_...", "url": "https://checkout.stripe.com/..." }`

- `POST /api/subscriptions/sync-session`
  - Body: `{ "sessionId": "cs_..." }`
  - Response: `{ "status": "synced", "stripeSubscriptionId": "...", "stripeStatus": "..." }`

- `POST /api/subscriptions/cancel`
  - Response: `{ "status": "canceled" }`

- `POST /api/subscriptions/admin/fix-status`
  - Response: `{ "status": "fixed", "message": "..." }`

- `GET /api/subscriptions/me`
  - Response: `200 OK SubscriptionDTO` or `204 No Content`

- `GET /api/subscriptions/invoices`
  - Response: `SubscriptionInvoiceDTO[]`

- `POST /api/subscriptions/upgrade/preview`
  - Body: `{ "newPlan": "plus|pro" }`
  - Response: `SubscriptionUpgradePreviewDTO`

- `POST /api/subscriptions/upgrade/confirm`
  - Body: `{ "newPlan": "plus|pro" }`
  - Response: `SubscriptionDTO`

## Messaging

### REST Messaging

Base path: `/api/messages`

- `GET /api/messages/conversations`
  - Response: `UserSummaryDTO[]`

- `GET /api/messages/with/{userId}`
  - Response: `MessageDTO[]`

- `POST /api/messages/`
  - Body:
    - `receiverEmail?: string`
    - `receiverId?: string` (UUID or email)
    - `content?: string`
    - `imageUrl?: string`
  - Response: `MessageDTO`

- `GET /api/messages/inbox`
  - Response: `MessageDTO[]`

- `GET /api/messages/outbox`
  - Response: `MessageDTO[]`

- `DELETE /api/messages/{id}`
  - Response: `{ "status": "deleted" }`

### WebSocket (STOMP)

- Endpoint: `GET /ws` (SockJS enabled)
- App destination prefix: `/app`
- Broker destinations: `/topic`, `/queue`

Message mappings:
- Send message: `/app/chat.send` with `ChatMessageDTO`
  - Server broadcasts to:
    - `/topic/messages.{receiverId}`
    - `/topic/messages.{senderId}`

- Presence: `/app/presence.online` with body `userId` (UUID string)
  - Server broadcasts to: `/topic/presence`

## Partner APIs

Base path: `/api/partner`

- `GET /api/partner/my-partners`
  - Response: `PartnerDTO[]`

- `POST /api/partner/register`
  - Body: `PartnerRegistrationRequest`
  - Response: `PartnerDTO`

- `POST /api/partner/listings`
  - Body: `PartnerCreateListingRequest`
  - Response: `ListingDTO` (partner listing: `partnerId != null`, `ownerId` may be null)

- `GET /api/partner/listings`
  - Response: `ListingDTO[]`

- `PUT /api/partner/listings/{listingId}`
  - Body: `PartnerCreateListingRequest`
  - Response: `ListingDTO`

- `DELETE /api/partner/listings/{listingId}`
  - Response: `{ "status": "deleted" }`

- `GET /api/partner/requests`
  - Response: `PartnerBorrowRequestDTO[]`

- `POST /api/partner/requests/{listingId}/approve`
  - Response: `ListingDTO`

- `POST /api/partner/requests/{listingId}/reject`
  - Response: `ListingDTO`

- `GET /api/partner/settings?partnerId={uuid?}`
  - Response: `PartnerSettingsDTO`

- `PUT /api/partner/settings?partnerId={uuid?}`
  - Body: `PartnerSettingsDTO`
  - Response: `PartnerSettingsDTO`

## Reviews

Base path: `/api/reviews`

- `GET /api/reviews/user/{userId}`
  - Response: `ReviewDTO[]`

- `POST /api/reviews/`
  - Body: `{ "targetUserId": "uuid", "listingId": "uuid", "rating": 1-5, "comment": "string" }`
  - Response: `ReviewDTO`

### Review Invites (Token-based)

Base path: `/api/reviews/invite`

- `GET /api/reviews/invite/{token}`
  - Response: `ReviewInviteDTO`

- `POST /api/reviews/invite/{token}`
  - Auth: optional; if provided must match invite reviewer
  - Body: `{ "rating": 1-5, "comment": "string" }`
  - Response: `ReviewDTO`

## Devices (Trusted devices)

Base path: `/api/devices`

- `GET /api/devices`
  - Response: `Device[]`

- `DELETE /api/devices/{id}`
  - Response: `200 OK`

## AI (Demand Nudge)

Base path: `/api/ai`

- `POST /api/ai/demand-check`
  - Body: `{ "itemName": "string" }`
  - Response example:
    - `{ "nudgeType": "give_to_lend", "message": "...", "potentialEarnings": "60", "demandScore": 0.85 }`

## Admin APIs

All admin APIs require `ROLE_ADMIN` (or equivalent).

### Admin Management

Base path: `/api/admin`

- `GET /api/admin/summary`
- `GET /api/admin/users?q=&page=&size=`
- `PATCH /api/admin/users/{id}/status` body `{ "status": "ACTIVE|SUSPENDED|..." }`
- `DELETE /api/admin/users/{id}`
- `GET /api/admin/listings?status=&page=&size=` (excludes partner-owned listings; use partner endpoints below)
- `POST /api/admin/listings/{listingId}/block` body `{ "blocked": true|false }`
- `DELETE /api/admin/listings/{listingId}`
- `GET /api/admin/transactions?status=&page=&size=`
- `DELETE /api/admin/transactions/{id}`
- `POST /api/admin/transactions/{id}/retry-release`
- `GET /api/admin/subscriptions?status=&page=&size=`
- `GET /api/admin/disputes?page=&size=`
- `POST /api/admin/disputes/{listingId}/cancel-refund` body `{ "reason": "string" }`
- `POST /api/admin/returns/{listingId}/accept` body `{ "reason": "string" }`
- `POST /api/admin/returns/{listingId}/reopen` body `{ "minutes": 15 }`

### Runtime App Settings (Admin)

Base path: `/api/admin/app-settings`

- `GET /api/admin/app-settings`
  - Response:
    ```json
    {
      "sections": [
        {
          "id": "enable",
          "title": "Enable",
          "items": [
            {
              "key": "settings.enable.subscription",
              "type": "boolean",
              "value": true,
              "defaultValue": true,
              "overridden": false
            }
          ]
        }
      ]
    }
    ```

- `PUT /api/admin/app-settings`
  - Body:
    ```json
    { "updates": [ { "key": "settings.enable.subscription", "value": false } ] }
    ```
  - Notes:
    - Send `value: null` to remove an override and revert to default.

### Reports (legacy/simple)

- `GET /api/admin/reports`
  - Response: `ReportDTO[]`

- `DELETE /api/admin/reports/{id}`
  - Response: `{ "status": "deleted" }`

## Multi-Tenant Environment Note

This project supports static database-per-tenant routing in the backend configuration layer.

- Main env vars: `SETTING_USE_DEFAULT_DATABASE`, `TENANT_HEADER_NAME`, `TENANT_DEFAULT_ID`, `TENANT_DEFAULT_DB_URL`, `TENANT_DEFAULT_DB_USERNAME`, `TENANT_DEFAULT_DB_PASSWORD`, `TENANT_DEFAULT_DB_DRIVER`
- Optional extra tenant examples: `TENANT_A_*`, `TENANT_B_*`
- Legacy `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_DRIVER` variables are not required in the multi-tenant setup
- Active tenant ids are defined by the keys under `tenants.config.*` in `src/main/resources/application.properties`; the current sample configuration uses `default`, `vicinity24_tenant_a`, and `vicinity24_tenant_b`
- `SETTING_USE_DEFAULT_DATABASE=true` uses the default database only when the tenant header is missing; a valid tenant header still routes to the matching tenant database
- Startup bootstrap initializes or upgrades schema and seed data for the default database and every configured tenant database
- Full setup details live in `DOC/configuration-guide.md` and `.env.template`


