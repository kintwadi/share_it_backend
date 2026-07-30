You are an expert backend developer. I need you to generate a **Spring Boot module** that implements multi‑tenant product management with row‑based isolation for PostgreSQL. This module will be added to an existing core application **without modifying any existing code**.

---

## Existing Application Context

- **Package base**: `com.vicinity24.api.core`
- **API prefix**: `/api/v1` (runs locally on `http://localhost:8081/api/v1`)
- The core already handles authentication, payments, etc. You **must not change** any existing files, security configuration, or module structure.
- You will create **all new classes inside a new sub‑package**: `com.vicinity24.api.core.store`
- The endpoints you expose must be under the existing prefix and not interfere with other modules.

---

## Multi‑Tenancy Strategy (Row‑Based Isolation)

- A `stores` table holds tenants; every product‑related table has a `store_id` column.
- Tenant is resolved **per request** from an `X-Store-Id` header.
- Use a **Hibernate filter** to automatically append `store_id = :tenantId` to all queries, so repository methods never need to pass a store ID manually.
- The filter should be activated **only for the store‑related entities/tables** and not affect other parts of the system.

---

## Database Schema (PostgreSQL 16+)

All tables are isolated by `store_id`. Use composite foreign keys where necessary to prevent cross‑store data leaks.

```sql
CREATE TABLE stores (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       TEXT NOT NULL,
    slug       TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE categories (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id         BIGINT NOT NULL REFERENCES stores(id),
    parent_id        BIGINT,
    name             TEXT NOT NULL,
    slug             TEXT NOT NULL,
    attribute_schema JSONB,
    FOREIGN KEY (parent_id, store_id) REFERENCES categories(id, store_id),
    UNIQUE (store_id, parent_id, slug)
);
CREATE INDEX idx_categories_store ON categories(store_id);

CREATE TABLE products (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id    BIGINT NOT NULL REFERENCES stores(id),
    sku         TEXT NOT NULL,
    name        TEXT NOT NULL,
    description TEXT,
    base_price  NUMERIC(12,2) NOT NULL,
    currency    TEXT NOT NULL DEFAULT 'EUR',
    category_id BIGINT,
    properties  JSONB NOT NULL DEFAULT '{}',
    is_active   BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (category_id, store_id) REFERENCES categories(id, store_id),
    UNIQUE (store_id, sku)
);
CREATE INDEX idx_products_store ON products(store_id);
CREATE INDEX idx_products_properties ON products USING GIN (properties);

CREATE TABLE product_variants (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id   BIGINT NOT NULL REFERENCES stores(id),
    product_id BIGINT NOT NULL,
    sku        TEXT NOT NULL,
    price      NUMERIC(12,2),
    stock      INT NOT NULL DEFAULT 0,
    options    JSONB NOT NULL DEFAULT '{}',
    is_active  BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (product_id, store_id) REFERENCES products(id, store_id),
    UNIQUE (store_id, sku)
);
CREATE INDEX idx_variants_store ON product_variants(store_id);
CREATE INDEX idx_variants_options ON product_variants USING GIN (options);

Implementation Requirements (inside com.vicinity24.api.core.store)
1. Tenant Context & Filter
TenantContext class with a ThreadLocal<Long>.

A Spring Filter (e.g. TenantFilter) that reads X-Store-Id from the request header, parses it, and sets it in TenantContext.

Register the filter to **only intercept requests under /api/v1/products/**, /api/v1/stores/**, /api/v1/categories/**, /api/v1/variants/**. This prevents any interference with other core APIs.

After the request completes, ThreadLocal must be cleared.

2. Hibernate Multi‑Tenancy Filter
Define a Hibernate filter named tenantFilter with parameter storeId.

Annotate every JPA entity (Store excluded) with @Filter(name = "tenantFilter", condition = "store_id = :storeId").

Create a HibernateFilterConfig component (or use AOP/service layer) that, for every database operation triggered from the store module, enables the filter and sets the parameter from TenantContext.getStoreId().

This can be done in a @Repository/@Transactional service base class, or via a Hibernate interceptor.

The filter must be active only for the current session of the store module’s transactions and should not leak to other sessions.

3. JPA Entities (in com.vicinity24.api.core.store.entity)
Store: id, name, slug, createdAt – no @Filter.

Category: id, store (@ManyToOne), parent (self‑referencing @ManyToOne), children (@OneToMany), name, slug, attributeSchema (JSONB → Map<String, Object>).

Product: id, store, sku, name, description, basePrice, currency, category (@ManyToOne), properties (JSONB), isActive, createdAt, updatedAt, variants (@OneToMany).

ProductVariant: id, store, product (@ManyToOne), sku, price, stock, options (JSONB), isActive, createdAt.

Use custom UserType to map PostgreSQL jsonb columns to Map<String, Object> (using Jackson internally). This avoids extra dependencies. Implement the UserType inside the store module and reference it via @Type(type = "com.vicinity24.api.core.store.usertype.JsonbUserType").

4. Repositories
Simple interfaces extending JpaRepository with no custom store‑scoped queries. Example:

java
Optional<Product> findBySku(String sku); // automatically filtered by Hibernate @Filter
List<Product> findByCategoryId(Long categoryId);
All placed in com.vicinity24.api.core.store.repository.

5. REST Controllers (in com.vicinity24.api.core.store.controller)
Base URL prefix: /api/v1

StoreController (/api/v1/stores) – POST, GET by id, GET all.

ProductController (/api/v1/products) – CRUD. The store ID is never passed in the request body; 

it’s taken from the filter.

ProductVariantController (/api/v1/products/{productId}/variants) – CRUD.

CategoryController (/api/v1/categories) – CRUD with hierarchical awareness.

Ensure that the tenant isolation is fully enforced – no data from another store is ever returned, 

even if the client manually sends a different store header for a resource they shouldn’t access.

6. DTOs
Use record or simple POJOs for request/response DTOs (e.g. ProductRequest, ProductResponse).
Do not expose JPA entities directly.

7. Configuration & Non‑Intrusion
Do not modify any existing SecurityConfig, WebMvcConfigurer, or main application class.

Your filter, user type, and Hibernate interceptor must be self‑contained and automatically 
registered (e.g. via @Component + FilterRegistrationBean).

The Hibernate filter enabling logic should be triggered for the store module’s transactions only. 
A clean way is to create a @Repository base class or a StoreService that programmatically 
enables the filter in each method using Session.enableFilter(). 
If possible, implement a Hibernate statement inspector or a @PostLoad approach,
but the simplest is to enable the filter in a service layer with @Transactional.