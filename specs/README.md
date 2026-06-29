# Specs index

Two-layer spec system: **iOS behavior is canonical**; Android specs describe implementation.

| iOS spec | Android implementation |
|----------|------------------------|
| [`../../Puzzle-Buddy/specs/features/local-catalog.md`](../../Puzzle-Buddy/specs/features/local-catalog.md) | `domain/catalog/*`, `data/local/*`, `ui/puzzles/*` |
| [`../../Puzzle-Buddy/docs/features.md`](../../Puzzle-Buddy/docs/features.md) | `PARITY.md` rows |
| [`../../Puzzle-Buddy/specs/features/barcode-quick-add.md`](../../Puzzle-Buddy/specs/features/barcode-quick-add.md) | ⬜ deferred (`ProductSurface`) |
| [`../../Puzzle-Buddy/specs/planned/auth-cloud-sync.md`](../../Puzzle-Buddy/specs/planned/auth-cloud-sync.md) | 🚫 lean 1.0 |

## Android system specs

| Spec | Status |
|------|--------|
| `specs/ArchitectureSpec.md` | 🟡 inline in STATUS.md |
| `specs/ProductSurfaceSpec.md` | ✅ `domain/surface/ProductSurface.kt` |
| `specs/DataSchemaSpec.md` | 🟡 `PuzzleEntity` v1 |

## Verification

```bash
./gradlew test
```

Domain tests: `app/src/test/.../DomainCatalogTests.kt`
