# Android port status

> **Parity tracker:** [`PARITY.md`](PARITY.md)  
> **Last updated:** 2026-06-23

## Summary

| Field | Value |
|-------|-------|
| **Completion (lean 1.0 intent)** | ~95% |
| **Version** | 1.0.0 (1) |
| **applicationId** | `com.jacobrozell.puzzlebuddy` |
| **minSdk / targetSdk** | 28 / 36 |
| **Stack** | Kotlin · Compose · Hilt · Room · DataStore · CameraX · ML Kit · Firebase |
| **iOS baseline** | `Puzzle-Buddy/main` · local-first 1.0.0 |

## Verification

```bash
cd PuzzleBuddy-Android
cp app/google-services.json.example app/google-services.json   # CI does this automatically
./gradlew assembleDebug test
```

## Firebase setup

Uses the shared **`puzzle-buddy`** Firebase project (same as iOS).

1. Firebase Console → register Android app (`com.jacobrozell.puzzlebuddy`) if needed.
2. Download `google-services.json` → `app/google-services.json` (gitignored).
3. Analytics and Crashlytics activate when `mobilesdk_app_id` does **not** contain `REPLACE_WITH`.
4. Remote telemetry is **Release-only** by default (matches iOS Debug builds).
5. Enable Crashlytics in Firebase Console → Build → Crashlytics.

## Top remaining gaps

1. UI visual polish pass (match iOS `DesignTokens` exactly)
2. Full WCAG TalkBack audit
