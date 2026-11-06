# Android port status

> **Parity tracker:** [`PARITY.md`](PARITY.md)  
> **Plan:** [`docs/implementation/1.1.0-catch-up.md`](docs/implementation/1.1.0-catch-up.md) (order of work) · [`docs/implementation/1.1.0-ios-parity-plan.md`](docs/implementation/1.1.0-ios-parity-plan.md) (WP detail)  
> **Last updated:** 2026-11-05

## Summary

| Field | Value |
|-------|-------|
| **Completion** | M0–M3 of the catch-up plan are done, through What's New and the Play review prompt. M4 (telemetry audit, version 1.1.0, JSON backup) is next. |
| **Version** | 1.0.0 (1). Plan calls for 1.1.0 (2) after the UI work. |
| **applicationId** | `com.jacobrozell.puzzlebuddy` |
| **minSdk / targetSdk** | 28 / 36 |
| **Stack** | Kotlin · Compose · Hilt · Room v2 · DataStore · CameraX · ML Kit · Firebase |
| **Active branch** | `claude/pensive-knuth-liq0g4` |
| **iOS baseline** | `PuzzleBuddy` tag `1.1.0` (on iOS `main`, live 2026-11-02) |

## Verification

```bash
cd PuzzleBuddy-Android
cp app/google-services.json.example app/google-services.json   # CI does this automatically
./gradlew testDebugUnitTest assembleDebug
```

`testDebugUnitTest assembleDebug` passed on 2026-11-05 (253 tests). M2 and M3 screens were checked on the Medium Phone API 36.1 emulator. Not checked: photo picking, camera scanning, tablet, TalkBack, 150% font on the M3 screens, and the Play review dialog.

## Firebase setup

Uses the shared **`puzzle-buddy`** Firebase project (same as iOS).

1. Firebase Console → register Android app (`com.jacobrozell.puzzlebuddy`) if needed.
2. Download `google-services.json` → `app/google-services.json` (gitignored).
3. Analytics and Crashlytics activate when `mobilesdk_app_id` does **not** contain `REPLACE_WITH`.
4. Remote telemetry is **Release-only** by default (matches iOS Debug builds).
5. Enable Crashlytics in Firebase Console → Build → Crashlytics.

CI checks the allowlist against iOS tag `1.1.0`. Bump the `ref` in `.github/workflows/ci.yml` when Android targets the next iOS tag.

## Top remaining gaps

1. M4: telemetry call-site audit, version 1.1.0 (2), JSON backup parity
2. M5: emulator pass (tablet, foldable, TalkBack, large text, camera, photo picking)
