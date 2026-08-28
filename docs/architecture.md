# Today List — Architecture

Local-first todo app by **4CTech, LLC**. Shared product model across platforms; separate native codebases (no KMP).

| | Android | iOS |
|--|---------|-----|
| Package / bundle | `com.fourctech.todaylist` | `com.fourctech.todaylist` |
| UI | Jetpack Compose + Material 3 | SwiftUI |
| Persistence | Room + DataStore | SwiftData + UserDefaults |
| DI / composition | Hilt | `AppEnvironment` + SwiftUI environment |
| Notifications | AlarmManager + channels | `UNUserNotificationCenter` |
| IAP | Play Billing | StoreKit 2 |
| Ads | AdMob + UMP | Banner placeholder → AdMob when wired |
| Widget | Glance | WidgetKit |
| Deep link | `todaylist://task/{id}` | `todaylist://task/{id}` |

## Product model

Tasks live in **Today**, **Later**, or **History** (completion events). No account. Titles/notes stay on device.

```
┌─────────────────────────────────────────────────────────┐
│  UI  (lists, detail, settings, quick add, rollover)     │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│  Domain                                                 │
│  Models · Repository protocols · Recurrence · Rollover  │
│  Use cases (complete, undo, recreate, daily rollover) │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│  Data                                                   │
│  Tasks · Occurrences · Recurrence rules · Completions   │
│  Settings (theme, rollover, week start, ads entitlement)│
└─────────────────────────────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│  Platform                                               │
│  Notifications · Billing · Ads · Analytics · Widget     │
└─────────────────────────────────────────────────────────┘
```

## Domain concepts

| Concept | Meaning |
|---------|---------|
| `Task` / `TaskItem` | Title, notes, Today/Later, sort order, optional reminder + recurrence |
| `TaskStatus` | `TODAY` / `LATER` / `DELETED` (soft-delete on complete for non-recurring) |
| `RecurrenceRule` | Daily, weekdays, weekly, monthly (+ custom interval variants) |
| `TaskOccurrence` | Dated instance; open vs completed; `movedToLater` |
| `CompletionEvent` | Immutable `titleSnapshot` for History |
| `AppSettings` | Theme, rollover mode, week start, haptics, ads-removed cache, last rollover date |
| `RolloverMode` | Ask each day / keep on Today / move to Later |
| `DeleteScope` | This task vs entire series |

### Complete behavior

1. Close open occurrence; write completion event (snapshot + date).
2. **No recurrence** → status `DELETED`.
3. **Has recurrence** → compute next date; if none, delete; else new open occurrence, task → Today, clear reminder.

### Daily rollover

Evaluated on launch / foreground (not midnight alarm). If `lastRolloverDate < today`, apply mode (Ask dialog, or auto keep/move), then set last rollover date.

## Monetization

- Banner ads when `adsRemovedCached == false`.
- One-time Remove Ads: `com.fourctech.todaylist.removeads`.
- Wipe / clear history keeps IAP entitlement.

## Platform docs

- [Android persistence foundation](android-persistence-foundation.md)
- [iOS architecture](ios-architecture.md)
- [Project plan / phases](project-plan.md)
- [Accessibility](accessibility.md)

## Store & legal

- Play: [`store/play/listing.md`](../store/play/listing.md)
- App Store: [`store/appstore/listing.md`](../store/appstore/listing.md)
- Privacy / terms: [`store/legal/`](../store/legal/) · live URLs on [4ctech.io/today-list](https://www.4ctech.io/today-list/)
