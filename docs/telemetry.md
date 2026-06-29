# Telemetry (Android)

**Canonical spec (iOS + Android):** [`../Puzzle-Buddy/docs/telemetry.md`](../../Puzzle-Buddy/docs/telemetry.md)

**Cross-platform parity:** [`../workspace/firebase-cross-platform-parity.md`](../../workspace/firebase-cross-platform-parity.md)

**Verify allowlists match iOS:**

```bash
~/Desktop/personal/DaRules/scripts/check-firebase-parity.sh
```

When changing Analytics, Crashlytics, or `eventName` strings, update **both** repos and re-run the checker.
