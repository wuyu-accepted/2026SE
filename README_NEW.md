# 学院学生综合服务与党团管理平台（新版说明）

本项目是 2026SE 课程项目，面向学院学生服务、辅导员审批、通知发布、智能知识库和党团管理场景。当前代码采用“学生小程序 + 辅导员/管理员网页端 + 统一后端 API”的三端架构，已覆盖学生服务、审批处理、通知反馈、知识库维护与本地智能检索等核心能力。

> 协作提醒：请不要直接向远程仓库上传未经团队确认的改动；提交前先本地自测并同步接口文档。

## 1. 项目定位

### 1.1 用户角色

- **学生**：登录、查看首页信息、浏览知识库、下载模板、查看党团进度、提交和跟踪申请。
- **学生骨干**：在学生端拥有普通学生能力，后续可扩展班团协作权限。
- **辅导员**：在网页端查看待审批事项、处理申请、查看审批历史和学生信息。
- **管理员**：维护角色、辅导员账号、配置项和审计日志。

### 1.2 端侧分工

- **微信小程序 `miniprogram-1/`**：学生端入口，只放学生高频功能。
- **Vue 网页端 `web-counselor/`**：辅导员和管理员后台，用于审批、管理和统计。
- **Spring Boot 后端 `demo/`**：统一提供 `/api/**` 接口，负责鉴权、业务逻辑和数据访问。

## 2. 目录结构

```text
2026SE/
├─ demo/                         Spring Boot 后端服务
│  ├─ src/main/java/com/ruc/platform
│  │  ├─ auth/                   登录、注册、角色与登录态
│  │  ├─ leave/                  请假申请与审批
│  │  ├─ student/                学生档案与学生管理
│  │  ├─ home/                   学生端首页聚合
│  │  ├─ party/                  学生侧党团进度、提醒和活动申报
│  │  ├─ admin/                  管理端角色、党团、知识库、通知、审计、辅导员管理
│  │  ├─ knowledgeness/          知识库、模板、全文索引、语义检索与本地智能能力
│  │  ├─ notice/                 通知、消息、置顶、反馈
│  │  ├─ file/                   文件上传下载
│  │  ├─ wechat/                 微信公众号菜单等集成
│  │  ├─ config/                 鉴权、跨域、Sa-Token 等配置
│  │  └─ common/                 通用返回、异常、分页、工具和公共控制器
│  ├─ src/main/resources
│  │  ├─ application.yml         默认配置，默认 profile 为 h2
│  │  ├─ application-*.yml       H2/PostgreSQL/Kingbase/Docker/服务器 等配置
│  │  ├─ mapper/                 MyBatis XML 映射
│  │  └─ db/migration/           Flyway 数据库迁移脚本
│  ├─ Dockerfile                 常规后端镜像
│  └─ Dockerfile.knowledge       带 OCR/知识库本地智能依赖的后端镜像
├─ miniprogram-1/                学生端微信小程序
│  ├─ pages/                     登录、首页、知识库、通知、请假、党团、画像等页面
│  └─ utils/                     请求、登录态、配置、工具函数
├─ web-counselor/                辅导员/管理员 Vue 端
│  ├─ src/api/                   前端接口封装
│  ├─ src/router/                路由与角色守卫
│  ├─ src/views/                 审批、党团、知识库、通知、反馈、学生、设置等页面
│  ├─ src/layouts/               后台布局
│  ├─ Dockerfile                 管理端镜像
│  └─ deploy/default.conf        Nginx 部署配置
├─ documents/                    API、部署、需求和开发说明
├─ docs/superpowers/specs/       功能设计方案沉淀
├─ deploy/                       应用/数据库/知识库部署脚本和 Nginx 配置
├─ docker-compose.postgres.yml   后端 + PostgreSQL 本地编排
├─ docker-compose.kingbase.example.yml
├─ docker-compose.knowledge-intelligence.example.yml
├─ project.config.json           微信开发者工具项目配置
└─ README.md                     原说明文档
```

