# Agent Android conversion checklist — Puzzle Buddy

Copied from [`workspace/agent-android-conversion-checklist-template.md`](../../workspace/agent-android-conversion-checklist-template.md).

## Start here

| Field | Value |
|-------|-------|
| **iOS source** | `../Puzzle-Buddy/`, branch `main`, lean 1.0 local-first |
| **Android target** | `PuzzleBuddy-Android/`, `com.jacobrozell.puzzlebuddy`, minSdk 28 |
| **Parity goal** | iOS 1.0.0 local catalog surface |

## Progress log

| Phase | Status | Date |
|-------|--------|------|
| −1 iOS audit | ✅ | 2026-06-23 |
| 0 Android repo scaffold | ✅ | 2026-06-23 |
| 1 Spec system skeleton | 🟡 | 2026-06-23 |
| 2 Design system | 🟡 tokens in `ui/theme` | 2026-06-23 |
| 3 Domain port + tests | ✅ core catalog | 2026-06-23 |
| 4 Persistence | ✅ Room v1 | 2026-06-23 |
| 5 App shell + onboarding | ✅ | 2026-06-23 |
| 6 Core vertical slice | ✅ photos + tags | 2026-06-23 |
| 7 Shared chrome | 🟡 | |
| 7b UI visual parity | ⬜ | |
| 18 Documentation pass | 🟡 STATUS + PARITY | 2026-06-23 |

## Verification command

```bash
./gradlew assembleDebug test
```
