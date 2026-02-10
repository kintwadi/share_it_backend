#!/bin/sh

# Set data directory
PGDATA="/var/lib/postgresql/data"

# Default values if env vars are not set
: "${DB_USERNAME:=nearshare_user}"
: "${DB_PASSWORD:=nearshare_password}"

# Initialize database if not exists
if [ -z "$(ls -A "$PGDATA")" ]; then
    echo "Initializing PostgreSQL database..."
    initdb -D "$PGDATA"
    
    echo "Starting PostgreSQL temporarily..."
    pg_ctl start -D "$PGDATA" -l /var/lib/postgresql/log.log
    
    # Wait for PostgreSQL to start
    sleep 5
    
    echo "Creating user and database..."
    # Check if user already exists (unlikely in fresh init, but good practice)
    psql -d postgres -c "CREATE USER \"$DB_USERNAME\" WITH PASSWORD '$DB_PASSWORD';" || echo "User creation failed or user exists"
    psql -d postgres -c "CREATE DATABASE nearshare OWNER \"$DB_USERNAME\";" || echo "Database creation failed or database exists"
    psql -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE nearshare TO \"$DB_USERNAME\";"
    
    echo "Stopping PostgreSQL..."
    pg_ctl stop -D "$PGDATA"
    sleep 3
fi

echo "Starting PostgreSQL..."
pg_ctl start -D "$PGDATA" -l /var/lib/postgresql/log.log

# Wait for DB to be ready
sleep 5

echo "Starting Application..."
exec java -jar /app/app.jar