# AI 助手与模型供应商配置设计方案

## 1. 背景与目标

当前项目已经具备学生小程序、Vue 管理端、Spring Boot 后端、Sa-Token 登录鉴权、角色权限拦截、知识库检索与推荐能力。用户希望新增学生端 AI 助手，并允许管理员在管理端决定使用哪一家 AI API，重点支持 DeepSeek，同时可配置模型参数。

本设计目标是在现有架构上引入一个可配置、可降级、受权限约束的 AI 助手能力：

- 学生端新增 AI 助手入口，用户可以用自然语言咨询政策、流程、通知和功能入口。
- 后端统一处理 AI 对话、知识检索、功能入口匹配和权限过滤。
- 管理端系统设置增加“AI 助手配置”，管理员可维护 DeepSeek 或 OpenAI 兼容 API 配置。
- API Key 仅保存在后端，管理端只显示脱敏信息，小程序永不接触供应商密钥。
- AI 调用失败或未启用时，系统降级为“知识库检索 + 功能入口推荐”。

## 2. 当前项目依据

### 2.1 小程序现状

`miniprogram-1/app.json` 当前底部 `tabBar` 仍为 4 项：首页、通知、服务、个人中心，且页面列表未包含 AI 助手页面。微信原生 tabBar 最多支持 5 项，因此可以新增 `pages/ai-chat/ai-chat` 并将其作为第 5 个 tab。若希望“位于首页左边”，可将 AI 助手放在 tabBar 第一项，首页放第二项。

服务入口目前主要集中在 `miniprogram-1/pages/service/service.js` 的 `blocks/items` 中，包含党团、请假、证明、政策知识库、学业分析、学生画像、荣誉等入口。AI 助手的“帮用户检索功能入口”能力应复用这些入口信息，但需要抽出为统一的功能入口注册表，避免服务页和 AI 助手维护两份入口。

小程序网络请求已统一封装在 `miniprogram-1/utils/request.js`，会自动携带登录 token。因此 AI 页面只需要调用平台后端 `/api/ai/chat`，不直接调用 DeepSeek 或其他模型接口。

### 2.2 管理端现状

`web-counselor/src/views/SettingsView.vue` 当前系统设置包含“辅导员账户管理”和“审计日志”两个 tab，并已增加辅导员注册审核能力。`web-counselor/src/router/index.js` 将 `/settings` 限定为 `ADMIN_ROLES = ['admin']`。因此 AI 配置适合直接加到系统设置页作为第三个 tab，不需要新增独立顶级菜单。

管理端 API 封装目前有 `web-counselor/src/api/admin.js`，可新增 AI 配置相关函数；HTTP 拦截器已统一处理 token。

### 2.3 后端现状

后端使用 Spring Boot 3.2.5、Java 17、MyBatis-Plus、Flyway、Sa-Token。`ApiRoleInterceptor` 当前将 `/api/admin/roles`、`/api/admin/counselors`、`/api/admin/audit-logs`、公众号菜单等限制为管理员，将其他 `/api/admin/**` 限制为辅导员或管理员。AI 配置属于敏感系统配置，应在拦截器中显式将 `/api/admin/ai-config` 限制为管理员。

知识库已有学生端接口：

- `GET /api/knowledge/articles`
- `GET /api/knowledge/articles/{id}`
- `GET /api/knowledge/templates`
- `GET /api/knowledge/recommendations`
- `POST /api/knowledge/behavior`
- `GET /api/knowledge/suggestions`
- `GET /api/knowledge/spellcheck`

知识库服务已具备关键词检索、本地全文检索、语义检索、推荐和行为记录能力，是 AI 助手 RAG 上下文的主要来源。

Flyway 迁移文件当前最高版本为 `V29__seed_more_demo_data.sql`，新增 AI 配置应使用 `V30__ai_assistant_config.sql`。

## 3. DeepSeek 接入约束

DeepSeek 提供 OpenAI 风格的 Chat Completions API，推荐使用 OpenAI 兼容客户端抽象接入。首版按 DeepSeek 官方 Chat Completion 参数支持以下字段：

