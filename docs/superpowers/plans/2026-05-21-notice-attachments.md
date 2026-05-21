# Notice Attachments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow admin notices to carry one reusable file attachment and expose it in admin and student notice details.

**Architecture:** Reuse the existing `file_metadata` and `/api/files/{id}/download` file flow. Add `notice.attachment_file_id` as an optional `BIGINT` foreign-key-style reference, surface it as `attachmentFileId` through notice create/update/list/detail DTOs and VOs, and include it in student message detail queries by joining `notice`.

**Tech Stack:** Spring Boot 3, MyBatis-Plus, Flyway SQL migrations, JUnit 5, Vue 3, Element Plus.

---

### Task 1: Backend Attachment Contract

**Files:**
- Modify: `demo/src/test/java/com/ruc/platform/admin/notice/service/AdminNoticeServiceImplTest.java`
- Modify: `demo/src/test/java/com/ruc/platform/notice/service/MessageServiceImplTest.java`
- Modify: `demo/src/main/java/com/ruc/platform/notice/entity/Notice.java`
- Modify: `demo/src/main/java/com/ruc/platform/admin/notice/dto/NoticeCreateDTO.java`
- Modify: `demo/src/main/java/com/ruc/platform/admin/notice/dto/NoticeUpdateDTO.java`
- Modify: `demo/src/main/java/com/ruc/platform/admin/notice/vo/NoticeListItemVO.java`
- Modify: `demo/src/main/java/com/ruc/platform/admin/notice/vo/NoticeDetailVO.java`
- Modify: `demo/src/main/java/com/ruc/platform/admin/notice/service/AdminNoticeServiceImpl.java`
- Modify: `demo/src/main/java/com/ruc/platform/notice/vo/MessageDetailVO.java`
- Modify: `demo/src/main/java/com/ruc/platform/notice/mapper/UserMessageMapper.java`
- Create: `demo/src/main/resources/db/migration/V22__add_notice_attachment_file_id.sql`

- [ ] Add failing tests for create/detail normalization and student detail exposure of `attachmentFileId`.
- [ ] Run targeted tests and confirm they fail because attachment support is missing.
- [ ] Add `attachmentFileId` fields, mappings, and migration.
- [ ] Re-run targeted tests and confirm they pass.

### Task 2: Admin UI Attachment Field

**Files:**
- Modify: `web-counselor/src/views/NoticesView.vue`

- [ ] Add `attachmentFileId` to form state, payload, edit hydration, table hint, and detail drawer download link.
- [ ] Use existing `/api/files/{id}/download` endpoint for downloading.

### Task 3: API Documentation And Verification

**Files:**
- Modify: `documents/API.md`

- [ ] Document `attachmentFileId` on student detail and admin notice APIs.
- [ ] Run `cd demo && ./mvnw test`.
- [ ] Run `cd web-counselor && npm run build`.
