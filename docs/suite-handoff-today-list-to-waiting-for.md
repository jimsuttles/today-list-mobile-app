# Today List → Waiting For Handoff Contract

**Issue:** #8 · **Companion receiver:** `jimsuttles/waiting-for-mobile-app` (see the linked issue)
**Transport:** ProductivitySuiteCore v1 (App Group `Handoffs/Pending` → `Handoffs/Completed`)

## Discovery (2026-10-21)

The source side already exists on `main`. It was merged directly as "Sprint 05 Today List Waiting For handoff" (`630cb83`), so it does not appear in the PR history. This work hardens that implementation instead of duplicating it.

Supported Waiting For inbound origins, before this change:

| Source | Required metadata | Identity check |
| --- | --- | --- |
| Quick Capture | `sourceCaptureId` (UUID) | `payload.id` must equal the derived ID |
| Today List | none | **none** (any ID accepted) |

## Payload

| Field | Value |
| --- | --- |
| `id` | `SuiteHandoffID.generate(.todayList, sourceEntityID, .waitingFor)`, where `sourceEntityID` is `<taskId>` for attempt 1 and `<taskId>\|waitingfor\|attempt-<n>` for attempt n ≥ 2 |
| `version` / `sourceApp` / `destinationApp` | `1` / `todaylist` / `waitingfor` |
| `title` / `notes` | task title / task notes (unchanged; Waiting For trims and caps the subject at 250 characters) |
| `metadata.sourceTaskId` | Today List task ID (required) |
| `metadata.attempt` | `"1"`, `"2"`, … (**new**; lets Waiting For verify the ID exactly) |
| `metadata.sourceLocation` | `today` / `later` (informational) |
| `metadata.requestedDate` | ISO-8601 (informational; Waiting For defaults "requested" to now) |

URL: `waitingfor://v1/handoff/<id>`. The URL is opaque: it contains no task text or task ID.

**Fields Today List does not have** (who you're waiting on, follow-up date, expected response date, reminder) are never invented. Waiting For opens its normal form, prefilled with the subject and notes. The user must enter the person or organization. Dates and reminders are off unless the user sets them. Cancelling the form creates nothing and leaves the handoff pending.

## States

```
Today List task ──Send (confirm)──► attempt record (accepted = false) + Pending/<id>.json
   │                                         │
   │  open declined / app missing  ──────────┘ error shown; task unchanged; retry reuses <id>
   │
   └─ open accepted ─► record.accepted = true
                         │
     Waiting For: user saves the form ─► follow-up id = <id>, Pending → Completed
                         │
     Today List becomes active: "Follow-up created. Keep / Mark Done / Remove?"  (user choice)
                         │
                       resolve ─► record removed, next attempt = n + 1
```

- **Retry vs. re-handoff.** Until the user resolves the prompt, every send reuses the same ID. The pending file is overwritten, and Waiting For dedupes by ID. If Waiting For already completed the handoff, Today List reports "This task is already in Waiting For." After the prompt is resolved, a new send is an intentional re-handoff with attempt n + 1 and a new ID.
- **Source preservation.** Sending never modifies the task. The completed acknowledgement only triggers the prompt. Mark Done uses normal completion (recurrence and history semantics apply). Remove deletes only that occurrence (`.thisTask`). If the task was already deleted, the record is resolved silently.
- **Compatibility.** Attempt-1 IDs are unchanged, so existing records and installed builds keep working. Waiting For accepts payloads without `attempt` from older Today List builds by matching attempts 1 through 100.

## Device-only checks (user)

For each check, confirm the Today List task is still unchanged afterwards:

1. Send a Today task and a Later task with notes. Confirm the Waiting For form is prefilled and has an empty "Who".
2. Waiting For cold (killed) and warm (running): the same result each time.
3. Cancel the Waiting For form, then send again. Exactly one follow-up should be created after saving.
4. Send again after saving, before returning to Today List. You should see "already in Waiting For" and no duplicate.
5. Return to Today List and check Keep / Mark Done / Remove, including on a repeating task.
6. Uninstall Waiting For, then send. You should see "could not be opened".
