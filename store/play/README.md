# Play assets — Today List

## Legal pages (publish before Play submission)
Copy markdown from `store/legal/` to the live site:

- Privacy → https://www.4ctech.io/today-list/privacy/
- Terms → https://www.4ctech.io/today-list/terms/

## Screenshots
Phone shots (~1080×2400) live in `store/play/screenshots/`.

```bash
# Emulator or device connected
python3 store/play/screenshots/capture_play_shots.py
```

Expected captures:
1. `01-today.png` — Today with progress
2. `02-later.png` — Later list
3. `03-task-detail.png` — Task detail
4. `05-history.png` — History
5. `06-settings.png` — Settings

Manual (script skips these):
- Rollover review (change device date / leave unfinished overnight)
- Home screen widget
- Feature graphic 1024×500

## Icon / branding
Adaptive icon uses navy background + white checklist bars. Replace
`drawable/ic_launcher_foreground.xml` before production if you want a final mark.

## Signing + AAB
```bash
cd android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export TL_SIGN_STORE_FILE=...
export TL_SIGN_KEY_ALIAS=...
export TL_SIGN_STORE_PASSWORD=...
export TL_SIGN_KEY_PASSWORD=...
# optional production ads:
export ADMOB_APP_ID=...
export ADMOB_BANNER_UNIT_ID=...
./gradlew :app:bundleRelease
```

## Still needs your action
1. Publish privacy + terms URLs
2. Create/upload Play listing assets (icon 512, feature graphic, screenshots)
3. Configure release keystore env vars
4. Create Play Console app + internal testing track
5. Swap test AdMob IDs for production when ready
