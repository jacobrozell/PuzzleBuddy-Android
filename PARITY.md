# iOS ↔ Android parity tracker

> **Dashboard:** [`STATUS.md`](STATUS.md)  
> **Plan:** [`docs/implementation/1.1.0-catch-up.md`](docs/implementation/1.1.0-catch-up.md) (order of work) · [`docs/implementation/1.1.0-ios-parity-plan.md`](docs/implementation/1.1.0-ios-parity-plan.md) (WP detail)

**Target:** iOS tag `1.1.0` (shipped 2026-11-02)  
**Android branch:** `claude/pensive-knuth-liq0g4`  
**Repo version:** 1.1.0 (2)  
**Last updated:** 2026-11-06

## Legend

| Status | Meaning |
|--------|---------|
| ✅ | Matches iOS 1.1 behavior |
| 🟡 | Model or store is in; UI is partial or missing |
| ⬜ | Not started |
| 🚫 | Out of scope, or removed on iOS 1.1 |

## Summary

| Area | Android |
|------|---------|
| 3-tab shell | ✅ |
| Five statuses (Wishlist, To-Do, In-Progress, Completed, Abandoned) | ✅ list tabs (menu at large font), form, detail |
| Metadata (dates, price, type, material, shape, cut, disposition) | ✅ form sections and detail rows in iOS order |
| Photos, up to 5, files on disk | ✅ gallery editor and detail pager; picking not emulator-checked |
| Completion history, redo, undo | ✅ history editor, undo banner, Puzzle again |
| On loan + friends | ✅ form (name, due back), list badge and filters, detail rows, Mark returned; no People list (same as iOS) |
| In-app timer | ✅ detail card, list badge, one running timer |
| Pick next | ✅ list and Stats |
| Stats (finishes, year picker, charts, milestones) | ✅ |
| Share collage | ✅ list overflow and Stats |
| Demo catalog (13 puzzles, covers, Paris on loan, Floral Arch ×2) | ✅ emulator-checked 2026-11-05 |
| Online barcode lookup (UPCitemdb) | ✅ removed, as on iOS. Quick add prefills from saved puzzles only. |
| Settings import / export | 🚫 UI removed on iOS 1.1. JSON backup format matches; there is no import/export screen |
| Write a Review + in-app review prompt | 🟡 prompt wired after a scan or a non-demo edit; Settings row still behind `isPlayListingLive` (off) |
| What's New | ✅ 1.1 notes, without the iPhone Duo item |
| Store-reset and ephemeral banners | ✅ |
| Delete all data, including borrower names | ✅ |
| Analytics allowlist | ✅ matches tag `1.1.0` |
| Onboarding copy (no lookup, no IPDb) | ✅ |
| Tablet two-pane layout | ✅ existing Android layout |

## 1.1 features

| Feature | iOS 1.1 | Android |
|---------|---------|---------|
| Wishlist / Abandoned | ✅ | ✅ |
| Start date and date labels | ✅ | ✅ |
| Purchase location, price, year, type, material, shape, cut, dimensions, disposition | ✅ | ✅ |
| Multi-photo gallery (cover = first, arrow reorder) | ✅ | ✅ |
| Completion history edit / delete | ✅ | ✅ |
| Undo completion banner | ✅ | ✅ |
| Puzzle again (redo) | ✅ | ✅ |
| On loan, overdue, Mark returned | ✅ | ✅ |
| Friends table | ✅ model only | 🟡 model + Room; list UI stays off |
| In-app stopwatch | ✅ | ✅ |
| Pick next puzzle | ✅ | ✅ |
| Stats year picker, monthly chart, on-loan card, milestone banner | ✅ | ✅ |
| Share collage | ✅ | ✅ |
| What's New 1.1 | ✅ | ✅ without the iPhone Duo item |
| Play / App Store review prompt | ✅ | 🟡 once after a scan or non-demo edit; Settings row hidden |
| Demo data, 13 puzzles | ✅ | ✅ |
| JSON backup format | ✅ | ✅ format only; no Settings UI |

## Still the 1.0 shell

| Feature | Android |
|---------|---------|
| Splash, onboarding, appearance | ✅ |
| Search, sort, swipe delete, tags, half-star rating | ✅ |
| Barcode scan + shopping mode | ✅ |
| Barcode field on the form | ✅ |
| UPC barcode lookup | 🚫 removed on iOS 1.1 |
| IPDb CSV import and collection export in Settings | 🚫 removed on iOS 1.1 |
| Privacy, support, accessibility links | ✅ |
| Firebase Analytics + Crashlytics breadcrumbs | ✅ |
| Design tokens / brand background | ✅ contrast-safe text tokens, teal containers in both themes |
| TalkBack audit | 🟡 |
| Login / cloud sync | 🚫 off |

CI pins the iOS checkout to tag `1.1.0` in `.github/workflows/ci.yml`. iOS `1.1.0` is already on `main`. Remove that `ref:` pin once CI should follow iOS `main` again.

## Session log

| Date | Notes |
|------|-------|
| 2026-06-23 | Initial scaffold: domain, Room, 3-tab shell |
| 2026-06-23 | Photos, tags, progress, settings, IPDb import, splash — emulator verified |
| 2026-06-23 | Barcode scan, shopping mode, export share — ~90% parity |
| 2026-06-23 | Tablet: navigation rail, two-pane detail/form, stats grid, adaptive list |
| 2026-09-25 | 1.1 Analytics allowlist + session snapshot / milestones / user properties; product 1.1 deferred |
| 2026-10-28 | Domain, Room v2, and repository for iOS 1.1 landed on `claude/pensive-knuth-liq0g4`. List, form, and detail show the new fields. Unit tests passed. Emulator QA not done. Open: timer card, history UI, undo banner, stats screen, pick next, share, What's New, review prompt, drop UPCitemdb and import/export UI, JSON backup parity, version 1.1.0 (2). |
| 2026-11-05 | Rebaselined on iOS tag `1.1.0`. CI repinned; parity script passes against the tag. 65 unit tests green. Plan: [`docs/implementation/1.1.0-catch-up.md`](docs/implementation/1.1.0-catch-up.md). |
| 2026-11-05 | M1 done: stats count finish logs after Puzzle again (iOS #44), `PuzzleCompletion.ratingLabel`, 48 ported store tests on in-memory DAOs with rollback, ported iOS domain tests (stats, list filters, timer, photos, history, undo, metrics, milestones, picker, share, loans, demo, enums), and UPCitemdb removed. 224 unit tests green. |
| 2026-11-05 | M2 done: list (filters, sort, rows, empty states, delete confirm), five-photo form, detail, barcode/quick add/shopping, Settings, onboarding copy, storage banners, contrast tokens. Fixed: form text fields losing focus when the keyboard opened, stale detail after edit, spaces dropped while typing brand/store/notes. Emulator-checked on a phone in light, dark, and 150% font. 239 unit tests green. |
| 2026-11-05 | M3 done: history, undo, timer card, pick next, stats, share collage, What's New (no iPhone Duo), Play review prompt. Phone emulator in light, and dark for stats. 253 unit tests green. |
| 2026-11-06 | M4 done: telemetry call sites, version 1.1.0 (2), product flags, JSON backup format (no Settings UI). |
| 2026-11-06 | M5 phone pass: history, loan, timer start, stats, settings 1.1.0. 262 unit tests. Tablet, TalkBack, camera, and a v1 upgrade not run. Not merged. |
