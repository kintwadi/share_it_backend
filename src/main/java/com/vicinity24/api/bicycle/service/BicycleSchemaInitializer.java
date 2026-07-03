package com.vicinity24.api.bicycle.service;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BicycleSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public BicycleSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureSchema() {
        jdbcTemplate.execute("create schema if not exists bicycle");
        jdbcTemplate.execute("""
                create table if not exists bicycle.bike_listings (
                    listing_id uuid primary key references public.listings(id) on delete cascade,
                    frame_size varchar(32),
                    bike_type varchar(32) not null,
                    assembly_buffer_minutes integer not null default 0,
                    rent_to_own_eligible boolean not null default false,
                    retail_purchase_price numeric(19, 2),
                    inventory_status varchar(32) not null default 'WORKSHOP_PREP_REQUIRED'
                )
                """);
        jdbcTemplate.execute("create index if not exists idx_bike_listings_bike_type on bicycle.bike_listings (bike_type)");
        jdbcTemplate.execute("create index if not exists idx_bike_listings_frame_size on bicycle.bike_listings (frame_size)");
        jdbcTemplate.execute("""
                create table if not exists bikes (
                    id bigserial primary key,
                    tenant_id varchar(50) not null,
                    brand_name varchar(100) not null,
                    model_name varchar(100) not null,
                    model_year integer not null,
                    category varchar(50) not null,
                    sale_type varchar(30) not null,
                    base_price decimal(10, 2) not null,
                    description text,
                    image_url text,
                    is_active boolean default true,
                    created_at timestamp default current_timestamp,
                    constraint unique_tenant_bike unique (tenant_id, brand_name, model_name, model_year, sale_type)
                )
                """);
        jdbcTemplate.execute("alter table bikes add column if not exists image_url text");
        jdbcTemplate.execute("""
                create table if not exists bike_spec_attributes (
                    id bigserial primary key,
                    tenant_id varchar(50) not null,
                    attribute_name varchar(100) not null,
                    is_custom boolean default false,
                    constraint unique_tenant_attribute unique (tenant_id, attribute_name)
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists bike_spec_values (
                    id bigserial primary key,
                    attribute_id bigint references bike_spec_attributes(id) on delete cascade,
                    value_text varchar(100) not null,
                    constraint unique_attribute_value unique (attribute_id, value_text)
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists bike_spec_mappings (
                    bike_id bigint references bikes(id) on delete cascade,
                    spec_value_id bigint references bike_spec_values(id) on delete cascade,
                    primary key (bike_id, spec_value_id)
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists bike_skus (
                    id bigserial primary key,
                    bike_id bigint references bikes(id) on delete cascade,
                    sku_code varchar(100) not null,
                    color_name varchar(50) not null,
                    size_value varchar(20) not null,
                    rider_height_min_cm integer,
                    rider_height_max_cm integer,
                    stack_mm integer,
                    reach_mm integer,
                    stock_quantity integer not null default 0,
                    price_modifier decimal(10, 2) default 0.00,
                    constraint unique_tenant_sku unique (sku_code)
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists bicycle.fahrad_fuchs_catalog (
                    listing_id uuid primary key references public.listings(id) on delete cascade,
                    slug varchar(120) not null unique,
                    display_order integer not null default 0
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists bicycle.fahrad_fuchs_bookings (
                    id uuid primary key,
                    booking_reference varchar(40) not null unique,
                    listing_id uuid not null references public.listings(id) on delete cascade,
                    borrower_id uuid not null references public.users(id) on delete cascade,
                    bike_slug varchar(120) not null,
                    bike_title varchar(200) not null,
                    start_date date not null,
                    end_date date not null,
                    frame_size_option varchar(160) not null,
                    total_amount decimal(10, 2) not null,
                    currency varchar(10) not null,
                    payment_method varchar(30) not null,
                    payment_token varchar(120),
                    status varchar(40) not null,
                    created_at timestamp not null default current_timestamp
                )
                """);
        jdbcTemplate.execute("create index if not exists idx_bikes_tenant_active on bikes (tenant_id, is_active)");
        jdbcTemplate.execute("create index if not exists idx_bikes_tenant_category on bikes (tenant_id, category)");
        jdbcTemplate.execute("create index if not exists idx_bikes_tenant_sale_type on bikes (tenant_id, sale_type)");
        jdbcTemplate.execute("create index if not exists idx_bikes_tenant_price on bikes (tenant_id, base_price)");
        jdbcTemplate.execute("create index if not exists idx_bike_spec_attributes_tenant on bike_spec_attributes (tenant_id)");
        jdbcTemplate.execute("create index if not exists idx_bike_spec_values_attribute on bike_spec_values (attribute_id)");
        jdbcTemplate.execute("create index if not exists idx_bike_spec_mappings_value on bike_spec_mappings (spec_value_id)");
        jdbcTemplate.execute("create index if not exists idx_bike_skus_bike on bike_skus (bike_id)");
        jdbcTemplate.execute("create index if not exists idx_bike_skus_stock on bike_skus (stock_quantity)");
        jdbcTemplate.execute("create index if not exists idx_fahrad_fuchs_catalog_order on bicycle.fahrad_fuchs_catalog (display_order)");
        jdbcTemplate.execute("create index if not exists idx_fahrad_fuchs_bookings_borrower on bicycle.fahrad_fuchs_bookings (borrower_id, created_at desc)");
    }
}