## 3. 技术栈

### 3.1 后端

- Java 17
- Spring Boot 3.2.5
- MyBatis-Plus
- Sa-Token
- Flyway
- H2 / PostgreSQL / Kingbase 兼容 PostgreSQL 协议
- Lucene / PDFBox / Apache POI / ONNX Runtime（知识库全文检索、文件解析和本地语义检索）

### 3.2 网页端

- Vue 3
- Vite
- Vue Router
- Pinia
- Element Plus
- Axios

### 3.3 小程序端

- 微信小程序原生框架
- `wx.request` 封装统一请求
- 本地存储维护 `accessToken` 和当前用户信息

## 4. 当前完成情况

### 4.1 已完成主链路

- 账号注册、登录、退出和当前用户查询。
- 小程序学生端首页聚合数据。
- 知识库文章、模板下载、搜索建议、拼写纠错、个性化推荐与行为上报。
- 智能知识库管理、学生检索与规则推荐；知识内容支持上传资料文件，也支持 Markdown/LaTeX 在线编排并保留可编辑源文件。
- 知识库本地全文索引、文件解析、OCR 校正、版本回滚、查重、过期下架、同义词和推荐权重配置。
- 学生党团进度查询。
- 学生请假申请创建、列表、详情。
- 辅导员/管理员请假审批列表、审批详情、通过、驳回。
- Vue 审批端待处理、已处理、详情审批基础页面。
- 学生提交申请后，辅导员审批，学生侧状态更新的端到端闭环。

### 4.2 已实现或预留的管理能力

- 学生信息分页查询。
- 管理端角色相关接口。
- 辅导员账号管理。
- 审计日志相关结构。
- 管理端党团活动、流程管理、导入和学生进度维护相关模块。
- 通知发布、通知下架、消息反馈处理和学生端消息阅读/置顶。

## 5. 本地运行

### 5.0 甲方完整功能体验启动指南

如果目的是给甲方完整体验，而不是只验证后端接口，优先使用知识库智能版 Docker 启动。它会同时启动 PostgreSQL、Spring Boot 后端、Vue 管理端前端，并启用上传文件解析、OCR、全文索引、向量索引等知识库能力。

如果你的目标是“后端完整功能测试”，结论如下：

- 不建议只执行 `docker compose -f docker-compose.postgres.yml up -d --build`。这个编排只启动普通后端镜像和 PostgreSQL，适合基础接口、登录、审批、通知、普通文件上传和基础知识库接口联调；它没有使用知识库增强镜像，也不会自动准备 OCR、Lucene/向量索引持久化目录、ONNX 模型目录和管理端前端。
- 建议执行 `./deploy/knowledge/bootstrap.sh` 和 `./deploy/knowledge/start.sh`。这套脚本面向完整体验和可迁移部署，会启动 PostgreSQL、知识库增强版 Spring Boot 后端、Vue 管理端前端，并挂载 `runtime/` 保存数据库、上传文件、全文索引、向量索引和本地模型。
- 小程序不在 Docker 里启动。后端和管理端启动后，需要用微信开发者工具导入 `miniprogram-1/`，并把 `miniprogram-1/utils/config.js` 的 `BASE_URL` 指向后端地址。

部署机器需要先具备：

- Docker 已安装并启动。
- Docker Compose V2 可用，即 `docker compose version` 能正常输出。
- `8080`、`18080`、`5432` 端口未被占用；如已占用，可改 `deploy/knowledge/.env`。
- 首次构建需要能访问 Docker 镜像源、Maven 源、npm 源和系统软件源；内网部署时建议提前配置镜像源或预构建镜像。

在仓库根目录执行：

```bash
./deploy/knowledge/bootstrap.sh
./deploy/knowledge/start.sh
```

如果不想使用脚本，也可以手动执行同等步骤：

