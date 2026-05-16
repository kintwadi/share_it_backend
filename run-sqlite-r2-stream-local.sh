#!/usr/bin/env sh

set -eu

echo "Starting NearShare (SQLite + Litestream -> R2) using Docker..."

if ! docker info >/dev/null 2>&1; then
  echo "Docker is not running or not installed. Start Docker Desktop and try again."
  exit 1
fi

docker compose -f docker-compose-app-only.yml up --build --force-recreate
