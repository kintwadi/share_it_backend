@echo off
setlocal EnableExtensions EnableDelayedExpansion

echo Starting NearShare (SQLite + Litestream -> R2) using Docker...

docker info >nul 2>&1
if not "%ERRORLEVEL%"=="0" (
  echo Docker is not running or not installed. Start Docker Desktop and try again.
  exit /b 1
)

set "SQLITE_R2_STREAM_FOR_RENDER=true"

docker compose --env-file .env -f docker-compose-app-only.yml up --build --force-recreate
