# Notice Feedback Workflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a notice-scoped feedback workflow where ordinary questions go to assigned student cadres first and private/escalated questions go to the responsible counselor, with counselor-inspectable cadre handling logs.

**Architecture:** Add `notice` routing fields plus `notice_feedback` and `notice_feedback_message` tables. Implement backend routing and permission checks first, then add mini-program student/cadre pages and counselor web management. Keep feedback assignment snapshots on each feedback row so historical routing remains auditable.

**Tech Stack:** Spring Boot 3, MyBatis-Plus, Flyway, JUnit 5, Vue 3, Element Plus, WeChat Mini Program.

---

### Task 1: Feedback Tables And Notice Routing Fields

**Files:**
- Create: `demo/src/main/resources/db/migration/V23__create_notice_feedback_tables.sql`
- Modify: `demo/src/main/java/com/ruc/platform/notice/entity/Notice.java`
- Create: `demo/src/main/java/com/ruc/platform/notice/entity/NoticeFeedback.java`
- Create: `demo/src/main/java/com/ruc/platform/notice/entity/NoticeFeedbackMessage.java`
- Create: `demo/src/main/java/com/ruc/platform/notice/mapper/NoticeFeedbackMapper.java`
- Create: `demo/src/main/java/com/ruc/platform/notice/mapper/NoticeFeedbackMessageMapper.java`
- Modify: `demo/src/main/java/com/ruc/platform/admin/notice/dto/NoticeCreateDTO.java`
- Modify: `demo/src/main/java/com/ruc/platform/admin/notice/dto/NoticeUpdateDTO.java`
- Modify: `demo/src/main/java/com/ruc/platform/admin/notice/vo/NoticeDetailVO.java`
- Modify: `demo/src/main/java/com/ruc/platform/admin/notice/service/AdminNoticeServiceImpl.java`
- Test: `demo/src/test/java/com/ruc/platform/admin/notice/service/AdminNoticeServiceImplTest.java`

- [ ] Add failing tests that create/update notices persist `feedbackCounselorId` and `feedbackCadreIds`.
- [ ] Add migration with notice routing fields and feedback/log tables.
- [ ] Add entity/mapper classes and notice DTO/VO fields.
- [ ] Implement JSON serialization for `feedbackCadreIds` and default counselor to creator.
- [ ] Run targeted admin notice tests.

### Task 2: Backend Feedback Service And Student APIs

**Files:**
- Create: `demo/src/main/java/com/ruc/platform/notice/dto/NoticeFeedbackCreateDTO.java`
- Create: `demo/src/main/java/com/ruc/platform/notice/vo/NoticeFeedbackVO.java`
- Create: `demo/src/main/java/com/ruc/platform/notice/vo/NoticeFeedbackMessageVO.java`
- Create: `demo/src/main/java/com/ruc/platform/notice/service/NoticeFeedbackService.java`
- Create: `demo/src/main/java/com/ruc/platform/notice/service/NoticeFeedbackServiceImpl.java`
- Modify: `demo/src/main/java/com/ruc/platform/notice/controller/MessageController.java`
- Modify: `demo/src/main/java/com/ruc/platform/notice/mapper/UserMessageMapper.java`
- Test: `demo/src/test/java/com/ruc/platform/notice/service/NoticeFeedbackServiceImplTest.java`

- [ ] Add failing tests for student ownership validation, ordinary routing, private routing, and noticeId fallback.
- [ ] Implement `POST /api/messages/{id}/feedback`.
- [ ] Implement `GET /api/messages/{id}/feedbacks`.
- [ ] Write submit logs to `notice_feedback_message` with `action_type=submit`.
- [ ] Run targeted feedback service tests.

### Task 3: Backend Cadre And Counselor APIs

**Files:**
- Create: `demo/src/main/java/com/ruc/platform/notice/controller/NoticeFeedbackController.java`
- Create: `demo/src/main/java/com/ruc/platform/notice/dto/NoticeFeedbackReplyDTO.java`
- Modify: `demo/src/main/java/com/ruc/platform/notice/service/NoticeFeedbackService.java`
- Modify: `demo/src/main/java/com/ruc/platform/notice/service/NoticeFeedbackServiceImpl.java`
- Modify: `demo/src/main/java/com/ruc/platform/notice/mapper/NoticeFeedbackMapper.java`
- Modify: `demo/src/main/java/com/ruc/platform/notice/mapper/NoticeFeedbackMessageMapper.java`
- Test: `demo/src/test/java/com/ruc/platform/notice/service/NoticeFeedbackServiceImplTest.java`

