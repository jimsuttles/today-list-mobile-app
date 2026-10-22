# Today List — iPhone Duo Audit Results

**Issue:** [#10](https://github.com/jimsuttles/today-list-mobile-app/issues/10) · **Static audit:** [IPHONE_DUO_STATIC_AUDIT_AND_HANDOFF.md](IPHONE_DUO_STATIC_AUDIT_AND_HANDOFF.md)
**Date:** October 22, 2026 · **Branch:** `feature/iphone-duo-adaptive-layout`

## Environment

| Item | Value |
| --- | --- |
| Xcode | 27.1 (27A9275), updated in place during this work from 27.0 (27A266a) |
| SDK | iOS 27.1 / iOS Simulator 27.1 |
| Duo simulator | iPhone Duo, iOS 27.1 runtime (24A94232). The device type requires runtime ≥ 27.1, so it cannot run on 27.0. |
| Other simulators | iPhone 17e and iPhone 17 (iOS 27.0) |
| Apple skill | `app-resizability` (from `xcrun agent skills export` in Xcode 27.1; not present in 27.0) |
| Driving | `simctl` and the Claude Code simulator panel (tap, type, touch path). There is no command-line fold control. |

## Apple `app-resizability` skill

**Prerequisites** (read-only):

| Check | Result |
| --- | --- |
| Launch screen | ✅ `INFOPLIST_KEY_UILaunchScreen_Generation: YES` |
| iPad orientations | ✅ All four declared |
| `UIRequiresFullScreen` | ✅ Absent |

The iPhone target is portrait-only. Apple's skill states that "a portrait-only iPhone declaration is normal", so the orientation setting is unchanged.

**Detection:** the skill's five tasks (UIScreen.main, interfaceOrientation, scene lifecycle, safe-area insets, userInterfaceIdiom) were run against all Swift files in the app, widget, Watch app, and Watch widget targets.

- **No occurrences.** The app is pure SwiftUI with a `WindowGroup` scene lifecycle.
- The only safe-area modifiers are two background fills (`Color….ignoresSafeArea`). The skill says to leave those alone.
- **No resizability code changes are required.**

## Automated tests and builds

All runs used Xcode 27.1, clean DerivedData, unit tests on the iPhone 17 simulator (iOS 27.0), and counts from the `.xcresult` bundle.

| Run | Tests | Builds |
| --- | --- | --- |
| `main` @ `3af4122` (baseline) | 45/45 passed | TodayList, TodayListWidget, TodayListWatchApp, TodayListWatchWidget: all succeeded |
| This branch @ `ba65753` | 45/45 passed | Same four schemes succeeded, plus the app for the iPhone Duo destination |

The `TodayListWatchWidget` scheme accepts only an iOS destination: its build list leads with the iOS app. This is pre-existing and unchanged. No unit tests were added, because both fixes are view modifiers verified by on-simulator before/after evidence.

## Findings

| # | Finding | Where verified | Result |
| --- | --- | --- | --- |
| 1 | At AX5 text size, the custom tab labels hyphenate ("To-day", "His-tory", "Set-tings"). The bar grows to about ¼ of the screen on iPhone 17e and about ⅓ on the Duo outer display, hiding task rows. | iPhone 17e and Duo outer display, before/after screenshots | **Fixed** (`bc66ae8`). The bar's text is capped at `xxxLarge` and labels stay on one line, matching the system tab bar. Each tab offers the Large Content Viewer. Default text size renders byte-identically on iPhone 17e. |
| 2 | A tap inside a tab's column but outside its icon or label did nothing. Each tab only hit-tested its glyphs. | Duo outer display: a tap at (102, 612) pt, 40 pt left of the Later icon, did nothing; a tap on the icon switched tabs. | **Fixed** (`ba65753`). `.contentShape(Rectangle())` on the tab label. The same tap now switches to Later. |
| 3 | Row completion target | Duo outer display: a tap 16 pt outside the drawn circle completed the task | Adequate. Not changed, as the issue asks. |
| 4 | With the keyboard up, the custom tab bar sits above the keyboard and takes about 80 pt of the editor's height on the outer display. | Duo outer display, task detail | Usable: title, notes, cursor, and Save stay visible. **Follow-up candidate:** a system `TabView` would hide behind the keyboard. Not changed speculatively. |
| 5 | "Later" tab label sits a few pixels higher than the others, because the tray glyph is shorter and labels aren't baseline-aligned. | iPhone 17e | Cosmetic. Not changed. |

## Folded (outer display) results with the fix

| Check | Result |
| --- | --- |
| All four tabs render | ✅ No clipping. The system rail holds the status bar and the End My Day / Add toolbar items. |
| Tab switching | ✅ Including the full tab column |
| Open task detail | ✅ Back button moves into the system rail; Save stays at top right |
| Edit notes with the keyboard | ✅ Text saved and shown in the row |
| Reorder | ✅ Dragged "Review budget" to the top; counts unchanged ("1 done · 3 left"), so nothing was completed accidentally |
| Complete task and undo banner | ✅ Banner lays out in the narrow pane. Undo itself was not verified: the 6 s auto-dismiss elapsed before the simulated tap. |

Evidence: [`docs/assets/iphone-duo-audit/`](assets/iphone-duo-audit/).

## Not verified (needs Simulator.app or a device)

- **Unfolded inner display (2007 × 2853) and fold/unfold transitions.** Xcode 27.1's `simctl` has no fold control; `screenConfig power` does not change posture. Powering a screen off and on left both displays black until the simulator was rebooted. That is a simulator artifact, not app behavior.
- **Resize while editing; data preserved across fold/unfold;** Split View and side-by-side. Same reason as above.
- **VoiceOver** (only Large Content Viewer support was added); **rollover and End My Day review** during resize; **iPad Split View**.
- **Physical iPhone Duo.**

## Screenshots for App Store Connect

Apple's sizes: outer **1398 × 2034** (or 2034 × 1398) and inner **2007 × 2853** (or 2853 × 2007). These match the Duo simulator's displays exactly.

- ✅ **Outer, captured:** [`store/appstore/screenshots/iphone-duo/outer/`](../store/appstore/screenshots/iphone-duo/outer/) has Today, Later, History, and Settings at 1398 × 2034. They come from the Duo simulator with demo data, light mode, default text size, and a 9:41 status bar. They are not composited or edited.
  - Regenerate them with `store/appstore/screenshots/iphone-duo/capture_duo_outer.sh <TodayList.app>`.
- ❌ **Inner, not captured.** It needs an unfolded posture. In Simulator.app, unfold the Duo, launch with `-ScreenshotDemo`, then run `xcrun simctl io <udid> screenshot --display=3 <file>` for each tab. Verify the size is 2007 × 2853.
- The owner uploads to App Store Connect manually.

## Rollback

The two code commits only touch `ios/TodayList/Features/Root/RootView.swift`. To undo them:

```bash
git revert ba65753 bc66ae8
```

No persistence, recurrence, reminder, suite-handoff, entitlement, version, or project-configuration changes were made.
