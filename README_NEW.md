# 学院学生综合服务与党团管理平台（新版说明）

本项目是 2026SE 课程项目，面向学院学生服务、辅导员审批和党团管理场景。当前代码采用“学生小程序 + 辅导员/管理员网页端 + 统一后端 API”的三端架构，已经完成请假申请审批闭环，并保留知识库、模板下载、党团进度、学生管理等扩展能力。

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
│  │  ├─ party/                  学生侧党团进度
│  │  ├─ admin/                  管理端角色、党团、审计、辅导员管理
│  │  ├─ knowledgeness/          知识库与模板
│  │  ├─ notice/                 消息通知
│  │  ├─ file/                   文件上传下载
│  │  └─ common/                 通用返回、异常、分页等
│  └─ src/main/resources
│     ├─ application-*.yml       H2/PostgreSQL/Kingbase/Docker 等配置
│     └─ db/migration/           Flyway 数据库迁移脚本
├─ miniprogram-1/                学生端微信小程序
│  ├─ pages/                     小程序页面
│  └─ utils/                     请求、登录态、配置、工具函数
├─ web-counselor/                辅导员/管理员 Vue 端
│  ├─ src/api/                   前端接口封装
│  ├─ src/router/                路由与角色守卫
│  ├─ src/views/                 页面视图
│  └─ src/layouts/               后台布局
├─ documents/                    API、部署、需求和开发说明
├─ deploy/                       部署相关文件
├─ docker-compose.postgres.yml   后端 + PostgreSQL 本地编排
├─ docker-compose.kingbase.example.yml
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
- 知识库文章与模板下载。
- 智能知识库管理、学生检索与规则推荐；知识内容支持上传资料文件，也支持 Markdown/LaTeX 在线编排并保留可编辑源文件。
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
- 管理端党团活动与流程管理相关模块。

## 5. 本地运行

### 5.1 后端：H2 快速启动

默认配置当前激活的是 `kingbase` profile。若只是本地快速跑通，建议显式使用 H2：

```bash
cd demo
bash ./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

默认端口：`18080`。

如果端口冲突：

```bash
cd demo
bash ./mvnw spring-boot:run -Dspring-boot.run.profiles=h2 -Dspring-boot.run.arguments=--server.port=18081
```

H2 是内存库，服务重启后数据会重置，适合演示和开发调试。

### 5.2 后端开发：Docker PostgreSQL + 本地 Spring Boot（推荐）

开发后端时，推荐只用 Docker 启动 PostgreSQL，后端在本机/WSL 直接运行，便于热重启、看日志和调试断点。

先在仓库根目录启动数据库：

```bash
cd /home/octopus/2026SE
docker compose -f docker-compose.postgres.yml up -d db-postgres
```

如果之前启动过 Docker 后端容器，先停掉它，避免占用 `18080`：

```bash
docker compose -f docker-compose.postgres.yml stop app
```

然后启动本地后端：

```bash
cd /home/octopus/2026SE/demo
bash ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev,postgres"
```

启动日志中应出现：

```text
The following 2 profiles are active: "dev", "postgres"
```

本地后端将连接 Docker 中的 PostgreSQL：`localhost:5432/ruc_platform`，账号密码均为 `postgres`。

如需确认 Java/Maven 环境：

```bash
java -version
javac -version
cd /home/octopus/2026SE/demo
bash ./mvnw -v
```

项目要求 Java 17，`./mvnw -v` 应显示 `Java version: 17...`。

### 5.3 后端 + PostgreSQL：全 Docker 启动

团队联调建议使用 PostgreSQL 编排，数据库数据会持久化到 Docker volume：

```bash
docker compose -f docker-compose.postgres.yml up -d --build
```

服务信息：

- 后端地址：`http://localhost:18080`
- PostgreSQL 地址：`localhost:5432`
- 数据库：`ruc_platform`
- 用户名：`postgres`
- 密码：`postgres`

停止服务：

```bash
docker compose -f docker-compose.postgres.yml down
```

如需同时清理数据库 volume，请谨慎执行：

```bash
docker compose -f docker-compose.postgres.yml down -v
```

### 5.4 Kingbase 说明

仓库提供 `docker-compose.kingbase.example.yml` 作为示例。由于 Kingbase 镜像、授权和初始化参数通常依赖课程或部署环境，使用前需要先替换镜像地址、账号、密码和数据库名。

