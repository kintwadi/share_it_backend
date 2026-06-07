

Act as a senior full-stack software architect specializing in Spring Boot  and Angular. 
Implement a location handling module for the existing REST API and angular app for the current item-lending application based on the attached technical markdown file location_specification.md located in the root folder of the project. 

The module must use LocationIQ APIs for address parsing. Most of location handling must be done in the backend the front end will only consume.

Please generate/update(following the existing code) the following decoupled code blocks:

### 1. BACKEND (Spring Boot)
- **Entity**: An `Item` entity containing basic details (id, title) and spatial fields: `latitude` (Double), `longitude` (Double), and `streetAddress`, `city`, `postalCode`, `country`. Include a `geohash` field.
- **DTOs**: 
  - `ItemListingRequest`: Address fields input by the lender.
  - `LocationResponse`: Structured city/street info parsed from an API.
- **Service Layer (`LocationService`)**: 
  - Use RestTemplate or WebClient to call LocationIQ Forward Geocoding (`/v1/search.php`) when saving an item. Convert text inputs to coordinates.
  - Write a method to call LocationIQ Reverse Geocoding (`/v1/reverse.php`) using native browser `lat`/`lon` to return an address payload.
- **Database Query**: Write a Repository method using native SQL or JPA executing the Haversine Formula. It must accept `borrowerLat`, `borrowerLng`, and a max `radiusKm` parameter to pull and sort items closest-first.

### 2. FRONTEND (Angular)
- **Lender Creation Component & Template (update the existing new_item ) **: 
  - Reactive form for the address fields with a country dropdown (`PT`,`DE`, `FR`, `BE` etc).
  - Add a button labeled "Use My Current Location". Use `navigator.geolocation.getCurrentPosition` to fetch hardware device location. 
  - Wire the coordinates to an Angular Service that triggers the backend's reverse-geocoding endpoint to auto-fill the form text inputs.
- **Borrower Main Feed Component ( remove free.freeipapi  **:
  - Implement a logic routine checking for geolocation permissions.
  - If rejected/blocked, display a text field fallback search bar.
  - Bind an autocomplete listener to the text input field calling LocationIQ's autocomplete endpoint (`/v1/autocomplete.php`). Limit query results to `countrycodes: 'pt,de,fr,be'etc`.
  - Cache the selected element's `lat`/`lon` directly into `localStorage`. Trigger an HTTP call passing those coordinates down to the feed retrieval service.

Ensure all external HTTP calls feature clean error handling structures (e.g., address not found, API timeout fallbacks) and apply production best practices throughout. Use standard spatial math for calculations.
In the location_specification.md you will find more information including examples.

## Multi-Tenant Environment Note

This project supports static database-per-tenant routing in the backend configuration layer.

- Main env vars: `SETTING_USE_DEFAULT_DATABASE`, `TENANT_HEADER_NAME`, `TENANT_DEFAULT_ID`, `TENANT_DEFAULT_DB_URL`, `TENANT_DEFAULT_DB_USERNAME`, `TENANT_DEFAULT_DB_PASSWORD`, `TENANT_DEFAULT_DB_DRIVER`
- Optional extra tenant examples: `TENANT_A_*`, `TENANT_B_*`
- Legacy `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_DRIVER` variables are not required in the multi-tenant setup
- Active tenant ids are defined by the keys under `tenants.config.*` in `src/main/resources/application.properties`; the current sample configuration uses `default`, `vicinity24_tenant_a`, and `vicinity24_tenant_b`
- `SETTING_USE_DEFAULT_DATABASE=true` uses the default database only when the tenant header is missing; a valid tenant header still routes to the matching tenant database
- Startup bootstrap initializes or upgrades schema and seed data for the default database and every configured tenant database
- Full setup details live in `DOC/configuration-guide.md` and `.env.template`


