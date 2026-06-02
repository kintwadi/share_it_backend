#!/usr/bin/env sh

set -eu

echo "Starting NearShare (SQLite + Litestream -> R2) using Docker..."

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

if ! docker info >/dev/null 2>&1; then
  echo "Docker is not running or not installed. Start Docker Desktop and try again."
  exit 1
fi

export SQLITE_R2_STREAM_FOR_RENDER=true

docker compose --env-file .env -f docker-compose-app-only.yml up --build --force-recreate
