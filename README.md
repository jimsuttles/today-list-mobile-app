# Today List

A local-first task app by **4CTech, LLC**, organized around three places: **Today**, **Later**, and **History**.

This repository contains native iOS and Android implementations. It demonstrates task persistence, recurring work, daily rollover, reminders, widgets, and platform integrations. The iOS implementation also includes an Apple Watch companion, App Intents, and handoffs to other productivity-suite apps.

## Engineering overview

| Area | iOS | Android |
| --- | --- | --- |
| UI | SwiftUI | Jetpack Compose / Material 3 |
| Persistence | SwiftData | Room |
| Settings | UserDefaults | DataStore |
| Composition | AppEnvironment and repository protocols | Hilt and repository interfaces |
| Widgets | WidgetKit | Glance |
| Reminders | UserNotifications | AlarmManager and notification channels |
| Suite integration | ProductivitySuiteCore and App Group file handoffs | No equivalent suite integration in this tree |
| Additional integrations | App Intents, deep links, WatchConnectivity | Task deep links |

The domain layer separates recurrence and rollover rules from UI and persistence. Repository abstractions keep data access behind explicit interfaces. Completion history records task snapshots, and recurring tasks use dated occurrences.

The platforms are at different stages: Android still contains AdMob and Play Billing integrations; the current iOS source does not contain the earlier ads or Remove Ads implementation. Older planning and store drafts may describe earlier product behavior.

## Where to start reviewing

- [iOS composition root](ios/TodayList/App/AppEnvironment.swift): dependencies and application behavior.
- [iOS task repository](ios/TodayList/Data/Repositories/SwiftDataTaskRepository.swift): persistence and task lifecycle.
- [Suite handoff adapter](ios/TodayList/Integrations/SuiteHandoff/SuiteHandoffStore.swift): shared payload validation and transport integration.
- [iOS handoff tests](ios/TodayListTests/SuiteHandoffSourceTests.swift): source-side handoff scenarios.
- [Android domain layer](android/app/src/main/java/com/fourctech/todaylist/domain): recurrence, rollover, repository contracts, and use cases.
- [Android repository tests](android/app/src/test/java/com/fourctech/todaylist/data/repository/RoomTaskRepositoryTest.kt): persistence behavior.

The shared transport is maintained in [Productivity Suite Core](https://github.com/jimsuttles/productivity-suite-core).

## Screenshots

Existing Android store assets are available in [store/play/screenshots](store/play/screenshots). These are Android captures, not previews of the current iOS interface.

## Run iOS

Requires macOS, Xcode 16 or later with the Swift 6 toolchain for ProductivitySuiteCore, and XcodeGen if regenerating the project. Deployment targets are iOS 17 and watchOS 10.

```sh
cd ios
xcodegen generate
open TodayList.xcodeproj
```

The generated project is also committed. [project.yml](ios/project.yml) is the project-generation configuration. Xcode resolves ProductivitySuiteCore from GitHub on `main`; access to that repository is required while it is private.

To run tests, list available simulators and substitute an installed iPhone simulator name:

```sh
xcrun simctl list devices available
xcodebuild -project TodayList.xcodeproj \
  -scheme TodayList \
  -destination 'platform=iOS Simulator,name=YOUR_INSTALLED_IPHONE_SIMULATOR' \
  test
```

For physical devices, configure your own signing team, bundle identifiers, and App Groups across the app, widget, and watch targets. Cross-app handoffs require matching suite App Group entitlements in participating apps; editing a signing team alone does not provision those groups.

## Run Android

Requires Android Studio / Android SDK 36 and JDK 17–21. Java and Kotlin compilation target JVM 17. Use Android Studio's compatible bundled JDK if your shell defaults to a newer unsupported version.

From the repository root:

```sh
cd android
./gradlew :app:testDebugUnitTest
./gradlew :app:installDebug
```

Install requires a connected device or running emulator (Android 9 / API 28 or later). Configure the SDK location through Android Studio or your local environment.

Debug builds use Google sample AdMob identifiers. Firebase plugins are applied only when a local `android/app/google-services.json` is present.

For a signed release, provide `TL_SIGN_STORE_FILE`, `TL_SIGN_KEY_ALIAS`, `TL_SIGN_STORE_PASSWORD`, and `TL_SIGN_KEY_PASSWORD` through environment variables or ignored `android/local.properties`, then run:

```sh
./gradlew :app:bundleRelease
```

Release AdMob identifiers are supplied through `ADMOB_APP_ID` and `ADMOB_BANNER_UNIT_ID`. Keep signing files, passwords, and private service configuration out of commits.

## Tests and documentation

The repository includes iOS tests for recurrence, rollover, task persistence, settings, routes, and suite handoffs; Android tests cover recurrence, rollover, repositories, task use cases, and analytics privacy behavior. Runtime and device validation remain separate from unit tests.

- [iOS test sources](ios/TodayListTests)
- [Android test sources](android/app/src/test/java/com/fourctech/todaylist)
- [Architecture notes](docs/architecture.md)
- [iOS architecture notes](docs/ios-architecture.md)
- [Android persistence foundation](docs/android-persistence-foundation.md)
- [Accessibility notes](docs/accessibility.md)
- [AI development workflow](AI_DEVELOPMENT_WORKFLOW.md)

Store listings and checklists under `store/` are working documents, not proof of release status. In particular, the App Store draft still includes historical ads/IAP wording; reconcile it with the release being submitted.
