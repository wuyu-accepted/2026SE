# 三端离线 Docker 部署流程

## 一、部署目标

本项目按三端理解：

| 端 | 目录 | 部署方式 | 说明 |
|---|---|---|---|
| 学生 / 学生骨干端 | `miniprogram-1` | 微信开发者工具上传发布 | 不部署到服务器，不做 Docker 镜像 |
| 辅导员 / 管理员 Web 端 | `web-counselor` | Docker + Nginx | 部署在应用服务器 `10.10.0.6` |
| 后端 API 服务 | `demo` | Docker + Spring Boot | 部署在应用服务器 `10.10.0.6` |
| 数据库服务 | PostgreSQL | Docker | 部署在数据库服务器 `10.10.0.7` |

服务器分工：

```text
10.10.0.6 应用服务器
- ruc-platform-frontend: 辅导员 / 管理员 Web 端
- ruc-platform-backend: Spring Boot 后端 API
- 持久化上传文件、知识库索引、本地模型文件

10.10.0.7 数据库服务器
- ruc-platform-postgres: PostgreSQL 16
- 持久化数据库数据和备份文件
```

访问关系：

```text
辅导员 / 管理员浏览器
  -> http://10.10.0.6
  -> frontend Nginx
  -> /api 转发到 backend:18080

微信小程序
  -> http://10.10.0.6/api
  -> frontend Nginx
  -> backend:18080

backend 后端容器
  -> jdbc:postgresql://10.10.0.7:5432/ruc_platform

PostgreSQL 容器
  -> /data/ruc-platform/postgres
```

> 说明：如果是微信正式小程序发布，通常需要 HTTPS 合法域名，不能长期使用 HTTP 内网 IP。内网 IP 更适合开发、演示、校园内网或 VPN 测试。

---

## 二、前置条件

两台服务器均不能访问互联网，因此需要提前准备：

1. 两台服务器已安装 Docker 和 Docker Compose 插件。
2. 本地或另一台可联网机器可构建 Docker 镜像。
3. `10.10.0.6` 可以访问 `10.10.0.7:5432`。
4. 数据库服务器 `10.10.0.7` 的 `5432` 端口只允许应用服务器 `10.10.0.6` 访问。

> 说明：如果服务器普通用户没有加入 `docker` 用户组，服务器上的 Docker 命令需要加 `sudo`。本文服务器操作统一使用 `sudo docker ...`，如果你们已经配置好 Docker 用户组，可以省略 `sudo`。

建议端口：

| 服务器 | 端口 | 用途 |
|---|---:|---|
| `10.10.0.6` | `80` | Web 端访问入口，小程序 API 入口 |
| `10.10.0.6` | `18080` | 后端 API，可只用于内网调试 |
| `10.10.0.7` | `5432` | PostgreSQL，仅允许 `10.10.0.6` 访问 |

---

## 三、在联网机器上准备离线镜像

在项目根目录执行。由于两台服务器是 `linux/amd64` 架构，即使在 Apple Silicon Mac 等 ARM 机器上构建，也必须显式指定 `--platform linux/amd64`：

```bash
docker build --platform linux/amd64 -t ruc-platform-backend:1.0 ./demo
docker build --platform linux/amd64 -t ruc-platform-frontend:1.0 ./web-counselor
docker pull --platform linux/amd64 postgres:16
```

> 说明：Web 端代码中的接口路径已经包含 `/api/...`，默认构建不需要额外传 `VITE_API_BASE_URL=/api`。如果未来要把 API 指到其他域名，应传域名根地址，例如 `https://example.com`，不要再额外拼 `/api`。

打包镜像：

```bash
docker save -o ruc-platform-backend-1.0-amd64.tar ruc-platform-backend:1.0
docker save -o ruc-platform-frontend-1.0-amd64.tar ruc-platform-frontend:1.0
docker save -o postgres-16-amd64.tar postgres:16
```

拷贝到两台服务器：

```bash
scp postgres-16-amd64.tar user@10.10.0.7:/tmp/
scp ruc-platform-backend-1.0-amd64.tar ruc-platform-frontend-1.0-amd64.tar user@10.10.0.6:/tmp/
```

