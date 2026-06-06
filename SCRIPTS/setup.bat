@echo off

:: Load environment variables from repo-root .env (if present) so this script always matches .env
set "ENV_FILE=%~dp0..\.env"
if exist "%ENV_FILE%" (
  for /f "usebackq eol=# delims=" %%L in ("%ENV_FILE%") do call :SetEnv "%%L"
)

:: Database Configuration
if "%DB_TYPE%"=="" set "DB_TYPE="
if "%DB_URL%"=="" set "DB_URL=jdbc:sqlite:./vicinity24.sqlite"
if "%DB_DRIVER%"=="" set "DB_DRIVER="

set "DB_URL_LC=%DB_URL%"
for %%A in ("%DB_URL_LC%") do set "DB_URL_LC=%%~A"
set "DB_URL_LC=%DB_URL_LC:~0,15%"
if /i "%DB_URL_LC%"=="jdbc:postgresql:" (
  if "%DB_USERNAME%"=="" set "DB_USERNAME=postgres"
  if "%DB_PASSWORD%"=="" set "DB_PASSWORD=postgres"
) else (
  if "%DB_USERNAME%"=="" set "DB_USERNAME="
  if "%DB_PASSWORD%"=="" set "DB_PASSWORD="
)

:: JWT Configuration
if "%JWT_SECRET%"=="" set "JWT_SECRET="

:: JWT Keystore Configuration
if "%JWT_KEYSTORE_LOCATION%"=="" set "JWT_KEYSTORE_LOCATION="
if "%JWT_KEYSTORE_PASSWORD%"=="" set "JWT_KEYSTORE_PASSWORD="
if not "%SSL_PASSWORD%"=="" if "%JWT_KEYSTORE_PASSWORD%"=="" set "JWT_KEYSTORE_PASSWORD=%SSL_PASSWORD%"
if "%JWT_KEYSTORE_TYPE%"=="" set "JWT_KEYSTORE_TYPE=PKCS12"
if "%KEYSTORE_ACCESS_TOKEN_ALIAS%"=="" set "KEYSTORE_ACCESS_TOKEN_ALIAS=accesstoken"
if "%KEYSTORE_ACCESS_TOKEN_PW%"=="" set "KEYSTORE_ACCESS_TOKEN_PW="
if "%KEYSTORE_REFRESH_TOKEN_ALIAS%"=="" set "KEYSTORE_REFRESH_TOKEN_ALIAS=refreshtoken"
if "%KEYSTORE_REFRESH_TOKEN_PW%"=="" set "KEYSTORE_REFRESH_TOKEN_PW="

if "%JWT_SECRET%"=="" if "%JWT_KEYSTORE_LOCATION%"=="" (
  echo [ERROR] JWT signing is not configured. Set JWT_SECRET or JWT_KEYSTORE_LOCATION/JWT_KEYSTORE_PASSWORD in .env
  exit /b 1
)
if not "%JWT_KEYSTORE_LOCATION%"=="" if "%JWT_KEYSTORE_PASSWORD%"=="" (
  echo [ERROR] JWT keystore password is missing. Set JWT_KEYSTORE_PASSWORD in .env
  exit /b 1
)

:: App Encryption Configuration (required)
if "%ENCRYPTION_KEY%"=="" set "ENCRYPTION_KEY=1234567890123456"

:: Render / Docker runtime toggles
if "%PORT%"=="" set "PORT=8081"
if "%SSL_ENABLED%"=="" set "SSL_ENABLED=false"
if "%SETTINGS_HTTP_ENABLED%"=="" set "SETTINGS_HTTP_ENABLED=false"

:: Geolocation (FreeIPAPI)
if "%GEOLOCATION_BASE_URL%"=="" set "GEOLOCATION_BASE_URL=https://free.freeipapi.com/api/json/"

:: AWS Configuration (Placeholder values - update with actual AWS credentials)
if "%AWS_ACCESS_KEY_ID%"=="" set "AWS_ACCESS_KEY_ID=example"
if "%AWS_SECRET_ACCESS_KEY%"=="" set "AWS_SECRET_ACCESS_KEY=example"

