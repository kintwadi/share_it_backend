@echo off

:: Database Configuration
set "DB_URL=jdbc:postgresql://localhost:5432/nearshare"
set "DB_USERNAME=postgres"
set "DB_PASSWORD=postgres"

:: SSL Configuration
set "SSL_PASSWORD=Nshare@132"
set "KEYSTORE_ACCESS_TOKEN_ALIAS=accesstoken"
set "KEYSTORE_ACCESS_TOKEN_PW=Nshare@132"
set "KEYSTORE_REFRESH_TOKEN_ALIAS=refreshtoken"
set "KEYSTORE_REFRESH_TOKEN_PW=Nshare@132"
set "ENCRYPTION_KEY=1234567890123456"

:: AWS Configuration (Placeholder values - update with actual AWS credentials)
set "AWS_ACCESS_KEY_ID=example"
set "AWS_SECRET_ACCESS_KEY=example"

:: Cloudflare R2 Configuration
set "R2_ACCOUNT_ID=example"
set "R2_ACCESS_KEY_ID=example"
set "R2_SECRET_ACCESS_KEY=example"
set "R2_BUCKET_NAME=example"
set "R2_ENDPOINT=https://example.r2.cloudflarestorage.com"
set "R2_PUBLIC_URL=https://example.r2.dev"

:: Email Configuration for Password Recovery
set "MAIL_HOST=smtp.gmail.com"
set "MAIL_PORT=587"
set "MAIL_USERNAME=test"
set "MAIL_PASSWORD=test"
set "STRIPE_PUBLIC_KEY=test_public_key"
set "STRIPE_SECRET_KEY=test_secret_key"
set "STRIPE_WEBHOOK_SECRET=test_webhook_secret"
set "SUBSCRIPTION_PLUS_STRIPE_PRICE_ID=price_plus"
set "SUBSCRIPTION_PRO_STRIPE_PRICE_ID=price_pro"
set "FRONTEND_BASE_URL=https://localhost:4200"

:: Display configuration
echo Environment variables set for NearShare Backend:
echo - DB_URL: %DB_URL%
echo - DB_USERNAME: %DB_USERNAME%
echo - SSL_PASSWORD: ********
echo - KEYSTORE_ACCESS_TOKEN_ALIAS: %KEYSTORE_ACCESS_TOKEN_ALIAS%
echo - KEYSTORE_ACCESS_TOKEN_PW: ********
echo - KEYSTORE_REFRESH_TOKEN_ALIAS: %KEYSTORE_REFRESH_TOKEN_ALIAS%
echo - KEYSTORE_REFRESH_TOKEN_PW: ********
echo - ENCRYPTION_KEY: ********
echo - R2_ACCOUNT_ID: %R2_ACCOUNT_ID%
echo - R2_ACCESS_KEY_ID: %R2_ACCESS_KEY_ID%
echo - R2_SECRET_ACCESS_KEY: ********
echo - R2_BUCKET_NAME: %R2_BUCKET_NAME%
echo - R2_ENDPOINT: %R2_ENDPOINT%
echo - R2_PUBLIC_URL: %R2_PUBLIC_URL%
echo - MAIL_HOST: %MAIL_HOST%
echo - MAIL_PORT: %MAIL_PORT%
echo - MAIL_USERNAME: %MAIL_USERNAME%
echo - STRIPE_PUBLIC_KEY: %STRIPE_PUBLIC_KEY%
echo - STRIPE_SECRET_KEY: ********
echo.
echo Run the application with: mvn spring-boot:run
