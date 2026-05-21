# Notice Feedback Workflow Design

## 1. Goal

Add a notice-scoped feedback workflow so students can ask questions about a specific notice, student cadres can handle ordinary questions, and counselors can handle private or escalated questions. The feedback handling route follows the notice delivery route: the counselor who publishes the notice is the final responsible handler, and that counselor can assign student cadres to handle ordinary questions for that notice.

## 2. Roles And Visibility

| Role | Can Submit | Can View Ordinary | Can View Private | Can Handle | Can Inspect Logs |
|---|---:|---:|---:|---:|---:|
| Student | Yes | Own only | Own only | No | Own progress only |
| Student cadre | Yes | Assigned ordinary feedback | Own private only | Assigned ordinary feedback | Own handling logs |
| Counselor | No by default | Notices they are responsible for | Notices they are responsible for | Private/escalated feedback | All cadre logs under their notices |
| Admin | Optional phase 2 | All | All | Optional | All |

Privacy rule: private feedback never appears in any cadre pending list or cadre detail endpoint.

## 3. Feedback Types

- `ordinary`: common/public question. Initially assigned to the notice's configured student cadres.
- `private`: sensitive/private question. Directly assigned to the notice's final counselor.

## 4. Status Model

| Status | Meaning | Visible To |
|---|---|---|
| `pending_cadre` | Ordinary feedback awaiting cadre handling | Assigned cadres, responsible counselor |
| `pending_counselor` | Private feedback or escalated ordinary feedback awaiting counselor handling | Responsible counselor |
| `resolved_by_cadre` | Ordinary feedback closed by a cadre | Student, assigned cadres, responsible counselor |
| `resolved_by_counselor` | Feedback closed by counselor | Student, responsible counselor; assigned cadres if ordinary |
| `closed` | Optional archive state for future use | Responsible counselor |

## 5. Data Model

### 5.1 Extend `notice`

Add optional fields:

- `feedback_counselor_id BIGINT`: final responsible counselor. Defaults to `created_by` on create/publish if empty.
- `feedback_cadre_ids TEXT`: JSON array of student cadre user IDs assigned to ordinary feedback, e.g. `[1002,1003]`.

Rationale: feedback assignment is per notice. Storing assignment on `notice` keeps routing stable and easy to inspect.

### 5.2 New `notice_feedback`

| Field | Type | Description |
|---|---|---|
| `id` | BIGINT PK | Feedback ID |
| `notice_id` | BIGINT NOT NULL | Related notice |
| `message_id` | BIGINT | Student inbox message used to submit, if available |
| `student_user_id` | BIGINT NOT NULL | Submitter |
| `feedback_type` | VARCHAR(16) NOT NULL | `ordinary` / `private` |
| `content` | TEXT NOT NULL | Original question |
| `status` | VARCHAR(32) NOT NULL | Workflow status |
| `assigned_counselor_id` | BIGINT NOT NULL | Final counselor |
| `assigned_cadre_ids` | TEXT | Snapshot JSON of assigned cadres at submit time |
| `current_handler_id` | BIGINT | Current owner if single owner is needed; nullable for multi-cadre pool |
| `handled_by` | BIGINT | User who closed latest state |
| `handled_at` | TIMESTAMP | Close/escalation handling time |
| `created_at` | TIMESTAMP NOT NULL | Submit time |
| `updated_at` | TIMESTAMP NOT NULL | Last update |

`assigned_cadre_ids` is a snapshot so later edits to the notice do not rewrite historical routing.

### 5.3 New `notice_feedback_message`

This table is the audit trail and counselor-inspectable cadre handling log.

