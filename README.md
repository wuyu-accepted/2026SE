# 学院学生综合服务与党团管理平台（新版说明）

本项目是 2026SE 课程项目，面向学院学生服务、辅导员审批、通知发布、智能知识库和党团管理场景。当前代码采用“学生小程序 + 辅导员/管理员网页端 + 统一后端 API”的三端架构，已覆盖学生服务、审批处理、通知反馈、知识库维护与本地智能检索等核心能力。

> 协作提醒：请不要直接向远程仓库上传未经团队确认的改动；提交前先本地自测并同步接口文档。

## 1. 项目定位

### 1.1 用户角色

- **学生**：仅在小程序端登录，账号由辅导员/管理员在网页端导入；可查看首页信息、浏览知识库、下载模板、查看党团进度、提交和跟踪申请。
- **学生骨干**：仅在小程序端登录，账号同样由网页端导入；拥有普通学生能力，并可承接通知反馈等班团协作事项。
- **辅导员**：仅在网页端登录；允许在网页端注册，但注册后必须由管理员审核通过后才能激活账号。
- **管理员**：内置超级管理员账号 `admin`，只允许登录，不开放注册；负责审核辅导员注册申请、维护账号、配置项和审计日志。

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

- 分端登录、退出和当前用户查询；学生/学生骨干由网页端导入，辅导员网页端注册后等待管理员审核，管理员固定为 `admin`。
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

- 学生信息分页查询、单个导入、CSV 模板下载和 CSV 批量导入。
- 管理端角色相关接口。
- 辅导员账号管理和注册申请审核；管理员工作台会提示待审核辅导员注册申请。
- 审计日志相关结构。
- 管理端党团活动、流程管理、导入和学生进度维护相关模块。
- 通知发布、通知下架、消息反馈处理和学生端消息阅读/置顶。

## 5. 本地运行

### 5.0 启动方式总览

| 方式 | 机理 | 适合场景 | 启动 | 关闭 | 配置位置 |
| --- | --- | --- | --- | --- | --- |
| `h2` 快速启动 | 后端直接连接内存数据库，启动即建库，退出即清空 | 拉代码后先跑通、演示、测试 | `cd demo && bash ./mvnw spring-boot:run -Dspring-boot.run.profiles=h2` | 直接 `Ctrl+C` 停止进程 | `demo/src/main/resources/application.yml`、`demo/src/main/resources/application-h2.yml` |
| 本机后端 + Docker PostgreSQL | PostgreSQL 由 Docker 提供，后端在本机运行 | 日常开发、接口调试、保留数据 | 先 `docker compose -f docker-compose.postgres.yml up -d db-postgres`，再 `cd demo && bash ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev,postgres"` | `Ctrl+C` 停止后端；`docker compose -f docker-compose.postgres.yml stop db-postgres` 停数据库 | `demo/src/main/resources/application-dev.yml`、`demo/src/main/resources/application-postgres.yml`、`docker-compose.postgres.yml` |
| 全 Docker PostgreSQL | 后端和数据库都在 Docker 容器里运行 | 团队联调、接近部署环境 | `docker compose -f docker-compose.postgres.yml up -d --build` | `docker compose -f docker-compose.postgres.yml down` | `docker-compose.postgres.yml`、`demo/src/main/resources/application-docker-postgres.yml` |
| Kingbase / 生产类部署 | 对接 Kingbase 或按服务器部署方式连接远程数据库 | 课程服务器、正式部署、Kingbase 环境 | 按环境选择 `application-prod.yml` 或 `application-server.yml`，再启动后端 | 停止对应容器或服务进程 | `demo/src/main/resources/application-prod.yml`、`demo/src/main/resources/application-server.yml`、`demo/src/main/resources/application-kingbase.yml`、`demo/src/main/resources/application-docker-kingbase.yml` |

补充说明：

- `dev` 主要控制日志级别，不单独决定数据库；真正决定数据库的是 `h2`、`postgres`、`docker-postgres`、`kingbase` 这类 profile。
- `SPRING_PROFILES_ACTIVE` 可以在命令行、环境变量或容器环境中设置；优先级通常高于 `application.yml` 默认值。
- `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD` 这类数据库参数主要在 `application-*.yml` 和 `docker-compose.postgres.yml` 里通过环境变量注入。
- 如果你只是想“先跑起来”，优先选 `h2`；如果你想“开发时数据能保留”，优先选 `postgres`；如果你想“和部署尽量一致”，优先选 `docker-postgres`。

