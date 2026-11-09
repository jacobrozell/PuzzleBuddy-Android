# Android port status

> **Parity tracker:** [`PARITY.md`](PARITY.md)  
> **Plan:** [`docs/implementation/1.1.0-catch-up.md`](docs/implementation/1.1.0-catch-up.md) (order of work) · [`docs/implementation/1.1.0-ios-parity-plan.md`](docs/implementation/1.1.0-ios-parity-plan.md) (WP detail)  
> **Last updated:** 2026-11-09  
> **Release steps:** [`docs/release-runbook.md`](docs/release-runbook.md)

## Summary

| Field | Value |
|-------|-------|
| **Completion** | M0–M4 are done: 1.1 UI, telemetry call sites, version 1.1.0 (2), and the JSON backup format. M5 is the emulator pass and the final report. |
| **Version** | 1.1.0 (2) |
| **applicationId** | `com.jacobrozell.puzzlebuddy` |
| **minSdk / targetSdk** | 28 / 36 |
| **Stack** | Kotlin · Compose · Hilt · Room v2 · DataStore · CameraX · ML Kit · Firebase |
| **Active branch** | `main` |
| **iOS baseline** | `PuzzleBuddy` tag `1.1.0` (on iOS `main`, live 2026-11-02) |

## Verification

```bash
cd PuzzleBuddy-Android
cp app/google-services.json.example app/google-services.json   # CI does this automatically
./gradlew testDebugUnitTest assembleDebug
```

`testDebugUnitTest assembleDebug` passed on 2026-11-06 (262 tests). The phone pass is in `docs/implementation/1.1.0-catch-up.md` §8. Tablet, TalkBack, camera, and a v1 upgrade were not run.

## Firebase setup

Uses the shared **`puzzle-buddy`** Firebase project (same as iOS).

1. Firebase Console → register Android app (`com.jacobrozell.puzzlebuddy`) if needed.
2. Download `google-services.json` → `app/google-services.json` (gitignored).
3. Analytics and Crashlytics activate when `mobilesdk_app_id` does **not** contain `REPLACE_WITH`.
4. Remote telemetry is **Release-only** by default (matches iOS Debug builds).
5. Enable Crashlytics in Firebase Console → Build → Crashlytics.

CI checks the allowlist against iOS tag `1.1.0`. Bump the `ref` in `.github/workflows/ci.yml` when Android targets the next iOS tag.

## Top remaining gaps

1. Samsung pass in [`docs/release-runbook.md`](docs/release-runbook.md): camera allow and deny, photo pick, TalkBack, landscape, and a short window. Tablet is the same pass if you have one.
2. Play listing stays off (`isPlayListingLive = false`) until the store page exists. Publish the privacy page, then follow the runbook. There is no Settings import/export screen. `puzzle_backup_restored` has no call site until a restore UI exists.