---

## 四、部署数据库服务器 10.10.0.7

登录数据库服务器：

```bash
ssh user@10.10.0.7
```

导入 PostgreSQL 镜像：

```bash
sudo docker load -i /tmp/postgres-16-amd64.tar
```

创建持久化目录：

```bash
sudo mkdir -p /opt/ruc-platform-db
sudo mkdir -p /data/ruc-platform/postgres
sudo mkdir -p /data/ruc-platform/backups
sudo chown -R 999:999 /data/ruc-platform/postgres
```

创建 `/opt/ruc-platform-db/docker-compose.yml`：

```bash
cd /opt/ruc-platform-db
sudo vi docker-compose.yml
```

内容如下，`POSTGRES_PASSWORD` 请替换为你们自己的正式密码。：

```yaml
name: ruc-platform-db

services:
  postgres:
    image: postgres:16
    container_name: ruc-platform-postgres
    restart: always
    environment:
      POSTGRES_DB: ruc_platform
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      TZ: Asia/Shanghai
    ports:
      - "5432:5432"
    volumes:
      - /data/ruc-platform/postgres:/var/lib/postgresql/data
      - /data/ruc-platform/backups:/backups
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d ruc_platform"]
      interval: 10s
      timeout: 5s
      retries: 10
```

启动数据库：

```bash
sudo docker compose up -d
sudo docker compose ps
```

验证数据库：

```bash
sudo docker exec -it ruc-platform-postgres psql -U postgres -d ruc_platform -c "select now();"
```

如果服务器启用了 `ufw`，只允许应用服务器访问数据库：

```bash
sudo ufw allow 22/tcp
sudo ufw allow from 10.10.0.6 to any port 5432
sudo ufw enable
sudo ufw status numbered
```

如果使用其他防火墙工具，也按同样原则配置：`5432` 只开放给 `10.10.0.6`。

---

## 五、部署应用服务器 10.10.0.6

登录应用服务器：

```bash
ssh user@10.10.0.6
```

导入后端和 Web 前端镜像：

```bash
sudo docker load -i /tmp/ruc-platform-backend-1.0-amd64.tar
sudo docker load -i /tmp/ruc-platform-frontend-1.0-amd64.tar
```

创建持久化目录：

```bash
sudo mkdir -p /opt/ruc-platform-app
sudo mkdir -p /data/ruc-platform/uploads
sudo mkdir -p /data/ruc-platform/lucene
sudo mkdir -p /data/ruc-platform/models
```

创建 `/opt/ruc-platform-app/docker-compose.yml`：

```bash
cd /opt/ruc-platform-app
sudo vi docker-compose.yml
```

内容如下，`DB_PASSWORD` 必须和数据库服务器的 `POSTGRES_PASSWORD` 一致：

```yaml
name: ruc-platform-app

services:
  backend:
    image: ruc-platform-backend:1.0
    container_name: ruc-platform-backend
    restart: always
    environment:
      SPRING_PROFILES_ACTIVE: server
      SERVER_PORT: 18080

      DB_HOST: 10.10.0.7
      DB_PORT: 5432
      DB_NAME: ruc_platform
      DB_USER: postgres
      DB_PASSWORD: postgres

      FILE_UPLOAD_PATH: /data/ruc-platform/uploads

      KNOWLEDGE_INTELLIGENCE_OCR_ENABLED: "true"
      KNOWLEDGE_INTELLIGENCE_OCR_COMMAND: tesseract
      KNOWLEDGE_INTELLIGENCE_OCR_LANGUAGE: chi_sim+eng
      KNOWLEDGE_INTELLIGENCE_LATEX_ENABLED: "true"
      KNOWLEDGE_INTELLIGENCE_LATEX_COMMAND: tectonic
      KNOWLEDGE_INTELLIGENCE_SEMANTIC_ONNX_MODEL_PATH: /opt/ruc-platform/models/embedding/model.onnx
      KNOWLEDGE_INTELLIGENCE_SEMANTIC_TOKENIZER_PATH: /opt/ruc-platform/models/embedding/vocab.txt
      KNOWLEDGE_INTELLIGENCE_SEMANTIC_VECTOR_INDEX_ENABLED: "true"
      KNOWLEDGE_INTELLIGENCE_SEMANTIC_VECTOR_INDEX_PATH: /data/ruc-platform/lucene/knowledge-vectors
    volumes:
      - /data/ruc-platform/uploads:/data/ruc-platform/uploads
      - /data/ruc-platform/lucene:/data/ruc-platform/lucene
      - /data/ruc-platform/models:/opt/ruc-platform/models:ro
    ports:
      - "18080:18080"

  frontend:
    image: ruc-platform-frontend:1.0
    container_name: ruc-platform-frontend
    restart: always
    depends_on:
      - backend
    ports:
      - "80:80"
```

