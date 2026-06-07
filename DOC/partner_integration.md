
## Context

You are helping implement a **partner listing feature** for an existing peer-to-peer lending platform. The backend uses **Spring Boot (JPA/Hibernate)**, frontend uses **Angular**. The current database has two core entities:

- `User` (with fields: UUID id, name, email, password, location, trustScore, etc.)
- `Listing` (with fields: UUID id, title, description, type, category, hourlyRate, status, location, owner (ManyToOne to User), borrower, pickupLocation, etc.)

**Goal:** Allow partner organizations (e.g., libraries, tool banks) to add lendable items without becoming regular users. Partner items should appear in the main listing feed alongside user items, but be managed separately by partner admins. The solution must be **decoupled** – i.e., avoid breaking existing user listing flows, keep partner logic in its own package/module, and ideally minimise changes to existing entities.

## Requirements

### Backend (Spring Boot)

1. **New Entities** (in a dedicated `partner` package):
   - `Partner`: id, name, email, phone, address, city, contactPerson, status (ACTIVE/PENDING/SUSPENDED), timestamps.
   - `PartnerAdmin`: many-to-many (or one-to-many) linking `User` (existing) to `Partner` with a role (ADMIN/VIEWER). A user can be admin for multiple partners.
   - Optionally `PartnerListing` – but we prefer to **reuse the existing `Listing` entity** to avoid duplication of search/booking logic. Therefore, add a nullable `partner` relationship to `Listing` and make `owner` nullable. (Existing `Listing.owner` is currently non-null? Assume we modify it to `@ManyToOne(optional = true)`. This is a small, controlled change.)

2. **Database changes**:
   - Create `partner` table.
   - Create `partner_admin` table (with FK to `partner` and `users`).
   - Alter `listings` table: add `partner_id` BIGINT NULL, add foreign key to `partner`. Also make `owner_id` nullable (if not already). Add a check constraint that either `owner_id` or `partner_id` is not null.

3. **REST Controllers** under `/api/partner` (secured with `ROLE_PARTNER_ADMIN` or custom permission checks):
   - `GET /api/partner/my-partners` – returns partners where current user is admin.
   - `POST /api/partner/listings` – create a new listing owned by partner (validates admin rights). Request body contains standard `Listing` fields (title, description, category, location, etc.) plus `partnerId`. The backend sets `owner = null` and `partner = partner`.
   - `GET /api/partner/listings` – list all partner listings for the admin's partner(s).
   - `PUT /api/partner/listings/{listingId}` – update partner listing (only certain fields).
   - `DELETE /api/partner/listings/{listingId}` – delete partner listing.
   - `GET /api/partner/requests` – list borrowing requests (from `Lending` entity – assume exists) for partner items.
   - `POST /api/partner/requests/{requestId}/approve` and `/reject` – handle lending requests.
   - `GET /api/partner/settings` and `PUT /api/partner/settings` – partner‑specific policies (e.g., max lending days, deposit required).

4. **Service Layer**:
   - `PartnerService`: logic for partner registration (admin creation), listing management.
   - Ensure that existing queries for user listings (e.g., `findByOwnerId`) are unchanged; partner listings will have `owner = null` and thus are ignored automatically.
   - Modify the global listing search to include both `owner`‑owned and `partner`‑owned items. Use a JPQL or specification that filters `WHERE (owner.id = :userId OR partner.id IS NOT NULL)` when showing all listings, but with appropriate visibility rules.

5. **Security**:
   - Create a new authority `PARTNER_ADMIN` that can be assigned to users via `User.role` (or via a separate `PartnerAdmin` entity check). Use the existing security mechanisms to verify that the current user is admin of the partner that owns the listing being modified.

### Frontend (Angular)

1. **New Routes** (lazy loaded `PartnerModule`):
   - `/partner/dashboard` – main partner admin dashboard.
   - `/partner/listings/add` – form to add new partner listing.
   - `/partner/listings/edit/:id` – edit existing partner listing.
   - `/partner/requests` – manage borrowing requests.

2. **Components**:
   - `PartnerDashboardComponent`: tabs for "My Listings", "Borrow Requests", "Lending History", "Settings".
   - `PartnerAddListingComponent`: form with fields: title, description, category, location (address + city), condition (optional), image upload, requires approval checkbox. Submits to `POST /api/partner/listings`.
   - `PartnerRequestsComponent`: table showing pending requests with approve/reject buttons.
   - `PartnerSettingsComponent`: form for partner policies (max lending days, deposit amount, auto‑approval flag).

3. **Services**:
   - `PartnerService` with methods: `getMyPartners()`, `addListing(listingData)`, `getListings()`, `getRequests()`, `approveRequest(id)`, `rejectRequest(id)`, `getSettings()`, `updateSettings()`.

4. **Integration with existing listing feed**:
   - Modify the existing `ListingService` in Angular to also fetch partner listings. The backend will return a unified list; no frontend change needed except possibly adding a badge "Partner" next to partner‑owned listings.

## Decoupling Principles

- Do **not** modify existing `User` entity for partner-specific fields. Use a separate `PartnerAdmin` join table.
- Do **not** change existing `Listing` endpoints (e.g., `/api/listings/{id}`) – partner listings will be readable via the same endpoint (since they are `Listing` records), but write operations are only allowed via `/api/partner/listings` for partners.
- Keep all partner-related Java packages under `com.yourapp.partner` – separate from `com.yourapp.user` and `com.yourapp.listing`.
- In the Angular app, create a `partner` folder with its own modules, routed lazily.


## Additional Notes

- the `Lending` entity already exists in the application with fields: `id`, `listingId`, `borrowerId`, `startDate`, `endDate`, `status`. Adjust accordingly.
- The existing `Listing` uses `UUID` as primary key – partner listing IDs will also be UUIDs.
- Use `@Builder` and Lombok consistently.
- Frontend should use Angular Reactive Forms, HttpClient, and follow the existing project's style .
- The solution must be production‑ready, secure (prevent partner admins from modifying other partners' listings), and maintainable.

## Example of Modified Listing Entity (skeleton)

```java
@Entity
@Table(name = "listings")
@Getter @Setter
public class Listing {
    @Id
    private UUID id;
    // ... existing fields ...
    @ManyToOne(optional = true)
    @JoinColumn(name = "owner_id")
    private User owner;
    @ManyToOne(optional = true)
    @JoinColumn(name = "partner_id")
    private Partner partner;
    // ... rest
}

## Multi-Tenant Environment Note

This project now supports static database-per-tenant routing in the backend configuration layer.

- Main env vars: `SETTING_USE_DEFAULT_DATABASE`, `TENANT_HEADER_NAME`, `TENANT_DEFAULT_ID`, `TENANT_DEFAULT_DB_URL`, `TENANT_DEFAULT_DB_USERNAME`, `TENANT_DEFAULT_DB_PASSWORD`, `TENANT_DEFAULT_DB_DRIVER`
- Optional extra tenant examples: `TENANT_A_*`, `TENANT_B_*`
- Default behavior is backward compatible when `SETTING_USE_DEFAULT_DATABASE=true`
- Full setup details live in `DOC/configuration-guide.md` and `.env.template`

