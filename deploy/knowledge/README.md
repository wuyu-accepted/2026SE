# 知识库本地智能一键部署说明

本目录提供“部署机器一键完成和启动”的脚本，面向可迁移部署：别人 clone 仓库后，只要安装 Docker 与 Docker Compose，就可以复用同一套脚本启动 PostgreSQL、后端服务、管理端前端，并保留知识库上传文件、全文索引、向量索引和模型目录。

## 1. 部署机器需要准备

必需：

- Linux/macOS/Windows WSL 均可，推荐 Linux 服务器。
- Docker 已安装并启动。
- Docker Compose V2 可用，即 `docker compose version` 能正常输出。
- 当前用户可运行 Docker；如果不能，需加入 `docker` 用户组或使用具备权限的账号。

可选：

- ONNX 中文 embedding 模型文件：用于真正本地语义检索。
- 自定义同义词文件：用于全文检索同义词扩展。
- LaTeX 编译工具：默认关闭，避免部署时强依赖大型 TeX 环境。

## 2. 一键初始化与启动

在仓库根目录执行：

```bash
./deploy/knowledge/bootstrap.sh
./deploy/knowledge/start.sh
```

这套脚本是“完整功能测试/甲方体验”的推荐启动方式，会启动：

- `db`：PostgreSQL 数据库。
- `backend`：知识库增强版 Spring Boot 后端，使用 `demo/Dockerfile.knowledge`。
- `frontend`：Vue 管理端前端，通过 Nginx 代理 `/api` 到后端。

不要把仓库根目录的 `docker compose -f docker-compose.postgres.yml up -d --build` 当作完整体验启动命令。那个编排只适合基础后端接口和 PostgreSQL 联调，不启动管理端前端，也不使用内置 OCR 依赖的知识库增强镜像。

启动后默认访问：

- 管理端前端：`http://localhost:8080`
- 后端接口：`http://localhost:18080/api`

如果部署在服务器，请把 `localhost` 换成服务器 IP 或域名。

### 不使用脚本的手动启动方式

脚本不是强制要求。它只是把下面几步封装起来：

```bash
# 在仓库根目录执行
cp deploy/knowledge/.env.example deploy/knowledge/.env

mkdir -p \
  runtime/postgres \
  runtime/uploads \
  runtime/lucene/knowledge \
  runtime/lucene/knowledge-vectors \
  runtime/models/embedding \
  runtime/logs

docker compose \
  --env-file deploy/knowledge/.env \
  -f deploy/knowledge/docker-compose.yml \
  up -d --build
```

手动启动后查看状态：

```bash
docker compose --env-file deploy/knowledge/.env -f deploy/knowledge/docker-compose.yml ps
```

查看后端日志：

```bash
docker compose --env-file deploy/knowledge/.env -f deploy/knowledge/docker-compose.yml logs -f backend
```

停止服务但保留数据：

```bash
docker compose --env-file deploy/knowledge/.env -f deploy/knowledge/docker-compose.yml down
```

## 3. 脚本说明

- `bootstrap.sh`：检查 Docker/Compose，生成 `deploy/knowledge/.env`，创建 `runtime/` 持久化目录。
- `start.sh`：构建并启动 `db`、`backend`、`frontend` 三个容器。
- `stop.sh`：停止并移除容器，不删除数据库和上传文件。
- `status.sh`：查看容器状态。
- `logs.sh`：查看所有服务日志；也可追加服务名，例如 `./deploy/knowledge/logs.sh backend`。
- `rebuild-index.sh`：调用后台接口批量重建知识库索引，需要管理员 `Authorization` Token。

## 4. 环境变量配置

首次运行 `bootstrap.sh` 会从 `.env.example` 复制生成：

```bash
deploy/knowledge/.env
```

常用配置：

- `FRONTEND_PORT`：前端访问端口，默认 `8080`。
- `BACKEND_PORT`：后端暴露端口，默认 `18080`。
- `DB_PASSWORD`：PostgreSQL 密码，正式部署建议修改。
- `FILE_UPLOAD_PATH`：容器内上传文件路径，默认 `/data/ruc-platform/uploads`。
- `KNOWLEDGE_OCR_ENABLED`：是否启用 OCR，默认 `true`。
- `KNOWLEDGE_LATEX_ENABLED`：是否启用 LaTeX 本地编译，默认 `false`。
- `KNOWLEDGE_SEMANTIC_ENABLED`：是否启用语义检索，默认 `true`。

修改 `.env` 后执行：

```bash
./deploy/knowledge/start.sh
```

Compose 会按新配置重建或重启相关服务。

## 5. 持久化目录

脚本会在仓库根目录创建 `runtime/`，该目录不应提交到 Git：