```bash
# 1. 生成环境变量文件，只需首次执行
cp deploy/knowledge/.env.example deploy/knowledge/.env

# 2. 创建持久化目录
mkdir -p \
  runtime/postgres \
  runtime/uploads \
  runtime/lucene/knowledge \
  runtime/lucene/knowledge-vectors \
  runtime/models/embedding \
  runtime/logs

# 3. 启动完整环境
docker compose \
  --env-file deploy/knowledge/.env \
  -f deploy/knowledge/docker-compose.yml \
  up -d --build
```

手动方式和脚本方式使用的是同一个 Compose 文件，能力一致。区别只是脚本会额外做 Docker 可用性检查、`.env` 自动生成、`runtime/` 权限检查和启动提示；不用脚本时这些检查需要自己确认。

启动完成后访问：

- 网页管理端：`http://localhost:8080`
- 后端接口：`http://localhost:18080/api`

如果部署在服务器上，把 `localhost` 换成服务器 IP 或域名。网页端通过 nginx 代理 `/api` 到后端，正常情况下不需要额外配置前端 API 地址。

甲方演示账号：

| 端侧 | 角色 | 账号 | 密码 |
| --- | --- | --- | --- |
| 网页端 | 超级管理员 | `admin` | `admin123` |
| 网页端 | 辅导员 | `10000001` | `counselor123` |
| 小程序 | 学生 | `2023001` | `password` |
| 小程序 | 学生骨干 | `2023003` | `password` |

建议演示流程：

1. 使用 `admin/admin123` 或 `10000001/counselor123` 登录网页端。
2. 进入知识库，维护分类，上传 PDF、Word、Excel、PPT、图片、Markdown、HTML、CSV 或 ZIP 附件。
3. 发布知识条目，查看解析状态；图片和扫描 PDF 会进入 OCR，解析后的正文可用于检索。
4. 在知识库搜索关键词，检查分类筛选、高亮、相关性解释、搜索建议、同义词和语义相近结果。
5. 发布通知，再用通知搜索验证通知标题、内容和状态筛选。
6. 使用微信开发者工具导入 `miniprogram-1/`，把 `miniprogram-1/utils/config.js` 的 `BASE_URL` 指向 `http://<部署机器IP>:18080`。
7. 用学生账号登录小程序，体验首页全局搜索、知识库搜索、通知搜索、服务入口跳转、请假和党团相关流程。

常用运维命令：

```bash
# 查看容器状态
./deploy/knowledge/status.sh

# 查看全部日志
./deploy/knowledge/logs.sh

# 只看后端日志
./deploy/knowledge/logs.sh backend

# 停止服务，不删除数据库和上传文件
./deploy/knowledge/stop.sh
```

可选能力说明：

- OCR：知识库 Docker 镜像默认安装 Tesseract 中文和英文 OCR，支持图片与扫描 PDF；如果部署机器无法访问系统软件源，可先关闭 `KNOWLEDGE_OCR_ENABLED=false` 启动基础能力。
- 语义检索：未放置 ONNX 模型时使用本地 hash embedding fallback，功能可启动；如需真实本地中文向量模型，把 `model.onnx` 和 `vocab.txt` 放到 `runtime/models/embedding/`。
- LaTeX：默认保留源文件和在线预览能力；真正 LaTeX PDF 编译默认关闭，需要在 `.env` 中启用并准备本地编译工具。
- 数据持久化：数据库、上传文件、Lucene 索引和模型都保存在 `runtime/`；迁移到新机器时复制该目录，或对 PostgreSQL 做 `pg_dump`/`pg_restore`。

排障优先看：

