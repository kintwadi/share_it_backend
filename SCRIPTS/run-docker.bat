@echo off
echo ========================================
echo Vicinity24 Backend Docker Runner
echo ========================================
echo.

set "ROOT_DIR=%~dp0.."
pushd "%ROOT_DIR%" >nul

echo Checking if Docker is running...
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker is not running or not installed!
    echo Please start Docker Desktop and try again.
    pause
    popd >nul
    exit /b 1
)

echo Docker is running. Checking for existing containers...
echo.

REM Stop any existing containers with the same name
echo Stopping any existing Vicinity24 containers...
docker stop shareit-backend-container 2>nul
if %errorlevel% equ 0 (
    echo Stopped existing container: shareit-backend-container
)

echo Removing any existing containers...
docker rm shareit-backend-container 2>nul
if %errorlevel% equ 0 (
    echo Removed existing container: shareit-backend-container
)

echo.
echo ========================================
echo Starting Vicinity24 Backend with Environment Variables
echo ========================================
echo.

echo Using environment variables from .env file...
echo.

REM Run the Docker container with environment variables from .env
echo Starting Docker container with environment variables...
docker run -d --name shareit-backend-container -p 80:80 -p 443:443 --env-file .env shareit-backend:latest

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Failed to start Docker container!
    echo Please check if the image exists: shareit-backend:latest
    echo Run deploy-to-docker.bat first to build the image.
    pause
    popd >nul
    exit /b 1
)

echo.
echo ========================================
echo Container started successfully!
echo ========================================
echo.
echo Application is now running at: https://localhost/
echo.
echo To view logs, run: docker logs -f shareit-backend-container
echo To stop the container, run: docker stop shareit-backend-container
echo To remove the container, run: docker rm shareit-backend-container
echo.
echo Waiting for application to start...
timeout /t 5 /nobreak >nul

echo.
echo Checking container status...
docker ps --filter "name=shareit-backend-container" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo.
echo Showing recent logs:
docker logs --tail 20 shareit-backend-container

echo.
echo ========================================
echo Vicinity24 Backend is now running!
echo Access: https://localhost/
echo ========================================
echo.
pause
popd >nul
