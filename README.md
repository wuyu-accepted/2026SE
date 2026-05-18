# 学院学生综合服务与党团管理平台

本项目是 2026SE 课程项目。根据最新规划，前端按用户角色拆分为双端：

- 学生端：微信小程序（仅学生使用）
- 辅导员/管理员端：Vue 网页端（审批与管理）
- 后端：Spring Boot 统一 API（共享一套业务与权限）

## 项目结构

```text
2026SE/
├─ miniprogram-1/          学生端微信小程序
├─ web-counselor/          辅导员/管理员 Vue 审批端
├─ demo/                   Spring Boot 后端
├─ docker-compose.postgres.yml
├─ docker-compose.kingbase.example.yml
├─ documents/API.md        接口文档（持续更新）
├─ documents/必要安装.md      环境安装与启动指南
├─ documents/Docker部署说明.md Docker 使用说明
├─ documents/后端脚手架.md    后端设计笔记
└─ README.md               本文件
```

## 一、产品与角色分工（新基线）

### 1. 角色边界

- 学生：信息查询、资料下载、发起申请、查看进度
- 学生骨干：在学生端使用普通学生能力，并可作为后续班团协作权限的扩展角色
- 辅导员：审批处理、驳回说明、批量查看学生申请
- 管理员：角色维护、配置管理、统计与审计

### 2. 前端边界

- 小程序（学生端）只保留学生高频功能：
  - 首页聚合
  - 知识库与模板下载
  - 党团进度
  - 请假/证明等“我的申请”
- Vue 网页端（辅导员端）承接审批与管理功能：
  - 待审批列表
  - 申请详情与通过/驳回
  - 审批历史
  - 后续可扩展统计看板

### 3. 后端边界

- 后端继续统一提供 `/api/**`，通过角色控制访问权限
- 学生接口与审批接口分前缀命名，避免职责混乱：
  - 学生侧：`/api/leave/me/**`
  - 审批侧：`/api/leave/reviewer/**`
- 登录注册按端隔离：
  - 小程序端仅允许学生、学生骨干注册和登录
  - Vue 管理端仅允许辅导员注册和登录
  - 超级管理员为内置 `admin` 账号，只允许登录，不开放注册

## 二、当前状态（2026-04）

- 已完成：
  - 首页真实数据聚合
  - 知识库与模板下载链路
  - 党团进度查询
  - 请假申请 MVP（创建、列表、详情、审批）
  - Vue 辅导员审批端基础闭环（待审批列表、详情审批、已处理列表）
  - 后端审批查询接口（`/api/leave/reviewer/applications`、`/api/leave/reviewer/applications/{id}`）
  - 端到端联调验收：学生提交 -> 网页端审批 -> 学生侧状态更新

## 三、下一阶段开发规划（按优先级）

### P1：完善审批体验（优先）

- 补齐审批列表筛选与分页（状态、时间、学生）
- 完善审批失败提示与重试体验

### P2：完善后端审批能力

- 请假接口补充：
  - 审批列表分页与筛选（状态、时间、学生）
  - 审批操作审计字段完善
- 权限收口：
  - 辅导员/管理员访问审批接口
  - 学生仅访问本人申请接口
  - 管理员独占角色维护、辅导员账号管理和审计日志

### P3：学生端小程序增强

- “我的申请”统一入口（请假、证明、盖章）
- 申请状态通知与结果提醒
- 真机下载体验与异常处理优化

## 四、协作与代码规范（新）

- 小程序目录 `miniprogram-1/` 仅放学生功能，不再加入辅导员审批页面
- 网页审批端单独目录维护，避免和小程序耦合
- 后端接口统一返回 `code=0` 表示成功
- 新增接口时同步更新 `documents/API.md`

## 五、本地运行

### 1. 启动后端（推荐 H2）

```bash
cd demo
mvn spring-boot:run
```

默认端口 `18080`，如端口冲突：

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=18081"
```

### 1.1 使用 Docker 启动后端 + PostgreSQL（推荐团队协作）

在仓库根目录执行：

```bash
docker compose -f docker-compose.postgres.yml up -d --build
```

- 后端地址：`http://localhost:18080`
- PostgreSQL：`localhost:5432`（db=`ruc_platform`，user=`postgres`，password=`postgres`）
- 停止并清理容器：

```bash
docker compose -f docker-compose.postgres.yml down
```

### 1.2 Kingbase Docker 说明

- 仓库已提供示例编排文件：`docker-compose.kingbase.example.yml`
- 你们需要替换其中的 Kingbase 镜像地址与初始化参数（取决于课程环境/授权）
- 替换后执行：

```bash
docker compose -f docker-compose.kingbase.example.yml up -d --build
```

### 2. 启动学生端小程序

1. 微信开发者工具导入 `miniprogram-1/`
2. 关闭域名校验
3. 配置 `miniprogram-1/utils/config.js` 的 `BASE_URL` 指向后端地址

### 3. 启动辅导员 Vue 端

```bash
cd web-counselor
npm install
npm run dev
```

默认访问地址：`http://localhost:5173`

- 首次进入可访问 `/login`，点击“进入审批系统”
- 当前前端默认请求后端地址：`http://127.0.0.1:18080`（见 `src/api/http.js`）
- 如后端端口不是 `18080`，请同步修改 `baseURL`

## 六、测试账号与数据

- 学生端测试账号：`2023001` / `password`
- 管理端辅导员账号：`counselor` / `counselor123`
- 管理端超级管理员账号：`admin` / `admin123`
- 小程序端登录请求会使用 `clientType=miniprogram`，管理端登录请求会使用 `clientType=web`
- 数据迁移会自动初始化演示数据（含请假场景）
- H2 为内存库，重启后会重置

## 七、里程碑验收标准

- M1：学生在小程序提交请假后，辅导员可在 Vue 端看到待审批数据
- M2：辅导员审批后，学生小程序状态实时可见
- M3：审批全链路具备操作记录与基础审计字段