- 脚本无执行权限：执行 `chmod +x deploy/knowledge/*.sh` 后重试。
- `runtime` 权限错误：如果提示 `Permission denied` 或 `runtime 目录当前用户不可写`，执行 `sudo chown -R $(id -u):$(id -g) runtime` 后重试；这是因为旧容器或其他用户创建了 `runtime/`。
- 端口冲突：修改 `deploy/knowledge/.env` 中的 `FRONTEND_PORT`、`BACKEND_PORT`、`DB_PORT`，再执行 `./deploy/knowledge/start.sh`。
- 后端未启动：执行 `./deploy/knowledge/logs.sh backend`，重点看数据库连接、Flyway 迁移、OCR 依赖和上传目录权限。
- 小程序请求失败：确认 `BASE_URL` 使用部署机器真实 IP，微信开发者工具本地调试关闭域名校验。

### 5.1 后端启动：最短可用方式

fresh clone 后优先用 H2 内存库启动，不需要安装 PostgreSQL，也不需要手动建表。后端会通过 Flyway 自动执行 `demo/src/main/resources/db/migration/` 下的迁移脚本。

```bash
cd demo
./mvnw spring-boot:run
```

等日志出现 `Started PlatformApplication` 后，后端默认监听：

```text
http://127.0.0.1:18080
```

默认 profile 来自 `demo/src/main/resources/application.yml`：

```yaml
spring.profiles.active: ${SPRING_PROFILES_ACTIVE:h2}
```

也就是说，如果你没有额外设置环境变量，直接运行 `./mvnw spring-boot:run` 就会使用 `h2`。

启动后可用下面的命令做最小检查：

```bash
curl http://127.0.0.1:18080/api/home
```

未登录时返回鉴权相关错误也说明服务已经起来；如果是连接失败，说明后端未启动或端口不对。

### 5.2 后端启动方式总览

| 方式 | 适合场景 | 启动命令 | 数据是否保留 |
| --- | --- | --- | --- |
| H2 内存库 | 第一次拉代码、快速演示、跑通接口 | `cd demo && ./mvnw spring-boot:run` | 不保留，重启后重置 |
| 指定 H2 | 当前环境变量污染时强制用 H2 | `cd demo && SPRING_PROFILES_ACTIVE=h2 ./mvnw spring-boot:run` | 不保留 |
| 本机后端 + Docker PostgreSQL | 日常开发、希望数据保留 | 先 `docker compose -f docker-compose.postgres.yml up -d db-postgres`，再 `cd demo && SPRING_PROFILES_ACTIVE=dev,postgres ./mvnw spring-boot:run` | 保留在 Docker volume |
| 全 Docker PostgreSQL | 团队联调、基础后端接口测试 | `docker compose -f docker-compose.postgres.yml up -d --build` | 保留在 Docker volume |
| 知识库智能版 Docker | 完整后端能力、甲方体验、OCR/全文索引/向量索引测试 | `./deploy/knowledge/bootstrap.sh && ./deploy/knowledge/start.sh` | 保留在 `runtime/` 目录 |
| Kingbase / 生产类部署 | 课程服务器、国产数据库环境 | 按实际环境设置 `SPRING_PROFILES_ACTIVE=server`、`prod` 或 `kingbase` 后启动 jar | 取决于外部数据库 |

完整功能优先使用“知识库智能版 Docker”。其他方式能验证部分接口，但不能覆盖完整知识库增强链路。

### 5.3 后端：H2 快速启动

H2 是最稳的本地启动方式，适合先确认项目能跑起来。

```bash
cd demo
./mvnw spring-boot:run
```

如果你想显式指定 H2：

```bash
cd demo
SPRING_PROFILES_ACTIVE=h2 ./mvnw spring-boot:run
```

Windows PowerShell：

```powershell
cd demo
$env:SPRING_PROFILES_ACTIVE="h2"
./mvnw.cmd spring-boot:run
```

默认端口是 `18080`。如果端口冲突：

```bash
cd demo
SERVER_PORT=18081 SPRING_PROFILES_ACTIVE=h2 ./mvnw spring-boot:run
```

H2 控制台仅在 `h2` profile 下启用：

```text
http://127.0.0.1:18080/h2-console
JDBC URL: jdbc:h2:mem:ruc_platform
用户名: sa
密码: 留空
```

### 5.4 后端：PostgreSQL 开发启动