- [ ] Add failing tests that assigned cadres can reply/escalate and unassigned cadres/private feedback are forbidden.
- [ ] Add failing test that counselor sees full cadre logs after reply/escalation.
- [ ] Implement cadre pending/detail/reply/escalate endpoints.
- [ ] Implement counselor pending/detail/reply/count endpoints.
- [ ] Run targeted feedback service tests.

### Task 4: Home Pending Counts

**Files:**
- Modify: `demo/src/main/java/com/ruc/platform/home/vo/TodoStatsVO.java`
- Modify: `demo/src/main/java/com/ruc/platform/home/service/HomeServiceImpl.java`
- Test: `demo/src/test/java/com/ruc/platform/home/service/HomeServiceImplTest.java` if a home test scaffold exists; otherwise cover count logic in feedback service tests.

- [ ] Add pending feedback count methods for cadre/counselor role contexts.
- [ ] Extend home todo stats with `pendingFeedbacks` and `pendingFeedbackRole`.
- [ ] Ensure non-cadre students see zero or omit the quick pending feedback card.
- [ ] Run backend tests.

### Task 5: Mini Program Student Feedback UI

**Files:**
- Modify: `miniprogram-1/pages/notice-detail/notice-detail.js`
- Modify: `miniprogram-1/pages/notice-detail/notice-detail.wxml`
- Modify: `miniprogram-1/pages/notice-detail/notice-detail.wxss`

- [ ] Add feedback type/content form on notice detail.
- [ ] Call `POST /api/messages/{id}/feedback`.
- [ ] Load and display current student's feedback history.
- [ ] Show private/ordinary status labels and visible replies.

### Task 6: Mini Program Cadre Pending UI

**Files:**
- Create: `miniprogram-1/pages/feedback-pending/feedback-pending.js`
- Create: `miniprogram-1/pages/feedback-pending/feedback-pending.wxml`
- Create: `miniprogram-1/pages/feedback-pending/feedback-pending.wxss`
- Create: `miniprogram-1/pages/feedback-pending/feedback-pending.json`
- Modify: `miniprogram-1/app.json`
- Modify: `miniprogram-1/pages/index/index.js`
- Modify: `miniprogram-1/pages/index/index.wxml`

- [ ] Add home pending feedback card for student cadres.
- [ ] Add pending feedback list and detail action UI.
- [ ] Implement reply and escalate actions with optional comment.
- [ ] Verify private feedback does not display in cadre UI.

### Task 7: Counselor Web Notice Routing And Feedback Page

**Files:**
- Modify: `web-counselor/src/api/notice.js`
- Create: `web-counselor/src/api/noticeFeedback.js`
- Modify: `web-counselor/src/views/NoticesView.vue`
- Create: `web-counselor/src/views/NoticeFeedbackView.vue`
- Modify: `web-counselor/src/router/index.js`
- Modify: `web-counselor/src/layouts/MainLayout.vue`

- [ ] Add `feedbackCadreIds` selector to notice create/edit form.
- [ ] Default counselor handling to current counselor; allow admin override only if current role data supports it.
- [ ] Add feedback management list, filters, pending counts, and detail drawer timeline.
- [ ] Add counselor reply/close action.
- [ ] Add sidebar/dashboard quick entry.
- [ ] Run `cd web-counselor && npm run build`.

### Task 8: Documentation And Final Verification

**Files:**
- Modify: `documents/API.md`

- [ ] Document feedback types, statuses, routing rules, and endpoints.
- [ ] Document privacy rule and counselor log inspection behavior.
- [ ] Run `cd demo && ./mvnw test`.
- [ ] Run `cd web-counselor && npm run build`.
- [ ] Manually smoke-test mini-program feedback submit and cadre pending flow if a WeChat devtools session is available.