### 5.1 后端：H2 快速启动

默认配置当前激活的是 `h2` profile（来自 `SPRING_PROFILES_ACTIVE:h2`），fresh clone 后无需本地数据库即可快速跑通。可以直接启动：

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

### 5.2 详细启动说明

更完整的启动/关闭/配置说明已收敛到上面的“启动方式总览”表。这里仅保留一个原则：

- 想快速验证功能，直接用 `h2`。
- 想日常开发并保留数据，用 `dev,postgres`。
- 想整套跑在 Docker 里，用 `docker-compose.postgres.yml`。
- 想部署到课程服务器或 Kingbase 环境，按 `application-server.yml` / `application-prod.yml` / `application-kingbase.yml` 选择对应 profile。

### 5.3 启动 Vue 管理端

```bash
cd web-counselor
npm install
npm run dev
```

远程访问：`http://10.10.0.6`。

如果只在本机开发，也可以访问 Vite 默认地址 `http://localhost:5173`。

网页端请求基地址来自 `VITE_API_BASE_URL`，未配置时使用同源路径。开发时可创建本地环境文件：

```bash
cd web-counselor
echo 'VITE_API_BASE_URL=http://10.10.0.6:18080' > .env.local
```

### 5.6 启动微信小程序

1. 使用微信开发者工具导入 `miniprogram-1/`。
2. 本地调试时关闭域名校验。
3. 检查 `miniprogram-1/utils/config.js` 中的 `BASE_URL`，确保指向后端地址，例如 `http://10.10.0.6:18080`。
4. 使用学生账号登录并测试首页、知识库、党团进度、请假申请。

## 6. 测试账号

当前 Flyway 初始化和本地开发库会准备以下测试账号。账号由 `V37__reset_miniprogram_demo_accounts.sql` 和 `V38__reset_web_demo_accounts.sql` 固定校准，密码会在后端启动时由 `AdminAccountInitializer` 再次确认。

| 端侧 | 角色 | 登录账号 | 姓名/显示名 | 密码 |
| --- | --- | --- | --- | --- |
| 小程序 | 学生 | `2023001` | `张同学` | `password` |
| 小程序 | 学生 | `2023002` | `李同学` | `password` |
| 小程序 | 学生骨干 | `2023003` | `王同学（骨干）` | `password` |
| 小程序 | 学生 | `2023004` | `测试学生A（普通）` | `password` |
| 小程序 | 学生 | `2023005` | `测试学生B（普通）` | `password` |
| 小程序 | 学生骨干 | `2023006` | `测试学生C（骨干）` | `password` |
| 网页端 | 辅导员 | `10000001` | `王辅导员` | `counselor123` |
| 网页端 | 超级管理员 | `admin` | `系统管理员` | `admin123` |

登录请求会根据端侧传入 `clientType`：

