# User Guide (Frontend Usage)

This guide describes how to use the Vicinity24 frontend, including the dedicated admin and partner connect flows.

## Access Paths

The frontend uses these key routes:

- User connect (regular users): `/connect`
- Admin connect (dedicated): `/connect/admin`
- Partner connect (dedicated): `/connect/partner`
- User dashboard: `/dashboard`
- Admin panel: `/admin`
- Partner area: `/partner/...` (partner dashboard and tools)
- Subscription: `/subscription`
- Listing details: `/listing/:id`
- Messages: `/mailbox`

## Roles and What They Mean

- Regular users (neighbors) are the default account type created from `/connect`.
  - They can lend and borrow depending on app rules, subscription, and listing settings.
- Admin users must use `/connect/admin` to log in.
  - Admin signup requires an admin signup secret configured in the backend.
- Partner admins must use `/connect/partner`.
  - Partner admins manage partner-owned listings and approve/reject partner borrow requests from the partner dashboard.

## Signing Up and Logging In

### Regular User (Borrower / Lender)

1. Go to `/connect`.
2. Create an account (name, email, password).
3. After signup, you are redirected to `/subscription`.
4. If subscriptions are enabled, complete subscription steps as required, then go to `/dashboard`.
5. If subscriptions are disabled, you can go directly to `/dashboard`.

Login is also done at `/connect`.

### Admin (Dedicated Connect)

1. Go to `/connect/admin`.
2. Login using the admin email/password.
3. If 2FA is enabled, you will be asked for a 6-digit code.
4. You will be routed to `/admin`.

Admin signup (if enabled in your environment):
1. Go to `/connect/admin`.
2. Switch to Sign up.
3. Enter name/email/password and the Admin Signup Secret.

### Partner (Dedicated Connect)

Partner login:
1. Go to `/connect/partner`.
2. Login using partner-admin email/password.
3. If 2FA is enabled, you will be asked for a 6-digit code.
4. You will be routed to `/partner/dashboard`.

Partner signup (creates a partner + partner admin user):
1. Go to `/connect/partner`.
2. Switch to Sign up.
3. Enter:
   - Your account info: name/email/password
   - Partner organization info: partner name, contact person, city, address, partner email/phone
4. You will be routed to `/partner/dashboard`.

## Browsing and Viewing Listings

1. From the home page (Discover), browse listings.
2. Click a listing to open `/listing/:id`.
3. The listing page shows:
   - Availability status
   - Pickup information
   - Pricing (for standard listings)
   - Borrow action

Partner-owned listings are displayed like standard listings but are treated as â€œoffline paymentâ€ listings in the borrow flow.

## Creating and Managing Listings (Regular Users)

1. Go to the listing creation flow (New Item) from the user dashboard or navigation.
2. Provide listing details:
   - Title, description, category
   - Listing type (rent/sell/give)
   - Hourly rate (if applicable)
   - Pickup fields (pickup location selection or custom pickup notes)
   - Optional gallery / image
3. Submit to create the listing.

To update or delete a listing, use the listing management actions available from your dashboard/listing detail view (depending on enabled UI sections).

## Borrowing Flow (Standard Listings)

Standard listings go through the checkout/escrow-aware flow.

1. Open the listing page.
2. Click Borrow.
3. Choose a borrowing path (based on enabled config):
   - Verified
   - Deposit
   - Fee
4. Choose a payment method:
   - Card
   - PayPal
   - Cash
   - Free (only if total is 0)
5. Confirm request / payment.
6. The listing moves to a pending/approved state depending on the listingâ€™s auto-approve setting:
   - `PENDING`: waiting for the lender to approve
   - `APPROVED`: approved, but not yet ready for pickup (you will be notified once itâ€™s ready)
7. When the lender marks the item as ready, the listing becomes `READY_FOR_PICKUP` and the borrower is notified (in-app + email).
8. Once the borrower picks up the item, either party can confirm pickup. The listing becomes `WAITING_FOR_RETURN` and the return flow can start.

If card payments are used, the app creates a Stripe payment intent and completes the transaction when the Stripe webhook confirms success.

## Borrowing Flow (Partner Listings â€” Offline Payment)

Partner listings bypass payment checkout and escrow deposit. The borrower pays/deals directly with the partner at pickup/return time.

1. Open a partner listing on `/listing/:id`.
2. Click Borrow.
3. Enter the required request details (duration where applicable).
4. Submit the request.

