# Agent Android conversion checklist — Puzzle Buddy

Copied from [`workspace/agent-android-conversion-checklist-template.md`](../../workspace/agent-android-conversion-checklist-template.md).

## Start here

| Field | Value |
|-------|-------|
| **iOS source** | `../PuzzleBuddy/`, `feature/1.2.0-puzzle-timer` @ `1f9845c` |
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
| 5 App shell + onboarding | ✅ shell; onboarding copy still 1.0 | 2026-06-23 |
| 6 Core vertical slice | 🟡 list, form, and detail show 1.1 fields; timer card and history UI open | 2026-10-28 |
| 7 Shared chrome | 🟡 | |
| 7b UI visual parity | ⬜ | |
| 18 Documentation pass | 🟡 STATUS + PARITY updated for the partial 1.1 port | 2026-10-28 |

## Verification command

```bash
./gradlew assembleDebug test
```
