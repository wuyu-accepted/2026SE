# Smart Knowledge Base Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a rule-based intelligent knowledge base where counselors/admins manage articles/templates and students search, download, and receive explainable recommendations.

**Architecture:** Extend the existing Spring Boot knowledge module with metadata, behavior events, recommendation scoring, and admin CRUD APIs. Replace the Vue placeholder with a focused management page, and enhance mini-program knowledge/template pages to show recommendations and tags.

**Tech Stack:** Java 17, Spring Boot 3.2.5, MyBatis-Plus, Flyway, H2/PostgreSQL-compatible SQL, Vue 3, Element Plus, WeChat mini-program native APIs.

---

## File Structure

### Backend database
- Modify: `demo/src/main/resources/db/migration/V3__create_knowledge_tables.sql` remains unchanged for existing installs.
- Create: `demo/src/main/resources/db/migration/V23__enhance_knowledge_smart.sql` for new metadata columns, behavior events, and recommendation logs.

### Backend student knowledge module
- Modify: `demo/src/main/java/com/ruc/platform/knowledgeness/entity/KnowledgeArticle.java` to map article metadata.
- Modify: `demo/src/main/java/com/ruc/platform/knowledgeness/entity/KnowledgeTemplate.java` to map template metadata.
- Create: `demo/src/main/java/com/ruc/platform/knowledgeness/entity/KnowledgeBehaviorEvent.java` for behavior events.
- Create: `demo/src/main/java/com/ruc/platform/knowledgeness/entity/KnowledgeRecommendationLog.java` for recommendation logs.
- Modify: `demo/src/main/java/com/ruc/platform/knowledgeness/dto/KnowledgeArticleQueryDTO.java` for content type and tag filters.
- Create: `demo/src/main/java/com/ruc/platform/knowledgeness/dto/KnowledgeBehaviorDTO.java` for behavior reporting.
- Create: `demo/src/main/java/com/ruc/platform/knowledgeness/dto/KnowledgeTemplateQueryDTO.java` for template filters.
- Modify: `demo/src/main/java/com/ruc/platform/knowledgeness/vo/KnowledgeArticleListItemVO.java` with metadata display fields.
- Modify: `demo/src/main/java/com/ruc/platform/knowledgeness/vo/KnowledgeArticleDetailVO.java` with metadata display fields.
- Modify: `demo/src/main/java/com/ruc/platform/knowledgeness/vo/KnowledgeTemplateVO.java` with metadata display fields.
- Create: `demo/src/main/java/com/ruc/platform/knowledgeness/vo/KnowledgeRecommendationVO.java` for mixed recommendations.
- Create: `demo/src/main/java/com/ruc/platform/knowledgeness/mapper/KnowledgeBehaviorEventMapper.java`.
- Create: `demo/src/main/java/com/ruc/platform/knowledgeness/mapper/KnowledgeRecommendationLogMapper.java`.
- Modify: `demo/src/main/java/com/ruc/platform/knowledgeness/mapper/KnowledgeArticleMapper.java` and XML to support metadata search.
- Modify: `demo/src/main/java/com/ruc/platform/knowledgeness/mapper/KnowledgeTemplateMapper.java` and XML to support filters.
- Modify: `demo/src/main/java/com/ruc/platform/knowledgeness/service/KnowledgeService.java` and `KnowledgeServiceImpl.java` for search, behavior logging, and recommendation.
- Modify: `demo/src/main/java/com/ruc/platform/knowledgeness/controller/KnowledgeController.java` for recommendations and behavior endpoint.

### Backend admin knowledge module
- Create: `demo/src/main/java/com/ruc/platform/admin/knowledge/controller/AdminKnowledgeController.java`.
- Create: `demo/src/main/java/com/ruc/platform/admin/knowledge/dto/KnowledgeArticleSaveDTO.java`.
- Create: `demo/src/main/java/com/ruc/platform/admin/knowledge/dto/KnowledgeTemplateSaveDTO.java`.
- Create: `demo/src/main/java/com/ruc/platform/admin/knowledge/dto/KnowledgeCategorySaveDTO.java`.
- Create: `demo/src/main/java/com/ruc/platform/admin/knowledge/dto/KnowledgeStatusUpdateDTO.java`.
- Create: `demo/src/main/java/com/ruc/platform/admin/knowledge/service/AdminKnowledgeService.java`.
- Create: `demo/src/main/java/com/ruc/platform/admin/knowledge/service/AdminKnowledgeServiceImpl.java`.
- Reuse existing `KnowledgeArticleMapper`, `KnowledgeCategoryMapper`, and `KnowledgeTemplateMapper` for CRUD.

### Frontend management app
- Create: `web-counselor/src/api/knowledge.js` for admin knowledge APIs and upload helper.
- Create: `web-counselor/src/views/KnowledgeView.vue` with article/template/category/stat tabs.
- Modify: `web-counselor/src/router/index.js` to route `/knowledge` to `KnowledgeView`.

