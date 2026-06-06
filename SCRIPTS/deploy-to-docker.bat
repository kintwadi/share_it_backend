@echo off
setlocal enabledelayedexpansion

:: Local Docker Deployment Script for Vicinity24 Backend (Windows Batch)
:: Usage: deploy-to-docker.bat [version-tag]
::
:: This script builds the Vicinity24 Backend Docker image for local use.
:: It can be run with an optional version tag. If no tag is provided, the version
:: from pom.xml will be used. If pom.xml version is not found, DEFAULT_TAG will be used.
::
:: Configuration
set IMAGE_NAME=shareit-backend
set DEFAULT_TAG=latest

set "ROOT_DIR=%~dp0.."
pushd "%ROOT_DIR%" >nul

echo [INFO] Starting local Docker image build...

:: Check if Docker is installed and running
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not installed. Please install Docker first.
    pause
    exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker daemon is not running. Please start Docker Desktop.
    pause
    exit /b 1
)

:: Get version from pom.xml or use provided tag
set version_tag=%1
if not "!version_tag!"=="" (
    set version=!version_tag!
) else (
    :: Extract version from pom.xml
    for /f "tokens=2 delims=<>" %%i in ('findstr /C:"<version>" pom.xml') do (
        set version=%%i
        goto :version_found
    )
    :version_found
    if "!version!"=="" set version=!DEFAULT_TAG!
)

echo [INFO] Using version: !version!

:: Build Docker image
set full_image_name=!IMAGE_NAME!:!version!
echo [INFO] Building Docker image: !full_image_name!

docker build -t "!full_image_name!" . 
if errorlevel 1 (
    echo [ERROR] Failed to build Docker image
    pause
    popd >nul
    exit /b 1
)

echo [SUCCESS] Image built successfully: !full_image_name!

:: Tag as latest if not already
if not "!version!"=="latest" (
    set target_image=!IMAGE_NAME!:latest
    echo [INFO] Tagging image: !full_image_name! -^> !target_image!
    
    docker tag "!full_image_name!" "!target_image!"
    if errorlevel 1 (
        echo [ERROR] Failed to tag image
        pause
        exit /b 1
    )
    
    echo [SUCCESS] Image tagged successfully: !target_image!
)

echo [SUCCESS] Docker image built successfully for local use: !full_image_name!

:: Display environment configuration information
echo.
echo ===== ENVIRONMENT CONFIGURATION =====
echo [INFO] The application requires environment variables to run properly.
echo [INFO] Use the .env file or set variables manually:
echo.
echo Using .env file (recommended):
echo   docker run -p 8081:8080 --env-file .env -e PORT=8080 -e SSL_ENABLED=false -e SETTINGS_HTTP_ENABLED=false !full_image_name!
echo.
echo Manual environment variables (from setup.bat):
echo   docker run -p 8081:8080 -e PORT=8080 -e SSL_ENABLED=false -e SETTINGS_HTTP_ENABLED=false ^
echo     -e DB_URL=jdbc:postgresql://localhost:5432/Vicinity24 ^
echo     -e DB_USERNAME=postgres ^
echo     -e DB_PASSWORD=postgres ^
echo     -e SSL_PASSWORD=******** ^
echo     -e KEYSTORE_ACCESS_TOKEN_ALIAS=accesstoken ^
echo     -e KEYSTORE_ACCESS_TOKEN_PW=******** ^
echo     -e KEYSTORE_REFRESH_TOKEN_ALIAS=refreshtoken ^
echo     -e KEYSTORE_REFRESH_TOKEN_PW=******** ^
echo     -e ENCRYPTION_KEY=******** ^
echo     !full_image_name!

echo.
echo Available local Docker images:
echo   - !IMAGE_NAME!:!version!
if not "!version!"=="latest" (
    echo   - !IMAGE_NAME!:latest
)
echo.
echo Run the application locally with environment variables:
echo   docker run -p 8081:8080 --env-file .env -e PORT=8080 -e SSL_ENABLED=false -e SETTINGS_HTTP_ENABLED=false !IMAGE_NAME!:!version!

echo [SUCCESS] Local Docker image build completed successfully!
pause
popd >nul
