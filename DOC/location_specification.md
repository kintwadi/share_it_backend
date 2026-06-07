# Item Location Specification for Lending Application

## 1. The Primary Option: Full Text Address (For Any Location)

If the user is listing an item located elsewhere, they must manually enter the physical address where the item resides.
Backend will then send this text to a geocoding API to calculate the latitude and longitude.
Use:  
  LOCATION_IQ_API_KEY: set this as an environment variable (do not commit keys to the repo)
  Remove free.freeipapi use only LocationIQ or manual entries

### Required Fields
* **Street Address**: House number and street name.
* **City / Town**: The specific municipality.
* **Postal / ZIP Code**: The regional grid identifier.
* **Country**: The country name. (select list)

### Technical Example (Manual Input to Geocoding API)
To avoid high API costs, this setup utilizes a cost-effective OpenStreetMap-powered provider (e.g., LocationIQ) which offers free tiers of up to 5,000+ requests per day.

**Frontend JSON Payload Sent to Backend:**
```json
{
  "street_address": "12 Rue de la Paix",
  "city": "Paris",
  "postal_code": "75002",
  "country": "France"
}
```

**Backend Geocoding API Request (Using LocationIQ):**
```http
GET https://locationiq.com
```

**Resulting Database Entry:**
```json
{
  "calculated_latitude": 48.8693,
  "calculated_longitude": 2.3321,
  "geohash_9_chars": "u09tvw0m8"
}
```

---

## 2. The Shortcut Option: "Use My Current Location"

Keep the device GPS as an optional, one-click shortcut button labeled **"Use my current location as the item's location."**

* **How it works**: Clicking this button auto-fills the text fields above using a reverse-geocoding API.
* **The benefit**: It saves the user from typing if they happen to be standing right next to the item.
* **The safety net**: The user can still edit the auto-filled text fields if the GPS is slightly inaccurate.

### Technical Example (GPS Shortcut to Reverse-Geocoding)
When the user clicks the shortcut button, the mobile app or browser grabs the native device coordinates first.

**Step 1: Device Hardware Output:**
```json
{
  "device_lat": 50.8503,
  "device_long": 4.3517
}
```

**Step 2: Reverse-Geocoding API Request (Using LocationIQ):**
```http
GET https://locationiq.com
```

**Step 3: API Response Auto-Fills Frontend Form Fields:**
* **Street Address**: `Grand Place 1` *(User can manually edit if GPS drifted)*
* **City / Town**: `Brussels`
* **Postal / ZIP Code**: `1000`
* **Country**: `Belgium`

---

## 3. Comparative Flow Matrix



| User Scenario | User Action | System Processing | Final UI State |
| :--- | :--- | :--- | :--- |
| **Listing item from a different location** (e.g., at work, item is at home) | Types address details manually into text boxes. | Calls Geocoding API using text string. | Text fields show user input; hidden fields store calculated coordinates. |
| **Listing item while standing next to it** (e.g., at home) | Clicks **"Use My Current Location"** button. | Calls Reverse-Geocoding API using device GPS. | Text fields auto-populate with address strings for user review/editing. |

---

## 4. Production Strategy: Cost-Effective European Launch (Option A)

For launching in **Germany, France, and Belgium**, utilizing OpenStreetMap data via a freemium cloud wrapper balances performance with zero upfront data costs.

### Provider Integration Details
* **Service Provider**: LocationIQ / Geocode Earth (OSM Engine underneath).
* **Cost Profile**: Free tier provides **5,000 requests/day**, which scales perfectly for a new app launch.
* **Regional Data Quality**: Western Europe features the world's most detailed open-source street mapping network, guaranteeing high precision coordinates.
* **Legal Compliance**: The application frontend must display a small text attribute to meet licensing rules: *"Data Â© OpenStreetMap contributors"*.

---

## 5. Main Feed & Desktop Location Strategy (No IP Tracking Alternative)

When a borrower accesses the main feed on a desktop web browser, LocationIQ cannot directly read user IPs to resolve location. IP-lookup databases (like freeipapi.com) are highly imprecise in Europe due to cellular routers, VPN usage, and dynamic hosting.

### The Recommended Architecture: Intent-Driven Search Fallback

Rather than attempting to guess location using an IP address, implement an explicit user-driven fallback strategy to save infrastructure complexity and bypass strict European GDPR data compliance rules regarding IP tracking.

#### Operational Logic Loop

1. **Step 1: Primary Precision Check**
   * The app triggers the browser's native location prompt (`navigator.geolocation.getCurrentPosition`).
   * **If Accepted**: Pass the exact coordinates directly to your proximity query logic.

2. **Step 2: The Non-Intrusive Prompt (If GPS Blocked/Denied)**
   * Hide any automatic mapping indicators on the home screen feed.
   * Render an overlay or clear header prompt: *"We couldn't detect your location. Enter your City or Postal Code to find items near you."*

3. **Step 3: LocationIQ Autocomplete Search**
   * As the user types (e.g., *"MÃ¼nchen"* or *"75002"*), fire a request to the **LocationIQ Autocomplete/Search API**.
   * Display a clean drop-down listing matching regional locations.

**Example Request to LocationIQ Autocomplete UI Engine:**
```http
GET https://locationiq.com
```

**Example API Matching Array Returned to Client:**
```json
[
  {
    "display_name": "Munich, Bavaria, Germany",
    "lat": "48.1351",
    "lon": "11.5820"
  }
]
```

4. **Step 4: Session Caching**
   * Once a choice is selected, store the returned `lat`/`lon` coordinates in the client's local web storage (`localStorage` or `sessionStorage`).
   * Fetch the list of nearby items immediately using those coordinates. The user context remains exact, matching their explicit physical intent.

---

## Multi-Tenant Environment Note

This project supports static database-per-tenant routing in the backend configuration layer.

- Main env vars: `SETTING_USE_DEFAULT_DATABASE`, `TENANT_HEADER_NAME`, `TENANT_DEFAULT_ID`, `TENANT_DEFAULT_DB_URL`, `TENANT_DEFAULT_DB_USERNAME`, `TENANT_DEFAULT_DB_PASSWORD`, `TENANT_DEFAULT_DB_DRIVER`
- Optional extra tenant examples: `TENANT_A_*`, `TENANT_B_*`
- Active tenant ids are defined by the keys under `tenants.config.*` in `src/main/resources/application.properties`; the current sample configuration uses `default`, `vicinity24_tenant_a`, and `vicinity24_tenant_b`
- `SETTING_USE_DEFAULT_DATABASE=true` uses the default database only when the tenant header is missing; a valid tenant header still routes to the matching tenant database
- Startup bootstrap initializes or upgrades schema and seed data for the default database and every configured tenant database
- Full setup details live in `DOC/configuration-guide.md` and `.env.template`


