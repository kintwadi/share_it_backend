# From React to Angular (Vicinity24)

This document explains how to convert the existing React application in:

- `shareit_client/share_it_client`

into an Angular application located in:

- `frontend`

without changing the Spring Boot REST API backend (`shareit_back`).

## Goals

- Rebuild the frontend in Angular while keeping the backend API contract unchanged.
- Preserve routes, UX flows, and integrations (auth, Stripe, returns, reviews, websockets).
- Keep the URL shape compatible with the current app (important for email links like `/#/rate?token=...`).

## 1) First do a structured inventory (React app)

Before writing Angular code, list what must exist 1:1:

- **Routes**: check `share_it_client/App.tsx` for all `Route path="..."`.
- **API client**: `share_it_client/services/mockApi.ts` is the reference for all endpoints, auth token storage, error parsing.
- **Domain types**: `share_it_client/types.ts` defines the data model expected from the backend.
- **Core screens**: pages under `share_it_client/pages/*` (Dashboard, ListingDetail, Settings, Messages, Subscription flows, Rate page).
- **Cross-cutting contexts**:
  - Language + currency: `share_it_client/contexts/LanguageContext.tsx`
  - Settings config: `share_it_client/contexts/SettingsConfigContext.tsx`
- **Special integrations**:
  - Stripe: `CheckoutForm.tsx`, `PaymentSettings.tsx`, and card/intent flows
  - Websocket: `services/ws.ts`
  - Return flow: `components/ReturnModal.tsx`

Write down:

- the route table (path → screen)
- the full endpoint list (method + URL + request/response)
- any client-side storage keys (token, current_user_id, notifications cache)

That inventory becomes the Angular migration checklist.

## 2) Create the Angular project in `frontend`

Recommended approach: a fresh Angular workspace, with one app.

1. Scaffold
   - Create the Angular app inside `frontend` using Angular CLI (standalone APIs are fine).
2. Routing mode
   - Use **hash-based routing** to stay compatible with current links: the React app uses `HashRouter`, and backend emails currently point to `/#/...`.
   - In Angular, use `provideRouter(..., withHashLocation())` (standalone) or `HashLocationStrategy` (NgModule approach).
3. Styling
   - The React UI uses Tailwind heavily. Add Tailwind to Angular so you can reuse the same design system and classnames.
4. Environment / API base
   - Keep a single `API_BASE` configuration like React’s `__API_BASE__`.
   - In Angular, place this in `environment.ts` and `environment.prod.ts`.

## 3) Match the route structure 1:1

From `App.tsx`, recreate the same paths in Angular Router.

Examples (verify exact list in your repo):

- `/` → Home
- `/connect` → Connect/Login/Register
- `/dashboard` → Dashboard
- `/messages` → Messages
- `/listing/:id` → Listing detail
- `/settings` → Settings
- `/subscription/*` → subscription flows
- `/rate?token=...` → rating page

Notes:

- If you keep hash routing, the user-facing URL will look like `/#/dashboard` which matches the existing app and email links.
- Keep query param parsing compatible (`token`, `tab`, `connect`, etc.).

## 4) Rebuild the API client (most important)

The backend must remain unchanged, so your Angular API client must behave the same as `mockApi.ts`:

### 4.1 Token handling (must match)

React uses:

- `sessionStorage` + `localStorage`
- keys:
  - `nearshare_token`
  - `nearshare_current_user_id`
  - `nearshare_notifications`

In Angular:

- Create an `AuthStorageService` that reads/writes exactly these keys.
- Create an HTTP interceptor that:
  - attaches `Authorization: Bearer <token>` when present
  - sets `Content-Type: application/json` unless using `FormData`
  - on `401`, clears session and produces a user-facing “Please sign in again.”

### 4.2 Error parsing (must match)

React `authFetch`:

- reads response text
- tries JSON parse
- uses `error` or `message` field if present
- maps a few special strings to nicer messages (`request_failed`, `unauthorized`, etc.)

In Angular:

- Centralize this in an `ApiClientService` so every call behaves the same.
- Return typed results (RxJS Observables or Promise wrappers).

### 4.3 Endpoint parity

Recreate every API call from `mockApi.ts` as Angular service methods.

Suggested structure:

- `api/auth-api.service.ts`
- `api/listings-api.service.ts`
- `api/payments-api.service.ts`
- `api/returns-api.service.ts`
- `api/reviews-api.service.ts`
- `api/notifications-api.service.ts`
- `api/messages-api.service.ts`

Then create a thin “facade” (optional) to match the React usage patterns.

## 5) Types: translate `types.ts` into Angular models

You have two good options:

1. **TypeScript interfaces** (fastest)
   - Keep the model as interfaces in Angular too.
2. **Generated types** from OpenAPI (best long-term)
   - If the backend exposes OpenAPI, generate types and clients.
   - If not, keep interfaces for now and generate later.

Do not “improve” the API shapes during migration. Match the backend responses exactly, then refactor later.

## 6) Replace React contexts with Angular equivalents

### 6.1 Language/Currency

React uses `LanguageContext` with a `t(key)` dictionary.

In Angular:

- Create `I18nService` (simple dictionary approach) or use `@ngx-translate/core`.
- Store the selected language and currency the same way (localStorage/sessionStorage if needed).
- Expose `t(key)` for templates (pipe `| t` or method).

### 6.2 SettingsConfig (feature flags)

React uses `SettingsConfigContext` to decide which UI sections are enabled.

In Angular:

- Create `SettingsConfigService` that loads the public config from backend (same endpoint as React).
- Provide helper methods like `isSectionEnabled(...)`.
- Use route guards only if you already had them; otherwise keep UI-level checks.

