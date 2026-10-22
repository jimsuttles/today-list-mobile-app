# Today List — iPhone Duo Audit Results

**Issue:** [#10](https://github.com/jimsuttles/today-list-mobile-app/issues/10) · **PR:** [#11](https://github.com/jimsuttles/today-list-mobile-app/pull/11) · **Static audit:** [IPHONE_DUO_STATIC_AUDIT_AND_HANDOFF.md](IPHONE_DUO_STATIC_AUDIT_AND_HANDOFF.md)
**Updated:** October 22, 2026 (second pass, per the PR #11 handoff) · **Branch:** `feature/iphone-duo-adaptive-layout`

## Environment

| Item | Value |
| --- | --- |
| Xcode | 27.1 (27A9275). The simulator UI is **Device Hub** (`Xcode.app/Contents/Applications/DeviceHub.app`); there is no `Simulator.app`. |
| SDK | iOS 27.1 / iOS Simulator 27.1 |
| Duo simulator | "iPhone Duo (TodayList audit)", iOS 27.1 runtime (24A94232). That runtime supports **only** iPhone Duo, so other iPhones run iOS 27.0. |
| Other simulators | iPhone 17e and iPhone 17 (iOS 27.0) |
| Apple skills | `app-resizability`, `swiftui-whats-new-27` (`xcrun agent skills export`, Xcode 27.1) |
| Driving | `simctl` and the Claude Code simulator panel (tap, type, swipe, touch path). Fold and unfold are Device Hub only; the owner unfolded the test Duo, and the second pass ran on the unfolded inner display (landscape, 951 × 669 pt). |
| Test data | The second pass did **not** reset the owner's test simulator: builds were installed over the app, and nothing was launched with `-ScreenshotDemo`. |

## Apple `app-resizability` skill

| Prerequisite | Result |
| --- | --- |
| Launch screen | ✅ `INFOPLIST_KEY_UILaunchScreen_Generation: YES` |
| iPad orientations | ✅ All four declared |
| `UIRequiresFullScreen` | ✅ Absent |

The iPhone target is portrait-only, which the skill describes as normal, so it is unchanged. Its five detection tasks (UIScreen.main, interfaceOrientation, scene lifecycle, safe-area insets, userInterfaceIdiom) found **no occurrences** in any target. The only safe-area modifiers are background fills, which the skill says to leave alone. **No resizability code changes are required.**

## Code changes

| Commit | File | Change | Why |
| --- | --- | --- | --- |
| `bc66ae8` | `RootView.swift` | Tab bar Dynamic Type capped at `xxxLarge`, one-line labels, Large Content Viewer per tab | At AX5 the labels hyphenated and the bar covered ¼ (iPhone 17e) to ⅓ (Duo outer) of the screen |
| `ba65753` | `RootView.swift` | `.contentShape(Rectangle())` on each tab | Taps in a tab column but outside its glyphs did nothing |
| `c9b11ed` | `TaskDetailView.swift` | `toolbarMinimizationBehavior(.never, for: .navigationBar)`, gated to iOS 27 | On the Duo, scrolling Task Detail minimized the bar and **Save disappeared** |
| `9f85283` | `capture_duo_outer.sh` | Fail-closed disposable-simulator gate | The script reset app data on any simulator with a matching name |

## Save disappears while scrolling Task Detail (owner report)

| Step | Duo inner, iOS 27.1 | iPhone 17e, iOS 27.0 |
| --- | --- | --- |
| Before: open a task, scroll to Actions / Delete | ❌ The whole navigation bar ("Task" and **Save**) minimizes away; only the back button remains, in the system rail ([before](assets/iphone-duo-audit/duo-inner-task-detail-scrolled-before.png)) | ✅ Bar and Save stay ([screenshot](assets/iphone-duo-audit/iphone17e-task-detail-scrolled.png)) |
| After `c9b11ed`, same steps | ✅ Save stays at top right while scrolled to Delete ([after](assets/iphone-duo-audit/duo-inner-task-detail-scrolled-after.png)) | ✅ Navigation bar region pixel-identical to before |
| Save tapped while scrolled (after) | ✅ Saves and returns to Today | Not repeated |

**API choice:**
- The SDK declares `ToolbarItemPlacement.topBarPinnedTrailing` (iOS 27.0). Apple's `swiftui-whats-new-27` toolbar reference says it only keeps an item **out of the overflow menu**. It does not stop the bar scrolling away, so it was not used.
- The scroll behavior is controlled by `toolbarMinimizationBehavior(_:for:)` with `ToolbarMinimizationBehavior.never` (iOS 27.0, verified in the iOS 27.1 SDK's SwiftUI interface). Apple's skill text calls it `toolbarMinimizeBehavior`, which does not exist in the SDK.
- Save keeps its `.confirmationAction` placement.

**Not verified:** Save with the keyboard open *while scrolled*. The typed text did not reach the Notes field in that run, and scrolling the form dismisses the keyboard. Fold/unfold with Save visible was confirmed by the owner, not by this agent.

## Owner-reported keyboard issue

The software keyboard not appearing was a simulator setting (hardware keyboard simulation). The owner resolved it by re-enabling the software keyboard. **No app change.**

## Native `TabView` evaluation (prototype only, not committed)

A throwaway prototype replaced the custom bar with `TabView(selection:)` and the iOS 18 `Tab` API, built with `IPHONEOS_DEPLOYMENT_TARGET=18.0` on the command line. Evidence is in [`assets/iphone-duo-audit/tabview-prototype/`](assets/iphone-duo-audit/tabview-prototype/).

| Check | Result |
| --- | --- |
| Duo inner (unfolded) | ✅ The system automatically shows a **vertical icon rail at the right edge**, which is the requested right-side vertical tabs. `.automatic` and `.sidebarAdaptable` look identical. Content gets full height. |
| Rail tab switching | ✅ Today → Later |
| Deep link `todaylist://v1/today` | ✅ Routed back to Today through the existing `handle(route)` |
| iPhone 17e | ⚠️ Still bottom tabs, but iOS 27's floating glass bar with bold filled icons; **not** today's flat bar in the app's colors |
| Unselected rail and tab icons | ⚠️ Render solid black instead of the app's outline color; needs tint and symbol-variant work |
| Deployment target | ⚠️ The `Tab` API needs iOS 18; the app targets iOS 17, so it needs a bump (a product decision) or a `.tabItem` fallback |
| Undo banner, Duo folded, Split View, VoiceOver | Not evaluated in the prototype |

**Recommendation:** do not convert in this PR. Native `TabView` is the right long-term direction for the Duo, but it changes the conventional-iPhone presentation and needs a deployment-target decision. Track it as a follow-up issue. The verified custom-bar fixes stay.

## Validation results

| Check | Result | Evidence |
| --- | --- | --- |
| Inner display rendering, all four tabs | ✅ | `store/appstore/screenshots/iphone-duo/inner/` |
| Folded outer display, four tabs, task detail, keyboard edit, reorder | ✅ (first pass) | `store/appstore/screenshots/iphone-duo/outer/`, `assets/iphone-duo-audit/duo-outer-*` |
| Undo within 6 s | ✅ "Buy groceries" completed then undone; count restored; no History record left. An earlier attempt timed out because of tool latency (~8 s), not an app fault. | — |
| End My Day | ✅ Sheet fits; Finish disabled until resolved; Close leaves the app's content pixel-identical | [screenshot](assets/iphone-duo-audit/duo-inner-end-my-day.png) |
| Rollover review | ✅ Triggered by setting `last_rollover_date` to yesterday through `cfprefsd`; sheet fits; Done with the defaults kept all tasks on Today and restored the date to today | [screenshot](assets/iphone-duo-audit/duo-inner-rollover-review.png) |
| Large Content Viewer | ✅ At AX5, long-pressing the capped Later tab shows the system's enlarged "Later" HUD | [screenshot](assets/iphone-duo-audit/duo-inner-ax5-large-content-viewer.png) |
| Fold/unfold transitions; Save after fold | ✅ **Owner-verified** in Device Hub; not driven by this agent | PR #11 comments |
| Resize while editing (data preserved across fold) | Not verified by this agent; fold is Device Hub only | — |
| Split View / side-by-side | Not verified; no simulator automation | — |
| VoiceOver | Not verified; no VoiceOver automation in this toolchain. Large Content Viewer is verified. | — |
| Physical iPhone Duo | Not available | — |

**Observations, not changed:**
- **Undo position:** Undo restores a task to the *end* of Today rather than its original position. This is pre-existing and not Duo-specific.
- **Tab bar above the keyboard:** the custom bar sits above the keyboard (~80 pt on the outer display).
- **Later label alignment:** the "Later" label is a few pixels higher than the other tab labels (cosmetic).

**Test-data changes on the owner's test Duo:**
- "Call dentist" was completed by the timed-out Undo attempt, then recreated through History → Recreate on Today.
- Its completion record remains in History, and task order changed.
- `last_rollover_date` was moved back one day and then restored to today by the app.

## Automated tests and builds

All runs used Xcode 27.1, clean DerivedData, unit tests on iPhone 17 (iOS 27.0), and counts from `.xcresult`.

| Run | Tests | Builds |
| --- | --- | --- |
| `main` @ `3af4122` | 45/45 | TodayList, TodayListWidget, TodayListWatchApp, TodayListWatchWidget ✅ |
| `ba65753` (first pass) | 45/45 | Same four, plus the app for the Duo ✅ |
| `9f85283` (second pass, all code changes) | 45/45 | Same four, plus the app for the Duo ✅ |

```bash
xcodebuild test -project ios/TodayList.xcodeproj -scheme TodayList \
  -destination "platform=iOS Simulator,name=iPhone 17,OS=27.0" -derivedDataPath <fresh> \
  -test-timeouts-enabled YES -maximum-test-execution-time-allowance 120
```

`TodayListWatchWidget` accepts only an iOS destination (pre-existing). No unit tests were added: all fixes are view modifiers, verified by before/after evidence on the simulator.

## Screenshots for App Store Connect

Apple's sizes are outer 1398 × 2034 (or 2034 × 1398) and inner 2007 × 2853 (or 2853 × 2007). All captures below are authentic simulator screens, not composited.

- ✅ **Outer:** [`store/appstore/screenshots/iphone-duo/outer/`](../store/appstore/screenshots/iphone-duo/outer/), four tabs at 1398 × 2034, from demo data.
- ✅ **Inner:** [`store/appstore/screenshots/iphone-duo/inner/`](../store/appstore/screenshots/iphone-duo/inner/), four tabs at **2853 × 2007 (landscape)**. They were captured from the owner's test data without a reset; the content matches the demo set. Portrait inner (2007 × 2853) needs a rotation in Device Hub.
- The owner uploads to App Store Connect manually.

**Regenerating the outer set safely:**

```bash
store/appstore/screenshots/iphone-duo/capture_duo_outer.sh --create
store/appstore/screenshots/iphone-duo/capture_duo_outer.sh --udid <UDID> --reset-app-data <TodayList.app>
```

The script refuses to run:
- without `--reset-app-data`;
- on any simulator it did not create (it checks a marker file in that device's directory, plus the exact name and device type).

Verified refusals: no arguments; the owner's test Duo with and without the flag; an unknown UDID; and a disposable device renamed after creation. The happy path on a fresh disposable device regenerated `duo-outer-1-today.png` byte-identically. That device was then deleted.

## Rollback

Revert the code commits newest first:

```bash
git revert 9f85283 c9b11ed ba65753 bc66ae8
```

The code changes are limited to view modifiers in `RootView.swift` and `TaskDetailView.swift`, plus the screenshot script. No persistence, recurrence, reminder, suite-handoff, entitlement, version, orientation, or project-configuration changes were made.