### Mini-program
- Modify: `miniprogram-1/pages/knowledge/knowledge.js` and `.wxml` to load recommendations and show tags/reasons.
- Modify: `miniprogram-1/pages/knowledge-detail/knowledge-detail.js` and `.wxml` to display metadata.
- Modify: `miniprogram-1/pages/template-download/template-download.js` and `.wxml` to show tags and report downloads.

### Tests
- Create: `demo/src/test/java/com/ruc/platform/knowledgeness/service/KnowledgeRecommendationServiceTest.java` for scoring behavior.
- Create: `demo/src/test/java/com/ruc/platform/admin/knowledge/service/AdminKnowledgeServiceImplTest.java` for admin CRUD basics.

---

### Task 1: Database Smart Metadata

**Files:**
- Create: `demo/src/main/resources/db/migration/V23__enhance_knowledge_smart.sql`

- [ ] **Step 1: Add Flyway migration**

Create SQL that adds metadata columns to existing knowledge tables and creates event/log tables:

```sql
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS content_type VARCHAR(32) NOT NULL DEFAULT 'guide';
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS tags VARCHAR(500);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS target_grades VARCHAR(255);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS target_majors VARCHAR(500);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS target_political_statuses VARCHAR(255);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS target_party_stages VARCHAR(255);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS scenario_codes VARCHAR(255);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS priority INT NOT NULL DEFAULT 0;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS effective_from TIMESTAMP;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS effective_to TIMESTAMP;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS tags VARCHAR(500);
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS target_grades VARCHAR(255);
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS target_majors VARCHAR(500);
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS target_political_statuses VARCHAR(255);
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS target_party_stages VARCHAR(255);
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS scenario_codes VARCHAR(255);
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS priority INT NOT NULL DEFAULT 0;
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS effective_from TIMESTAMP;
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS effective_to TIMESTAMP;
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS updated_by BIGINT;

CREATE TABLE IF NOT EXISTS knowledge_behavior_event (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    target_type VARCHAR(32),
    target_id BIGINT,
    keyword VARCHAR(255),
    source_page VARCHAR(64),
    feature_snapshot TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_recommendation_log (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    score INT NOT NULL DEFAULT 0,
    reason VARCHAR(1000),
    strategy_version VARCHAR(32) NOT NULL DEFAULT 'rule-v1',
    feature_snapshot TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_article_content_type ON knowledge_article(content_type);
CREATE INDEX IF NOT EXISTS idx_knowledge_article_priority ON knowledge_article(priority DESC);
CREATE INDEX IF NOT EXISTS idx_knowledge_template_priority ON knowledge_template(priority DESC);
CREATE INDEX IF NOT EXISTS idx_knowledge_behavior_user_time ON knowledge_behavior_event(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_knowledge_recommend_user_time ON knowledge_recommendation_log(user_id, created_at DESC);
```

- [ ] **Step 2: Run migration check**

Run: `cd demo && ./mvnw -q -DskipTests compile`
Expected: build succeeds and SQL resources are included.

---

### Task 2: Student Search and Recommendation Backend

**Files:**
- Modify/create files listed in backend student knowledge module.
- Test: `demo/src/test/java/com/ruc/platform/knowledgeness/service/KnowledgeRecommendationServiceTest.java`

- [ ] **Step 1: Write recommendation unit tests**

Create tests that instantiate `KnowledgeServiceImpl` with mocked mappers and verify:

```java
@Test
void shouldRecommendArticleForMatchingPartyStageAndGrade() {
    // Arrange student profile grade=2023 and party stage=activist.
    // Arrange one published article with targetGrades=2023,targetPartyStages=activist.
    // Act listRecommendations(1001L, 5).
    // Assert top item score >= 55 and reason contains both grade and party stage.
}

@Test
void shouldFallbackToHotArticlesWhenNoProfileSignalsMatch() {
    // Arrange no profile/party match but article has high viewCount.
    // Act listRecommendations(1001L, 5).
    // Assert result is not empty and reason contains 热门.
}
```

- [ ] **Step 2: Implement entities, DTOs, VOs, and mappers**

Add Java classes for behavior events, recommendation logs, query DTOs, and VOs with Lombok `@Data` and MyBatis-Plus `@TableName`/`@TableId` annotations.

- [ ] **Step 3: Implement enhanced student service**

Update `KnowledgeServiceImpl` to:

```java
public PageResult<KnowledgeArticleListItemVO> listArticles(KnowledgeArticleQueryDTO queryDTO);
public KnowledgeArticleDetailVO getArticleDetail(Long id);
public List<KnowledgeTemplateVO> listTemplates(KnowledgeTemplateQueryDTO queryDTO);
public List<KnowledgeRecommendationVO> listRecommendations(Long userId, Integer limit);
public void recordBehavior(Long userId, KnowledgeBehaviorDTO dto);
```

Scoring must include grade, major, political status, party stage, tag/category behavior preference, priority, and hotness. Invalid or unpublished content is excluded.

