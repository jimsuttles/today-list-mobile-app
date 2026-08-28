# Today List — iOS architecture

Bundle ID: `com.fourctech.todaylist`  
Deployment: iOS 17+  
Project: `ios/TodayList.xcodeproj` (generated from `ios/project.yml` via XcodeGen)

## Layout

```
ios/
  project.yml
  TodayList/
    App/                 TodayListApp, AppEnvironment
    Domain/
      Models/            enums, TaskItem, RecurrenceRule, AppSettings, …
      Repositories/      protocols
      Recurrence/        DefaultRecurrenceEngine
      Rollover/          RolloverManager
      UseCases/          RecreateTaskFromHistoryUseCase
    Data/
      SwiftData/         PersistedTask, Recurrence, Occurrence, Completion
      Repositories/      SwiftDataTaskRepository, SwiftDataHistoryRepository
      Settings/          UserDefaultsSettingsRepository
    Features/            Today, Later, History, Settings, TaskDetail, QuickAdd, Rollover, Premium, Root, Components
    Infrastructure/      Notifications, Billing, Ads, Analytics, Time, WidgetSnapshot
    Theme/
  TodayListTests/
  TodayListWidget/       WidgetKit extension (App Group snapshot)
```

## Persistence (SwiftData)

Mirrors Android Room v1:

| Model | Role |
|-------|------|
| `PersistedRecurrence` | Rule metadata |
| `PersistedTask` | Task row (`statusRaw` TODAY/LATER/DELETED) |
| `PersistedOccurrence` | Dated occurrence |
| `PersistedCompletion` | History event + title snapshot |

Settings keys (UserDefaults): `theme_mode`, `rollover_mode`, `week_start`, `haptics_enabled`, `ads_removed_cached`, `last_rollover_date`, `notification_permission_prompted`.

App Group `group.com.fourctech.todaylist` shares Today titles with the widget.

## Composition

`AppEnvironment` owns `ModelContainer`, repositories, rollover, billing, and UI-facing state (undo banner, rollover sheet, settings). Injected via SwiftUI `EnvironmentValues`.

## Tests

```bash
cd ios
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
xcodebuild -scheme TodayList -destination 'platform=iOS Simulator,name=iPhone 17 Pro' test
```

Coverage includes recurrence engine, task CRUD / complete / undo / reorder, settings, rollover Ask / auto-later.

## Signing notes

- Set Development Team on **TodayList** and **TodayListWidget**.
- Enable App Group for both targets if Xcode does not pick up entitlements automatically.
- Do not commit secrets; use Xcode / CI secrets for distribution.

Cross-platform: [architecture.md](architecture.md) · Plan: [project-plan.md](project-plan.md)