- 小程序：`clientType=miniprogram`，仅允许 `student`、`cadre`。
- 网页端：`clientType=web`，仅允许 `counselor`、`admin`。
- 小程序端只保留登录入口，不开放学生/学生骨干自助注册。
- 学生和学生骨干账号只能通过网页端学生管理导入，学号必须是纯数字。
- 辅导员允许在网页端注册，工号必须是纯数字；注册后状态为待审核，需管理员在网页端通过后才能登录。
- 网页端登录框只允许填写纯数字工号或固定管理员账号 `admin`。
- `admin` 是内置超级管理员，只允许登录，不开放注册，也不允许新增第二个管理员账号。
- 学生身份/年级使用已有 `grade` 字段，格式为四位年份加本/硕/博，例如 `2023本`、`2022硕`、`2021博`；个人中心和学生管理中会展示该字段。
- 历史账号 `stu1/stu2/stu3` 和 `00000001/00000002/00000003` 已被统一迁移为 `2023004/2023005/2023006`。

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
- `POST /api/admin/students`
- `GET /api/admin/students/import-template`
- `POST /api/admin/students/batch-import`
- `GET /api/admin/counselors`
- `POST /api/admin/counselors`
- `PUT /api/admin/counselors/{id}`
- `POST /api/admin/counselors/{id}/approve`
- `DELETE /api/admin/counselors/{id}`
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
- 学生账号和学生骨干账号只允许从小程序端登录；后端会拒绝这两类账号从网页端登录。
- 辅导员账号和管理员账号只允许从网页端登录；后端会拒绝这两类账号从小程序端登录。
- 辅导员注册账号在管理员审核前处于未激活状态，不能登录网页端。
- 知识库公开查询接口：`/api/knowledge/**` 面向登录用户，列表默认只返回已发布内容；管理端维护走 `/api/admin/knowledge/**`。
- 管理端业务接口：`/api/admin/**`、`/api/leave/reviewer/**`，默认允许 `counselor` 或 `admin`。
- 超级管理员接口：`/api/admin/roles/**`、`/api/admin/counselors/**`、`/api/admin/audit-logs/**`、`/api/wechat/official-account/menu/**`，仅允许 `admin`。
- 学生导入要求学号为纯数字；辅导员工号要求为纯数字，管理员账号例外固定为 `admin`。

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

1. 后端启动成功，访问 `http://10.10.0.6:18080` 不出现端口占用或数据库连接错误。
2. 小程序学生账号 `2023001/password` 登录成功；学生骨干账号可用 `2023003/password` 或 `2023006/password` 验证。
3. 学生提交一条请假申请。
4. Vue 网页端辅导员账号 `10000001/counselor123` 登录成功。
5. 辅导员在待审批列表看到该申请。
6. 辅导员通过或驳回申请。
7. 学生端查看申请状态已更新。
8. 管理员账号 `admin/admin123` 登录网页端，在工作台和设置页能看到辅导员注册申请提示。
9. 管理员/辅导员在学生管理页下载 CSV 模板，上传 CSV 后能批量导入学生，且导入学生的学号为纯数字、年级写入 `grade` 字段。

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
VITE_API_BASE_URL=http://10.10.0.6:18080
```

然后重启 `npm run dev`。

如果 Vite 控制台出现 `http proxy error: /api/auth/login` 和 `ECONNREFUSED 10.10.0.6:18080`，说明前端已启动但后端没有监听 `18080`，或当前机器无法访问 `10.10.0.6`。使用 Docker PostgreSQL 全套环境时，检查并启动后端容器：

```bash
docker compose -f docker-compose.postgres.yml ps
docker compose -f docker-compose.postgres.yml up -d --build app
```

如果 `2026se-db-postgres` 正常但 `2026se-app` 退出，优先查看后端日志。常见原因包括数据库连接失败、端口占用，或 Flyway 报已执行迁移脚本的 checksum 不一致。已执行过的 `V__*.sql` 不应再修改；需要调整数据时新增下一版迁移脚本。

### 12.3 小程序请求失败

检查 `miniprogram-1/utils/config.js` 的 `BASE_URL` 是否正确。本地调试还需要在微信开发者工具中关闭域名校验。

### 12.4 登录后没有权限

确认登录端侧和账号角色匹配：学生/学生骨干只能登录小程序，辅导员/管理员只能登录网页端。辅导员注册后还需要管理员审核通过；网页端普通工号必须是纯数字，只有管理员账号可以填写 `admin`。

### 12.5 学生 CSV 导入格式

学生管理页提供“下载模板”和“批量导入”。CSV 表头支持中文表头，推荐按模板填写：

 ```csv
学号,姓名,初始密码,身份类型,学生身份/年级,性别,专业,班级,政治面貌,手机号,邮箱,生源地,宿舍
2023010,测试学生D,password,student,2023本,男,软件工程,2023级1班,共青团员,13900000010,stu10@example.com,北京,1号楼101
2023011,测试骨干E,password,cadre,2023本,女,软件工程,2023级1班,中共预备党员,13900000011,stu11@example.com,上海,1号楼103
```

其中“身份类型”填写 `student` 或 `cadre`；“学生身份/年级”会写入已有 `grade` 字段。

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
