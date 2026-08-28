# Play assets — Today List

## Listing assets (ready)
| Asset | Path | Size |
|-------|------|------|
| App icon | `store/play/icon-512.png` | 512×512 |
| Feature graphic | `store/play/feature-graphic.png` | 1024×500 |
| Phone screenshots | `store/play/screenshots/*.png` | 1080×2400 |
| Signed AAB | `store/play/today-list-1.0.0.aab` | versionCode 1 / 1.0.0 |

Upload screenshots in this order:
1. `01-today.png`
2. `02-later.png`
3. `03-task-detail.png`
4. `05-history.png`
5. `06-settings.png`

## Legal pages (live)
- Privacy → https://www.4ctech.io/today-list/privacy/
- Terms → https://www.4ctech.io/today-list/terms/
- Product → https://www.4ctech.io/today-list/

## Signing (local only — do not commit)
```bash
cd android
./scripts/create-upload-keystore.sh   # once
# Writes TL_SIGN_* into local.properties; keystore under android/keystore/
./gradlew :app:bundleRelease
cp app/build/outputs/bundle/release/app-release.aab ../store/play/today-list-1.0.0.aab
```

**Back up** `android/keystore/today-list-upload.jks` and the `TL_SIGN_*` passwords from `local.properties`. Losing them blocks updates.

Upload key SHA-256 (for Play App signing enrollment / comparison):
`06:90:52:7E:08:77:9E:2A:A5:12:EC:8C:36:EF:DB:E9:F1:99:04:71:7B:6D:70:FC:86:90:E2:48:0F:35:E1:21`

## Screenshots regenerate
```bash
python3 store/play/screenshots/capture_play_shots.py
```

## Still needs Console action
1. Create app `com.fourctech.todaylist` in Play Console (if not already)
2. Main store listing — paste from `listing.md`; upload icon, feature graphic, screenshots
3. Privacy policy URL + Data safety + content rating + ads declaration
4. Internal testing → upload `today-list-1.0.0.aab` → add testers
5. Production AdMob IDs when ready (`ADMOB_APP_ID`, `ADMOB_BANNER_UNIT_ID`)
6. Optional: `google-services.json` for Firebase Analytics/Crashlytics
