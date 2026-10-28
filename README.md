# Puzzle Buddy Android

Native Android port of [Puzzle Buddy](https://github.com/jacobrozell/PuzzleBuddy) (iOS), following the workspace Android conversion checklist.

**Application ID:** `com.jacobrozell.puzzlebuddy`  
**Target:** iOS 1.1.0 local-first catalog (no login/cloud sync)

> **Current state:** [`STATUS.md`](STATUS.md) · **Feature matrix:** [`PARITY.md`](PARITY.md) · **Plan:** [`docs/implementation/1.1.0-ios-parity-plan.md`](docs/implementation/1.1.0-ios-parity-plan.md)

---

## Status (2026-10-28)

**In progress** on `claude/pensive-knuth-liq0g4`. The iOS 1.1 model, Room v2, and repository are in: five statuses, metadata, photo files, completion logs, on loan, the timer commands, and a 13-puzzle demo catalog. The list, form, and detail show those fields. The timer card, history editor, stats rewrite, and the rest of the 1.1 UI are still open. Repo version is still 1.0.0 (1).

Firebase Analytics and Crashlytics turn on when `google-services.json` is real. Detail: [`STATUS.md`](STATUS.md).

```bash
./gradlew assembleDebug test   # passing
```

**iOS repo:** [jacobrozell/PuzzleBuddy](https://github.com/jacobrozell/PuzzleBuddy) @ `1f9845c` · Behavior specs: `../PuzzleBuddy/specs/`

---

## Requirements

- **minSdk 28** · **targetSdk 36**
- Android Studio Ladybug or newer
- JDK 17

## Open in Android Studio

1. **File → Open** → select this repo root (`PuzzleBuddy-Android/`)
2. Copy Firebase config: `cp app/google-services.json.example app/google-services.json`
3. Sync Gradle, run on emulator or device

## Build from CLI

```bash
cp app/google-services.json.example app/google-services.json   # CI does this automatically
./gradlew assembleDebug test
```

---

## Architecture

| Layer | Android | iOS equivalent |
|-------|---------|----------------|
| UI | Compose + ViewModels | SwiftUI + ViewModels |
| Domain | Pure Kotlin models + store logic | `Puzzle` model + helpers |
| Data | Room + repositories | SwiftData + `PuzzleStore` |
| DI | Hilt | `@EnvironmentObject` / services |
| Photos | CameraX + ML Kit (optional) | UIImagePicker |

Package: `com.jacobrozell.puzzlebuddy` · Implementation specs: [`specs/README.md`](specs/README.md)

---

## Scope (Android)

| Area | Highlights |
|------|------------|
| **Catalog** | Five statuses, metadata, piece count, rating, difficulty, tags |
| **Photos** | Up to five JPEG files on disk; the form still attaches one |
| **List & detail** | Status segments, loan and timer badges, detail rows for the new fields |
| **Collection** | Completions, on loan, and the stopwatch are stored; history and timer UI are next |
| **Settings** | Appearance, demo data (13 puzzles), legal links. Import/export UI is still here and comes out with the 1.1 settings pass |
| **Firebase** | Analytics + Crashlytics when real `google-services.json` present |
| **Offline** | Local-first — no login or cloud sync |

Unit tests cover store logic, migrations, and domain helpers.

---

## Remaining (see PARITY.md)

1. Timer card, completion history, undo banner, stats, pick next, share collage
2. What's New, Play review prompt, and removing lookup plus import/export from Settings
3. UI visual polish and a TalkBack audit
4. Play Store release pipeline

---

## Docs

| Doc | Purpose |
|-----|---------|
| [`STATUS.md`](STATUS.md) | Current port state, routes, verification |
| [`PARITY.md`](PARITY.md) | iOS ↔ Android feature scoreboard |
| [`specs/README.md`](specs/README.md) | Two-layer spec index |
| [`docs/agent-android-conversion-checklist.md`](docs/agent-android-conversion-checklist.md) | Conversion progress log |

## Verify

UI changes use the `android-verify` skill. Package id and test commands are in [`AGENTS.md`](AGENTS.md).

---

## iOS reference

iOS source: sibling `../PuzzleBuddy/` at `1f9845c` (`feature/1.2.0-puzzle-timer`, the 1.1.0 build).  
Behavior specs: `PuzzleBuddy/specs/` and `PuzzleBuddy/docs/features.md`.
