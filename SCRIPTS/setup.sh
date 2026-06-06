#!/usr/bin/env sh

set -a

ENV_FILE="$(cd "$(dirname "$0")/.." && pwd)/.env"
if [ -f "$ENV_FILE" ]; then
  . "$ENV_FILE"
fi

: "${DB_TYPE:=}"
: "${DB_DRIVER:=}"
: "${DB_URL:=jdbc:postgresql://localhost:5432/Vicinity24}"
: "${DB_USERNAME:=postgres}"
: "${DB_PASSWORD:=postgres}"

: "${JWT_SECRET:=}"

: "${JWT_KEYSTORE_LOCATION:=}"
: "${JWT_KEYSTORE_PASSWORD:=}"
: "${SSL_PASSWORD:=}"
: "${JWT_KEYSTORE_PASSWORD:=${SSL_PASSWORD}}"
: "${JWT_KEYSTORE_TYPE:=PKCS12}"
: "${KEYSTORE_ACCESS_TOKEN_ALIAS:=accesstoken}"
: "${KEYSTORE_ACCESS_TOKEN_PW:=}"
: "${KEYSTORE_REFRESH_TOKEN_ALIAS:=refreshtoken}"
: "${KEYSTORE_REFRESH_TOKEN_PW:=}"

if [ -z "$JWT_SECRET" ] && [ -z "$JWT_KEYSTORE_LOCATION" ]; then
  echo "JWT signing is not configured. Set JWT_SECRET or JWT_KEYSTORE_LOCATION/JWT_KEYSTORE_PASSWORD in .env"
  exit 1
fi
if [ -n "$JWT_KEYSTORE_LOCATION" ] && [ -z "$JWT_KEYSTORE_PASSWORD" ]; then
  echo "JWT keystore password is missing. Set JWT_KEYSTORE_PASSWORD in .env"
  exit 1
fi

: "${ENCRYPTION_KEY:=1234567890123456}"

: "${PORT:=8080}"
: "${SSL_ENABLED:=false}"
: "${SETTINGS_HTTP_ENABLED:=false}"

: "${MAIL_HOST:=smtp.gmail.com}"
: "${MAIL_PORT:=587}"
: "${MAIL_USERNAME:=}"
: "${MAIL_PASSWORD:=}"
: "${MAIL_FROM:=${MAIL_USERNAME}}"

: "${FRONTEND_BASE_URL:=https://localhost:4200}"

: "${STRIPE_PUBLIC_KEY:=}"
: "${STRIPE_SECRET_KEY:=}"
: "${STRIPE_WEBHOOK_SECRET:=}"

: "${AWS_ACCESS_KEY_ID:=}"
: "${AWS_SECRET_ACCESS_KEY:=}"
: "${R2_ACCOUNT_ID:=}"
: "${R2_ACCESS_KEY_ID:=}"
: "${R2_SECRET_ACCESS_KEY:=}"
: "${R2_BUCKET_NAME:=}"
: "${R2_ENDPOINT:=}"
: "${R2_PUBLIC_URL:=}"

: "${ADMIN_SIGNUP_SECRET:=}"

: "${LOCAL_HOST:=https://localhost:*}"
: "${LOCAL_127:=https://127.0.0.1:*}"
: "${REMOTE_RENDE:=https://share-it-client.onrender.com}"

set +a

echo "Environment variables set for Vicinity24 Backend:"
echo "- DB_URL: ${DB_URL}"
echo "- DB_USERNAME: ${DB_USERNAME}"
echo "- DB_PASSWORD: ********"
echo "- JWT_SECRET: ********"
echo "- JWT_KEYSTORE_LOCATION: ${JWT_KEYSTORE_LOCATION}"
echo "- JWT_KEYSTORE_PASSWORD: ********"
echo "- JWT_KEYSTORE_TYPE: ${JWT_KEYSTORE_TYPE}"
echo "- KEYSTORE_ACCESS_TOKEN_ALIAS: ${KEYSTORE_ACCESS_TOKEN_ALIAS}"
echo "- KEYSTORE_ACCESS_TOKEN_PW: ********"
echo "- KEYSTORE_REFRESH_TOKEN_ALIAS: ${KEYSTORE_REFRESH_TOKEN_ALIAS}"
echo "- KEYSTORE_REFRESH_TOKEN_PW: ********"
echo "- ENCRYPTION_KEY: ********"
echo "- PORT: ${PORT}"
echo "- SSL_ENABLED: ${SSL_ENABLED}"
echo "- SETTINGS_HTTP_ENABLED: ${SETTINGS_HTTP_ENABLED}"
echo "- FRONTEND_BASE_URL: ${FRONTEND_BASE_URL}"
echo "- STRIPE_PUBLIC_KEY: ${STRIPE_PUBLIC_KEY}"
echo "- STRIPE_SECRET_KEY: ********"
echo "- STRIPE_WEBHOOK_SECRET: ********"
echo "- ADMIN_SIGNUP_SECRET: ********"
echo "- LOCAL_HOST: ${LOCAL_HOST}"
echo "- LOCAL_127: ${LOCAL_127}"
echo "- REMOTE_RENDE: ${REMOTE_RENDE}"
echo
echo "Run the application with: mvn spring-boot:run -DfrontendSkip=true"
