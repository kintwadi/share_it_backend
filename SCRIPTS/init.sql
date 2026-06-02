-- NearShare Database Initialization Script
-- This script runs when the PostgreSQL container starts for the first time

-- Create additional roles if needed
-- CREATE ROLE read_only_role;
-- CREATE ROLE write_role;

-- Grant necessary permissions
-- GRANT CONNECT ON DATABASE nearshare TO read_only_role;
-- GRANT USAGE ON SCHEMA public TO read_only_role;
-- GRANT SELECT ON ALL TABLES IN SCHEMA public TO read_only_role;

-- Set up extensions (uncomment if needed)
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create additional schemas if needed
-- CREATE SCHEMA IF NOT EXISTS audit;
-- CREATE SCHEMA IF NOT EXISTS reporting;

-- Note: The Spring Boot application will automatically create tables
-- based on your JPA entities when it starts with spring.jpa.hibernate.ddl-auto=update

-- You can add custom initialization data here if needed
-- INSERT INTO users (email, password, name, created_at) 
-- VALUES ('admin@nearshare.com', crypt('admin123', gen_salt('bf')), 'Admin User', NOW());

-- Create indexes for better performance (optional)
-- CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
-- CREATE INDEX IF NOT EXISTS idx_listings_owner_id ON listings(owner_id);
-- CREATE INDEX IF NOT EXISTS idx_listings_status ON listings(status);

-- Print success message
DO $$
BEGIN
    RAISE NOTICE 'NearShare database initialization completed successfully';
END $$;