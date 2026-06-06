#!/usr/bin/env sh

set -a

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

if [ -f "$ROOT_DIR/.env" ]; then
  . "$ROOT_DIR/.env"
fi

if [ -f "$ROOT_DIR/local_database" ]; then
  . "$ROOT_DIR/local_database"
fi

: "${DB_TYPE:=sqlite}"
: "${DB_URL:=jdbc:sqlite:./vicinity24.sqlite}"
: "${DB_USERNAME:=sqlite_user}"
: "${DB_PASSWORD:=sqlite_password_123}"
: "${DB_DRIVER:=org.sqlite.JDBC}"

: "${AWS_ACCESS_KEY_ID:=example}"
: "${AWS_SECRET_ACCESS_KEY:=example}"
: "${R2_ACCOUNT_ID:=example}"
: "${R2_ACCESS_KEY_ID:=example}"
: "${R2_SECRET_ACCESS_KEY:=example}"
: "${R2_BUCKET_NAME:=example}"
: "${R2_ENDPOINT:=https://example.r2.cloudflarestorage.com}"
: "${R2_PUBLIC_URL:=https://example.r2.dev}"

set +a

mvn -DfrontendSkip=true spring-boot:run
