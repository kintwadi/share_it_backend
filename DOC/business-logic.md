# Vicinity24 Business Logic Documentation

This document outlines the core business logic and rules implemented in the Vicinity24 backend application.

## 1. User Trust & Reputation System

The application uses a **Trust Score** (0-100) to measure user reliability. This score is influenced by three main factors: Vouching, Verification, and Reviews.

### 1.1 Vouching
Users can "vouch" for other users to signal trust.
- **Action**: A user triggers a vouch for another user.
- **Effect**: 
  - The target user's `vouchCount` increments by 1.
  - The target user's `trustScore` increments by **1 point**.
  - **Cap**: The trust score cannot exceed 100.
- **Code Reference**: `UserService.vouch()`

### 1.2 Verification
Users can request verification by providing a phone number and address.
- **Process**:
  1. User submits a `verificationRequest` (Status becomes `PENDING`).
  2. Admin/System approves via `approveVerification`.
- **Effect of Approval**:
  - User's `verificationStatus` becomes `VERIFIED`.
  - User's `trustScore` increases by **5 points**.
  - **Cap**: The trust score cannot exceed 100.
- **Revocation**: If verification is revoked, status reverts to `UNVERIFIED` (no explicit score penalty in current logic).
- **Code Reference**: `UserService.approveVerification()`

### 1.3 Reviews & Ratings
After a transaction, users can review each other.
- **Logic**: 
  - A Review consists of a `rating` (1-5 stars) and a `comment`.
  - **Trust Score Recalculation**: 
    - When a new review is created, the system calculates the **average rating** of *all* reviews received by the target user.
    - The `trustScore` is **reset** to `Average Rating * 20`.
    - *Example*: An average rating of 4.5 stars results in a Trust Score of 90 (4.5 * 20).
- **Note**: This calculation overwrites previous increments from vouching or verification, acting as a "calibration" to actual performance once reviews exist.
- **Code Reference**: `ReviewService.create()`

---

## 2. Listing & Borrowing Lifecycle

### 2.1 Listing Creation
- Owners can create listings with:
  - Title, Description, Category
  - Hourly Rate (Price)
  - Location (Lat/Lng)
  - `autoApprove` setting (Boolean)
- Initial Status: `AVAILABLE`

### 2.2 Borrowing Process
When a user wants to borrow an item:
1. **Cost Calculation**:
   - `Base Cost` = `Hourly Rate` * `Duration (hours)` (time-based listings)
   - Borrowing paths can add extra amounts:
     - `VERIFIED`: no extra amount
     - `DEPOSIT`: adds a fixed deposit (currently `50.00`)
     - `FEE`: adds a service fee (currently `8%` of base cost)
   - Subscription-disabled rule (runtime setting):
     - If `settings.enable.subscription=false` and the listing type is `LEND`, a fixed service fee is added:
       - `settings.service.fee` (default `2.99`)
   - `Total Amount` = `Base Cost` + `Service Fee` + `Deposit`
2. **Payment Processing**:
   - If `Total Amount > 0` and method is not "CASH", a payment is processed via `PaymentManager`.
   - Supported methods: Card, etc.
3. **Approval**:
   - If `autoApprove` is true, the request is automatically accepted.
   - Otherwise, the owner must manually approve (logic implicit in status flow).

---

## 3. User Management

### 3.1 Roles
- **MEMBER**: Default role for new users.
- **LENDER**: Users who have active listings (implicit or explicit role promotion).
- **ADMIN**: System administrators.

### 3.2 Profile Updates
- Users can update: Name, Avatar, Phone, Address.
- **Security**: Password changes require verifying the old password first.

---

## 4. Search & Discovery
- **Filtering**: Listings can be filtered by:
  - Search term (matches Title)
  - Category
  - Type (RENT/SHARE)
  - Minimum Price
- **Pagination**: Results are paginated (default page size configurable).
- **Exclusions**: Listings with status `BLOCKED` or `HIDDEN` are excluded from results.

---

## 5. Trust & Safety

### 5.1 Listing Reporting
Users can report listings that violate community guidelines or appear suspicious.
- **Process**:
  - A user submits a report with a `reason` (e.g., Scam, Inappropriate) and `details`.
  - The system records the report with the reporter's ID, the listing ID, and the timestamp.
- **Constraint**:
  - A user can report the same item multiple times, but **only for different reasons**.
  - **Duplicate Prevention**: If a user attempts to report the same listing for the same reason again, the system rejects the request with a 400 Bad Request error.

### 5.2 Admin Report Management
Admins have access to a dashboard grouping reports by listing.
- **Grouped Display**: Reports for the same listing are grouped together.
- **Actions**:
  - **Block/Unblock Listing**: Toggles the listing status between `AVAILABLE` (or previous state) and `BLOCKED`. When `BLOCKED`, the listing is removed from search results and cannot be booked.
  - **Dismiss Reports**: Removes the reports from the active view, effectively "closing" the case.
  - **Delete Listing**: Permanently removes the listing.
  - **Cancel**: Closes the detail view without taking action.

## Multi-Tenant Environment Note

This project supports static database-per-tenant routing in the backend configuration layer.

- Main env vars: `SETTING_USE_DEFAULT_DATABASE`, `TENANT_HEADER_NAME`, `TENANT_DEFAULT_ID`, `TENANT_DEFAULT_DB_URL`, `TENANT_DEFAULT_DB_USERNAME`, `TENANT_DEFAULT_DB_PASSWORD`, `TENANT_DEFAULT_DB_DRIVER`
- Optional extra tenant examples: `TENANT_A_*`, `TENANT_B_*`
- Legacy `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_DRIVER` variables are not required in the multi-tenant setup
- Active tenant ids are defined by the keys under `tenants.config.*` in `src/main/resources/application.properties`; the current sample configuration uses `default`, `vicinity24_tenant_a`, and `vicinity24_tenant_b`
- `SETTING_USE_DEFAULT_DATABASE=true` uses the default database only when the tenant header is missing; a valid tenant header still routes to the matching tenant database
- Startup bootstrap initializes or upgrades schema and seed data for the default database and every configured tenant database
- Full setup details live in `DOC/configuration-guide.md` and `.env.template`