- `model`：如 `deepseek-chat`、`deepseek-reasoner`。
- `messages`：系统提示词、用户问题、检索上下文。
- `temperature`：采样温度。
- `top_p`：核采样参数。
- `max_tokens`：最大输出 token 数。
- `frequency_penalty`：频率惩罚。
- `presence_penalty`：存在惩罚。
- `response_format`：可选，用于 JSON 输出。
- `stream`：是否流式输出。

DeepSeek API 地址采用可配置方式，首版默认值为 `https://api.deepseek.com`，调用路径为 `/chat/completions`。OpenAI 兼容供应商允许管理员设置 `baseUrl`、`model` 和参数。

## 4. 功能范围

### 4.1 学生端 AI 助手

学生可以：

- 在底部 tab 进入 AI 助手。
- 输入自然语言问题。
- 看到 AI 回答、引用知识、推荐功能入口。
- 点击功能入口跳转到可访问的小程序页面。
- 当 AI 服务不可用时，仍能看到基于知识库和功能入口的兜底建议。

首版不做语音输入、图片输入、多轮长记忆和实时流式打字效果。对话历史可以存储最近消息，但不是首版验收必需项。

### 4.2 管理端 AI 配置

管理员可以：

- 查看当前启用的 AI 配置。
- 新增、编辑、删除 AI 配置。
- 选择供应商：`deepseek`、`openai-compatible`，预留 `openai`。
- 配置 Base URL、模型、API Key、温度、Top P、最大输出、超时、是否启用流式、知识检索数量、入口推荐数量、系统提示词。
- 将某个配置设为当前启用配置。
- 使用测试问题验证连接、模型和参数是否可用。

API Key 新增或修改时可填写；编辑时留空表示保留原密钥。列表和详情接口只返回脱敏后的 key 摘要。

### 4.3 后端 AI 服务

后端负责：

- 根据当前用户身份和角色过滤上下文。
- 检索知识库中当前用户可见的资料。
- 匹配当前用户可访问的功能入口。
- 组装模型提示词。
- 调用当前启用的模型供应商。
- 解析模型输出，返回统一格式。
- 记录 AI 对话审计、耗时、错误类型和 token 用量。
- 在模型异常时降级为规则回答。

## 5. 总体架构

### 5.1 模块划分

新增后端包：`demo/src/main/java/com/ruc/platform/ai`。

建议结构：

```text
ai
├── config
│   └── AiAssistantProperties.java
├── controller
│   ├── AiChatController.java
│   └── AdminAiConfigController.java
├── client
│   ├── AiChatClient.java
│   └── OpenAiCompatibleChatClient.java
├── dto
│   ├── AiChatRequest.java
│   ├── AiConfigSaveDTO.java
│   └── AiConfigTestDTO.java
├── entity
│   ├── AiProviderConfig.java
│   ├── AiConversation.java
│   └── AiMessage.java
├── mapper
│   ├── AiProviderConfigMapper.java
│   ├── AiConversationMapper.java
│   └── AiMessageMapper.java
├── service
│   ├── AiChatService.java
│   ├── AiConfigService.java
│   ├── AiContextService.java
│   └── AiFeatureEntryService.java
└── vo
    ├── AiChatResponse.java
    ├── AiCitationVO.java
    ├── AiActionVO.java
    └── AiConfigVO.java
```

### 5.2 调用链路

```text
小程序 ai-chat 页面
  -> POST /api/ai/chat
    -> Sa-Token 校验登录
    -> AiContextService 检索知识库与业务上下文
    -> AiFeatureEntryService 匹配可用功能入口
    -> AiConfigService 读取当前 active 配置
    -> OpenAiCompatibleChatClient 调用 DeepSeek 或兼容 API
    -> AiChatService 解析和兜底
  <- answer + citations + actions
```

### 5.3 管理端配置链路

```text
管理端 SettingsView.vue / AI 助手配置 tab
  -> GET /api/admin/ai-config
  -> POST /api/admin/ai-config
  -> PUT /api/admin/ai-config/{id}
  -> POST /api/admin/ai-config/{id}/activate
  -> POST /api/admin/ai-config/{id}/test
```

## 6. 数据模型设计

### 6.1 `ai_provider_config`