What happens next:
- The request is routed to the partner dashboard for manual approval/rejection.
- There is no Stripe checkout and no escrow deposit for partner items.

## Approving / Rejecting Borrow Requests

### Regular Listings (Owner/Lender Flow)

When someone requests to borrow your listing:
1. Open your dashboard.
2. Open the listing/request details.
3. Approve or deny the request.
4. After approval, mark the item as ready for pickup when itâ€™s available.
5. Once the borrower picks up the item, either you or the borrower can confirm pickup (this enables the return flow).

Auto-approve listings may skip manual approval.

### Partner Listings (Partner Dashboard Flow)

1. Login at `/connect/partner`.
2. Open `/partner/dashboard`.
3. Go to Requests (or History for past requests).
4. Approve or reject requests.

## Return Flow (Standard + Partner Listings)

The return mechanism applies to both regular and partner listings.

From the listingâ€™s return flow (available once pickup is confirmed and the listing is `WAITING_FOR_RETURN`):
1. Initiate a return session.
2. Complete return confirmation via:
   - QR scan (borrower/lender scans codes), or
   - Manual fallback, or
   - Dispute initiation if something goes wrong

For partner items:
- The return flow is still valid, but no escrow release/refund is involved (offline handling).

## Payments & Payout Setup (Regular Users)

In Settings (Payments tab):
- Add card payment methods.
- Connect Stripe (Express) for payouts if you receive escrow releases.

Partner listings do not use these payment steps for borrower checkout.

## Subscription

Subscription availability is controlled by runtime settings (`settings.enable.subscription`).

Current app behavior:

- The active paid subscription flow for borrowers lives inside the borrowing journey.
- The legacy platform subscription pages still exist, but real paid platform checkout is intentionally disabled.

### Borrowing subscription

1. Open a lend listing.
2. Choose the verified/subscription borrowing option when shown.
3. Request and enter the email verification code.
4. Complete borrower subscription checkout in Stripe.
5. Return to the listing booking flow and finish the borrow request.

What changes after activation:

- The borrower can use the verified path directly.
- The service fee is waived for subscribed borrowing and is not included in the total.
- The waived fee is still shown in the UI with a strike-through for clarity.

### Platform subscription (currently disabled)

- The `/subscription` flow is the legacy platform/lender subscription area.
- Paid platform checkout is disabled on purpose, so it should not start a real Stripe subscription session.
- If platform subscription is disabled globally, admins can still apply the configured fixed lend service fee for non-subscribed borrowers.

## Messaging

1. Go to `/mailbox`.
2. View conversations or open a chat with a user.
3. Send text or images.

Real-time messaging uses WebSockets (STOMP) where supported. If WebSockets are unavailable, REST-based message history still works.

## Reviews

After a completed borrow/return flow:
- Users can submit reviews for other users.
- Some flows use a token-based review invite link.

## Admin Panel

Admin functions are accessed from `/admin` after logging in via `/connect/admin`.

Capabilities (depending on enabled UI):
- View platform summary metrics
- Manage users (status changes, deletion)
- Manage listings (block/delete) (excludes partner-owned listings)
- View transactions and retry releases
- View subscriptions and disputes
- Manage return disputes (accept/reopen)
- View reported listings/users via reports
- Edit runtime app settings (Admin â†’ Settings)

## Multi-Tenant Environment Note

This project supports static database-per-tenant routing in the backend configuration layer.

- Main env vars: `SETTING_USE_DEFAULT_DATABASE`, `TENANT_HEADER_NAME`, `TENANT_DEFAULT_ID`, `TENANT_DEFAULT_DB_URL`, `TENANT_DEFAULT_DB_USERNAME`, `TENANT_DEFAULT_DB_PASSWORD`, `TENANT_DEFAULT_DB_DRIVER`
- Optional extra tenant examples: `TENANT_A_*`, `TENANT_B_*`
- Legacy `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_DRIVER` variables are not required in the multi-tenant setup
- Active tenant ids are defined by the keys under `tenants.config.*` in `src/main/resources/application.properties`; the current sample configuration uses `default`, `vicinity24_tenant_a`, and `vicinity24_tenant_b`
- `SETTING_USE_DEFAULT_DATABASE=true` uses the default database only when the tenant header is missing; a valid tenant header still routes to the matching tenant database
- Startup bootstrap initializes or upgrades schema and seed data for the default database and every configured tenant database
- Full setup details live in `DOC/configuration-guide.md` and `.env.template`

