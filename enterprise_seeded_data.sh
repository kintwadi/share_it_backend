#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
RESET="${RESET:-true}"
LIMIT="${LIMIT:-80}"

URL="${BASE_URL%/}/api/enterprise/seeded-data/load?reset=${RESET}&limit=${LIMIT}"

curl -sS -X POST "$URL" -H "Content-Type: application/json"
echo

