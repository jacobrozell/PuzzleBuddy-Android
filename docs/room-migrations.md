# Room migrations

v2 is the first shipped schema. It stores puzzles, photos (file names only), completion logs, and friends.

After the first Play release:

- Never remove columns.
- Never use a destructive migration fallback.
- Every schema bump gets a `Migration` and a `MigrationTestHelper` test.
- Commit the exported schema JSON under `app/schemas/`.

v1 installs (nothing shipped) may be wiped via `fallbackToDestructiveMigrationFrom(1)`. Any other missing migration fails loudly.
