# Puzzle Buddy Android

Native Android port of [Puzzle Buddy](https://github.com/jacobrozell/Puzzle-Buddy) (iOS), following the workspace Android conversion checklist.

**Application ID:** `com.jacobrozell.puzzlebuddy`  
**Lean surface:** iOS 1.0.0 local-first catalog (no login/cloud sync)

> **Current state:** [`STATUS.md`](STATUS.md) · **Feature matrix:** [`PARITY.md`](PARITY.md) · **Specs:** [`specs/README.md`](specs/README.md)

---

## Status (2026-10-28)

**Parked.** Lean 1.0 is about 95% of the iOS 1.0 catalog: puzzle CRUD, photo attach (CameraX), star rating, difficulty, status tabs, settings. Firebase Analytics and Crashlytics turn on when `google-services.json` is real.

The Analytics allowlist matches iOS `release/1.1.0`, including the iOS 1.2 timer event names. Android does not ship the timer, On loan, completion-history edit/undo, or What's New.

**Not shipping soon.** Still open: visual polish against iOS tokens, a TalkBack audit, and the Play Store pipeline. Detail: [`STATUS.md`](STATUS.md).

```bash
./gradlew assembleDebug test   # passing
```

**iOS repo:** [jacobrozell/Puzzle-Buddy](https://github.com/jacobrozell/Puzzle-Buddy) on `release/1.1.0` · Behavior specs: `../PuzzleBuddy/specs/`

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

## Lean 1.0 scope (Android)

| Area | Highlights |
|------|------------|
| **Catalog** | Add/edit/delete puzzles; piece count, rating, difficulty, status |
| **Photos** | Camera or gallery attach, compressed storage |
| **List & detail** | Tabbed browse, swipe delete, detail/edit flows |
| **Settings** | Legal links, appearance |
| **Firebase** | Analytics + Crashlytics when real `google-services.json` present |
| **Offline** | Local-first — no login/cloud in lean 1.0 |

Unit tests cover store logic, migrations, and domain helpers.

---

## Remaining (see PARITY.md)

1. iOS 1.1 product (On loan, completion history, What's New) — deferred
2. UI visual polish pass (match iOS `DesignTokens` exactly)
3. Full WCAG TalkBack audit
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

iOS source: sibling `../PuzzleBuddy/` on `release/1.1.0` (1.1.0 Connect prep). Android stays on the 1.0 catalog.  
Behavior specs: `PuzzleBuddy/specs/` and `PuzzleBuddy/docs/features.md`.
