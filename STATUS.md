# Android port status

> **Parity tracker:** [`PARITY.md`](PARITY.md)  
> **Last updated:** 2026-09-25

## Summary

| Field | Value |
|-------|-------|
| **Completion (lean 1.0 intent)** | ~95% product · **1.1 Analytics allowlist synced** |
| **Version** | 1.0.0 (1) |
| **applicationId** | `com.jacobrozell.puzzlebuddy` |
| **minSdk / targetSdk** | 28 / 36 |
| **Stack** | Kotlin · Compose · Hilt · Room · DataStore · CameraX · ML Kit · Firebase |
| **Active branch** | `release/1.0.0` (first Play ship would be 1.0.0) |
| **iOS baseline** | `PuzzleBuddy/release/1.1.0` allowlists · **not shipping Android soon** |

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

1. iOS 1.1 product: On loan, completion-history edit/undo, What's New (deferred — not releasing soon)
2. UI visual polish pass (match iOS `DesignTokens` exactly)
3. Full WCAG TalkBack audit
