# Today List — Project plan

Phased native builds for Android and iOS. Same product scope; independent implementations.

## Goals

1. Ship a calm local-first Today / Later / History todo app.
2. Feature parity across Android and iOS for 1.0.
3. Monetize with optional Remove Ads (no account required).
4. Publish via Google Play and App Store (TestFlight / internal testing first).

## Out of scope (1.0)

- Cloud sync / accounts
- Backup/export UI
- Kotlin Multiplatform shared modules
- Custom recurrence UI beyond presets (engine may support custom intervals)

---

## Android phases (done)

| Phase | Scope | Status |
|-------|--------|--------|
| 0–1 | Scaffold, Room/DataStore, settings | Done |
| 2 | Today / Later shell, Quick Add, reorder | Done |
| 3 | Complete / undo, History | Done |
| 4–5 | Task detail, reminders, recurrence | Done |
| 6 | Rollover, full settings | Done |
| 7+ | AdMob, Play Billing, Glance widget, Play listing | Done (internal testing track live) |

See also [android-persistence-foundation.md](android-persistence-foundation.md) and [`store/play/listing.md`](../store/play/listing.md).

---

## iOS phases

Native **SwiftUI + SwiftData** under `ios/` (XcodeGen: `project.yml`).

| Phase | Scope | Status |
|-------|--------|--------|
| 0 | Xcode project, folder layout, theme, README | Done |
| 1 | SwiftData schema, repositories, settings, tests | Done |
| 2 | Today / Later tabs, Quick Add, move / reorder | Done |
| 3 | Complete / undo, History list / detail / recreate | Done |
| 4 | Task detail, recurrence engine, notifications, delete scopes | Done |
| 5 | Daily rollover Ask / auto, full Settings | Done |
| 6 | Ads placeholder, StoreKit Remove Ads, WidgetKit, App Store listing draft | Done |

Next (release ops, not code phases):

1. Set Apple Developer Team + App Groups in Xcode.
2. Create App Store Connect app + IAP product `com.fourctech.todaylist.removeads`.
3. Wire production AdMob iOS units (replace placeholder).
4. TestFlight → App Store review.

See [ios-architecture.md](ios-architecture.md) and [`store/appstore/listing.md`](../store/appstore/listing.md).

---

## Shared checklist for public launch

- [ ] Privacy / terms live (done on web)
- [ ] Store listings + screenshots
- [ ] Data safety / App Privacy labels match actual SDKs
- [ ] Content rating
- [ ] Production signing / upload keys secured (not in git)
- [ ] Ads + IAP verified on real devices
- [ ] Widgets verified on home screen

## Architecture reference

Cross-platform overview: [architecture.md](architecture.md)
