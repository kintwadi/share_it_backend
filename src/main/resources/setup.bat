@echo off
echo Setting up environment variables for NearShare Backend...

set DB_URL=jdbc:postgresql://localhost:5432/nearshare
set DB_USERNAME=postgres
set DB_PASSWORD=postgres
set AWS_ACCESS_KEY_ID=example
set AWS_SECRET_ACCESS_KEY=example
set R2_ACCOUNT_ID=example
set R2_ACCESS_KEY_ID=example
set R2_SECRET_ACCESS_KEY=example
set R2_BUCKET_NAME=example
set R2_ENDPOINT=https://example.r2.cloudflarestorage.com
set R2_PUBLIC_URL=https://example.r2.dev
set STRIPE_PUBLIC_KEY=test_public_key
set STRIPE_SECRET_KEY=test_secret_key
set STRIPE_WEBHOOK_SECRET=test_webhook_secret
set SSL_PASSWORD=Nshare@132
set KEYSTORE_ACCESS_TOKEN_ALIAS=accesstoken
set KEYSTORE_ACCESS_TOKEN_PW=Nshare@132
set KEYSTORE_REFRESH_TOKEN_ALIAS=refreshtoken
set KEYSTORE_REFRESH_TOKEN_PW=Nshare@132
set ENCRYPTION_KEY=1234567890123456
set FRONTEND_BASE_URL=https://localhost:4200

echo Environment variables set for this session.
