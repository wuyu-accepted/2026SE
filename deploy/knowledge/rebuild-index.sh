#!/usr/bin/env bash
set -euo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

ensure_env
TOKEN="${1:-}"
if [ -z "${TOKEN}" ]; then
  cat >&2 <<MSG
用法：$0 <管理员 Authorization Token>

说明：该接口需要管理员登录态。可在浏览器登录管理端后，从请求头中复制 Authorization。
MSG
  exit 2
fi

BACKEND_PORT_VALUE="$(grep -E '^BACKEND_PORT=' "${ENV_FILE}" | tail -1 | cut -d= -f2- || true)"
BACKEND_PORT_VALUE="${BACKEND_PORT_VALUE:-18080}"

curl -fsS -X POST "http://127.0.0.1:${BACKEND_PORT_VALUE}/api/admin/knowledge/index/rebuild" \
  -H "Authorization: ${TOKEN}" \
  -H "Content-Type: application/json"
echo