如果你希望数据在重启后保留，使用 Docker 启动 PostgreSQL，后端仍在本机运行。

```bash
# 在仓库根目录启动数据库
docker compose -f docker-compose.postgres.yml up -d db-postgres

# 启动后端
cd demo
SPRING_PROFILES_ACTIVE=dev,postgres ./mvnw spring-boot:run
```

默认数据库参数：

```text
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=ruc_platform
DB_USER=postgres
DB_PASSWORD=postgres
```

如果本机已有 PostgreSQL 占用 `5432`，请修改 `docker-compose.postgres.yml` 里的端口映射，或直接连接你本机已有数据库。

关闭：

```bash
# 停止后端：在后端终端 Ctrl+C
# 停止数据库：
docker compose -f docker-compose.postgres.yml stop db-postgres
```

### 5.5 后端：全 Docker 启动

如果你想让后端和 PostgreSQL 都跑在 Docker 中：

```bash
docker compose -f docker-compose.postgres.yml up -d --build
```

这个命令可以测试：

- 登录、鉴权、角色权限等基础后端能力。
- 学生端首页、请假、党团、通知、普通知识库接口。
- 管理端接口在有前端或接口工具配合时的基础联调。
- 普通文件上传下载和数据库迁移。

这个命令不等于完整功能体验，主要缺口是：

- 不启动 Vue 管理端前端。
- 不使用 `demo/Dockerfile.knowledge` 知识库增强镜像。
- 不保证容器内具备 Tesseract OCR 中文语言包。
- 不准备 `runtime/uploads`、`runtime/lucene`、`runtime/models` 这套可迁移持久化目录。
- 不覆盖知识库完整演示所需的 OCR、扫描 PDF 解析、全文索引持久化、向量索引和本地 ONNX 模型目录挂载。

如果要测试完整功能，改用：

```bash
./deploy/knowledge/bootstrap.sh
./deploy/knowledge/start.sh
```

不使用脚本时，等价命令是：

```bash
cp deploy/knowledge/.env.example deploy/knowledge/.env
mkdir -p runtime/postgres runtime/uploads runtime/lucene/knowledge runtime/lucene/knowledge-vectors runtime/models/embedding runtime/logs
docker compose --env-file deploy/knowledge/.env -f deploy/knowledge/docker-compose.yml up -d --build
```

查看日志：

```bash
docker compose -f docker-compose.postgres.yml logs -f app
```

停止：

```bash
docker compose -f docker-compose.postgres.yml down
```

### 5.6 后端：打包后用 jar 启动

需要模拟部署时，可以先打包再运行 jar：

```bash
cd demo
./mvnw -DskipTests clean package
SPRING_PROFILES_ACTIVE=h2 java -jar target/ruc-service-platform-1.0.0-SNAPSHOT.jar
```

连接 PostgreSQL 时：

```bash
SPRING_PROFILES_ACTIVE=prod,postgres DB_HOST=127.0.0.1 DB_PORT=5432 DB_NAME=ruc_platform DB_USER=postgres DB_PASSWORD=postgres java -jar target/ruc-service-platform-1.0.0-SNAPSHOT.jar
```

### 5.7 后端启动后的注意事项

- 文件上传目录默认是 `${user.home}/ruc-platform/uploads`，可通过 `FILE_UPLOAD_PATH` 修改。
- 知识库 Lucene/向量索引目录会在运行时自动创建，默认在用户目录下的 `ruc-platform` 子目录。
- OCR 默认不强制依赖；只有开启 `KNOWLEDGE_INTELLIGENCE_OCR_ENABLED=true` 时才需要本机或容器内安装 `tesseract` 和中文语言包。
- LaTeX PDF 编译同样是可选能力，未安装本地编译命令不影响后端基础启动。
- 如果使用 Docker 知识库智能版，一键脚本和迁移说明见 `deploy/knowledge/README.md`。

### 5.8 启动 Vue 管理端