## 7) Key flows that must be preserved

### 7.1 Borrowing + Payments (Stripe)

From React:

- PaymentIntent creation: backend endpoint stays the same.
- Card flow uses Stripe Elements.
- “Saved cards” flow uses PaymentMethod IDs and confirmCardPayment.

In Angular:

- Use `@stripe/stripe-js` + Stripe Elements integration (Angular wrapper or manual integration).
- Keep the logic:
  - Create payment intent
  - Confirm payment
  - Call borrow endpoint with `paymentToken = pi.id`

Do not change backend payment logic during frontend migration.

### 7.2 Returns (mutual verification)

React ReturnModal calls:

- `GET /api/listings/{id}/return` (active session)
- `POST /api/listings/{id}/return/initiate` (create session)
- `POST /api/listings/{id}/return/scan`
- `POST /api/listings/{id}/return/manual`
- `POST /api/listings/{id}/return/dispute`

In Angular:

- Implement a `ReturnModalComponent` with the same tab behavior and polling.
- Ensure “Return” is shown only for active loans after pickup (status `WAITING_FOR_RETURN` or legacy `BORROWED`, and `DISPUTED` where applicable).

### 7.2.1 Pickup workflow (standard listings)

Standard LEND listings now use a 3-step status flow:

- `PENDING` → waiting for lender approval
- `APPROVED` → approved but not ready yet (borrower waits for “ready for pickup” notification)
- `READY_FOR_PICKUP` → lender marked ready (borrower is notified in-app + by email)
- `WAITING_FOR_RETURN` → pickup confirmed by lender or borrower (return flow enabled)

New endpoints:

- `POST /api/listings/{id}/ready-for-pickup`
- `POST /api/listings/{id}/picked-up`

### 7.3 Reviews / Rating links

Backend now supports token-based invite links:

- `GET /api/reviews/invite/{token}`
- `POST /api/reviews/invite/{token}`

In Angular:

- Implement `/rate` route that reads `token` query param.
- Load invite → render UI → submit rating.
- Keep hash routing so email links continue to work: `/#/rate?token=...`.

### 7.4 Messages + Websocket

React uses `services/ws.ts` for real-time messaging.

In Angular:

- Use native `WebSocket` or `rxjs/webSocket`.
- Keep the same topics/events and auth approach used by the backend.

## 8) Suggested folder layout (Angular)

Example structure:

- `src/app/core/` (singletons)
  - auth storage, interceptors, guards
  - api base + api client
- `src/app/api/` (endpoint services)
- `src/app/shared/` (reusable UI components)
- `src/app/features/`
  - `dashboard/`
  - `listing-detail/`
  - `settings/`
  - `messages/`
  - `subscription/`
  - `rate/`

## 9) Migration strategy (safe, incremental)

Recommended: feature-by-feature rewrite, validated against the backend.

1. Skeleton + routing + Tailwind
2. Auth (Connect/Login/Register) + `me` endpoint
3. Dashboard (read-only first)
4. Listing detail + borrow flow
5. Return flow + escrow release confirmation UX
6. Payment settings + saved cards
7. Messages + websocket
8. Settings + translations
9. Admin + edge cases

At each step:

- Compare network calls between React and Angular (same endpoints, same payloads).
- Compare UI states (success/failure flows).

## 10) Acceptance checklist (parity)

Minimum parity tests (manual is fine initially):

- Login/logout, token persisted same way
- Dashboard loads listings and borrows
- Borrow with card works (PaymentIntent + confirm + borrow)
- Return requires mutual verification and triggers escrow release logic
- Rating link from email opens and can submit review
- Settings pages load and save
- Messages load and send, websocket updates work

## AI Prompt (for a full migration run)

Copy/paste and adapt this prompt into your AI tool:

---

You are a senior full-stack engineer. Convert an existing React/Tailwind/Vite app located at:
`c:/Users/core101/Desktop/desk/shareit_client/share_it_client`
into an Angular app located at:
`c:/Users/core101/Desktop/desk/shareit_back/frontend`.

Hard constraints:
- Do NOT change the Spring Boot backend API or any endpoints.
- Preserve hash-based routing (URLs like `/#/dashboard`, `/#/rate?token=...`).
- Re-implement all routes in `App.tsx` as Angular routes.
- Re-implement all API calls from `services/mockApi.ts` with the same:
  - auth token behavior (storage keys and 401 handling)
  - error parsing (`error`/`message` fields, mapped messages like `unauthorized`, `request_failed`)
- Preserve core flows: login/register, dashboard, listing detail + borrow + Stripe card payments, returns (mutual verification), rating invites, settings, messages (including websocket).

Implementation requirements:
- Use Tailwind in Angular.
- Create Angular services for APIs (auth/listings/payments/returns/reviews/messages).
- Use Angular's `HttpClient` for all API calls.
- Handle errors and retries as in Angular.
- 
Never add html elements into component code, always use external html source
@Component({
  standalone: true,
  selector: 'app-devices',
  imports: [CommonModule, ...],
  templateUrl: './index.html',
  styleUrls: ['./style.css'],
})
export class exampleComponent {
  
- Add an HTTP interceptor for auth headers and unified error handling.
- Create Angular components/pages matching the React pages.
- Create a `/rate` page that reads `token` from query params, loads the invite, and submits a rating.
- Keep the app buildable and runnable at all times; add routes incrementally.

Output:
- Provide the Angular file structure and implement the core services, routing, and the all  pages.
- Include commands to run/build.
- Ensure no breaking changes to existing backend behavior.

---