```text
runtime/
  postgres/                    # PostgreSQL 数据
  uploads/                     # 用户上传文件、在线编排产物、图片
  lucene/knowledge/            # Lucene 中文全文索引
  lucene/knowledge-vectors/    # Lucene HNSW 向量索引
  models/embedding/            # 本地 ONNX embedding 模型
  logs/                        # 预留日志目录
```

迁移到新机器时，如需保留数据，复制 `runtime/postgres`、`runtime/uploads`、`runtime/lucene`、`runtime/models` 即可。更推荐数据库用 `pg_dump`/`pg_restore` 做正式迁移。

## 6. OCR 能力

默认后端使用 `demo/Dockerfile.knowledge`，镜像内安装：

- `tesseract-ocr`
- `tesseract-ocr-chi-sim`
- `tesseract-ocr-eng`
- `fonts-noto-cjk`

因此部署后支持：

- 图片上传后自动 OCR。
- 扫描 PDF 拆页后 OCR。
- OCR 结果人工校正。
- OCR 失败任务重试和日志查看。

OCR 依赖容器构建时从系统软件源安装包；如果部署机器不能访问软件源，可提前构建镜像并推送到内网镜像仓库，或把 `KNOWLEDGE_OCR_ENABLED=false` 关闭 OCR 后启动基础能力。

## 7. 语义检索模型

系统不调用外部在线服务。语义检索有两级：

1. 未提供 ONNX 模型时，使用本地 hash embedding fallback，保证功能可启动、可迁移。
2. 提供 ONNX 模型后，使用本地中文 embedding 模型生成向量，并写入 Lucene HNSW 向量索引。

模型放置路径：

```text
runtime/models/embedding/model.onnx
runtime/models/embedding/vocab.txt
```

`.env` 中默认已配置为容器内路径：

```bash
KNOWLEDGE_ONNX_MODEL_PATH=/opt/ruc-platform/models/embedding/model.onnx
KNOWLEDGE_ONNX_TOKENIZER_PATH=/opt/ruc-platform/models/embedding/vocab.txt
```

如果模型输入名不同，修改：

```bash
KNOWLEDGE_ONNX_INPUT_IDS_NAME=input_ids
KNOWLEDGE_ONNX_ATTENTION_MASK_NAME=attention_mask
KNOWLEDGE_ONNX_POOLING=mean
```

替换模型后建议重建索引。

## 8. 重建知识库索引

方式一：在管理端页面触发索引重建。

方式二：使用脚本：

```bash
./deploy/knowledge/rebuild-index.sh '<管理员 Authorization Token>'
```

Token 获取方式：登录管理端后，从浏览器开发者工具的接口请求头中复制 `Authorization`。

## 9. 升级与迁移

常规升级：

```bash
git pull
./deploy/knowledge/start.sh
```

脚本会重新构建镜像，后端启动时通过 Flyway 自动执行数据库迁移脚本。

正式迁移建议：

```bash
# 旧机器导出
pg_dump -h 127.0.0.1 -p 5432 -U postgres -d ruc_platform > ruc_platform.sql

# 新机器导入后再启动服务
psql -h 127.0.0.1 -p 5432 -U postgres -d ruc_platform < ruc_platform.sql
```

上传文件、索引、模型可复制：

```bash
runtime/uploads
runtime/lucene
runtime/models
```

如果只迁移数据库和上传文件，全文索引/向量索引可以在新机器通过重建索引生成。

## 10. 常见问题

### 端口被占用

修改 `deploy/knowledge/.env`：

```bash
FRONTEND_PORT=8081
BACKEND_PORT=18081
DB_PORT=15432
```

然后重新启动：

```bash
./deploy/knowledge/start.sh
```

### 后端启动失败

查看日志：

```bash
./deploy/knowledge/logs.sh backend
```

重点检查数据库连接、Flyway 迁移、上传目录权限、OCR 命令路径。

### OCR 没结果

检查：

```bash
./deploy/knowledge/logs.sh backend
```

确认 `.env` 中：

```bash
KNOWLEDGE_OCR_ENABLED=true
KNOWLEDGE_OCR_COMMAND=tesseract
KNOWLEDGE_OCR_LANGUAGE=chi_sim+eng
```

如果图片质量较差、PDF 页数过多或超时，可增大：

```bash
KNOWLEDGE_OCR_TIMEOUT_SECONDS=180
```

### LaTeX 编译无 PDF

默认未启用 LaTeX 真编译。启用前需确保容器中存在对应命令，或自定义镜像安装 `tectonic`/`xelatex`。然后设置：

```bash
KNOWLEDGE_LATEX_ENABLED=true
KNOWLEDGE_LATEX_COMMAND=tectonic
```

即使未启用真编译，平台仍保留 `.tex` 原文作为可编辑主产物，并提供源码预览/下载。

### ONNX 模型未生效

检查模型文件是否存在：

```text
runtime/models/embedding/model.onnx
runtime/models/embedding/vocab.txt
```

再查看后端日志。如果模型加载失败，系统会降级到本地 fallback，不影响服务启动。