启动应用服务：

```bash
sudo docker compose up -d
sudo docker compose ps
```

查看后端日志：

```bash
sudo docker logs -f ruc-platform-backend
```

首次启动时，后端会自动执行 Flyway 迁移脚本，创建业务表。迁移脚本位于：

```text
demo/src/main/resources/db/migration/
```

---

## 六、配置并发布微信小程序端

小程序端位于：

```text
miniprogram-1
```

它不是服务器进程，不需要 Docker 部署。它运行在微信客户端里，通过 HTTP/HTTPS 请求应用服务器的 API。

修改小程序接口地址：

```text
miniprogram-1/utils/config.js
```

将：

```js
const BASE_URL = 'http://127.0.0.1:18080'
```

改为：

```js
const BASE_URL = 'http://10.10.0.6'
```

这样小程序请求 `/api/...` 时，会访问：

```text
http://10.10.0.6/api/...
```

发布流程：

1. 使用微信开发者工具打开 `miniprogram-1`。
2. 确认 `appid` 配置正确。
3. 在开发者工具中测试登录、首页、通知、请假、党团、知识库等接口。
4. 上传体验版。
5. 测试通过后提交审核或发布正式版。

开发或内网演示时，可以在微信开发者工具里勾选“不校验合法域名”。正式发布时，需要在微信公众平台配置合法请求域名，通常要求 HTTPS 域名。

---

## 七、验证流程

### 7.1 验证数据库

在 `10.10.0.7` 执行：

```bash
cd /opt/ruc-platform-db
sudo docker compose ps
sudo docker exec -it ruc-platform-postgres psql -U postgres -d ruc_platform -c "\dt"
```

如果后端已成功启动并执行 Flyway，应该可以看到业务表。

### 7.2 验证后端

在 `10.10.0.6` 执行：

```bash
sudo docker logs ruc-platform-backend --tail=100
curl http://127.0.0.1:18080/api/health
```

如果项目没有 `/api/health` 接口，以日志中无数据库连接错误、无 Flyway 报错为主要判断依据。

### 7.3 验证 Web 端

在能访问应用服务器的电脑浏览器打开：

```text
http://10.10.0.6
```

登录辅导员 / 管理员账号，检查页面是否能正常调用 API。

### 7.4 验证小程序端

在微信开发者工具打开 `miniprogram-1`，检查：

1. 登录接口是否成功。
2. 首页数据是否正常。
3. 通知、请假、知识库等接口是否正常。

---

## 八、日常运维命令

应用服务器 `10.10.0.6`：

```bash
cd /opt/ruc-platform-app
sudo docker compose ps
sudo docker compose logs -f backend
sudo docker compose logs -f frontend
sudo docker compose restart backend
sudo docker compose restart frontend
```

数据库服务器 `10.10.0.7`：

```bash
cd /opt/ruc-platform-db
sudo docker compose ps
sudo docker compose logs -f postgres
sudo docker compose restart postgres
```

---

## 九、持久化目录

必须重点保护和备份以下目录：

```text
10.10.0.7:
/data/ruc-platform/postgres     # PostgreSQL 数据目录
/data/ruc-platform/backups      # 数据库备份目录

10.10.0.6:
/data/ruc-platform/uploads      # 用户上传文件
/data/ruc-platform/lucene       # 知识库索引
/data/ruc-platform/models       # 本地模型文件，如有
```

