# Today List

Local-first todo app by 4CTech, LLC. Tasks live in Today, Later, or History.

## Android

Requires JDK 17–21 (Android Studio’s bundled JBR works). Newer JDKs such as 25+ may fail Kotlin script compilation.

```bash
cd android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testDebugUnitTest
```

Package: `com.fourctech.todaylist`
