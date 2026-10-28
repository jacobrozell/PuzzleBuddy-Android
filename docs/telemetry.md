# Telemetry (Android)

**Canonical spec (iOS + Android):** [`../PuzzleBuddy/docs/telemetry.md`](../../PuzzleBuddy/docs/telemetry.md)

**Cross-platform parity:** [`../workspace/firebase-cross-platform-parity.md`](../../workspace/firebase-cross-platform-parity.md)

**Verify allowlists match iOS:**

```bash
~/Desktop/personal/DaRules/scripts/check-firebase-parity.sh
```

When changing Analytics, Crashlytics, or `eventName` strings, update **both** repos and re-run the checker.

## 1.1.0 depth (allowlist + live call sites)

**Last updated:** 2026-10-28

Android allowlists match iOS `1f9845c` (`FirebaseAnalyticsEventMapping`, `AnalyticsMetadataKeys`). Work is on `claude/pensive-knuth-liq0g4`. The app version is still 1.0.0 (1).

**Live on Android today**

| Helper or event | When |
|-----------------|------|
| `AnalyticsSessionContext` | Cold launch (`AppBootstrapper`) + first list load |
| `AnalyticsUserContext` | Launch, onboarding complete/skip, collection mutations |
| `AnalyticsMilestones` | After add / complete / collection change |
| `count_wishlist` / `count_abandoned` | Real counts from puzzle status |
| `puzzle_completion_recorded` | Real completion number from `appendCompletion` |
| `puzzle_completion_deleted` / `updated` / `undone` | Repository methods. No history UI yet. |
| `puzzle_timer_started` / `puzzle_timer_paused` | Repository timer commands. No timer card yet. |
| `puzzle_redo_started`, `puzzle_marked_on_loan`, `puzzle_marked_returned` | Repository. Loan UI can mark a loan; redo and return have no buttons yet. |
| `model_container_load_failed`, `model_container_reset_failed`, `model_container_ephemeral_fallback`, `model_container_store_reset` | Database factory |

**Allowlisted, no Android call site yet**

- `whats_new_shown` / `whats_new_dismissed`
- `pick_next_puzzle_selected`
- `puzzle_backup_restored`
- `store_review_requested` / `store_review_link_opened` (log-only)
