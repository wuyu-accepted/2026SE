# Notice Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first-phase notice publishing loop: admin creates/publishes filtered notices and students receive them in the existing mini-program inbox.

**Architecture:** Reuse existing `notice` and `user_message` tables. Add admin notice DTO/service/controller code under `admin/notice`, extend student/message mappers for target selection and delivery counting, replace the Vue placeholder route with a real notices page.

**Tech Stack:** Spring Boot 3, MyBatis-Plus, Sa-Token, Vue 3, Vite, Element Plus, Axios.

---

### Task 1: Backend Admin Notices

**Files:**
- Create: `demo/src/main/java/com/ruc/platform/admin/notice/dto/*.java`
- Create: `demo/src/main/java/com/ruc/platform/admin/notice/vo/*.java`
- Create: `demo/src/main/java/com/ruc/platform/admin/notice/service/*.java`
- Create: `demo/src/main/java/com/ruc/platform/admin/notice/controller/AdminNoticeController.java`
- Modify: `demo/src/main/java/com/ruc/platform/student/mapper/StudentProfileMapper.java`
- Modify: `demo/src/main/java/com/ruc/platform/notice/mapper/UserMessageMapper.java`

- [ ] Add DTOs and VOs for notice CRUD, target filters, and publish result.
- [ ] Add target student query based on grade, major, className, authType.
- [ ] Add service methods for list, detail, create, update, publish, offline, delete.
- [ ] Publish writes one `user_message` per matched student and returns delivery count.
- [ ] Add REST controller under `/api/admin/notices`.

### Task 2: Vue Management Page

**Files:**
- Create: `web-counselor/src/api/notice.js`
- Create: `web-counselor/src/views/NoticesView.vue`
- Modify: `web-counselor/src/router/index.js`

- [ ] Add API wrappers for CRUD and publish/offline.
- [ ] Replace `/notices` placeholder with `NoticesView`.
- [ ] Add list filters, table, pagination, editor dialog, detail drawer.
- [ ] Add publish/offline/delete confirmation and success messages.

### Task 3: API Documentation

**Files:**
- Modify: `documents/API.md`

- [ ] Document `/api/admin/notices` endpoints.
- [ ] Document filter publish target fields and status rules.

### Task 4: Verification

- [ ] Run `cd demo && mvn test`.
- [ ] Run `cd web-counselor && npm run build`.
- [ ] Report exact verification output and any environment failures.

### Task 5: Mini Program Notice Detail

**Files:**
- Create: `demo/src/main/java/com/ruc/platform/notice/vo/MessageDetailVO.java`
- Modify: `demo/src/main/java/com/ruc/platform/notice/controller/MessageController.java`
- Modify: `demo/src/main/java/com/ruc/platform/notice/service/MessageService.java`
- Modify: `demo/src/main/java/com/ruc/platform/notice/service/MessageServiceImpl.java`
- Modify: `demo/src/main/java/com/ruc/platform/notice/mapper/UserMessageMapper.java`
- Create: `miniprogram-1/pages/notice-detail/notice-detail.*`
- Modify: `miniprogram-1/pages/notice/notice.js`
- Modify: `miniprogram-1/app.json`
- Modify: `documents/API.md`

- [x] Add `GET /api/messages/{id}` with user ownership validation.
- [x] Change read marking to require `messageId + userId`.
- [x] Add mini-program detail page and route from notice list.
- [x] Document the student message detail endpoint.

### Task 6: Notice Estimate And Read Stats

**Files:**
- Create: `demo/src/main/java/com/ruc/platform/admin/notice/vo/NoticeStatsVO.java`
- Create: `demo/src/main/java/com/ruc/platform/admin/notice/vo/NoticeTargetEstimateVO.java`
- Modify: `demo/src/main/java/com/ruc/platform/admin/notice/controller/AdminNoticeController.java`
- Modify: `demo/src/main/java/com/ruc/platform/admin/notice/service/AdminNoticeService.java`
- Modify: `demo/src/main/java/com/ruc/platform/admin/notice/service/AdminNoticeServiceImpl.java`
- Modify: `demo/src/main/java/com/ruc/platform/notice/mapper/UserMessageMapper.java`
- Modify: `web-counselor/src/api/notice.js`
- Modify: `web-counselor/src/views/NoticesView.vue`
- Modify: `documents/API.md`

- [x] Add `GET /api/admin/notices/{id}/target-estimate` for pre-publish target count.
- [x] Add `GET /api/admin/notices/{id}/stats` for delivered/read/unread/read-rate.
- [x] Show estimate action in the notice list before publishing.
- [x] Show read statistics in the notice detail drawer.

### Task 7: Multi-Select Grade And Major Targets

**Files:**
- Modify: `demo/src/main/java/com/ruc/platform/admin/notice/dto/NoticeTargetDTO.java`
- Modify: `demo/src/main/java/com/ruc/platform/admin/notice/service/AdminNoticeServiceImpl.java`
- Modify: `demo/src/main/java/com/ruc/platform/student/mapper/StudentProfileMapper.java`
- Modify: `web-counselor/src/views/NoticesView.vue`
- Modify: `documents/API.md`

- [x] Keep legacy `grade` and `major` fields for backward compatibility.
- [x] Add `grades` and `majors` array fields for multi-select filtering.
- [x] Merge single-value and multi-value fields server-side and deduplicate.
- [x] Query target students with SQL `IN` for grade and major arrays.
- [x] Change admin form to Element Plus multi-select controls with `filterable` and `allow-create`.