| Field | Type | Description |
|---|---|---|
| `id` | BIGINT PK | Log/message ID |
| `feedback_id` | BIGINT NOT NULL | Parent feedback |
| `sender_user_id` | BIGINT NOT NULL | Actor |
| `sender_role` | VARCHAR(32) NOT NULL | `student` / `cadre` / `counselor` / `admin` |
| `action_type` | VARCHAR(32) NOT NULL | `submit` / `cadre_reply` / `escalate` / `counselor_reply` / `resolve` |
| `content` | TEXT | Message/comment |
| `created_at` | TIMESTAMP NOT NULL | Action time |

Counselor log inspection uses this table to show: who handled, when, what reply was sent, whether it was escalated, and any escalation comment.

## 6. Routing Rules

1. Student opens a delivered notice and submits feedback.
2. Backend resolves the student's `messageId` or `noticeId` to `notice_id` and validates ownership via `user_message.user_id`.
3. Backend reads the notice routing snapshot:
   - `feedback_counselor_id` or fallback `notice.created_by`.
   - `feedback_cadre_ids` JSON array.
4. If feedback type is `ordinary` and at least one cadre is assigned:
   - Status becomes `pending_cadre`.
   - Assigned cadres can see it.
   - Responsible counselor can also inspect it and its logs.
5. If feedback type is `ordinary` but no cadre is assigned:
   - Status becomes `pending_counselor`.
   - This prevents unanswered ordinary feedback.
6. If feedback type is `private`:
   - Status becomes `pending_counselor`.
   - No cadre endpoint returns it.
7. A cadre can:
   - reply and close as `resolved_by_cadre`;
   - escalate to counselor with an optional comment, changing status to `pending_counselor`.
8. A counselor can:
   - reply and close as `resolved_by_counselor`;
   - inspect all message logs for notices they are responsible for.

## 7. API Design

### 7.1 Student APIs

`POST /api/messages/{id}/feedback`

Request:

```json
{
  "feedbackType": "ordinary",
  "content": "请问报名材料上传入口在哪里？"
}
```

Rules:

- `{id}` accepts `user_message.id`; for compatibility it may also resolve by `noticeId + current user`.
- `feedbackType` must be `ordinary` or `private`.
- content is required.

Response:

```json
{
  "id": "9001",
  "noticeId": "10001",
  "feedbackType": "ordinary",
  "status": "pending_cadre",
  "createdAt": "2026-05-21T12:00:00"
}
```

`GET /api/messages/{id}/feedbacks`

Returns only feedback submitted by the current student for this message/notice, including current status and visible replies.

### 7.2 Cadre APIs

`GET /api/notice-feedback/cadre/pending?pageNum=1&pageSize=10`

Returns ordinary feedback where the current user is in `assigned_cadre_ids` and status is `pending_cadre`.

`GET /api/notice-feedback/cadre/{id}`

Returns feedback detail and message logs visible to the assigned cadre. Private feedback is forbidden.

`POST /api/notice-feedback/{id}/cadre-reply`

```json
{
  "content": "报名入口在服务页-党团活动申请。"
}
```

Sets status to `resolved_by_cadre`, writes a `cadre_reply` log, and records `handled_by/handled_at`.

`POST /api/notice-feedback/{id}/escalate`

```json
{
  "content": "该同学情况需要辅导员确认。"
}
```

Sets status to `pending_counselor`, writes an `escalate` log, and keeps the original submitter/cadre logs visible to the responsible counselor.

### 7.3 Counselor APIs

`GET /api/notice-feedback/counselor/pending?pageNum=1&pageSize=10&type=&status=&noticeId=`

Returns private, escalated, and optionally ordinary feedback under notices where `assigned_counselor_id` equals current counselor.

`GET /api/notice-feedback/counselor/{id}`

Returns full feedback detail and all logs, including cadre replies/escalation comments.

`POST /api/notice-feedback/{id}/counselor-reply`

```json
{
  "content": "请按学院最新通知补交材料，截止时间延至周五。"
}
```

Sets status to `resolved_by_counselor`, writes a `counselor_reply` log, and records `handled_by/handled_at`.

### 7.4 Admin Notice APIs

Extend create/update/detail payloads with:

