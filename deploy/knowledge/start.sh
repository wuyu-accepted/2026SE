#!/usr/bin/env bash
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

require_docker
ensure_env
ensure_runtime_dirs

if [ ! -f "${REPO_ROOT}/runtime/models/embedding/model.onnx" ] || [ ! -f "${REPO_ROOT}/runtime/models/embedding/vocab.txt" ]; then
  echo "提示：未检测到 ONNX embedding 模型，将使用本地 fallback 语义向量；如需真模型，请查看 deploy/knowledge/README.md。"
fi

compose up -d --build

echo "等待服务启动..."
compose ps

FRONTEND_PORT_VALUE="$(grep -E '^FRONTEND_PORT=' "${ENV_FILE}" | tail -1 | cut -d= -f2- || true)"
BACKEND_PORT_VALUE="$(grep -E '^BACKEND_PORT=' "${ENV_FILE}" | tail -1 | cut -d= -f2- || true)"
FRONTEND_PORT_VALUE="${FRONTEND_PORT_VALUE:-8080}"
BACKEND_PORT_VALUE="${BACKEND_PORT_VALUE:-18080}"

cat <<MSG
启动命令已执行。

访问地址：
  - 管理端前端：http://localhost:${FRONTEND_PORT_VALUE}
  - 后端接口：http://localhost:${BACKEND_PORT_VALUE}/api

常用命令：
  - 查看日志：${SCRIPT_DIR}/logs.sh
  - 停止服务：${SCRIPT_DIR}/stop.sh
  - 重建知识索引：${SCRIPT_DIR}/rebuild-index.sh <管理员 Authorization Token>
MSG
