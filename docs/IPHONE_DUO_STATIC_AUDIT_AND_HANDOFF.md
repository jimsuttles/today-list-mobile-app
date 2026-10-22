# Today List — iPhone Duo Static Resizability Audit and Codex Handoff

**Date:** October 22, 2026  
**Status:** Source review complete; device/simulator testing pending  
**Tracking issue:** [#10](https://github.com/jimsuttles/today-list-mobile-app/issues/10)  
**Suite-wide plan:** [iPhone Duo App Store Readiness](https://github.com/jimsuttles/iPhoneProductivitySuite/blob/main/docs/architecture/IPHONE_DUO_APP_STORE_READINESS.md)

## Baseline and limitations

This audit examines the committed `main` sources. No Xcode simulator, physical iPhone Duo, build, or automated test was run from ChatGPT. Therefore the findings below are **code-level risks**, not observed simulator defects. Codex/Claude should validate before changing UI.

## Confirmed project configuration

From `ios/project.yml`:
- Minimum iOS version: **17.0**; Watch minimum: **10.0**.
- `INFOPLIST_KEY_UISupportedInterfaceOrientations_iPhone: UIInterfaceOrientationPortrait` (portrait-only).
- iPad orientations include portrait, upside-down, and both landscapes.
- Swift package `ProductivitySuiteCore` tracks its `main` branch; preserve compatibility and lock resolved version for reproducible release testing where appropriate.
- Committed `MARKETING_VERSION: 1.1`, `CURRENT_PROJECT_VERSION: 12`. **Do not infer current App Store version from these settings** or bump versions during an audit.
- App, widget, Watch app, Watch widget, and test targets are declared. Signatures and capabilities must be validated across relevant targets if project generation changes.

## Static screen audit

| File | Finding | Risk | Verification/action |
|---|---|---|---|
| `Features/Root/RootView.swift` | A `VStack` renders one selected screen plus a **custom four-button bottom HStack** with 8-point top/bottom padding. | **High:** System's Duo vertical tab/sidebar affordance will not automatically apply; limited-width labels and accessible hit targets may suffer. | Compare baseline on narrow Split View and unfolded inner display. Consider a native `TabView` conversion only if it passes conventional iPhone regression and scene restoration checks. |
| `Features/Today/TodayView.swift` | `NavigationStack` and `List` with task `onMove`, forced edit mode active, trailing toolbar actions. | **Medium:** reorder gesture, crowded trailing bar, and two interactive row buttons on a narrow pane. | Test task reorder, scroll, quick-add and End My Day at minimum and maximum sizes. |
| `Features/Components/TaskRowView.swift` | Two separate buttons (complete/open) inside an HStack; text constrained with line limits. | **Medium:** cramped targets at narrow width; any later `.draggable` risks gesture conflicts. | Measure targets/accessibility; do not add drag behavior as part of badge sprint. |
| `Features/TaskDetail/TaskDetailView.swift` | `Form` with multiline notes, segmented picker, date picker, recurrence, and suite handoffs. | **Medium:** keyboard and resize during unsaved editing could impact state; view has local `@State` text copies. | While typing, resize/fold/unfold and navigate; ensure pending edits persist; test handoff action separately. |
| `Features/Later/LaterView.swift`, `History/HistoryView.swift`, `QuickAdd/QuickAddView.swift`, `Settings/SettingsView.swift` | Standard `NavigationStack`, lists/forms and toolbars; no obvious main-screen-size dependency in inspected UI lines. | **Low–Medium:** still susceptible to compact toolbar and sheet behavior. | Smoke test all routes and dynamic resizing. |
| `Features/EndMyDay/EndMyDayView.swift`, `Rollover/RolloverReviewView.swift` | Navigation-based review flows with confirmation toolbars and grouped content. | **Medium:** stateful review/confirmation during geometry changes. | Test both with real data without accidental completion/move. |

The inspected seven primary view files and additional feature screens did not show obvious `UIScreen.main.bounds` dependencies. **Do not treat this as a repository-wide absence** until the full iOS target and dependencies have been scanned with the Apple resizability skill.

## Official Apple reference and screenshot specifications

- [Apple — Prepare for iPhone Duo](https://developer.apple.com/iphone-duo/prepare/): Recompile with Xcode 27.1; review fixed orientation, `UIScreen.main`, hardcoded sizes, and UI idiom assumptions. Verify fold, unfold, Split View, and app resizing.
- [Apple — Screenshot specifications](https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications): outer display **1398 × 2034** or **2034 × 1398**; inner display **2007 × 2853** or **2853 × 2007** pixels.
- [Apple Developer News](https://developer.apple.com/news/?id=kkphp5qo): Submitting Duo screenshots in App Store Connect triggers badge display. The owner performs submissions manually.

## Exact Codex / Claude work order

> You are implementing Today List iPhone Duo **adaptive-layout readiness** only. Begin by checking out the latest `main` and creating a feature branch named `feature/iphone-duo-adaptive-layout`; do not commit changes to `main` or open a merge request until you have baseline evidence. Use the full specifications in [Issue #10](https://github.com/jimsuttles/today-list-mobile-app/issues/10).
>
> 1. Report your Xcode version, SDK, supported Duo simulator/device, and current branch. If Xcode 27.1 or the Duo simulator is unavailable, document that blocker and do a source-only audit; do **not** claim device validation.
> 2. Run the current iOS test suite using the generated Xcode project. Run `xcrun agent skills export` to locate Apple's app resizability skill or use Xcode's assistant command **“get my app ready for iPhone Duo.”** Inspect flagged files independently.
> 3. Baseline test unfolded inner view, folded outer view, Split View narrow/wide, and resizing while editing. Save screenshots and defect observations. Confirm how iPhone-only portrait orientation interacts with supported Duo poses before changing it.
> 4. Compare existing custom RootView bottom tabs with native `TabView` in an isolated prototype. **Only** adopt a system tab layout if required and if tab selection, deep links, undo banner, rollover dialogs and appearance remain correct on standard iPhones.
> 5. Fix **reproducible** layout issues only. Keep changes in `ios/project.yml` authoritative if XcodeGen regeneration is required, and check generated `.pbxproj` consistency.
> 6. Preserve task persistence, data migration, reminders, recurrence, App Group entitlements, deep links, all seven suite handoff paths, and Watch/Widget targets. Do not implement cross-app dragging, schema migrations, linked completion, or change versions/build numbers.
> 7. Run and report full automated tests, accessibility checks, and manual pose-change tests. Capture authentic Duo screenshots at Apple's documented sizes.
> 8. Open a PR with the smallest patch, before/after screenshots, exact tests and results, source-only caveats, and rollback notes. Do not merge, submit to TestFlight, or perform App Store Connect operations.

## User acceptance checklist

- [ ] Xcode 27.1 and Duo simulator validation completed and evidenced
- [ ] Existing iPhone navigation and task state preserved
- [ ] Dynamic resizing/folding leaves editing, reminders and rollover reliable
- [ ] No truncation or unusable controls at supported widths
- [ ] Tests pass on existing iPhone as well as Duo
- [ ] Authentic Duo inner/outer screenshot captures prepared
- [ ] PR reviewed and smoke-tested on a physical device where available
- [ ] Version bump and App Store Connect remain manually controlled

**Definition of done:** a reviewable PR with verified Duo behavior and screenshot evidence, without changing application business logic.