```bash
cd web-counselor
npm install
npm run dev
```

默认访问：`http://localhost:5173`。

网页端请求基地址来自 `VITE_API_BASE_URL`，未配置时使用同源路径。开发时可创建本地环境文件：

```bash
cd web-counselor
echo 'VITE_API_BASE_URL=http://127.0.0.1:18080' > .env.local
```

### 5.9 启动微信小程序

1. 使用微信开发者工具导入 `miniprogram-1/`。
2. 本地调试时关闭域名校验。
3. 检查 `miniprogram-1/utils/config.js` 中的 `BASE_URL`，确保指向后端地址，例如 `http://127.0.0.1:18080`。
4. 使用学生账号登录并测试首页、知识库、党团进度、请假申请。

## 6. 测试账号

| 端侧 | 角色 | 账号 | 密码 |
| --- | --- | --- | --- |
| 小程序 | 学生 | `2023001` | `password` |
| 小程序 | 学生 | `2023002` | `password` |
| 小程序 | 学生骨干 | `2023003` | `password` |
| 小程序 | 学生 | `2023004` | `password` |
| 小程序 | 学生 | `2023005` | `password` |
| 小程序 | 学生骨干 | `2023006` | `password` |
| 网页端 | 辅导员 | `10000001` | `counselor123` |
| 网页端 | 超级管理员 | `admin` | `admin123` |

登录请求会根据端侧传入 `clientType`：

- 小程序：`clientType=miniprogram`，仅允许 `student`、`cadre`。
- 网页端：`clientType=web`，仅允许 `counselor`、`admin`。
- `admin` 是内置超级管理员，只允许登录，不开放注册。
- 历史账号 `stu1/stu2/stu3` 和 `00000001/00000002/00000003` 已统一迁移为 `2023004/2023005/2023006`。

## 7. 关键接口约定

统一返回格式中，`code = 0` 表示成功。前端请求需要携带 `Authorization` 请求头。

