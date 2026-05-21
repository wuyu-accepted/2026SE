# 智能知识库设计方案

## 1. 背景与目标

当前知识库已经具备学生端文章列表、详情和模板下载能力，但管理端仍是占位页，内容维护、适用人群配置、行为记录和个性化推荐能力尚未形成闭环。本方案目标是在现有“学生小程序 + Vue 管理端 + Spring Boot 后端”架构上，将知识库升级为面向学院事务办理、政策查询、流程指引和模板下载的智能知识服务模块。

本次采用“规则推荐 + 行为画像 + 可配置标签”的实现路径，先落地可解释、可验收的智能推荐能力，同时预留行为事件、推荐日志和特征快照字段，为后续接入向量检索、AI 摘要或模型化推荐提供扩展点。

## 2. 功能范围

### 2.1 学生端能力

- 学生可以检索知识资料，支持按关键词、分类、内容类型、标签筛选。
- 学生可以查看事务流程、政策说明、常见问答和办理指南等资料元数据，并下载/打开资料文件。
- 学生可以查看和下载模板文件。
- 学生知识库首页展示“为你推荐”，推荐结果带推荐原因。
- 学生浏览文章、搜索、下载模板、点击推荐时记录行为事件。

### 2.2 辅导员/管理员端能力

- 管理知识资料：列表、详情、新增、编辑、发布、下架、删除；知识内容可选择上传文件，也可选择 Markdown/LaTeX 在线编排，在线编排保存可编辑源文案。
- 管理模板文件：列表、新增、编辑、启停、删除，模板文件上传复用现有文件上传接口。
- 管理知识分类：分类列表、新增、编辑、启停。
- 配置内容适用对象：年级、专业、政治面貌、党团阶段、场景标签、有效期、优先级。
- 查看基础统计：文章数、模板数、分类数、行为事件数、推荐日志数，并支持后续扩展浏览量、下载量、推荐点击量和近期热门内容。

### 2.3 推荐能力

推荐策略基于规则打分，不引入外部机器学习服务。推荐依据包括：

- 学生基础画像：年级、专业、班级、政治面貌。
- 党团状态：当前党团阶段、党团提醒。
- 近期业务状态：最近申请、未读通知、待办或提醒。
- 行为偏好：近期搜索词、浏览文章、下载模板、点击推荐。
- 内容属性：分类、标签、适用范围、场景、优先级、发布时间、有效期、热度。

推荐结果需要返回分数和原因，例如“与你当前党团阶段匹配”“适用于 2023 级学生”“近期关注过奖助资助内容”。

## 3. 数据模型设计

### 3.1 扩展 `knowledge_article`

新增字段：

- `content_type`：内容类型，建议取值 `policy`、`process`、`faq`、`guide`。
- `tags`：标签，使用逗号分隔字符串，第一阶段避免新增多对多表。
- `target_grades`：适用年级，逗号分隔，空表示不限。
- `target_majors`：适用专业，逗号分隔，空表示不限。
- `target_political_statuses`：适用政治面貌，逗号分隔，空表示不限。
- `target_party_stages`：适用党团阶段编码，逗号分隔，空表示不限。
- `scenario_codes`：场景编码，逗号分隔，如 `leave`、`certificate`、`party_report`、`aid`。
- `priority`：内容推荐优先级，默认 0。
- `effective_from`、`effective_to`：有效期。
- `updated_by`：最近更新人。

保留现有 `status`、`publish_time`、`view_count` 等字段。搜索仍基于数据库字段完成，后续如接全文检索或向量检索，可以从这些字段抽取文档。

### 3.2 扩展 `knowledge_template`

新增字段与文章类似：`tags`、`target_grades`、`target_majors`、`target_political_statuses`、`target_party_stages`、`scenario_codes`、`priority`、`effective_from`、`effective_to`、`created_by`、`updated_by`。

模板文件本体继续通过 `file_metadata` 关联，上传仍复用 `/api/files/upload`。

### 3.3 新增 `knowledge_behavior_event`

用于记录学生知识库行为，并为后续模型训练或推荐评估留存基础数据。

核心字段：

- `id`
- `user_id`
- `event_type`：`view_article`、`search`、`download_template`、`click_recommendation`
- `target_type`：`article`、`template`、`search`
- `target_id`
- `keyword`
- `source_page`
- `feature_snapshot`：JSON 字符串，保存触发行为时的学生画像和上下文摘要。
- `created_at`

### 3.4 新增 `knowledge_recommendation_log`

用于记录推荐结果，支持解释、统计和后续算法评估。

核心字段：

- `id`
- `user_id`
- `target_type`：`article`、`template`
- `target_id`
- `score`
- `reason`
- `strategy_version`
- `feature_snapshot`：JSON 字符串。
- `created_at`

## 4. 后端接口设计

### 4.1 学生端接口

