# Puzzle Buddy Android

Native Android port of [Puzzle Buddy](https://github.com/jacobrozell/PuzzleBuddy) (iOS), following the workspace Android conversion checklist.

**Application ID:** `com.jacobrozell.puzzlebuddy`  
**Target:** iOS 1.1.0 local-first catalog (no login/cloud sync)

> **Current state:** [`STATUS.md`](STATUS.md) · **Feature matrix:** [`PARITY.md`](PARITY.md) · **Plan:** [`docs/implementation/1.1.0-ios-parity-plan.md`](docs/implementation/1.1.0-ios-parity-plan.md)

---

## Status (2026-11-06)

**In progress** on `claude/pensive-knuth-liq0g4`. Version **1.1.0 (2)**. The 1.1 catalog UI is in: history, undo, timer, pick next, stats, share, What's New, and the Play review prompt. Online barcode lookup and Settings import/export are gone, matching iOS. The JSON backup format matches iOS and has no Settings screen. Detail: [`STATUS.md`](STATUS.md).

Firebase Analytics and Crashlytics turn on when `google-services.json` is real. Detail: [`STATUS.md`](STATUS.md).

```bash
./gradlew assembleDebug test   # passing
```

**iOS repo:** [jacobrozell/PuzzleBuddy](https://github.com/jacobrozell/PuzzleBuddy) @ tag `1.1.0` · Behavior specs: `../PuzzleBuddy/specs/`

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
| **Photos** | Up to five JPEG files on disk |
| **List & detail** | History, undo, timer card, loan, and the rest of the 1.1 fields |
| **Collection** | Pick next, stats, and a share collage |
| **Settings** | Appearance, demo data, legal links, Delete All Data. No import/export screen |
| **Firebase** | Analytics + Crashlytics when real `google-services.json` present |
| **Offline** | Local-first — no login or cloud sync |

Unit tests cover store logic, migrations, and domain helpers.

---

## Remaining (see PARITY.md)

1. TalkBack audit, tablet pass, and photo/camera checks on a device
2. Play Store release pipeline (`isPlayListingLive` stays off)

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

iOS source: sibling `../PuzzleBuddy/` at tag `1.1.0`.  
Behavior specs: `PuzzleBuddy/specs/` and `PuzzleBuddy/docs/features.md`.
