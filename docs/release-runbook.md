# Puzzle Buddy Android 1.1.0 release runbook

**Version:** 1.1.0 (`versionCode` 2)
**Application ID:** `com.jacobrozell.puzzlebuddy`
**Ship from:** `main`
**Leave untouched:** `release/1.0.0` (parked 1.0 catalog, not this upload)
**Last updated:** 2026-11-09

This is the first Play release. It matches iOS 1.1.0: local collection, no account, no import/export screen. Write a Review stays hidden until the listing is live.

Do not commit `app/google-services.json` or a keystore.

---

## 1. Publish the privacy page first

Play and the in-app Privacy row both open `https://jacobrozell.github.io/PuzzleBuddy/privacy.html`. That file lives in the iOS repo, `docs/privacy.html`, and GitHub Pages serves `/docs` from `main`.

1. Merge the privacy change (device backup, camera frames, no advertising ID) into PuzzleBuddy `main`.
2. Wait a few minutes, then open the live privacy and support URLs.
3. Confirm the privacy page says **November 2026** and describes the phone's own encrypted backup (iCloud or a computer backup on iPhone, Google backup on Android).
4. Confirm support no longer says the collection is "on device only" with no backup caveat.

The collection stays in Android Auto Backup on purpose. A new phone can restore puzzles, photos, and borrower names. Puzzle Buddy cannot read that backup. Delete All Data removes the on-device copy; an older backup can still restore it. That is what the privacy page now says.

---

## 2. Pass on your Samsung before you upload

Install a release build, not a debug build. Debug turns remote Analytics and Crashlytics off.

| Check | Pass looks like |
|-------|-----------------|
| First launch | Onboarding is an opaque page, not a wash over the puzzle list. Skip and finish both leave the list usable. |
| Home, portrait | Filters, search, empty state, and Add puzzle are fully on screen. |
| Home, landscape | Add puzzle is fully visible. A scrollbar appears only when the list is taller than the screen. The side navigation clears the hole-punch and the gesture bar. |
| Split screen or a short window | Filters scroll away with the list. What's New, Pick next, and a completion edit scroll. The last form field can move above the keyboard. |
| Camera allowed | Scan from the list, from shopping mode, and from the barcode field on the form. A known barcode opens the match. An unknown one offers add. |
| Camera denied | A dialog says the camera is only used to read a barcode, with **Open Settings** and **Not now**. Open Settings lands on this app's system page. |
| Photos | Add from the gallery and from the camera. Reorder so the first photo is the cover. The photo appears on the detail screen. |
| TalkBack | List row, star rating (the value is spoken), timer (the elapsed time is spoken), and Save. Onboarding is one flow, not the list underneath. |
| Font size 150% and dark mode | Settings rows and the list still fit. Status filters become a menu at large text. |
| Collection | Load Demo Data (13 puzzles). Load it again and the app refuses the duplicate shelf. Delete All Data empties the list. Start, pause, and reset a timer. Puzzle again and undo a completion. |

A tablet or fold pass is the same landscape and short-window checks, plus opening a puzzle beside the list.

These are already known and match iOS. They are not reasons to hold the upload:

- White text on the brand teal is about 4.0:1.
- Saving an edit that was opened before a timer started can stop that timer.
- There is no import/export screen and no friends list.

---

## 3. Firebase and the release bundle

1. Firebase Console → project **puzzle-buddy** → add Android app `com.jacobrozell.puzzlebuddy` if it is not there.
2. Download `google-services.json` into `app/google-services.json`. Confirm `mobilesdk_app_id` does not contain `REPLACE_WITH`. The example file leaves Analytics and Crashlytics off.
3. Firebase Console → Crashlytics → enable Crashlytics.
4. Build a signed app bundle. This repo has no signing config. In Android Studio: **Build → Generate Signed App Bundle**, with Play App Signing. `versionName` is `1.1.0`, `versionCode` is `2`. Minify is off, so there is no mapping file to upload.

```bash
cp /path/to/real/google-services.json app/google-services.json
./gradlew bundleRelease
```

`bundleRelease` produces an unsigned bundle unless a signing config is added locally. Prefer Android Studio's signed-bundle flow so the upload key stays out of the repo.

After upload, open the bundle in Play Console and confirm there is no 16 KB page-size warning. CameraX 1.4.2 and AGP 8.9 are new enough to pass. If Play warns, bump CameraX and ML Kit barcode before production.

The manifest removes `com.google.android.gms.permission.AD_ID` and sets `google_analytics_adid_collection_enabled` to false. The app does not use the advertising ID.

---

## 4. Play Console

Create the app with the application ID above. Start on the **internal testing** track. Move to production only after the Samsung pass on that same bundle.

**Store listing**

| Field | Value |
|-------|-------|
| Privacy policy | https://jacobrozell.github.io/PuzzleBuddy/privacy.html |
| Category | Lifestyle |
| Ads | No |
| In-app purchases | No |
| Account required | No |
| Target age | Not designed for children. Do not opt into Designed for Families. |
| Camera declaration | Used to scan a barcode. Not required to install (`android.hardware.camera` is optional). Frames are not saved or uploaded. |

Use phone screenshots of the list, a puzzle detail (timer or history), stats, and settings. A landscape shot is worth including because that layout is easy to get wrong.

Short description, under 80 characters: `Track your jigsaw collection, timers, and finishes.`

**Data safety.** Google does not treat Android backup as data the developer collects, because Puzzle Buddy cannot read it. Declare only what the app sends.

| Data type | Collected | Shared | Purpose |
|-----------|-----------|--------|---------|
| App interactions | Yes | No | Analytics |
| Crash logs | Yes | No | Analytics, app functionality |
| Device or other IDs (Firebase app instance ID) | Yes | No | Analytics |
| Photos, names, puzzle titles, barcode digits, advertising ID | No | No | — |

- Encrypted in transit: **Yes** (Firebase uses HTTPS).
- Users can request deletion: **Yes.** Settings → Collection → Delete All Data removes the collection, photos, completions, and borrower names. Analytics events have no titles, photos, or names. Deletion requests can also go to jacob.rozell83@gmail.com.
- Data sold: **No.** Used for advertising: **No.**

**Content rating:** no violence, no user-generated public sharing, no unrestricted web view. The share action builds a collage on the device and hands it to the system share sheet.

**Release notes** for 1.1.0:

```text
Track wishlist, to-do, in-progress, finished, and abandoned puzzles.
Log finishes, time a puzzle, and pick what to build next.
Scan a barcode to see if you already own that box. Your collection stays on this device.
```

Leave **Write a Review** hidden for this upload. `ProductSurface.isPlayListingLive` is false so Settings does not open a store page that does not exist yet. The in-app review prompt can stay; Play will not show it until the app is installed from Play.

---

## 5. After the listing is live

Ship a small follow-up, `versionCode` 3, that sets `isPlayListingLive` to true. That shows Settings → Write a Review, which opens the Play listing. Do that only after the public page exists.

Watch Crashlytics for the first internal-test day, especially camera bind failures and database open failures. A failed open resets the collection and shows the store-reset notice. Nothing has shipped before this version, so there is no v1 upgrade to preserve.
