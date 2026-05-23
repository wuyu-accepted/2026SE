#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"

compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" "$@"
  elif command -v docker-compose >/dev/null 2>&1; then
    docker-compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" "$@"
  else
    echo "未找到 docker compose 或 docker-compose，请先安装 Docker Compose。" >&2
    exit 1
  fi
}

require_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "未找到 docker，请先安装 Docker。" >&2
    exit 1
  fi
  if ! docker info >/dev/null 2>&1; then
    echo "Docker 未运行，或当前用户没有访问 Docker 的权限。" >&2
    exit 1
  fi
}

ensure_env() {
  if [ ! -f "${ENV_FILE}" ]; then
    cp "${SCRIPT_DIR}/.env.example" "${ENV_FILE}"
    echo "已生成 ${ENV_FILE}，可按需修改数据库密码、端口、OCR/ONNX 配置。"
  fi
}

ensure_runtime_dirs() {
  mkdir -p \
    "${REPO_ROOT}/runtime/postgres" \
    "${REPO_ROOT}/runtime/uploads" \
    "${REPO_ROOT}/runtime/lucene/knowledge" \
    "${REPO_ROOT}/runtime/lucene/knowledge-vectors" \
    "${REPO_ROOT}/runtime/models/embedding" \
    "${REPO_ROOT}/runtime/logs"
}
