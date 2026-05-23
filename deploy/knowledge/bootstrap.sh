#!/usr/bin/env bash
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

require_docker
ensure_env
ensure_runtime_dirs

cat <<MSG
部署初始化完成。

下一步：
  1. 如需修改端口/数据库密码/OCR/ONNX，请编辑 ${ENV_FILE}
  2. 如需 ONNX 语义模型，请放置：
     - ${REPO_ROOT}/runtime/models/embedding/model.onnx
     - ${REPO_ROOT}/runtime/models/embedding/vocab.txt
  3. 启动：${SCRIPT_DIR}/start.sh
MSG
