# Specs index

**Last updated:** 2026-10-28

Two-layer spec system: **iOS behavior is canonical**; Android specs describe implementation.

| iOS spec | Android implementation |
|----------|------------------------|
| [`../../PuzzleBuddy/specs/features/local-catalog.md`](../../PuzzleBuddy/specs/features/local-catalog.md) | `domain/catalog/*`, `data/local/*`, `ui/puzzles/*` |
| [`../../PuzzleBuddy/docs/features.md`](../../PuzzleBuddy/docs/features.md) | `PARITY.md` rows |
| [`../../PuzzleBuddy/specs/features/barcode-quick-add.md`](../../PuzzleBuddy/specs/features/barcode-quick-add.md) | ✅ scan, shopping mode, and quick add (`ui/barcode/*`) |
| [`../../PuzzleBuddy/specs/planned/in-app-timer.md`](../../PuzzleBuddy/specs/planned/in-app-timer.md) | 🟡 `domain/timer` + repository; timer card not built |
| [`../../PuzzleBuddy/specs/planned/auth-cloud-sync.md`](../../PuzzleBuddy/specs/planned/auth-cloud-sync.md) | 🚫 off |
| [`../docs/implementation/1.1.0-ios-parity-plan.md`](../docs/implementation/1.1.0-ios-parity-plan.md) | Active 1.1 port |

## Android system specs

| Spec | Status |
|------|--------|
| `specs/ArchitectureSpec.md` | 🟡 inline in STATUS.md |
| `specs/ProductSurfaceSpec.md` | ✅ `domain/surface/ProductSurface.kt` |
| `specs/DataSchemaSpec.md` | ✅ Room v2 — see [`../docs/room-migrations.md`](../docs/room-migrations.md) |

## Verification

```bash
./gradlew testDebugUnitTest
```

Domain tests: `app/src/test/.../domain/IosParityDomainTest.kt`
