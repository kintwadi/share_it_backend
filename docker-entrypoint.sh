#!/usr/bin/env sh

set -eu

FLAG="$(printf '%s' "${SQLITE_R2_STREAM_FOR_RENDER:-false}" | tr -d '\r' | tr '[:upper:]' '[:lower:]')"

if [ "$FLAG" = "true" ] || [ "$FLAG" = "1" ]; then
  : "${SQLITE_DB_PATH:=/data/nearshare.sqlite}"

  DB_TYPE=sqlite
  DB_URL="jdbc:sqlite:${SQLITE_DB_PATH}"
  DB_DRIVER=org.sqlite.JDBC
  DB_USERNAME=${DB_USERNAME:-sqlite_user}
  DB_PASSWORD=${DB_PASSWORD:-sqlite_password_123}

  export DB_TYPE DB_URL DB_DRIVER DB_USERNAME DB_PASSWORD SQLITE_DB_PATH

  : "${LITESTREAM_ACCESS_KEY_ID:=${SQLITE_R2_ACCESS_KEY_ID:-${R2_ACCESS_KEY_ID:-}}}"
  : "${LITESTREAM_SECRET_ACCESS_KEY:=${SQLITE_R2_SECRET_ACCESS_KEY:-${R2_SECRET_ACCESS_KEY:-}}}"
  : "${LITESTREAM_ENDPOINT:=${SQLITE_R2_ENDPOINT:-https://a9350353a46ee6697062d84ad27c0c2e.r2.cloudflarestorage.com}}"
  : "${LITESTREAM_BUCKET:=${SQLITE_R2_BUCKET:-sqlitedb}}"
  : "${LITESTREAM_PATH:=${SQLITE_R2_PATH:-production}}"

  mkdir -p "$(dirname "$SQLITE_DB_PATH")"

  litestream restore -config /app/litestream.yml -if-db-not-exists -if-replica-exists "$SQLITE_DB_PATH"
  exec litestream replicate -config /app/litestream.yml -exec "java -jar /app/app.jar"
fi

exec java -jar /app/app.jar
