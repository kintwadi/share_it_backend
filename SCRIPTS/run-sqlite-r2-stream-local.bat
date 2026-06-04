@echo off
setlocal EnableExtensions EnableDelayedExpansion

echo Starting Vicinity24 (SQLite + Litestream -> R2) using Docker...

set "ROOT_DIR=%~dp0.."
pushd "%ROOT_DIR%" >nul

docker info >nul 2>&1
if not "%ERRORLEVEL%"=="0" (
  echo Docker is not running or not installed. Start Docker Desktop and try again.
  popd >nul
  exit /b 1
)

set "SQLITE_R2_STREAM_FOR_RENDER=true"

docker compose --env-file .env -f docker-compose-app-only.yml up --build --force-recreate

popd >nul