删除容器不会删除这些目录。只要这些目录还在，数据就可以恢复。

---

## 十、数据库备份与恢复

在 `10.10.0.7` 备份：

```bash
sudo sh -c 'docker exec ruc-platform-postgres pg_dump -U postgres ruc_platform > /data/ruc-platform/backups/ruc_platform_$(date +%F_%H%M).sql'
```

恢复：

```bash
sudo sh -c 'cat /data/ruc-platform/backups/xxx.sql | docker exec -i ruc-platform-postgres psql -U postgres -d ruc_platform'
```

建议每天至少备份一次数据库，并定期将 `/data/ruc-platform/backups` 复制到其他安全位置。

---

## 十一、版本升级流程

在联网机器上重新构建新镜像：

```bash
docker build --platform linux/amd64 -t ruc-platform-backend:1.1 ./demo
docker build --platform linux/amd64 -t ruc-platform-frontend:1.1 ./web-counselor

docker save -o ruc-platform-backend-1.1-amd64.tar ruc-platform-backend:1.1
docker save -o ruc-platform-frontend-1.1-amd64.tar ruc-platform-frontend:1.1
```

拷贝到应用服务器：

```bash
scp ruc-platform-backend-1.1-amd64.tar ruc-platform-frontend-1.1-amd64.tar user@10.10.0.6:/tmp/
```

在 `10.10.0.6` 导入：

```bash
sudo docker load -i /tmp/ruc-platform-backend-1.1-amd64.tar
sudo docker load -i /tmp/ruc-platform-frontend-1.1-amd64.tar
```

修改 `/opt/ruc-platform-app/docker-compose.yml` 的镜像版本：

```yaml
image: ruc-platform-backend:1.1
image: ruc-platform-frontend:1.1
```

重启：

```bash
cd /opt/ruc-platform-app
sudo docker compose up -d
sudo docker compose ps
```

升级前建议先备份数据库：

```bash
sudo sh -c 'docker exec ruc-platform-postgres pg_dump -U postgres ruc_platform > /data/ruc-platform/backups/before_upgrade_$(date +%F_%H%M).sql'
```

---

## 十二、常见问题

### 12.1 Docker 权限不足

如果出现：

```text
permission denied while trying to connect to the docker API at unix:///var/run/docker.sock
```

先用 `sudo docker ...` 继续部署。后续如果希望普通用户直接执行 Docker 命令，可以将用户加入 `docker` 组：

```bash
sudo usermod -aG docker $USER
```

执行后退出 SSH 并重新登录，再用 `docker ps` 验证。

### 12.2 镜像架构不匹配

如果出现：

```text
The requested image's platform (linux/arm64/v8) does not match the detected host platform (linux/amd64/v3)
```

说明镜像是在 ARM 架构下拉取或构建的。重新按本文命令使用 `--platform linux/amd64` 构建、拉取、打包并导入服务器。

### 12.3 后端容器反复重启

先看后端日志：

```bash
cd /opt/ruc-platform-app
sudo docker compose ps
sudo docker logs ruc-platform-backend --tail=200
```

常见原因：

1. 数据库密码不一致，检查 `POSTGRES_PASSWORD` 和 `DB_PASSWORD`。
2. `10.10.0.6` 无法访问 `10.10.0.7:5432`，用 `nc -vz 10.10.0.7 5432` 验证。
3. Flyway 迁移失败，日志里会出现 `Migration Vxx__... failed`。

如果 Flyway 迁移失败后数据库里留下失败记录，修复镜像或 SQL 后，可在数据库服务器清理失败记录：

```bash
sudo docker exec -it ruc-platform-postgres psql -U postgres -d ruc_platform
```

进入 psql 后执行：

```sql
SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;
DELETE FROM flyway_schema_history WHERE success = false;
```

然后回到应用服务器重启后端：

```bash
cd /opt/ruc-platform-app
sudo docker compose restart backend
sudo docker logs -f ruc-platform-backend
```
