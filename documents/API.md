# API 清单（2026-04 当前版本）

> 说明：本项目采用双端分工。
> - 学生端：微信小程序
> - 辅导员/管理员端：Vue 网页（`web-counselor`）
> - 统一返回：`code = 0` 表示成功

## 1. 已实现并联调通过

### 1.1 认证与登录态

- `POST /api/auth/wx-login` 微信登录（开发环境支持 `code=demo`）
- `GET /api/auth/me` 获取当前用户信息
- `POST /api/auth/logout` 退出登录

### 1.2 首页聚合

- `GET /api/home` 首页聚合数据

### 1.3 知识库与模板

- `GET /api/knowledge/articles` 知识条目列表
- `GET /api/knowledge/articles/{id}` 知识条目详情
- `GET /api/knowledge/templates` 模板列表
- `GET /api/knowledge/templates/{id}/download` 模板下载

### 1.4 党团进度

- `GET /api/party/me/progress` 获取本人党团进度
- `GET /api/party/me/records` 获取本人党团记录
- `GET /api/party/me/reminders` 获取本人提醒

### 1.5 请假申请（学生侧）

- `POST /api/leave/me/applications` 创建请假申请
- `GET /api/leave/me/applications` 我的请假列表
- `GET /api/leave/me/applications/{id}` 我的请假详情

### 1.6 请假审批（辅导员/管理员侧）

- `GET /api/leave/reviewer/applications?status={status}` 审批列表（可按状态筛选）
- `GET /api/leave/reviewer/applications/{id}` 审批详情
- `POST /api/leave/reviewer/applications/{id}/approve` 审批通过
- `POST /api/leave/reviewer/applications/{id}/reject` 审批驳回

## 2. 请求约定

- 鉴权头：`Authorization`
- 内容类型：`application/json`
- 时间字段：ISO 或后端默认序列化格式（按实际返回为准）

### 2.1 学生端消息详情（增强）

- `GET /api/messages/{id}` 获取当前登录学生自己的消息详情，并自动标记已读

返回示例：

```json
{
  "id": 1001,
  "noticeId": 1,
  "title": "党团活动报名通知",
  "summary": "本周五前完成报名",
  "content": "请符合条件的同学及时提交报名材料。",
  "noticeType": "党团",
  "tag": "重要",
  "priority": 1,
  "readStatus": 1,
  "readTime": "2026-05-20T11:00:00",
  "publishTime": "2026-05-20T10:00:00",
  "createdAt": "2026-05-20T10:00:01"
}
```

说明：消息详情接口会校验 `user_message.user_id`，学生不能查看或标记他人的消息。

## 3. 关键 DTO 示例

### 3.1 创建请假

`POST /api/leave/me/applications`

```json
{
  "title": "参加学术会议请假",
  "reason": "参加学院推荐的学术会议",
  "leaveStartDate": "2026-04-23",
  "leaveEndDate": "2026-04-24",
  "contactPhone": "13800138001",
  "fileId": null
}
```

### 3.2 审批动作

`POST /api/leave/reviewer/applications/{id}/approve`

```json
{
  "comment": "材料完整，审批通过"
}
```

`POST /api/leave/reviewer/applications/{id}/reject`

```json
{
  "comment": "请补充附件后重新提交"
}
```

## 4. 规划中接口（未全部落地）

以下为需求文档中的后续规划，当前版本未承诺全部实现：

- 学生基础信息完善与管理端学生列表/导入导出
- 管理端通知发布与精准推送
- 电子证明、盖章、重批等完整审批族接口
- 荣誉展示的管理端维护能力
- 后台审计日志、配置中心、字典维护
- 学业分析相关预留接口

## 5. 管理端通知发布（新增）

> 负责模块：HBW 通知板块。管理端接口默认允许 `counselor` 或 `admin` 访问。

### 5.1 通知状态与发布范围

通知状态：

- `0`：草稿
- `1`：已发布
- `2`：已下架

发布范围字段：

| 字段 | 说明 |
|---|---|
| `grade` | 年级单值，留空表示不限；兼容旧请求 |
| `grades` | 年级多选数组，留空表示不限；与 `grade` 合并去重 |
| `major` | 专业单值，留空表示不限；兼容旧请求 |
| `majors` | 专业多选数组，留空表示不限；与 `major` 合并去重 |
| `className` | 班级，留空表示不限 |
| `authType` | 学生身份，留空表示不限；可选 `student`、`cadre` |

多个发布范围字段同时填写时取交集。发布时后端会按 `student_profile` 匹配目标学生，并写入 `user_message`。

### 5.2 分页查询通知

`GET /api/admin/notices?pageNum=1&pageSize=10&keyword=报名&noticeType=党团&status=1`

查询参数：

- `pageNum`：页码，默认 `1`
- `pageSize`：每页大小，默认 `10`，最大 `100`
- `keyword`：标题/摘要关键词
- `noticeType`：通知类型
- `status`：通知状态

### 5.3 查询通知详情

`GET /api/admin/notices/{id}`

返回通知正文、发布范围和投递人数。

### 5.4 创建通知草稿

`POST /api/admin/notices`

```json
{
  "title": "党团活动报名通知",
  "summary": "本周五前完成报名",
  "content": "请符合条件的同学及时提交报名材料。",
  "noticeType": "党团",
  "tag": "重要",
  "priority": 1,
  "target": {
    "grade": "2023",
    "grades": ["2023", "2024"],
    "major": "软件工程",
    "majors": ["软件工程", "人工智能"],
    "className": "",
    "authType": "cadre"
  }
}
```

创建后状态为草稿，不会投递到学生端。

### 5.5 编辑通知

`PUT /api/admin/notices/{id}`

请求体同创建通知。已发布通知允许编辑标题、摘要、正文、类型、标签和优先级，但不会改变已投递人群。

### 5.6 发布通知

`POST /api/admin/notices/{id}/publish`

发布规则：

- 仅未发布且未投递过的通知可以发布。
- 发布时按通知保存的 `target` 条件筛选学生。
- 无匹配学生时返回业务错误。
- 发布成功后写入 `user_message`，学生端可通过 `/api/messages/recent` 查看。

返回示例：

```json
{
  "noticeId": 1,
  "deliveredCount": 36
}
```

### 5.7 预估发布人数

`GET /api/admin/notices/{id}/target-estimate`

按当前通知保存的发布范围筛选学生，但不实际投递消息。

返回示例：

```json
{
  "target": {
    "grade": "2023",
    "grades": ["2023", "2024"],
    "major": "软件工程",
    "majors": ["软件工程", "人工智能"],
    "className": "",
    "authType": "cadre"
  },
  "targetCount": 12
}
```

### 5.8 查询阅读统计

`GET /api/admin/notices/{id}/stats`

返回示例：

```json
{
  "noticeId": 1,
  "deliveredCount": 36,
  "readCount": 20,
  "unreadCount": 16,
  "readRate": 55.56
}
```

### 5.9 下架通知

`POST /api/admin/notices/{id}/offline`

第一阶段下架只更新通知状态，学生端已收到的消息仍保留。

### 5.10 删除通知

`DELETE /api/admin/notices/{id}`

第一阶段仅支持删除未发布且未投递的通知草稿；已发布通知需先下架，已有投递记录的通知暂不支持删除。
