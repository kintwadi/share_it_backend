#!/usr/bin/env sh

set -eu

FLAG="$(printf '%s' "${SQLITE_R2_STREAM_FOR_RENDER:-false}" | tr -d '\r' | tr '[:upper:]' '[:lower:]')"

if [ "$FLAG" = "true" ] || [ "$FLAG" = "1" ]; then
  : "${SQLITE_DB_PATH:=/data/vicinity24.sqlite}"

  DB_TYPE=sqlite
  DB_URL="jdbc:sqlite:${SQLITE_DB_PATH}"
  DB_DRIVER=org.sqlite.JDBC
  DB_USERNAME=${DB_USERNAME:-sqlite_user}
  DB_PASSWORD=${DB_PASSWORD:-sqlite_password_123}

  SPRING_DATASOURCE_URL="$DB_URL"
  SPRING_DATASOURCE_DRIVER_CLASS_NAME="$DB_DRIVER"
  SPRING_DATASOURCE_USERNAME="$DB_USERNAME"
  SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD"
  SPRING_JPA_DATABASE_PLATFORM=org.hibernate.community.dialect.SQLiteDialect

  export DB_TYPE DB_URL DB_DRIVER DB_USERNAME DB_PASSWORD SQLITE_DB_PATH
  export SPRING_DATASOURCE_URL SPRING_DATASOURCE_DRIVER_CLASS_NAME SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD
  export SPRING_JPA_DATABASE_PLATFORM

  : "${LITESTREAM_ACCESS_KEY_ID:=${SQLITE_R2_ACCESS_KEY_ID:-${R2_ACCESS_KEY_ID:-}}}"
  : "${LITESTREAM_SECRET_ACCESS_KEY:=${SQLITE_R2_SECRET_ACCESS_KEY:-${R2_SECRET_ACCESS_KEY:-}}}"
  : "${LITESTREAM_ENDPOINT:=${SQLITE_R2_ENDPOINT:-https://a9350353a46ee6697062d84ad27c0c2e.r2.cloudflarestorage.com}}"
  : "${LITESTREAM_BUCKET:=${SQLITE_R2_BUCKET:-sqlitedb}}"
  : "${LITESTREAM_PATH:=${SQLITE_R2_PATH:-production}}"

  export LITESTREAM_ACCESS_KEY_ID LITESTREAM_SECRET_ACCESS_KEY LITESTREAM_ENDPOINT LITESTREAM_BUCKET LITESTREAM_PATH

  if [ -z "${LITESTREAM_ACCESS_KEY_ID:-}" ] || [ -z "${LITESTREAM_SECRET_ACCESS_KEY:-}" ] || [ -z "${LITESTREAM_BUCKET:-}" ] || [ -z "${LITESTREAM_ENDPOINT:-}" ]; then
    echo "Litestream is enabled but required R2 settings are missing."
    echo "Set SQLITE_R2_ACCESS_KEY_ID / SQLITE_R2_SECRET_ACCESS_KEY / SQLITE_R2_BUCKET / SQLITE_R2_ENDPOINT (or LITESTREAM_* equivalents)."
    exit 1
  fi

  case "${LITESTREAM_SECRET_ACCESS_KEY:-}" in
    cfat_*)
      echo "LITESTREAM_SECRET_ACCESS_KEY looks like a Cloudflare API token (cfat_*)."
      echo "Use an R2 S3 secret access key from Cloudflare R2 -> Manage R2 API tokens."
      exit 1
      ;;
  esac

  mkdir -p "$(dirname "$SQLITE_DB_PATH")"

  litestream restore -config /app/litestream.yml -if-db-not-exists -if-replica-exists "$SQLITE_DB_PATH"
  exec litestream replicate -config /app/litestream.yml -exec "java -jar /app/app.jar"
fi

exec java -jar /app/app.jar
