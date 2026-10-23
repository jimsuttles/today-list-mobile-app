# Native Adaptive Tab Navigation — Decision and Evidence

**Issue:** [#12](https://github.com/jimsuttles/today-list-mobile-app/issues/12) · **Branch:** `feature/12-native-adaptive-tabs` · **Base:** `main` @ `277487e` (PR #11)
**Date:** October 23, 2026 · **Status:** option B approved by ChatGPT review; review fix applied; owner fold/unfold check pending

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
- **`1befc60`, hybrid (option B):** regular width gets the system rail; compact width hides the system bar and shows the existing custom bar.
- **`db87c36`, review fix:** the root is a `VStack`, as on `main`: the `TabView`, then **one** undo banner, then the custom bar in compact width.
  - The per-tab bottom inset from `1befc60` had two defects. **After any tab switch, completing a task showed no undo banner**, on both iPhone 17e and the unfolded Duo, so Undo was unreachable. The banner also sat under the rail.
  - Removing the banner's transition did not fix the first; moving the banner out of the tab pages did.
  - In regular width the `TabView`, and with it the rail, now ends above the banner.
- **`469231d`:** `UndoBanner`'s fill now ignores only the bottom safe-area edge. The default (`.all`) bled into the Duo's trailing rail/status column. No change on portrait iPhone.
- **Untouched:** route handling (`handle(_:)` still sets `tab`), rollover sheet, alerts, Waiting For prompt, scene-phase work, PR #11's tab hit-area and Dynamic Type fixes (moved verbatim into `customTab`), Task Detail's Save fix, persistence, and suite handoffs.

## Evidence

Screenshots are in [`assets/native-adaptive-tabs/`](assets/native-adaptive-tabs/). The Duo is the owner's test simulator (unfolded, iOS 27.1); app data was preserved and never seeded.

| Check | Device | Result |
| --- | --- | --- |
| Right-edge rail, iOS 17 API | Duo inner | ✅ `duo-inner-hybrid-rail.png`; the rail's position is pixel-identical between `1befc60` and `469231d` when no banner shows |
| Rail tab switching (Today, Later, History) | Duo inner | ✅ |
| Rail stays available in a pushed view (Task Detail, History Details) | Duo inner | ✅ |
| Deep link `todaylist://v1/later` | Duo inner | ✅ (`duo-inner-hybrid-deeplink-later.png`) |
| Task Detail Save visible while scrolled (PR #11 fix) | Duo inner | ✅ `duo-inner-hybrid-task-detail-save.png` |
| End My Day opens and closes | Duo inner | ✅ |
| Rollover review (real new-day prompt) | Duo inner and iPhone 17e | ✅ Done kept tasks on Today |
| **Banner vs. rail** | Duo inner | ❌ before (`duo-inner-banner-overlaps-rail-before.png`, `1befc60`) → ✅ after: banner ends at 866 pt and the rail sits above it (`duo-inner-banner-below-rail-after.png`) |
| **Banner after a tab switch** | Duo inner and iPhone 17e | ❌ before: no banner (`duo-inner-no-banner-after-tab-switch-before.png`, `iphone17e-no-banner-after-tab-switch-before.png`) → ✅ after: banner seen within 0.5 s (Duo) and 1.5 s (iPhone) of completing, on Later and Today |
| **Undo works** | Duo inner (rail mode, Later tab) | ✅ "Plan weekend trip" completed, banner shown, Undo restored it (`duo-inner-banner-below-rail-after.png` → `duo-inner-undo-restored-after.png`) |
| **Undo works** | iPhone 17e (after a tab switch, Later tab) | ✅ "Plan weekend trip" restored with notes (`iphone17e-later-before-undo.png` → `iphone17e-later-undo-restored-after.png`) |
| Conventional iPhone unchanged (`469231d` vs `main`) | iPhone 17e | ✅ Default size **pixel-identical**. At AX5 the only differing pixels are inside the system glass toolbar capsule (End My Day/+), which looks identical; this is material-rendering variance between captures. |
| Native-everywhere comparison | iPhone 17e | `iphone17e-native-everywhere.png` vs `iphone17e-main.png` |

## Automated tests and builds (`469231d`, Xcode 27.1, clean DerivedData)

| Command / target | Result |
| --- | --- |
| `xcodebuild test -scheme TodayList -destination "platform=iOS Simulator,name=iPhone 17,OS=27.0"` | **45/45 passed** (`.xcresult`); compiled `-target arm64-apple-ios17.0-simulator` |
| `TodayListWidget` (generic iOS Simulator) | BUILD SUCCEEDED |
| `TodayListWatchApp` (generic watchOS Simulator) | BUILD SUCCEEDED |
| `TodayList` for the iPhone Duo destination | BUILD SUCCEEDED |
| `TodayListWatchWidget` **target** (`-target … -sdk watchsimulator`) | BUILD SUCCEEDED. The **scheme** still fails destination resolution (pre-existing mixed-platform scheme; no project files changed). |

## Limitations and open items

- **Rail styling:** unselected rail glyphs render solid black, the system default; the selected item uses the app tint. Not tuned.
- **Folded Duo and fold/unfold transitions:** folding is Device Hub only, so this is an owner check. Open Task Detail, fold and unfold, and confirm the tab and navigation stack persist, the folded outer shows the custom bar, the unfolded inner shows the rail, and Save stays reachable. **Not verified by the agent.**
- **VoiceOver and Split View:** deferred by the owner. **Not verified**, not passing.
- **Simulator input:** taps sometimes take several seconds to land, and a few did not register. Several Undo attempts missed the 6 s window; the verified Undo runs are listed above.

## Pre-existing defects and corrections

1. **Undo/History persistence ([#14](https://github.com/jimsuttles/today-list-mobile-app/issues/14)):** `uncompleteTask` deletes the completion record in a `defer` that runs after `try modelContext.save()`.
   - **Correction:** the earlier "reproduction" on iPhone 17e used the demo's **"Review budget", which repeats on weekdays**. Completing it keeps the task on Today and adds a History record, which alone explains "on Today and in History".
   - The code-level concern stands, but it is **not confirmed by reproduction**. That earlier run also does not count as Undo evidence.
   - Not touched in this PR, per review.
2. **Rollover review truncates at AX5:** picker values read "Keep…oday" on iPhone 17e, identical on `main` and the branch.
3. **Recreate from History restores the title only:** notes are not restored. This is existing behavior; noted because test-data restores lost notes.
4. **Undo restores a task to the end of its list,** not its original position (noted in PR #11).

## Test-data changes during validation

All tasks completed during testing were restored with History → Recreate, in their original list.
- **Remaining effects:** extra History records; restored copies lose notes ("Buy groceries", and "Review budget" on iPhone 17e); task order changed.
- **Owner's test Duo:** the real Oct 23 rollover was resolved with the defaults ("Keep on Today").

## Rollback

`git revert 469231d db87c36 1befc60 ce34870` restores the custom-bar-only `RootView` and `UndoBanner` from `main`.
