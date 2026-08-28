# Today List

Local-first todo app by 4CTech, LLC. Tasks live in Today, Later, or History.

## Android

Requires JDK 17–21 (Android Studio’s bundled JBR works). Newer JDKs such as 25+ may fail Kotlin script compilation.

```bash
cd android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testDebugUnitTest
./gradlew :app:installDebug
```

Package: `com.fourctech.todaylist`

### Release bundle (Play internal testing)

Configure signing in `android/local.properties` (or env):

```
TL_SIGN_STORE_FILE=../path/to/today-list-upload.jks
TL_SIGN_KEY_ALIAS=...
TL_SIGN_STORE_PASSWORD=...
TL_SIGN_KEY_PASSWORD=...
```

Optional production AdMob IDs: `ADMOB_APP_ID`, `ADMOB_BANNER_UNIT_ID` (debug uses Google sample units).

```bash
cd android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:bundleRelease
```

Store listing draft and Play checklist: [`store/play/listing.md`](store/play/listing.md)  
Accessibility notes: [`docs/accessibility.md`](docs/accessibility.md)
