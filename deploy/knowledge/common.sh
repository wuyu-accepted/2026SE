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
  local runtime_root="${REPO_ROOT}/runtime"

  if [ -e "${runtime_root}" ] && [ ! -d "${runtime_root}" ]; then
    echo "runtime 路径已存在但不是目录：${runtime_root}" >&2
    exit 1
  fi

  if [ ! -e "${runtime_root}" ]; then
    mkdir -p "${runtime_root}" || {
      echo "无法创建 runtime 目录：${runtime_root}" >&2
      echo "请检查仓库目录权限：$(ls -ld "${REPO_ROOT}")" >&2
      exit 1
    }
  fi

  if [ ! -w "${runtime_root}" ]; then
    echo "runtime 目录当前用户不可写：${runtime_root}" >&2
    echo "当前权限：$(ls -ld "${runtime_root}")" >&2
    echo "修复建议：sudo chown -R $(id -u):$(id -g) \"${runtime_root}\"" >&2
    echo "然后重新执行：${SCRIPT_DIR}/bootstrap.sh" >&2
    exit 1
  fi

  mkdir -p \
    "${runtime_root}/postgres" \
    "${runtime_root}/uploads" \
    "${runtime_root}/lucene/knowledge" \
    "${runtime_root}/lucene/knowledge-vectors" \
    "${runtime_root}/models/embedding" \
    "${runtime_root}/logs"
}