用于保存模型供应商配置。允许保存多套配置，但同一时间只有一套启用配置。

```sql
CREATE TABLE ai_provider_config (
    id BIGINT PRIMARY KEY,
    config_name VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    base_url VARCHAR(255) NOT NULL,
    api_key_cipher TEXT,
    api_key_mask VARCHAR(64),
    model VARCHAR(100) NOT NULL,
    temperature DECIMAL(4,2),
    top_p DECIMAL(4,2),
    max_tokens INT,
    presence_penalty DECIMAL(4,2),
    frequency_penalty DECIMAL(4,2),
    response_format VARCHAR(32),
    timeout_seconds INT NOT NULL DEFAULT 30,
    stream_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    retrieval_top_k INT NOT NULL DEFAULT 5,
    action_top_k INT NOT NULL DEFAULT 3,
    system_prompt TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT
);
```

字段说明：

- `provider`：`deepseek`、`openai-compatible`、`openai`。
- `api_key_cipher`：加密后的 API Key。后端使用 AES-GCM 存储，生产环境应设置 `AI_CONFIG_SECRET`，避免使用默认开发密钥。
- `api_key_mask`：脱敏展示，如 `sk-****abcd`。
- `active`：当前 AI 助手实际使用的配置。
- `enabled`：配置是否可用。`active=true` 但 `enabled=false` 时视为 AI 未启用。
- `stream_enabled`：首版保存配置，但小程序首版可以不消费流式。

### 6.2 `ai_conversation`

用于保存会话。首版可只保存最近上下文，不强依赖完整历史。

