# Agent Android conversion checklist — Puzzle Buddy

Copied from [`workspace/agent-android-conversion-checklist-template.md`](../../../workspace/agent-android-conversion-checklist-template.md).

## Start here

| Field | Value |
|-------|-------|
| **iOS source** | `../PuzzleBuddy/`, tag `1.1.0` |
| **Android target** | `PuzzleBuddy-Android/`, `com.jacobrozell.puzzlebuddy`, minSdk 28 |
| **Parity goal** | iOS 1.1.0 local catalog, including the in-app timer |

## Progress log

| Phase | Status | Date |
|-------|--------|------|
| −1 iOS audit | ✅ | 2026-06-23 |
| 0 Android repo scaffold | ✅ | 2026-06-23 |
| 1 Spec system skeleton | 🟡 | 2026-06-23 |
| 2 Design system | 🟡 tokens in `ui/theme` | 2026-06-23 |
| 3 Domain port + tests | ✅ iOS 1.1 model | 2026-10-28 |
| 4 Persistence | ✅ Room v2 (photos, completions, friends, timer) | 2026-10-28 |
| 5 App shell + onboarding | ✅ 1.1 copy | 2026-11-05 |
| 6 Core vertical slice | ✅ history, undo, timer, pick next, stats, share | 2026-11-06 |
| 7 Shared chrome | ✅ What's New and review prompt | 2026-11-06 |
| 7b UI visual parity | 🟡 phone checked; tablet and TalkBack still open | 2026-11-06 |
| 18 Documentation pass | ✅ STATUS, PARITY, telemetry, version 1.1.0 (2) | 2026-11-06 |

## Verification command

```bash
./gradlew assembleDebug test
```
