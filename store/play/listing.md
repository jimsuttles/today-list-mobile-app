# Google Play listing — Today List
# Paste into Play Console when the app listing is ready.

## Short description (max 80 characters)
A calm today / later list. Local tasks, reminders, and history.

## Full description
Today List keeps your day simple: what’s for Today, what’s Later, and what’s done.

Add tasks quickly, move them between Today and Later, set optional reminders, and review unfinished work when a new day starts. History stays on your device. No account required.

Features
• Today and Later lists with reorder
• Quick add and task detail (notes, reminder, repeat)
• Daily unfinished-task rollover (ask or auto)
• Local completion history
• Home screen Today widget
• Optional one-time Remove Ads purchase

Your task titles and notes stay on your device. Today List does not require an account.

Published by 4CTech, LLC.

## App category
Productivity

## Tags / notes for Play
Free with ads · Contains ads · In-app purchases

## Privacy policy URL
https://www.4ctech.io/today-list/privacy/  
(Draft ready to publish: `store/legal/privacy.md`)

## Terms URL
https://www.4ctech.io/today-list/terms/  
(Draft ready to publish: `store/legal/terms.md`)

## Contact email
jim@4ctech.io

## Screenshot plan (phone, ~1080×2400)
Upload in this order when assets are ready:
1. Today list with a few tasks + progress
2. Later list
3. Task detail (reminder / repeat)
4. Rollover review
5. History
6. Settings (appearance / unfinished mode)
7. Home screen widget (optional)

## Release artifact
Build a signed AAB when keystore env is configured:

```bash
cd android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:bundleRelease
```

Signing keys (local.properties or env):
- `TL_SIGN_STORE_FILE`
- `TL_SIGN_KEY_ALIAS`
- `TL_SIGN_STORE_PASSWORD`
- `TL_SIGN_KEY_PASSWORD`

Production AdMob (optional until live units exist):
- `ADMOB_APP_ID`
- `ADMOB_BANNER_UNIT_ID`

## Play Console checklist (internal testing → production)
1. [ ] Main store listing — short/full description, icon, feature graphic, phone screenshots
2. [ ] App category = Productivity; contact email = jim@4ctech.io
3. [ ] Privacy policy URL set (publish page before submission)
4. [ ] Data safety form (see answers below)
5. [ ] Content rating questionnaire
6. [ ] Target audience / ads declaration (Contains ads = Yes; IAP = Yes)
7. [ ] Create internal testing track → upload AAB → add testers
8. [ ] Promote to production when ready

## Data safety — recommended answers
**Does your app collect or share user data?** Yes (ads/analytics)

| Data type | Collected? | Shared? | Purpose | Required/Optional |
|-----------|------------|---------|---------|-------------------|
| App activity / analytics events | Yes (Firebase Analytics) | Yes (with Google) | Analytics | Optional / app functionality |
| Crash logs | Yes (Crashlytics) | Yes (with Google) | App functionality / stability | Optional |
| Device / advertising IDs | Yes (AdMob / ads) | Yes (with ad partners via Google) | Advertising / analytics | Optional |
| Purchase history | Yes (Play Billing Remove Ads) | Yes (with Google Play) | App functionality | Optional |
| Task titles / notes | **No** (local only — do not declare as collected) | No | — | — |

Also answer:
- Data is encrypted in transit: **Yes**
- Users can request deletion: **Yes** (Settings → Clear History / Delete All App Data; also via privacy policy contact)
- Account required: **No**

## Content rating
Expect a low rating (Everyone / PEGI 3-ish). Declare ads. No violence/gambling/UGC.

## Accessibility checklist (before store screenshots)
1. [ ] TalkBack: complete / open / move task; Quick Add; Settings radios; History row
2. [ ] Font size largest: Today / Later / Settings still usable
3. [ ] Touch targets ≥ 48dp on complete, overflow, FAB, settings rows
4. [ ] Light + dark theme: text readable on backgrounds
5. [ ] Reminder permission / system settings deep link still reachable
