@echo off
echo Starting Vicinity24 Backend with H2 Database (Local Mode)...

set "ROOT_DIR=%~dp0.."
pushd "%ROOT_DIR%" >nul

set DB_TYPE=h2
set DB_URL=jdbc:h2:mem:Vicinity24;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
set DB_DRIVER=org.h2.Driver
set DB_USERNAME=sa
set DB_PASSWORD=
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
if "%STRIPE_WEBHOOK_SECRET%"=="" set STRIPE_WEBHOOK_SECRET=
set ENCRYPTION_KEY=1234567890123456
set SSL_PASSWORD=Nshare@132
set KEYSTORE_ACCESS_TOKEN_ALIAS=accesstoken
set KEYSTORE_ACCESS_TOKEN_PW=Nshare@132
set KEYSTORE_REFRESH_TOKEN_ALIAS=refreshtoken
set KEYSTORE_REFRESH_TOKEN_PW=Nshare@132

set SUBSCRIPTION_PLUS_STRIPE_PRICE_ID=price_plus
set SUBSCRIPTION_PRO_STRIPE_PRICE_ID=price_pro
set FRONTEND_BASE_URL=https://localhost:4200
rem Use provided Gmail app credentials for local email sending (override only if not already set)
if "%MAIL_USERNAME%"=="" set MAIL_USERNAME=test
if "%MAIL_PASSWORD%"=="" set MAIL_PASSWORD=test

mvn -DfrontendSkip=true spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.driverClassName=org.h2.Driver --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect --spring.datasource.url=%DB_URL% --spring.datasource.username=%DB_USERNAME% --spring.datasource.password=%DB_PASSWORD%"

popd >nul
