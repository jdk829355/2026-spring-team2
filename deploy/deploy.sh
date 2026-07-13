#!/usr/bin/env bash

set -Eeuo pipefail

AWS_REGION="ap-northeast-2"
APP_DIR="/opt/goodsmap"

NEW_IMAGE="${1:?Usage: deploy.sh <image-uri>}"
ECR_REGISTRY="${NEW_IMAGE%%/*}"

COMPOSE_FILE="$APP_DIR/docker-compose.yml"
IMAGE_ENV_FILE="$APP_DIR/current-image.env"

HEALTH_URL="http://127.0.0.1/actuator/health"

cd "$APP_DIR"

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$1"
}

get_current_image() {
  if [[ -f "$IMAGE_ENV_FILE" ]]; then
    sed -n 's/^APP_IMAGE=//p' "$IMAGE_ENV_FILE"
  fi
}

write_current_image() {
  local image="$1"

  printf 'APP_IMAGE=%s\n' "$image" > "$IMAGE_ENV_FILE"
}

show_diagnostics() {
  log "Container status"
  docker-compose \
    --env-file "$IMAGE_ENV_FILE" \
    --file "$COMPOSE_FILE" \
    ps || true

  log "Application logs"
  docker-compose \
    --env-file "$IMAGE_ENV_FILE" \
    --file "$COMPOSE_FILE" \
    logs --tail=200 app || true

  log "Nginx logs"
  docker-compose \
    --env-file "$IMAGE_ENV_FILE" \
    --file "$COMPOSE_FILE" \
    logs --tail=100 nginx || true
}

wait_for_container_health() {
  local max_attempts=30

  for attempt in $(seq 1 "$max_attempts"); do
    local health_status

    health_status="$(
      docker inspect \
        --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' \
        goodsmap-app 2>/dev/null || true
    )"

    log "Container health attempt $attempt/$max_attempts: $health_status"

    if [[ "$health_status" == "healthy" ]]; then
      return 0
    fi

    if [[ "$health_status" == "unhealthy" ]]; then
      return 1
    fi

    sleep 5
  done

  return 1
}

wait_for_nginx_health() {
  local max_attempts=20

  for attempt in $(seq 1 "$max_attempts"); do
    if curl \
      --fail \
      --silent \
      --show-error \
      --max-time 5 \
      "$HEALTH_URL" >/dev/null; then

      log "Nginx proxy health check succeeded"
      return 0
    fi

    log "Nginx proxy health attempt $attempt/$max_attempts failed"
    sleep 3
  done

  return 1
}

rollback() {
  local old_image="$1"

  if [[ -z "$old_image" || "$old_image" == "$NEW_IMAGE" ]]; then
    log "Rollback image is unavailable"
    return 1
  fi

  log "Rolling back to $old_image"

  write_current_image "$old_image"

  docker-compose \
    --env-file "$IMAGE_ENV_FILE" \
    --file "$COMPOSE_FILE" \
    up \
    --detach \
    --no-deps \
    --force-recreate \
    app

  if ! wait_for_container_health; then
    log "Rollback container did not become healthy"
    show_diagnostics
    return 1
  fi

  docker-compose \
    --env-file "$IMAGE_ENV_FILE" \
    --file "$COMPOSE_FILE" \
    up \
    --detach \
    nginx

  if ! wait_for_nginx_health; then
    log "Rollback proxy health check failed"
    show_diagnostics
    return 1
  fi

  log "Rollback succeeded"
}

OLD_IMAGE="$(get_current_image || true)"

log "Previous image: ${OLD_IMAGE:-none}"
log "New image: $NEW_IMAGE"

log "Logging in to Amazon ECR"

aws ecr get-login-password \
  --region "$AWS_REGION" |
docker login \
  --username AWS \
  --password-stdin "$ECR_REGISTRY"

log "Pulling new image"

docker pull "$NEW_IMAGE"

log "Updating image environment file"

write_current_image "$NEW_IMAGE"

log "Starting application container"

docker-compose \
  --env-file "$IMAGE_ENV_FILE" \
  --file "$COMPOSE_FILE" \
  up \
  --detach \
  --no-deps \
  --force-recreate \
  app

if ! wait_for_container_health; then
  log "New application container failed its health check"
  show_diagnostics
  rollback "$OLD_IMAGE" || true
  exit 1
fi

log "Starting or updating Nginx"

docker-compose \
  --env-file "$IMAGE_ENV_FILE" \
  --file "$COMPOSE_FILE" \
  up \
  --detach \
  nginx

if ! wait_for_nginx_health; then
  log "Nginx could not reach the new application"
  show_diagnostics
  rollback "$OLD_IMAGE" || true
  exit 1
fi

log "Deployment succeeded"

docker image prune --force