- [ ] **Step 4: Add controller endpoints**

Expose:

```java
@GetMapping("/recommendations")
@PostMapping("/behavior")
@GetMapping("/templates") with KnowledgeTemplateQueryDTO
```

- [ ] **Step 5: Run focused tests**

Run: `cd demo && ./mvnw -q -Dtest=KnowledgeRecommendationServiceTest test`
Expected: tests pass.

---

### Task 3: Admin Knowledge Backend

**Files:**
- Create: `demo/src/main/java/com/ruc/platform/admin/knowledge/**`
- Test: `demo/src/test/java/com/ruc/platform/admin/knowledge/service/AdminKnowledgeServiceImplTest.java`

- [ ] **Step 1: Write admin service tests**

Create tests that verify creating an article sets creator/updater, publishing sets status and publish time, and template save preserves fileId.

- [ ] **Step 2: Implement admin DTOs and service**

Implement methods:

```java
PageResult<KnowledgeArticleListItemVO> listArticles(KnowledgeArticleQueryDTO queryDTO);
KnowledgeArticleDetailVO getArticle(Long id);
Long createArticle(Long operatorId, KnowledgeArticleSaveDTO dto);
void updateArticle(Long operatorId, Long id, KnowledgeArticleSaveDTO dto);
void updateArticleStatus(Long operatorId, Long id, Integer status);
void deleteArticle(Long id);
List<KnowledgeTemplateVO> listTemplates(KnowledgeTemplateQueryDTO queryDTO);
Long createTemplate(Long operatorId, KnowledgeTemplateSaveDTO dto);
void updateTemplate(Long operatorId, Long id, KnowledgeTemplateSaveDTO dto);
void updateTemplateStatus(Long operatorId, Long id, Integer status);
void deleteTemplate(Long id);
```

- [ ] **Step 3: Implement controller**

Expose `/api/admin/knowledge/**` endpoints and read operator id via `StpUtil.getLoginIdAsLong()` for write operations.

- [ ] **Step 4: Run focused tests**

Run: `cd demo && ./mvnw -q -Dtest=AdminKnowledgeServiceImplTest test`
Expected: tests pass.

---

### Task 4: Vue Knowledge Management Page

**Files:**
- Create: `web-counselor/src/api/knowledge.js`
- Create: `web-counselor/src/views/KnowledgeView.vue`
- Modify: `web-counselor/src/router/index.js`

- [ ] **Step 1: Add API wrapper**

Add functions for admin articles, templates, categories, stats, and `uploadKnowledgeTemplate(file)` using `bizType=knowledge-template`.

- [ ] **Step 2: Build management page**

Implement tabs for articles, templates, categories, and stats. Article/template forms include title/name, category, type, tags, target grades, majors, political statuses, party stages, scenarios, priority, status, and content/description.

- [ ] **Step 3: Wire router**

Replace `PlaceholderView` route for `/knowledge` with `KnowledgeView`.

- [ ] **Step 4: Build frontend**

Run: `cd web-counselor && npm run build`
Expected: Vite build succeeds.

---

### Task 5: Mini-program Recommendation UI

**Files:**
- Modify: `miniprogram-1/pages/knowledge/knowledge.js`
- Modify: `miniprogram-1/pages/knowledge/knowledge.wxml`
- Modify: `miniprogram-1/pages/knowledge-detail/knowledge-detail.js`
- Modify: `miniprogram-1/pages/knowledge-detail/knowledge-detail.wxml`
- Modify: `miniprogram-1/pages/template-download/template-download.js`
- Modify: `miniprogram-1/pages/template-download/template-download.wxml`

- [ ] **Step 1: Load recommendations**

Call `/api/knowledge/recommendations?limit=6` on knowledge page load and display recommendation cards with reason.

- [ ] **Step 2: Enhance list/detail metadata**

Display content type, tags, effective info, and target audience text when present.

- [ ] **Step 3: Report behavior**

Call `POST /api/knowledge/behavior` for search, recommendation click, and template download after successful user action.

- [ ] **Step 4: Manual mini-program smoke test**

Open pages in WeChat DevTools, verify no JS runtime errors in knowledge list, detail, and template download pages.

---

### Task 6: Documentation and Full Verification

**Files:**
- Modify: `documents/API.md`
- Modify: `README_NEW.md`

- [ ] **Step 1: Document new endpoints**

Add student recommendation and admin knowledge endpoints to API docs.

- [ ] **Step 2: Update current completion status**

Update README to mark intelligent knowledge base as implemented and explain recommendation limitations.

- [ ] **Step 3: Run backend tests**

Run: `cd demo && ./mvnw test`
Expected: all tests pass.

- [ ] **Step 4: Run web build**

Run: `cd web-counselor && npm run build`
Expected: build succeeds.

- [ ] **Step 5: Review git diff**

Run: `git status --short && git diff --stat`
Expected: only knowledge-related files and docs changed.