```json
{
  "feedbackCounselorId": "100",
  "feedbackCadreIds": ["1002", "1003"]
}
```

Rules:

- For counselor-created notices, `feedbackCounselorId` defaults to current user and can be omitted.
- `feedbackCadreIds` must be users with the `cadre` role/auth type.
- Published notices may allow editing feedback cadres for future feedback; existing feedback keeps its snapshot.

### 7.5 Home/Pending APIs

Extend `GET /api/home` `todoStats` with:

```json
{
  "pendingFeedbacks": 3,
  "pendingFeedbackRole": "cadre"
}
```

For web counselor dashboard, either reuse a home-like admin summary endpoint or add:

`GET /api/notice-feedback/counselor/pending-count`

## 8. UI Design

### 8.1 Student Notice Detail

- Add `疑问反馈` button below notice content/attachment.
- Modal/form fields:
  - problem type: ordinary/private;
  - content textarea.
- Show feedback history below the notice:
  - submitted time;
  - status label;
  - replies/logs visible to student.

### 8.2 Student Cadre Mini Program

- Home `我的待办` adds `待处理反馈` card for users with role/authType `cadre`.
- Card opens `pages/feedback-pending/feedback-pending`.
- Pending list shows notice title, student question, submit time.
- Detail page actions:
  - `回复并处理`;
  - `上报辅导员` with optional comment.

### 8.3 Counselor Web

- Add sidebar/menu entry `反馈处理`.
- Dashboard/home card shows pending count and quick entry.
- Feedback page supports:
  - tabs: pending/private/escalated/resolved/all;
  - filters: notice, type, status;
  - detail drawer with full log timeline;
  - action: reply/close.
- Notice management detail drawer includes a feedback summary and a link to feedback list for that notice.

### 8.4 Notice Publish/Edit Page

- Add `普通反馈处理骨干` multi-select.
- Default `最终处理辅导员` is current counselor; admin can select counselor if needed.
- Use student/cadre list endpoint filtered by `authType=cadre` for selector.

## 9. Permission Checks

- Student submit/list: must own a `user_message` for the notice.
- Cadre pending/detail/action: must be in feedback's `assigned_cadre_ids`, feedback type must be `ordinary`, status must allow cadre action.
- Counselor pending/detail/action: current user must equal `assigned_counselor_id` or be admin.
- Private feedback: never returned from cadre endpoints even if the current user is assigned as cadre on the notice.

## 10. Testing Strategy

Backend tests:

- Student cannot submit feedback for someone else's notice message.
- Ordinary feedback routes to `pending_cadre` when cadres are assigned.
- Ordinary feedback routes to `pending_counselor` when no cadres are assigned.
- Private feedback routes to counselor and is invisible to cadre queries.
- Assigned cadre can reply and close ordinary feedback.
- Assigned cadre can escalate with comment; counselor sees escalation log.
- Unassigned cadre cannot view or act.
- Counselor can inspect full cadre handling logs.
- Existing `messageId`/`noticeId` compatibility is preserved.

Frontend/manual validation:

- Student submits ordinary/private feedback from notice detail.
- Cadre sees ordinary pending count and handles/escalates.
- Counselor sees private/escalated pending count and full log timeline.
- Published notice with no cadre still routes ordinary feedback to counselor.

## 11. Phasing

### Phase 1: Backend core

- Tables/migrations.
- Notice routing fields.
- Student submit/list APIs.
- Cadre pending/action APIs.
- Counselor pending/action/log APIs.
- Tests for routing and permissions.

### Phase 2: Mini program

- Student feedback submit/history on notice detail.
- Cadre pending list/detail/action pages.
- Home pending card.

### Phase 3: Counselor web

- Notice publish/edit cadre selector.
- Feedback management page.
- Dashboard/pending quick entry.

### Phase 4: Polish

- Notification badges.
- Search/export.
- Optional admin oversight.
