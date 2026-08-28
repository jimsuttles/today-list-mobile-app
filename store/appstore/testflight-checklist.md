# TestFlight checklist — Today List (iOS)

Team: **4CTech LLC** (`X6T2496428`)  
Bundle ID: `com.fourctech.todaylist`  
Widget: `com.fourctech.todaylist.widget`  
App Group: `group.com.fourctech.todaylist`

## 1. Apple Developer portal

- [ ] App ID `com.fourctech.todaylist` with App Groups + Push Notifications (optional) + Associated Domains (if needed)
- [ ] App ID `com.fourctech.todaylist.widget` with App Groups
- [ ] App Group `group.com.fourctech.todaylist` enabled on both

## 2. App Store Connect — create app

- [ ] My Apps → **+** → New App
- [ ] Platforms: iOS
- [ ] Name: Today List
- [ ] Primary language: English (U.S.)
- [ ] Bundle ID: `com.fourctech.todaylist` (must exist in portal first)
- [ ] SKU: `todaylist-ios` (or similar)
- [ ] User Access: Full Access

## 3. In-App Purchase

- [ ] Features → In-App Purchases → **+**
- [ ] Type: Non-Consumable
- [ ] Product ID: `com.fourctech.todaylist.removeads`
- [ ] Reference name: Remove Ads
- [ ] Price tier + localized display name / description
- [ ] Submit with first binary (or Cleared for Sale when ready)

## 4. Xcode archive

```bash
cd ios
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
open TodayList.xcodeproj
```

- [ ] Both targets show Team **4CTech LLC**
- [ ] Product → Archive
- [ ] Distribute App → App Store Connect → Upload
- [ ] Wait for processing → add to TestFlight internal group

## 5. Listing metadata (can draft anytime)

Copy from [`listing.md`](listing.md): description, subtitle, keywords, privacy URL, support URL.

## 6. App Privacy

Declare: purchase history (IAP), device IDs / advertising (if AdMob live), crash/diagnostics if you add Firebase later.