:: Cloudflare R2 Configuration
if "%R2_ACCOUNT_ID%"=="" set "R2_ACCOUNT_ID=example"
if "%R2_ACCESS_KEY_ID%"=="" set "R2_ACCESS_KEY_ID=example"
if "%R2_SECRET_ACCESS_KEY%"=="" set "R2_SECRET_ACCESS_KEY=example"
if "%R2_BUCKET_NAME%"=="" set "R2_BUCKET_NAME=example"
if "%R2_ENDPOINT%"=="" set "R2_ENDPOINT=https://example.r2.cloudflarestorage.com"
if "%R2_PUBLIC_URL%"=="" set "R2_PUBLIC_URL=https://example.r2.dev"

:: Email Configuration for Password Recovery
if "%MAIL_HOST%"=="" set "MAIL_HOST=smtp.gmail.com"
if "%MAIL_PORT%"=="" set "MAIL_PORT=587"
if "%MAIL_USERNAME%"=="" set "MAIL_USERNAME=test"
if "%MAIL_PASSWORD%"=="" set "MAIL_PASSWORD=test"
if "%MAIL_FROM%"=="" set "MAIL_FROM=%MAIL_USERNAME%"
if "%STRIPE_PUBLIC_KEY%"=="" set "STRIPE_PUBLIC_KEY=test_public_key"
if "%STRIPE_SECRET_KEY%"=="" set "STRIPE_SECRET_KEY=test_secret_key"
if "%STRIPE_WEBHOOK_SECRET%"=="" set "STRIPE_WEBHOOK_SECRET=test_webhook_secret"
if "%FRONTEND_BASE_URL%"=="" set "FRONTEND_BASE_URL=https://localhost:4200"
if "%ADMIN_SIGNUP_SECRET%"=="" set "ADMIN_SIGNUP_SECRET="

:: CORS Origin Patterns (used by security.cors.allowed-origin-patterns)
set "LOCAL_HOST=http://localhost:*"
set "LOCAL_127=http://127.0.0.1:*"
set "REMOTE_RENDE=https://vicinity24.com"

:: Display configuration
set "DB_URL_SAFE=%DB_URL%"
for /f "tokens=1* delims=@" %%A in ("%DB_URL_SAFE%") do if not "%%B"=="" set "DB_URL_SAFE=%%B"

echo Environment variables set for Vicinity24 Backend:
echo - DB_URL: %DB_URL_SAFE%
echo - DB_USERNAME: %DB_USERNAME%
echo - PORT: %PORT%
echo - SSL_ENABLED: %SSL_ENABLED%
echo - SETTINGS_HTTP_ENABLED: %SETTINGS_HTTP_ENABLED%
echo - GEOLOCATION_BASE_URL: %GEOLOCATION_BASE_URL%
echo - JWT_KEYSTORE_LOCATION: %JWT_KEYSTORE_LOCATION%
echo - FRONTEND_BASE_URL: %FRONTEND_BASE_URL%
echo - LOCAL_HOST: %LOCAL_HOST%
echo - LOCAL_127: %LOCAL_127%
echo - REMOTE_RENDE: %REMOTE_RENDE%
echo.
echo Run the application with: mvn spring-boot:run

exit /b 0

:SetEnv
set "LINE=%~1"
if "%LINE%"=="" exit /b 0
for /f "tokens=1* delims==" %%A in ("%LINE%") do (
  set "KEY=%%A"
  set "VAL=%%B"
)
for /f "tokens=* delims= " %%K in ("%KEY%") do set "KEY=%%K"
for /f "tokens=* delims= " %%V in ("%VAL%") do set "VAL=%%V"
if not "%KEY%"=="" (
  if not "%VAL%"=="" (
    if "%VAL:~0,1%"=="\"" if "%VAL:~-1%"=="\"" set "VAL=%VAL:~1,-1%"
  )
  set "%KEY%=%VAL%"
)
exit /b 0