### 7.1 认证

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/wx-login`
- `GET /api/auth/me`
- `POST /api/auth/logout`

### 7.2 学生端

- `GET /api/home`
- `GET /api/student/profile`
- `PUT /api/student/profile`
- `GET /api/knowledge/articles`
- `GET /api/knowledge/articles/{id}`
- `GET /api/knowledge/templates`
- `GET /api/files/{fileId}/download`
- `GET /api/party/me/progress`
- `GET /api/party/me/records`
- `GET /api/party/me/reminders`
- `GET /api/messages`
- `GET /api/messages/unread-count`
- `POST /api/messages/{id}/read`
- `POST /api/messages/{id}/pin`
- `POST /api/messages/{id}/unpin`
- `POST /api/leave/me/applications`
- `GET /api/leave/me/applications`
- `GET /api/leave/me/applications/{id}`

### 7.3 管理端

- `GET /api/leave/reviewer/applications?status={status}`
- `GET /api/leave/reviewer/applications/{id}`
- `POST /api/leave/reviewer/applications/{id}/approve`
- `POST /api/leave/reviewer/applications/{id}/reject`
- `GET /api/admin/students`
- `GET /api/admin/knowledge/articles`
- `POST /api/admin/knowledge/articles`
- `PUT /api/admin/knowledge/articles/{id}`
- `PUT /api/admin/knowledge/articles/{id}/status`
- `POST /api/admin/knowledge/index/rebuild`
- `GET /api/admin/knowledge/index/tasks`
- `GET /api/admin/notices`
- `POST /api/admin/notices`
- `POST /api/admin/notices/{id}/publish`
- `POST /api/admin/notices/{id}/offline`
- `GET /api/admin/notice-feedbacks`

完整接口以 `documents/API.md` 为准。新增或调整接口时必须同步更新该文件。

## 8. 权限边界

- 学生侧接口：`/api/home`、`/api/student/**`、`/api/party/me/**`、`/api/leave/me/**`、`/api/messages/**`，仅允许 `student` 或 `cadre`。
- 知识库公开查询接口：`/api/knowledge/**` 面向登录用户，列表默认只返回已发布内容；管理端维护走 `/api/admin/knowledge/**`。
- 管理端业务接口：`/api/admin/**`、`/api/leave/reviewer/**`，默认允许 `counselor` 或 `admin`。
- 超级管理员接口：`/api/admin/roles/**`、`/api/admin/counselors/**`、`/api/admin/audit-logs/**`、`/api/wechat/official-account/menu/**`，仅允许 `admin`。

前端路由守卫只能改善体验，不能代替后端权限校验。新增接口时应同时检查后端角色限制。

## 9. 开发规范

### 9.1 分支与提交

- 不要直接上传未经确认的改动到远程仓库。
- 开发前先确认当前分支和本地改动：`git status`。
- 每次改动尽量聚焦一个功能或一个问题，避免混入无关重构。
- 提交前至少跑通对应端的构建或关键流程。

### 9.2 前后端协作

- 后端接口统一使用 `/api/**` 前缀。
- 学生侧接口使用 `/me` 表示“当前登录学生本人”。
- 审批侧接口使用 `/reviewer` 表示“当前登录审批人”。
- 新增接口时同时更新 `documents/API.md`。
- 前端不要硬编码业务状态文案，尽量统一枚举和展示映射。

### 9.3 配置与安全

- 不要把真实密钥、数据库密码、微信 `secret` 提交到仓库。
- 本地配置优先使用环境变量或本地 `.env.local`。
- 生产环境 profile 应关闭调试日志，避免输出敏感信息。
- 文件上传目录和数据库连接参数应由部署环境注入。

## 10. 建议的后续开发路线

### P0：先修基础体验

- 统一 README、API 文档和实际 profile 的启动说明。
- 将敏感配置改成环境变量占位。
- 清理或替换前端模板遗留文件，例如默认 Vite 示例说明。
- 补充最小冒烟测试说明。

### P1：完善审批体验

- 审批列表支持分页。
- 审批列表支持状态、时间范围、学生姓名或学号筛选。
- 审批失败时展示明确错误原因。
- 通过或驳回后自动刷新列表和详情状态。

### P2：完善后端审批能力

- 审批操作补充审计字段：审批人、审批时间、审批意见、操作来源。
- 学生只能查看自己的申请。
- 辅导员只能处理自己权限范围内的学生申请。
- 管理员拥有审计查询和配置维护能力。

### P3：统一申请中心

- 小程序新增“我的申请”统一入口。
- 将请假、证明、盖章等流程抽象为统一申请模型。
- 管理端提供统一待办、已办、详情和审批动作。
- 支持申请状态通知和结果提醒。

### P4：管理后台完善

- 学生导入导出。
- 通知发布和精准推送。
- 知识库 AI 摘要、推荐评估报表和更细粒度的治理工作流。
- 荣誉展示维护。
- 配置中心和字典维护。
- 审计日志检索与导出。

## 11. 验收建议

### 11.1 最小验收链路

1. 后端启动成功，访问 `http://localhost:18080` 不出现端口占用或数据库连接错误。
2. 小程序学生账号 `2023001/password` 登录成功。
3. 学生提交一条请假申请。
4. Vue 网页端辅导员账号 `10000001/counselor123` 登录成功。
5. 辅导员在待审批列表看到该申请。
6. 辅导员通过或驳回申请。
7. 学生端查看申请状态已更新。

### 11.2 推荐检查命令

后端编译测试：

```bash
cd demo
bash ./mvnw test
```

网页端构建：

```bash
cd web-counselor
npm run build
```

如命令失败，先判断是否为依赖未安装、数据库未启动、profile 不匹配或本地环境差异，不要直接修改无关业务代码。

## 12. 常见问题

### 12.1 后端启动时报数据库连接失败

先看上面的“启动方式总览”表，通常是 profile、数据库容器或环境变量三者之一没对上：

- 用 `h2` 时，确认没有额外传入 `postgres` / `kingbase` profile。
- 用 `dev,postgres` 时，确认 `db-postgres` 容器已启动，且 `DB_HOST=localhost`、`DB_PORT=5432`、`DB_NAME=ruc_platform`。
- 用 `docker-compose.postgres.yml` 时，确认 `app` 和 `db-postgres` 都已起来。
- 用 `server` / `prod` / `kingbase` 时，确认目标数据库地址、用户名、密码和驱动已配置正确。

### 12.2 网页端请求接口失败

检查后端是否已启动，并在 `web-counselor/.env.local` 中设置：

```text
VITE_API_BASE_URL=http://127.0.0.1:18080
```

然后重启 `npm run dev`。

### 12.3 小程序请求失败

检查 `miniprogram-1/utils/config.js` 的 `BASE_URL` 是否正确。本地调试还需要在微信开发者工具中关闭域名校验。

### 12.4 登录后没有权限

确认登录端侧和账号角色匹配：学生账号只能登录小程序，辅导员和管理员账号只能登录网页端。

## 13. 重要文档

- `documents/API.md`：接口清单和请求约定。
- `documents/双端登录调试说明.md`：登录联调说明。
- `documents/服务器部署方案.md`：服务器部署规划。
- `documents/项目分工与开发指南.md`：团队开发分工和协作说明。
- `demo/src/main/resources/db/README.md`：数据库迁移说明。


## 默认后端 profile

- 默认 `SPRING_PROFILES_ACTIVE=h2`，fresh clone 后无需本地数据库即可运行测试和基础启动。
- 如需 PostgreSQL/Kingbase，设置 `SPRING_PROFILES_ACTIVE=postgres` 或 `kingbase`，并通过 `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD` 配置连接。
- 微信 AppID/Secret 默认空值，通过 `WECHAT_MINIAPP_APPID`、`WECHAT_MINIAPP_SECRET` 注入，避免仓库携带个人密钥。

## 知识库本地开源能力

- 默认可直接运行：clone 后执行后端测试/启动会自动通过 Flyway 创建知识库索引任务表，Lucene 索引目录会按需创建。
- 文件解析：PDF/Word/TXT 依赖 Maven 开源库 PDFBox、Apache POI，无需额外服务。
- OCR：默认关闭；如需识别扫描 PDF/图片，可在 `knowledge.intelligence.ocr.enabled=true` 后配置本地 OCR 命令。
- LaTeX 编译：默认关闭；如需真实 PDF 编译，可配置本地 `tectonic` 或 `xelatex` 命令。未配置时仍使用轻量 HTML 预览。
- 全文索引：管理端可点击“重建全文索引”，后台会写入 `knowledge_index_task`，由定时任务异步解析和更新 Lucene。

### 知识库本地智能能力可选部署

项目默认不依赖外部在线服务；OCR、LaTeX 和 ONNX embedding 都是可选本地能力。

- 示例配置：`docker-compose.knowledge-intelligence.example.yml`
- OCR：安装 `tesseract` 与中文语言包后配置 `KNOWLEDGE_INTELLIGENCE_OCR_ENABLED=true`
- LaTeX：安装 `tectonic` 后配置 `KNOWLEDGE_INTELLIGENCE_LATEX_ENABLED=true`
- 本地 embedding：将模型放到 `runtime/models/embedding/model.onnx`，词表放到 `runtime/models/embedding/vocab.txt`
- 向量索引：默认使用本地 Lucene HNSW，目录可挂载到 `runtime/lucene/knowledge-vectors`

如果没有安装 OCR/LaTeX 或没有挂载 ONNX 模型，系统会自动降级：OCR/LaTeX 不启用，语义检索使用本地哈希向量回退，仍可 clone 后直接启动和迁移。

- 知识库本地智能一键部署：见 `deploy/knowledge/README.md`，支持 Docker Compose 初始化、启动、日志、索引重建与迁移说明。
