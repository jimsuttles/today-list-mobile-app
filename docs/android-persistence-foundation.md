# Today List — Android persistence foundation

Package: `com.fourctech.todaylist`

## Dependencies

- Kotlin 2.1 / AGP 8.8 / compileSdk 36 / minSdk 28
- Jetpack Compose + Material 3 (shell only)
- Room 2.6 + KSP (`exportSchema` → `app/schemas`)
- Hilt 2.54
- DataStore Preferences (dependency present for settings later)
- Coroutines / Flow
- Navigation Compose (dependency present; routes not wired yet)
- Test: JUnit, Truth, coroutines-test, Room testing, Robolectric

## Layout

```
TodayListApplication
MainActivity (placeholder Text only)
core/time/ClockProvider, SystemClockProvider
data/local/{entity,dao,converter,db}
data/repository/RoomTaskRepository, TaskMappers
domain/model/{Task,TaskLocation,RecurrenceRule,RecurrenceType}
domain/repository/TaskRepository
di/{DatabaseModule,RepositoryModule}
```

## Migration policy

`TodayListDatabase` version 1 with `exportSchema = true`.  
`DatabaseModule` installs `TodayListMigrations.ALL` and does **not** call `fallbackToDestructiveMigration()`.
