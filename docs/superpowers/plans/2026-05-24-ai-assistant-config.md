# AI Assistant Config Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build configurable AI provider support focused on DeepSeek, plus a mini-program AI assistant entry that uses secure backend-mediated chat.

**Architecture:** Add a Spring Boot AI module with provider config persistence, admin CRUD/test APIs, a DeepSeek/OpenAI-compatible client, feature-entry matching, RAG-lite context, and fallback answers. Add an admin Settings tab for AI configuration and a mini-program AI chat tab/page.

**Tech Stack:** Java 17, Spring Boot 3.2.5, MyBatis-Plus, Flyway, JUnit/Mockito, Vue 3 + Element Plus, WeChat mini-program native APIs.

---

## File Structure

### Backend database
- Create `demo/src/main/resources/db/migration/V30__ai_assistant_config.sql`: AI config, conversation, and message tables.

### Backend AI module
- Create `demo/src/main/java/com/ruc/platform/ai/entity/AiProviderConfig.java`: provider config entity.
- Create `demo/src/main/java/com/ruc/platform/ai/entity/AiConversation.java`: conversation entity.
- Create `demo/src/main/java/com/ruc/platform/ai/entity/AiMessage.java`: message audit entity.
- Create `demo/src/main/java/com/ruc/platform/ai/mapper/AiProviderConfigMapper.java`: MyBatis mapper.
- Create `demo/src/main/java/com/ruc/platform/ai/mapper/AiConversationMapper.java`: MyBatis mapper.
- Create `demo/src/main/java/com/ruc/platform/ai/mapper/AiMessageMapper.java`: MyBatis mapper.
- Create `demo/src/main/java/com/ruc/platform/ai/dto/AiConfigSaveDTO.java`: admin config save request.
- Create `demo/src/main/java/com/ruc/platform/ai/dto/AiConfigTestDTO.java`: admin config test request.
- Create `demo/src/main/java/com/ruc/platform/ai/dto/AiChatRequest.java`: student chat request.
- Create `demo/src/main/java/com/ruc/platform/ai/vo/AiConfigVO.java`: masked config response.
- Create `demo/src/main/java/com/ruc/platform/ai/vo/AiConfigTestVO.java`: test response.
- Create `demo/src/main/java/com/ruc/platform/ai/vo/AiActionVO.java`: recommended action response.
- Create `demo/src/main/java/com/ruc/platform/ai/vo/AiCitationVO.java`: citation response.
- Create `demo/src/main/java/com/ruc/platform/ai/vo/AiChatResponse.java`: chat response.
- Create `demo/src/main/java/com/ruc/platform/ai/client/AiChatClient.java`: provider client interface.
- Create `demo/src/main/java/com/ruc/platform/ai/client/OpenAiCompatibleChatClient.java`: HTTP client for DeepSeek-compatible chat completions.
- Create `demo/src/main/java/com/ruc/platform/ai/service/AiConfigService.java`: config CRUD/activation/test.
- Create `demo/src/main/java/com/ruc/platform/ai/service/AiFeatureEntryService.java`: feature matching.
- Create `demo/src/main/java/com/ruc/platform/ai/service/AiContextService.java`: knowledge context retrieval.
- Create `demo/src/main/java/com/ruc/platform/ai/service/AiChatService.java`: chat orchestration and fallback.
- Create `demo/src/main/java/com/ruc/platform/ai/controller/AdminAiConfigController.java`: admin APIs.
- Create `demo/src/main/java/com/ruc/platform/ai/controller/AiChatController.java`: mini-program chat API.
- Modify `demo/src/main/java/com/ruc/platform/config/ApiRoleInterceptor.java`: restrict admin AI config and student chat roles.

### Backend tests
- Create `demo/src/test/java/com/ruc/platform/ai/service/AiConfigServiceTest.java`.
- Create `demo/src/test/java/com/ruc/platform/ai/service/AiFeatureEntryServiceTest.java`.
- Create `demo/src/test/java/com/ruc/platform/ai/service/AiChatServiceTest.java`.

### Management frontend
- Modify `web-counselor/src/api/admin.js`: AI config API wrappers.
- Modify `web-counselor/src/views/SettingsView.vue`: AI config tab, form, test flow.

### Mini-program
- Create `miniprogram-1/pages/ai-chat/ai-chat.js`.
- Create `miniprogram-1/pages/ai-chat/ai-chat.wxml`.
- Create `miniprogram-1/pages/ai-chat/ai-chat.wxss`.
- Create `miniprogram-1/pages/ai-chat/ai-chat.json`.
- Modify `miniprogram-1/app.json`: add page and tab.

## Task 1: Backend AI Config Foundation

- [x] Write failing `AiConfigServiceTest` for key masking, key retention, activation, and active deletion rejection.
- [x] Run focused test and verify it fails because classes are missing.
- [x] Add Flyway migration and AI config entity/mapper/DTO/VO.
- [x] Implement `AiConfigService` validation, masking, simple reversible storage, activation, and deletion rules.
- [x] Run focused test and verify it passes.

## Task 2: AI Feature Entry Matching

- [x] Write failing `AiFeatureEntryServiceTest` for leave, party report, and tab page matching.
- [x] Run focused test and verify it fails because service is missing.
- [x] Implement `AiFeatureEntryService` with code-defined entries and role filtering.
- [x] Run focused test and verify it passes.

## Task 3: AI Chat Orchestration

- [x] Write failing `AiChatServiceTest` for fallback without active config, client usage with active config, and fallback on client exception.
- [x] Run focused test and verify it fails because chat service is missing.
- [x] Implement client interface, compatible HTTP client, context service, chat service, message persistence, and fallback response.
- [x] Run focused test and verify it passes.

## Task 4: Backend Controllers and Permissions

- [x] Add `AdminAiConfigController` and `AiChatController`.
- [x] Update `ApiRoleInterceptor` for `/api/admin/ai-config/**` admin-only and `/api/ai/chat` student/cadre.
- [x] Run backend compile or focused tests.

## Task 5: Management UI

- [x] Add AI config API wrappers to `web-counselor/src/api/admin.js`.
- [x] Add AI config tab to `SettingsView.vue` with list, dialog, activation, and test.
- [x] Run frontend build if dependencies are available.

## Task 6: Mini-program AI Chat Page

- [x] Add mini-program AI chat page files.
- [x] Update `app.json` page list and tabBar.
- [x] Verify JSON syntax and basic page consistency.

## Task 7: Final Verification

- [x] Run backend focused AI tests.
- [x] Run backend compile.
- [x] Run frontend build if available.
- [x] Report any blocked verification with exact command and output.