- `GET /api/knowledge/articles`：增强文章检索，支持 `keyword`、`categoryId`、`contentType`、`tag`、`pageNum`、`pageSize`。
- `GET /api/knowledge/articles/{id}`：文章详情，增加标签、适用对象等字段，并记录浏览行为。
- `GET /api/knowledge/templates`：模板列表，支持分类、标签和场景筛选。
- `GET /api/knowledge/recommendations`：个性化推荐，返回文章和模板混合推荐。
- `POST /api/knowledge/behavior`：可选行为上报接口，用于搜索、推荐点击等前端主动事件。

### 4.2 管理端接口

- `GET /api/admin/knowledge/articles`：管理端文章分页。
- `POST /api/admin/knowledge/articles`：新增知识资料元数据，需关联已上传文件。
- `GET /api/admin/knowledge/articles/{id}`：管理端文章详情。
- `PUT /api/admin/knowledge/articles/{id}`：编辑文章。
- `PUT /api/admin/knowledge/articles/{id}/status`：发布、下架、恢复草稿。
- `DELETE /api/admin/knowledge/articles/{id}`：删除文章。
- `GET /api/admin/knowledge/templates`：模板分页。
- `POST /api/admin/knowledge/templates`：新增模板。
- `PUT /api/admin/knowledge/templates/{id}`：编辑模板。
- `PUT /api/admin/knowledge/templates/{id}/status`：启用或禁用模板。
- `DELETE /api/admin/knowledge/templates/{id}`：删除模板。
- `GET /api/admin/knowledge/categories`、`POST /api/admin/knowledge/categories`、`PUT /api/admin/knowledge/categories/{id}`：分类维护。
- `GET /api/admin/knowledge/stats`：基础统计。

## 5. 推荐策略设计

推荐服务读取学生画像和内容属性后进行规则打分。第一阶段使用同步计算，不引入异步任务。

建议初始权重：

- 党团阶段匹配：+35。
- 年级匹配：+20。
- 专业匹配：+15。
- 政治面貌匹配：+15。
- 场景匹配最近业务状态：+25。
- 近期行为偏好匹配分类或标签：+20。
- 内容优先级：每级 +5。
- 热度加分：按浏览量、下载量做上限封顶加分。
- 不在有效期或已下架：排除。

每个推荐项保留命中的最高价值原因，多个原因用分号拼接。推荐结果写入 `knowledge_recommendation_log`，学生点击推荐时再写入 `knowledge_behavior_event`。

## 6. 前端设计

### 6.1 小程序

- 知识库首页顶部增加搜索框和推荐区。
- 推荐区展示标题、分类、标签、推荐原因。
- 文章列表展示内容类型、分类、标签和摘要。
- 详情页展示适用对象、标签、来源、发布时间、资料说明和下载/打开按钮。
- 模板下载页展示分类、标签、适用对象，点击下载时记录行为。

### 6.2 Vue 管理端

替换当前知识库占位页，新增 `KnowledgeView.vue`。页面使用四个标签页：

- 资料管理：查询、上传文件、创建元数据、编辑、发布/下架、删除。
- 模板管理：查询、上传文件、新增模板、启停、删除。
- 分类管理：维护分类名称、编码、排序和状态。
- 数据统计：展示浏览、下载、推荐点击和热门内容。

管理端接口封装新增 `web-counselor/src/api/knowledge.js`，路由 `/knowledge` 指向新页面。

## 7. 错误处理与权限

- 学生端只返回已发布、已启用、在有效期内的内容。
- 管理端接口仅允许辅导员和管理员访问，写操作记录更新人。
- 文件上传失败、文件不存在、文章不存在等情况使用现有 `Result` 和 `BizException` 机制返回。
- 删除优先采用物理删除或状态删除需与现有风格保持一致；第一阶段建议对文章保留硬删除接口，但管理端默认使用下架。
- 推荐为空时返回热门知识作为兜底。

## 8. 测试与验收

后端测试重点：

- 文章检索支持关键词、分类、内容类型、标签。
- 管理端文章和模板 CRUD 正常。
- 学生端只能看到已发布和有效期内内容。
- 推荐服务对年级、党团阶段、行为偏好能产生可解释原因。
- 行为事件和推荐日志可以写入。

前端验收重点：

- 小程序能看到推荐区，并能进入详情和下载模板。
- Vue 管理端能上传知识资料文件、配置适用对象并发布。
- 配置适用年级或党团阶段后，匹配学生能看到对应推荐。

## 9. 非目标与扩展点

本次不引入 Elasticsearch、向量数据库、外部 AI 模型或离线训练任务。通过 `knowledge_behavior_event.feature_snapshot` 和 `knowledge_recommendation_log.feature_snapshot` 保留特征快照，后续可以增加：

- 向量化字段和 embedding 同步任务。
- 推荐策略配置后台。
- A/B 测试策略版本。
- AI 摘要、相似问题推荐、自然语言问答。