后端也提供了 `application-kingbase.yml`、`application-docker-kingbase.yml`、`application-prod.yml` 等配置，请按实际环境选择 profile。

### 5.5 启动 Vue 管理端

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

### 5.6 启动微信小程序

1. 使用微信开发者工具导入 `miniprogram-1/`。
2. 本地调试时关闭域名校验。
3. 检查 `miniprogram-1/utils/config.js` 中的 `BASE_URL`，确保指向后端地址，例如 `http://127.0.0.1:18080`。
4. 使用学生账号登录并测试首页、知识库、党团进度、请假申请。

## 6. 测试账号

| 端侧 | 角色 | 账号 | 密码 |
| --- | --- | --- | --- |
| 小程序 | 学生 | `2023001` | `password` |
| 网页端 | 辅导员 | `counselor` | `counselor123` |
| 网页端 | 超级管理员 | `admin` | `admin123` |

登录请求会根据端侧传入 `clientType`：

- 小程序：`clientType=miniprogram`，仅允许 `student`、`cadre`。
- 网页端：`clientType=web`，仅允许 `counselor`、`admin`。
- `admin` 是内置超级管理员，只允许登录，不开放注册。

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
- `POST /api/leave/me/applications`
- `GET /api/leave/me/applications`
- `GET /api/leave/me/applications/{id}`

### 7.3 管理端

- `GET /api/leave/reviewer/applications?status={status}`
- `GET /api/leave/reviewer/applications/{id}`
- `POST /api/leave/reviewer/applications/{id}/approve`
- `POST /api/leave/reviewer/applications/{id}/reject`
- `GET /api/admin/students`

完整接口以 `documents/API.md` 为准。新增或调整接口时必须同步更新该文件。

## 8. 权限边界

- 学生侧接口：`/api/home`、`/api/student/**`、`/api/party/me/**`、`/api/leave/me/**`、`/api/messages/**`，仅允许 `student` 或 `cadre`。
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
- 知识库推荐策略配置、向量检索或 AI 摘要等智能化增强。
- 荣誉展示维护。
- 配置中心和字典维护。
- 审计日志检索与导出。

## 11. 验收建议

### 11.1 最小验收链路

1. 后端启动成功，访问 `http://localhost:18080` 不出现端口占用或数据库连接错误。
2. 小程序学生账号 `2023001/password` 登录成功。
3. 学生提交一条请假申请。
4. Vue 网页端辅导员账号 `counselor/counselor123` 登录成功。
5. 辅导员在待审批列表看到该申请。
6. 辅导员通过或驳回申请。
7. 学生端查看申请状态已更新。

### 11.2 推荐检查命令

后端编译测试：

```bash
cd demo
mvn test
```

网页端构建：

```bash
cd web-counselor
npm run build
```

如命令失败，先判断是否为依赖未安装、数据库未启动、profile 不匹配或本地环境差异，不要直接修改无关业务代码。

## 12. 常见问题

### 12.1 后端启动时报 Kingbase 连接失败

默认 profile 可能指向 Kingbase。若本地没有 Kingbase，请使用 H2：

```bash
cd demo
bash ./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

若需要连接本地 Docker PostgreSQL，请使用：

```bash
cd /home/octopus/2026SE
docker compose -f docker-compose.postgres.yml up -d db-postgres
docker compose -f docker-compose.postgres.yml stop app
cd /home/octopus/2026SE/demo
bash ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev,postgres"
```

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


## 知识库本地开源能力

- 默认可直接运行：clone 后执行后端测试/启动会自动通过 Flyway 创建知识库索引任务表，Lucene 索引目录会按需创建。
- 文件解析：PDF/Word/TXT 依赖 Maven 开源库 PDFBox、Apache POI，无需额外服务。
- OCR：默认关闭；如需识别扫描 PDF/图片，可在 `knowledge.intelligence.ocr.enabled=true` 后配置本地 OCR 命令。
- LaTeX 编译：默认关闭；如需真实 PDF 编译，可配置本地 `tectonic` 或 `xelatex` 命令。未配置时仍使用轻量 HTML 预览。
- 全文索引：管理端可点击“重建全文索引”，后台会写入 `knowledge_index_task`，由定时任务异步解析和更新 Lucene。