```sql
CREATE TABLE ai_conversation (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    client_type VARCHAR(32) NOT NULL DEFAULT 'miniprogram',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 6.3 `ai_message`

用于保存问答、引用、入口和调用状态。

```sql
CREATE TABLE ai_message (
    id BIGINT PRIMARY KEY,
    conversation_id BIGINT,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    provider VARCHAR(50),
    model VARCHAR(100),
    citations_json TEXT,
    actions_json TEXT,
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    latency_ms INT,
    status VARCHAR(32) NOT NULL DEFAULT 'success',
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 6.4 `ai_feature_entry`

功能入口可以先用代码配置，后续再表化。为了首版降低复杂度，推荐先创建后端常量注册表，不新增表。入口字段如下：

```text
code, name, description, path, tabPage, keywords, requiredRoles, scenarioCodes
```

若后续希望管理端可维护入口，再迁移为数据库表。

## 7. 后端接口设计

### 7.1 学生端 AI 对话接口

`POST /api/ai/chat`

请求：

```json
{
  "message": "我想请假应该怎么办？",
  "conversationId": null
}
```

响应：

```json
{
  "conversationId": 10001,
  "answer": "你可以进入“请假审批流程”提交申请。若是病假，建议提前准备证明材料。",
  "fallback": false,
  "citations": [
    {
      "type": "knowledge",
      "id": 12,
      "title": "学生请假办理指南",
      "summary": "请假申请提交、审批与材料说明",
      "path": "/pages/knowledge-detail/knowledge-detail?id=12"
    }
  ],
  "actions": [
    {
      "code": "leave",
      "title": "请假审批流程",
      "description": "提交和查看请假申请",
      "path": "/pages/leave-list/leave-list",
      "tabPage": false
    }
  ]
}
```

规则：

- `message` 必填，限制 1 到 1000 字。
- 未找到启用配置或调用失败时，`fallback=true`，返回规则生成回答。
- 回答必须绑定 `citations` 或 `actions`，没有依据时明确说明“暂未找到明确资料”。

### 7.2 管理端 AI 配置接口

`GET /api/admin/ai-config`

返回所有配置，API Key 脱敏。

`GET /api/admin/ai-config/active`

返回当前启用配置。

`POST /api/admin/ai-config`

新增配置。

`PUT /api/admin/ai-config/{id}`

更新配置。`apiKey` 为空时保留原密钥。

`POST /api/admin/ai-config/{id}/activate`

设置为当前启用配置。服务端需在事务中先将其他配置 `active=false`，再设置目标配置 `active=true`。

`POST /api/admin/ai-config/{id}/test`

用配置发起一次测试调用。请求体：

```json
{
  "message": "请用一句话回复：连接测试成功"
}
```

响应：

```json
{
  "success": true,
  "answer": "连接测试成功。",
  "latencyMs": 832,
  "provider": "deepseek",
  "model": "deepseek-chat"
}
```

`DELETE /api/admin/ai-config/{id}`

只允许删除非 active 配置。

## 8. AI 调用设计

### 8.1 统一客户端接口

```java
public interface AiChatClient {
    AiProviderCallResult chat(AiProviderConfig config, List<AiMessageParam> messages, AiCallOptions options);
}
```

首版实现 `OpenAiCompatibleChatClient`，用于 DeepSeek 和 OpenAI 兼容接口。调用 URL 规则：

- 如果 `baseUrl` 以 `/v1` 结尾，OpenAI 兼容路径可为 `{baseUrl}/chat/completions`。
- DeepSeek 默认 `baseUrl=https://api.deepseek.com`，路径为 `{baseUrl}/chat/completions`。
- 配置保存时去除末尾 `/`，避免双斜杠。

### 8.2 Prompt 结构

系统提示词由两部分组成：平台内置安全提示词 + 管理员自定义提示词。

内置提示词固定包含：

```text
你是学院学生综合服务与党团管理平台的 AI 助手。
只能基于系统提供的“可见知识”“用户上下文”“可用功能入口”回答。
不能编造政策、通知、入口、审批结果或个人数据。
如果上下文不足，明确说明暂未找到依据，并建议用户查看相关模块或联系辅导员。
不得泄露用户无权限查看的信息。
回答应简洁、中文、适合学生阅读。
```

用户消息上下文结构：

```text
用户问题：{message}

可见知识：
1. 标题：学生请假办理指南
   摘要：请假申请提交、审批与材料说明
   内容片段：学生可在请假审批流程中提交事假或病假申请

可用功能入口：
1. 名称：请假审批流程
   路径：/pages/leave-list/leave-list
   描述：提交和查看请假申请

用户上下文摘要：
角色：student/cadre
年级：2023 级
党团阶段：入党积极分子
```

### 8.3 结构化输出

首版不强制要求模型输出完整 JSON，避免不同供应商 JSON 支持差异导致失败。推荐做法：

- 后端先确定 `citations` 和 `actions`。
- 模型只生成 `answer`。
- 返回给前端时由后端附加引用和入口。

如果管理员配置 `response_format=json_object`，可在后续版本让模型返回结构化回答，但仍由后端校验引用和入口是否存在。

### 8.4 参数默认值

DeepSeek 默认配置建议：

```text
provider: deepseek
baseUrl: https://api.deepseek.com
model: deepseek-chat
temperature: 0.3
topP: 1.0
maxTokens: 1200
presencePenalty: 0
frequencyPenalty: 0
timeoutSeconds: 30
streamEnabled: false
retrievalTopK: 5
actionTopK: 3
```

`deepseek-reasoner` 可作为高级配置，用于复杂解释型问题，但默认不启用，避免成本和时延过高。

## 9. 权限与安全设计

### 9.1 管理端权限

- `/api/admin/ai-config/**` 仅允许 `admin` 访问。
- 需要修改 `ApiRoleInterceptor`，将该路径加入管理员白名单式判断。
- 管理端 `/settings` 已限制管理员，无需新增路由。

### 9.2 学生端权限

- `/api/ai/chat` 允许 `student`、`cadre` 访问。
- 若未来辅导员端也需要 AI，可新增 `/api/admin/ai/chat` 或允许 `counselor/admin` 使用不同上下文策略。
- AI 上下文必须由后端过滤后生成，小程序不得传入角色、权限或可访问范围。

### 9.3 API Key 安全

- API Key 只在新增/更新时接收明文。
- 数据库保存加密值 `api_key_cipher` 和脱敏值 `api_key_mask`。
- 列表、详情、测试响应均不返回明文 Key。
- 日志中不得打印请求头 Authorization、明文 API Key 或完整模型请求体。
- 加密密钥可通过环境变量 `AI_CONFIG_SECRET` 配置。未配置时可使用服务端固定降级方案，但生产环境必须配置。

### 9.4 数据最小化

传给模型的内容只包含回答所需片段：

- 知识库标题、摘要、命中的正文片段。
- 功能入口名称、描述、路径。
- 必要的用户上下文摘要，如角色、年级、党团阶段。

不得把完整学生档案、完整通知列表、其他用户信息或管理员数据直接传给模型。

## 10. 功能入口注册表设计

首版创建 `AiFeatureEntryService`，在后端维护入口列表，与小程序服务页保持一致。入口建议：

```text
partyProgress: 入党流程追踪 -> /pages/party-progress/party-progress
partyReport: 思想汇报提交 -> /pages/party-report/party-report
partyActivity: 党团活动申请 -> /pages/party-activity/party-activity
certificate: 电子证明生成 -> /pages/e-certificate/e-certificate
leave: 请假审批流程 -> /pages/leave-list/leave-list
policyKnowledge: 政策知识库 -> /pages/policy-knowledge/policy-knowledge
studyAnalysis: 学业分析与预警 -> /pages/study-analysis/study-analysis
portrait: 学生画像 -> /pages/student-portrait/student-portrait
honor: 奖励荣誉 -> /pages/honor/honor
knowledge: 知识库 -> /pages/knowledge/knowledge
notice: 通知 -> /pages/notice/notice，tabPage=true
service: 服务 -> /pages/service/service，tabPage=true
profile: 个人中心 -> /pages/profile/profile，tabPage=true
```

每个入口配置关键词，例如请假入口包含“请假、销假、病假、事假、审批、离校”等。匹配流程：

1. 根据当前用户角色过滤入口。
2. 对用户问题做关键词匹配。
3. 结合知识库命中的 `scenarioCode` 提升入口分数。
4. 返回前 `actionTopK` 个入口。

后续可将入口注册表抽为 `miniprogram-1/utils/feature-registry.js` 与后端 JSON 同源生成，避免双端重复维护。

## 11. 小程序页面设计

新增页面：

```text
miniprogram-1/pages/ai-chat/ai-chat.js
miniprogram-1/pages/ai-chat/ai-chat.wxml
miniprogram-1/pages/ai-chat/ai-chat.wxss
miniprogram-1/pages/ai-chat/ai-chat.json
```

修改 `miniprogram-1/app.json`：

- `pages` 增加 `pages/ai-chat/ai-chat`。
- `tabBar.list` 增加 AI 助手。若要求位于首页左侧，则放在第一项。
- 需要准备 `images/ai.png` 和 `images/ai-active.png`。

页面交互：

- 顶部展示“AI 助手”。
- 初始欢迎语提示可咨询“请假、证明、党团流程、通知、政策”。
- 输入框固定底部。
- 回答卡片展示 `answer`。
- 引用知识显示为可点击卡片，跳转知识详情。
- 功能入口显示为按钮，`tabPage=true` 使用 `wx.switchTab`，否则使用 `wx.navigateTo`。
- 请求失败时展示“AI 暂不可用，已为你返回相关入口”。

## 12. 管理端页面设计

在 `SettingsView.vue` 增加第三个 tab：`AI 助手配置`。

### 12.1 页面区域

1. 当前启用配置卡片
   - 显示配置名称、供应商、模型、状态、更新时间。
   - 提供“测试连接”“停用/启用”按钮。

2. 配置列表
   - 列：名称、供应商、Base URL、模型、Key 状态、启用状态、当前使用、更新时间、操作。
   - 操作：编辑、设为当前、测试、删除。

3. 配置表单
   - 名称、供应商、Base URL、API Key、模型。
   - 参数：temperature、topP、maxTokens、presencePenalty、frequencyPenalty、timeoutSeconds、streamEnabled、retrievalTopK、actionTopK、responseFormat。
   - 系统提示词 textarea。

4. 测试抽屉或弹窗
   - 输入测试问题。
   - 展示回答、耗时、供应商、模型、错误信息。

### 12.2 供应商表单联动

选择 `deepseek` 时：

- Base URL 默认 `https://api.deepseek.com`。
- 模型下拉为 `deepseek-chat`、`deepseek-reasoner`。
- 参数展示 DeepSeek 支持字段。

选择 `openai-compatible` 时：

- Base URL 和模型名允许自由输入。
- 参数字段保持一致。

选择 `openai` 时：

- 预留，默认 Base URL `https://api.openai.com/v1`。
- 首版可隐藏或标记“可选”。

## 13. 降级策略

AI 助手必须可降级，避免模型 API、余额或网络问题影响平台主流程。

降级触发条件：

- 没有 active 配置。
- active 配置未启用。
- API Key 为空。
- 模型调用超时。
- 模型返回 401、403、429、5xx。
- 模型输出为空。

降级响应：

- 后端基于知识库搜索结果和功能入口生成模板化回答。
- `fallback=true`。
- 前端显示轻提示：“AI 服务暂不可用，已为你展示相关资料和入口”。

模板示例：

```text
我暂时无法连接 AI 服务，但根据你的问题，为你找到了以下相关资料和入口。你可以先查看“请假审批流程”，也可以阅读“学生请假办理指南”。
```

## 14. 审计与运维

### 14.1 审计日志

AI 配置变更写入现有审计日志模块：

- module: `ai`
- action: `create_config`、`update_config`、`activate_config`、`delete_config`、`test_config`
- description: 记录配置名称、供应商、模型，不记录 API Key。

### 14.2 调用监控

`ai_message` 记录：

- provider、model。
- latencyMs。
- token 用量。
- status 和错误摘要。

管理端首版不必做统计页，但数据结构预留后续展示“今日调用量、失败率、平均耗时、token 消耗”。

## 15. 错误处理

### 15.1 配置校验

保存配置时校验：

- `configName` 非空，长度不超过 100。
- `provider` 在允许枚举内。
- `baseUrl` 必须是 HTTP/HTTPS URL。
- `model` 非空。
- `temperature` 范围 0 到 2。
- `topP` 范围 0 到 1。
- `maxTokens` 范围 1 到 8192。
- `timeoutSeconds` 范围 5 到 120。
- `retrievalTopK` 范围 1 到 10。
- `actionTopK` 范围 1 到 10。

### 15.2 模型错误映射

- 401/403：配置无效或无权限。
- 429：调用频率或余额限制。
- 5xx：供应商服务异常。
- timeout：模型响应超时。
- network：网络不可达。

对用户端统一返回友好降级结果；对管理端测试接口返回明确错误摘要。

## 16. 测试与验收

### 16.1 后端测试

- `AiConfigServiceTest`
  - 新增配置保存脱敏 key。
  - 更新配置不传 key 时保留原 key。
  - 激活配置时其他配置被取消 active。
  - 删除 active 配置失败。

- `AiChatServiceTest`
  - 有 active 配置时调用 `AiChatClient`。
  - 无 active 配置时返回 fallback。
  - 模型异常时返回 fallback。
  - 返回 actions 只包含当前用户角色可访问入口。

- `AiFeatureEntryServiceTest`
  - “请假”命中请假审批流程。
  - “思想汇报”命中思想汇报提交。
  - tab 页入口返回 `tabPage=true`。

### 16.2 前端验收

- 管理员可在系统设置中看到 AI 助手配置 tab。
- 管理员可新增 DeepSeek 配置并测试连接。
- API Key 保存后再次打开只显示脱敏值。
- 学生可打开 AI 助手 tab，发送问题并收到回答。
- AI 回答中的功能入口可正常跳转。
- 模型配置停用或错误时，小程序仍返回相关资料和入口。

## 17. 非目标与后续扩展

首版不做：

- 多供应商负载均衡。
- 每个角色单独选择模型。
- 复杂 Agent 工具调用。
- 语音对话。
- 图片识别。
- 流式打字 UI。
- 完整 token 计费看板。

后续可扩展：

- 为辅导员端新增 AI 助手，用于总结通知反馈、辅助检索学生情况。
- 管理端展示 AI 调用统计与错误趋势。
- 为不同场景配置不同 prompt 和模型，例如“知识问答”“流程导航”“通知解释”。
- 支持校内部署大模型，只需新增 `openai-compatible` 配置。
- 将功能入口注册表后台可配置化。
