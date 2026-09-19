# Today List 1.1 Implementation Plan

## Scope

Today List 1.1 strengthens the standalone Today List app. Cross-app integration with Quick Capture is explicitly deferred until Quick Capture's current App Store submission is resolved and the 1.1 apps are independently stable.

This release will preserve Today List as the owner of its own task database and will not introduce the suite-wide App Group or cross-app handoff queue.

## Objectives

1. Add a focused end-of-day workflow.
2. Improve system-level capture and navigation through App Intents and versioned deep links.
3. Expand the existing widget experience to Home Screen and Lock Screen surfaces.
4. Add a deliberately small Apple Watch companion for Today-only execution.
5. Preserve all existing SwiftData records, reminders, completion history, settings, advertising/IAP behavior, and current production functionality.

## Existing Architecture

- iOS 17+
- SwiftUI
- SwiftData
- AppEnvironment composition root
- Existing WidgetKit target
- Existing app-specific App Group: `group.com.fourctech.todaylist`
- Existing URL scheme: `todaylist://`
- Xcode project generated from `ios/project.yml`
- Unit tests already cover recurrence, CRUD, completion, undo, reorder, settings, and rollover behavior

All target and build-setting changes must be authored in `ios/project.yml` first and the Xcode project regenerated from that source.

## Feature 1 — End My Day

### Entry points

- Today screen toolbar
- App Intent: Start End My Day
- Versioned deep link: `todaylist://v1/end-my-day`

### Flow

#### Step 1 — Review Today

Show:
- completed items from today
- unfinished Today items

The screen should make it obvious that unfinished items require an explicit disposition.

#### Step 2 — Resolve unfinished items

For each unfinished item, allow exactly these actions:
- Move to Tomorrow
- Move to Later
- Mark Done
- Delete

No unfinished item should be silently moved or completed.

The workflow cannot finish until each unfinished item has an explicit resolution.

#### Step 3 — Review Tomorrow

Show items scheduled for tomorrow.

Allow:
- reorder
- quick add
- remove/move back where supported by existing repository semantics

#### Step 4 — Finish

Show a concise summary such as:

```
Today complete
5 done
2 moved to tomorrow
1 moved to later
```

A persistent summary model is optional for 1.1. Do not change the SwiftData schema solely to store the summary unless required by implementation.

### Data rules

- Reuse existing task, occurrence, completion, recurrence, and rollover semantics.
- Do not duplicate completion-history behavior.
- No destructive SwiftData migration.
- Existing recurring-task behavior must remain intact.

## Feature 2 — App Intents / Siri / Shortcuts

Add:

### Add Today Item
Parameter:
- title/text

Behavior:
- create a normal Today task through the existing repository/domain layer
- return a concise confirmation

### Add Later Item
Parameter:
- title/text

Behavior:
- create a normal Later task through the existing repository/domain layer

### Complete Item
Parameters:
- task identity

Behavior:
- expose only active actionable tasks
- complete using the same business logic as the app
- preserve recurrence/history semantics

### Show Today List
Behavior:
- opens Today List on Today

### Start End My Day
Behavior:
- opens Today List directly into End My Day

App Intents must not create a parallel persistence implementation.

## Feature 3 — Versioned Deep Links

Support:

- `todaylist://v1/today`
- `todaylist://v1/later`
- `todaylist://v1/add`
- `todaylist://v1/add?destination=today`
- `todaylist://v1/add?destination=later`
- `todaylist://v1/end-my-day`

Routing should be centralized and typed rather than parsed ad hoc in individual views.

The existing `todaylist` URL scheme remains unchanged so there is no compatibility break.

## Feature 4 — Widgets

Enhance the existing `TodayListWidget` target rather than adding a duplicate widget target.

### Small Home Screen widget
Display:
- Today remaining count
- concise progress/status

Tap:
- opens `todaylist://v1/today`

### Medium Home Screen widget
Display:
- top 3–5 Today items
- completion state

Interaction:
- use App Intent-backed completion where platform/runtime support is reliable
- otherwise item tap opens Today List focused on Today

### Lock Screen
Support appropriate accessory families.

Display:
- remaining count or simple progress
- no sensitive task text by default on Lock Screen

Tap:
- opens Today

### Snapshot architecture
Continue using the existing app-specific App Group:
`group.com.fourctech.todaylist`

The widget reads a lightweight snapshot, never the production SwiftData database directly.

Snapshot publishing must occur after:
- add
- complete/uncomplete
- delete
- reorder
- move Today/Later
- End My Day resolutions

## Feature 5 — Apple Watch

### Scope

Only:
- Today list
- remaining count
- complete/uncomplete
- quick add to Today by native text/dictation input
- complication

Do not include:
- Later management
- History
- Settings
- Premium/IAP UI
- End My Day workflow
- complex editing

### Watch Today screen
Display:
- remaining count
- active Today items
- completion state

Item action:
- complete/uncomplete

### Quick Add
- native watch text/dictation entry
- destination is always Today
- preserve a local pending operation until delivery is acknowledged or otherwise safely confirmed

### Synchronization

iPhone remains source of truth.

Use WatchConnectivity with:
- app context for Today snapshot
- queued operation transport for mutations
- operation UUIDs
- idempotent handling on iPhone
- retry-safe local pending queue on Watch
- no silent loss when phone is unreachable

### Complication
Display:
- e.g. `3 left` or compact progress

Tap:
- opens Today list on Watch

## Project / Target Changes

Update `ios/project.yml` to define any new Watch and Watch widget targets.

Recommended bundle identifiers:
- iPhone: existing `com.fourctech.todaylist`
- existing widget: `com.fourctech.todaylist.widget`
- Watch app: `com.fourctech.todaylist.watchkitapp`
- Watch widget: `com.fourctech.todaylist.watchkitapp.widgets`

Keep version/build numbers synchronized across embedded targets for App Store validation.

## Explicitly Deferred

Not part of Today List 1.1:

- Quick Capture → Today List handoff
- Today List → Top 3 handoff
- Today List → Waiting For handoff
- suite-wide shared App Group
- shared `ProductivitySuiteCore` package
- cross-app payload queues
- accounts/cloud sync
- new analytics SDKs
- changes to ad/IAP strategy

## Regression Requirements

### Existing app
- upgrade existing production installation without data loss
- Today CRUD
- Later CRUD
- recurrence
- reminders/notifications
- complete/uncomplete/undo
- reorder
- history
- rollover
- settings
- ads/IAP behavior
- dark mode
- Dynamic Type
- VoiceOver smoke test

### Deep links
- Today
- Later
- Add Today
- Add Later
- End My Day

### App Intents
- Add Today
- Add Later
- Complete Item
- Show Today
- Start End My Day

### Widgets
- small
- medium
- Lock Screen
- snapshot refresh after mutations
- app relaunch
- device reboot
- privacy on Lock Screen

### Watch
- phone reachable
- phone unavailable
- queued quick add
- completion/uncompletion
- retry behavior
- duplicate delivery safety
- snapshot refresh
- complication
- app upgrade/reinstall smoke test

### Release
- clean build
- unit tests
- physical iPhone regression
- physical Watch regression
- archive
- Validate App before upload

## Definition of Done

Today List 1.1 is complete when:

- End My Day gives every unfinished Today item an explicit disposition
- Today can be added through Siri/Shortcuts
- Today status is visible from widgets without opening the app
- Watch can show Today, add a Today item, and complete/uncomplete reliably
- versioned deep links work
- production data upgrades intact
- existing features regress cleanly
- archive and App Store validation succeed
