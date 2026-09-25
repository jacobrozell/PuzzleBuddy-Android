# Telemetry (Android)

**Canonical spec (iOS + Android):** [`../PuzzleBuddy/docs/telemetry.md`](../../PuzzleBuddy/docs/telemetry.md)

**Cross-platform parity:** [`../workspace/firebase-cross-platform-parity.md`](../../workspace/firebase-cross-platform-parity.md)

**Verify allowlists match iOS:**

```bash
~/Desktop/personal/DaRules/scripts/check-firebase-parity.sh
```

When changing Analytics, Crashlytics, or `eventName` strings, update **both** repos and re-run the checker.

## 1.1.0 depth (allowlist + live call sites)

Android allowlists match iOS 1.1 names (`FirebaseAnalyticsEventMapping`, `AnalyticsMetadataKeys`). This Android branch is **`release/1.0.0`** — first Play ship would be 1.0.0.

**Live on Android today**

| Helper | When |
|--------|------|
| `AnalyticsSessionContext` | Cold launch (`AppBootstrapper`) + first list load |
| `AnalyticsUserContext` | Launch, onboarding complete/skip, collection mutations |
| `AnalyticsMilestones` | After add / complete / collection change |

`count_wishlist` / `count_abandoned` stay `0` until those statuses exist on Android.

**Allowlisted, no Android UI yet** (iOS-only call sites)

- `puzzle_completion_deleted` / `updated` / `undone`
- `whats_new_shown` / `whats_new_dismissed`
- `puzzle_backup_restored`
