#!/usr/bin/env sh

set -a

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [ -f "$ROOT_DIR/.env" ]; then
  . "$ROOT_DIR/.env"
fi

if [ -f "$ROOT_DIR/local_database" ]; then
  . "$ROOT_DIR/local_database"
fi

: "${DB_TYPE:=sqlite}"
: "${DB_URL:=jdbc:sqlite:./nearshare.sqlite}"
: "${DB_USERNAME:=sqlite_user}"
: "${DB_PASSWORD:=sqlite_password_123}"
: "${DB_DRIVER:=org.sqlite.JDBC}"

set +a

mvn -DfrontendSkip=true spring-boot:run
