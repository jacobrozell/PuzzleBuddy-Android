# Puzzle Buddy Android

Native Android port of [Puzzle Buddy](https://github.com/jacobrozell/Puzzle-Buddy) (iOS), following the workspace Android conversion checklist.

**Application ID:** `com.jacobrozell.puzzlebuddy`  
**Lean surface:** iOS 1.0.0 local-first catalog (no login/cloud sync)

> **Current state:** [`STATUS.md`](STATUS.md) · **Feature matrix:** [`PARITY.md`](PARITY.md) · **Specs:** [`specs/README.md`](specs/README.md)

---

## Status (2026-06-23)

**~95% parity** with iOS 1.0 lean intent. End-to-end: puzzle CRUD, photo attach (CameraX), star rating, difficulty, status tabs, settings, Firebase Analytics/Crashlytics gating.

**Not yet:** full WCAG TalkBack audit, pixel-perfect design-token match with iOS.

```bash
./gradlew assembleDebug test   # passing
```

**iOS repo:** [jacobrozell/Puzzle-Buddy](https://github.com/jacobrozell/Puzzle-Buddy) · Behavior specs: `../Puzzle-Buddy/specs/`

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

1. UI visual polish pass (match iOS `DesignTokens` exactly)
2. Full WCAG TalkBack audit
3. Play Store release pipeline

---

## Docs

| Doc | Purpose |
|-----|---------|
| [`STATUS.md`](STATUS.md) | Current port state, routes, verification |
| [`PARITY.md`](PARITY.md) | iOS ↔ Android feature scoreboard |
| [`specs/README.md`](specs/README.md) | Two-layer spec index |
| [`docs/agent-android-conversion-checklist.md`](docs/agent-android-conversion-checklist.md) | Conversion progress log |

## Cursor ↔ Android Studio (MCP)

Connect Cursor chat to Android Studio via the [JetBrains MCP Server plugin](https://plugins.jetbrains.com/plugin/26071-mcp-server). Use **`build_project`** with `projectPath` set to this repo root to validate edits.

---

## iOS reference

iOS source: sibling `../Puzzle-Buddy/` on `main` @ 1.0.0 lean local surface.  
Behavior specs: `Puzzle-Buddy/specs/` and `Puzzle-Buddy/docs/features.md`.
