# Native Adaptive Tab Navigation — Decision and Evidence

**Issue:** [#12](https://github.com/jimsuttles/today-list-mobile-app/issues/12) · **Branch:** `feature/12-native-adaptive-tabs` · **Base:** `main` @ `277487e` (PR #11)
**Date:** October 23, 2026 · **Status:** implemented as the recommended option, pending owner approval

## Question

Can Today List give the unfolded iPhone Duo its system right-edge vertical tab rail without:
- changing the conventional-iPhone experience, or
- raising the iOS 17 minimum?

## Findings

1. **iOS 17 is enough.** `TabView(selection:)` with `.tabItem` and `.tag` (iOS 17 APIs) produces the same right-edge rail on the unfolded Duo (iOS 27.1) as the iOS 18 `Tab` API prototype from PR #11. The build compiles with `-target arm64-apple-ios17.0-simulator`. **No deployment-target change.**
2. **Native tabs everywhere change the iPhone look.**
   - On iPhone 17e (iOS 27.0) the system shows iOS 27's floating glass bar with **filled black** unselected icons, instead of today's flat bar with muted outline icons in the 4CTech palette.
   - `.environment(\.symbolVariants, .none)` had no effect, because the system tab bar enforces filled symbols.
3. **A hybrid keeps the iPhone unchanged.**
   - One `TabView` serves every width.
   - In **regular** width (unfolded Duo) it shows the system rail.
   - In **compact** width (iPhone, folded Duo, narrow Split View) it hides the system tab bar with `.toolbar(.hidden, for: .tabBar)` (iOS 16+) and shows the existing custom bar.
   - On iPhone 17e the hybrid is **pixel-identical to `main`** across the full screen, at both default text size and AX5.

## Options

| | A. Native everywhere | **B. Hybrid (implemented)** | C. Keep custom bar only |
| --- | --- | --- | --- |
| Duo unfolded | System rail ✅ | System rail ✅ | Bottom bar stretched across ~870 pt |
| Conventional iPhone | iOS 27 glass bar, filled black icons; a visible restyle | **Unchanged** (pixel-identical) | Unchanged |
| Brand palette | Lost on the tab bar | Kept on iPhone; the rail uses system styling | Kept |
| iOS minimum | 17 | 17 | 17 |
| Large text / Large Content Viewer | System-provided | Custom bar keeps PR #11 behavior; rail is system-provided | PR #11 behavior |
| Code | Smallest | +~80 lines in `RootView.swift` | None |
| Risk | Product restyle without approval | Two chrome paths to maintain; rail glyphs are system black | Duo stays un-adapted |

**Recommendation: B.** It delivers the Duo rail and changes nothing on conventional iPhones, which matches the issue's constraint. A remains available later as a deliberate restyle, if the brand moves to the iOS 27 tab bar.

## Implementation (`RootView.swift`)

- **`ce34870`, native spike (option A):** `TabView(selection: $tab)` with `.tabItem` and `.tag` per tab.
- **`1befc60`, hybrid (option B):**
  - `tabContent(_:)` wraps each tab with a single bottom `safeAreaInset` holding the undo banner and, in compact width, the custom bar.
  - The system bar is hidden in compact width.
  - A first attempt with two nested insets hid the undo banner, so the two must stay in one inset.
- **Untouched:** route handling (`handle(_:)` still sets `tab`), rollover sheet, alerts, Waiting For prompt, scene-phase work, PR #11's tab hit-area and Dynamic Type fixes (moved verbatim into `customTab`), Task Detail's Save fix, persistence, and suite handoffs.

## Evidence

Screenshots are in [`assets/native-adaptive-tabs/`](assets/native-adaptive-tabs/). The Duo is the owner's test simulator (unfolded, iOS 27.1); app data was preserved and never seeded.

| Check | Device | Result |
| --- | --- | --- |
| Right-edge rail, iOS 17 API | Duo inner | ✅ `duo-inner-hybrid-rail.png` |
| Rail tab switching (Today, Later, History) | Duo inner | ✅ |
| Rail stays available in a pushed view (Task Detail, History Details) | Duo inner | ✅ |
| Deep link `todaylist://v1/later` | Duo inner | ✅ Switched to Later (`duo-inner-hybrid-deeplink-later.png`) |
| Task Detail Save visible while scrolled (PR #11 fix) | Duo inner | ✅ `duo-inner-hybrid-task-detail-save.png` |
| End My Day opens and closes | Duo inner | ✅ |
| Rollover review (real new-day prompt) | Duo inner and iPhone 17e | ✅ Presented; Done kept tasks on Today |
| Undo banner shows | Duo inner | ✅ but **overlaps the rail** (see Limitations) |
| Undo banner shows above the custom bar | iPhone 17e | ✅ `iphone17e-hybrid-undo-banner.png`; Undo restored the task |
| Custom bar tab switching | iPhone 17e | ✅ |
| Conventional iPhone unchanged | iPhone 17e | ✅ Full-screen pixel-identical to `main` at default and AX5 |
| Native-everywhere comparison | iPhone 17e | `iphone17e-native-everywhere.png` vs `iphone17e-main.png` |

## Automated tests and builds (`1befc60`, Xcode 27.1, clean DerivedData)

| Command / target | Result |
| --- | --- |
| `xcodebuild test -scheme TodayList -destination "platform=iOS Simulator,name=iPhone 17,OS=27.0"` | **45/45 passed** (`.xcresult`); compiled `-target arm64-apple-ios17.0-simulator` |
| `TodayListWidget` (generic iOS Simulator) | BUILD SUCCEEDED |
| `TodayListWatchApp` (generic watchOS Simulator) | BUILD SUCCEEDED |
| `TodayList` for the iPhone Duo destination | BUILD SUCCEEDED |
| `TodayListWatchWidget` **scheme** | ⚠️ `xcodebuild` exit 70, "Unable to find a destination", with both iOS and watchOS generic destinations. The scheme mixes platforms (pre-existing; it passed with an iOS destination on PR #11). No project files changed on this branch. |
| `TodayListWatchWidget` **target** (`-target … -sdk watchsimulator`) | BUILD SUCCEEDED |

## Limitations and open items

- **Undo banner over the rail:** in regular width the banner spans the full width and the floating rail draws over its right end (the Settings glyph sits on the banner). Undo and close stay to the left of the rail. This needs a design decision: end the banner before the rail, or move it to the top.
- **Rail styling:** unselected rail glyphs render solid black, the system default; the selected item uses the app tint. Not tuned.
- **Folded Duo and fold/unfold transitions:** not driven by the agent, because folding is Device Hub only. The hybrid keeps one `TabView` so state should survive a fold, but this is **unverified**. Owner check requested.
- **VoiceOver and Split View:** deferred by the owner. **Not verified**, not passing.
- **Undo timing:** simulated taps are slow, and some Undo attempts missed the 6 s window on both `main` and the branch. One successful hybrid Undo is recorded per device.

## Pre-existing defects found (not caused by #12, not fixed here)

1. **Undo can leave a stale History record.** In `SwiftDataTaskRepository.uncompleteTask`, `modelContext.delete(event)` runs in a `defer` *after* `try modelContext.save()`. The restored task is saved, but the completion-record deletion is not persisted until some later save. Relaunching right after an Undo showed the task on Today **and** still in History. This is a persistence fix, so it belongs in a separate issue.
2. **Rollover review truncates at AX5:** picker values read "Keep…oday" on iPhone 17e, identical on `main` and the branch.
3. **Recreate from History restores the title only:** notes are not restored. This is existing behavior; noted because test-data restores lost notes.

## Test-data changes during validation

Owner's test Duo:
- "Call dentist" was completed by a missed Undo, then recreated from History.
- The real Oct 23 rollover was resolved with the defaults ("Keep on Today").

iPhone 17e:
- Three tasks were completed during Undo testing and recreated from History.
- Their History records remain, and "Buy groceries" and "Review budget" lost their notes (see defect 3).
- The rollover was resolved with the defaults.

## Rollback

`git revert 1befc60 ce34870` restores the custom-bar-only `RootView` from `main`.
