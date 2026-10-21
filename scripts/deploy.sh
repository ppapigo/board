#!/usr/bin/env bash
set -euo pipefail
cd "$HOME/board"
: "${BOARD_IMAGE_TAG:?BOARD_IMAGE_TAG required}"
: "${GHCR_USER:?GHCR_USER required}"
: "${GHCR_TOKEN:?GHCR_TOKEN required}"
test -s .env || { echo "Missing existing ~/board/.env; refusing to overwrite it" >&2; exit 1; }
docker network inspect board-net >/dev/null
docker network inspect proxy-net >/dev/null
docker inspect caddy >/dev/null
mkdir -p .deploy
# The workflow stages only the Compose manifest, NOT the shared Caddyfile.
test -s .deploy/docker-compose.yml || { echo "Missing staged compose file" >&2; exit 1; }
echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
trap 'docker logout ghcr.io >/dev/null 2>&1 || true' EXIT
export BOARD_IMAGE_TAG
docker compose --project-directory "$HOME/board" -f .deploy/docker-compose.yml -p board config --quiet
docker compose --project-directory "$HOME/board" -f .deploy/docker-compose.yml -p board pull app frontend chat-app
# Redis and MySQL are prerequisites, never deployed or removed here.
docker compose --project-directory "$HOME/board" -f .deploy/docker-compose.yml -p board up -d --no-deps --no-build --wait app chat-app
docker compose --project-directory "$HOME/board" -f .deploy/docker-compose.yml -p board up -d --no-deps --no-build --wait frontend
docker compose --project-directory "$HOME/board" -f .deploy/docker-compose.yml -p board ps
