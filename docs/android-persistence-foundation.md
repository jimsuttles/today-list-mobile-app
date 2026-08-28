# Today List — Android persistence foundation

Package: `com.fourctech.todaylist`

## Dependencies

- Kotlin 2.1 / AGP 8.8 / compileSdk 36 / minSdk 28
- Jetpack Compose + Material 3
- Room 2.6 + KSP (`exportSchema` → `app/schemas`)
- Hilt 2.54
- DataStore Preferences (theme, rollover, week start, haptics, ads cache, last rollover, notification prompt)
- Coroutines / Flow
- Navigation Compose (Today / Later / History / Settings)
- Test: JUnit, Truth, coroutines-test, Room testing, Robolectric

## Layout

```
TodayListApplication
MainActivity (loads settings → rollover stub → TodayListApp)
core/time/ClockProvider, SystemClockProvider
data/local/{entity,dao,converter,db,prefs}
data/repository/RoomTaskRepository, RoomHistoryRepository, DataStoreSettingsRepository
domain/model/{Task,TaskLocation,RecurrenceRule,AppSettings,CompletionRecord,...}
domain/repository/{TaskRepository,HistoryRepository,SettingsRepository}
domain/rollover/RolloverStub
di/{DatabaseModule,RepositoryModule,CoreModule}
ui/{navigation,theme,today,later,history,settings,quickadd,components}
```

## Migration policy

`TodayListDatabase` version 1 with `exportSchema = true`.  
`DatabaseModule` installs `TodayListMigrations.ALL` and does **not** call `fallbackToDestructiveMigration()`.
