# iOS ↔ Android parity tracker

> **Dashboard:** [`STATUS.md`](STATUS.md)  
> **Plan:** [`docs/implementation/1.1.0-ios-parity-plan.md`](docs/implementation/1.1.0-ios-parity-plan.md)

**Target:** iOS 1.1.0 (`feature/1.2.0-puzzle-timer` @ `1f9845c`)  
**Android branch:** `claude/pensive-knuth-liq0g4`  
**Repo version:** 1.0.0 (1) — not bumped yet  
**Last updated:** 2026-10-28

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
| Five statuses (Wishlist, To-Do, In-Progress, Completed, Abandoned) | 🟡 list segments + form picker |
| Metadata (dates, price, type, material, shape, cut, disposition) | 🟡 domain, Room, form, and detail |
| Photos, up to 5, files on disk | 🟡 store keeps 5; UI still attaches one photo |
| Completion history, redo, undo | 🟡 repository only |
| On loan + friends | 🟡 form, list badge, detail; no People list (same as iOS) |
| In-app timer | 🟡 repository commands; list badge only |
| Pick next | ⬜ |
| Stats (finishes, year picker, charts, milestones) | 🟡 domain math; screen is still the old ~14 metrics |
| Share collage | ⬜ |
| Demo catalog (13 puzzles, covers, Paris on loan, Floral Arch ×2) | 🟡 loader writes them; not emulator-checked |
| Online barcode lookup (UPCitemdb) | 🚫 removed on iOS 1.1; still in the Android app |
| Settings import / export | 🚫 removed on iOS 1.1; still in Android Settings |
| Write a Review + in-app review prompt | ⬜ |
| What's New | ⬜ |
| Store-reset and ephemeral banners | 🟡 flag and Crashlytics events; no banner |
| Delete all data, including borrower names | 🟡 repository; Settings copy still says puzzles |
| Analytics allowlist | ✅ matches `1f9845c` |
| Onboarding copy (no lookup, no IPDb) | ⬜ still the old copy |
| Tablet two-pane layout | ✅ existing Android layout |

## 1.1 features

| Feature | iOS 1.1 | Android |
|---------|---------|---------|
| Wishlist / Abandoned | ✅ | 🟡 |
| Start date and date labels | ✅ | 🟡 detail labels |
| Purchase location, price, year, type, material, shape, cut, dimensions, disposition | ✅ | 🟡 |
| Multi-photo gallery (cover = first, arrow reorder) | ✅ | 🟡 files + semantics; form is still one photo |
| Completion history edit / delete | ✅ | 🟡 repository |
| Undo completion banner | ✅ | 🟡 repository, 5-minute window |
| Puzzle again (redo) | ✅ | 🟡 `startRedo` |
| On loan, overdue, Mark returned | ✅ | 🟡 form switch, list badge, detail rows |
| Friends table | ✅ model only | 🟡 model + Room; list UI stays off |
| In-app stopwatch | ✅ | 🟡 store; no timer card |
| Pick next puzzle | ✅ | ⬜ |
| Stats year picker, monthly chart, on-loan card, milestone banner | ✅ | 🟡 `CollectionStats` math only |
| Share collage | ✅ | ⬜ |
| What's New 1.1 | ✅ | ⬜ |
| Play / App Store review prompt | ✅ | ⬜ |
| Demo data, 13 puzzles | ✅ | 🟡 |

## Still the 1.0 shell

| Feature | Android |
|---------|---------|
| Splash, onboarding, appearance | ✅ |
| Search, sort, swipe delete, tags, half-star rating | ✅ |
| Barcode scan + shopping mode | ✅ |
| Barcode field on the form | ✅ |
| UPC barcode lookup | 🚫 still present; remove (plan WP3.8) |
| IPDb CSV import and collection export in Settings | 🚫 still present; remove the UI (plan WP4.9) |
| Privacy, support, accessibility links | ✅ |
| Firebase Analytics + Crashlytics breadcrumbs | ✅ |
| Design tokens / brand background | 🟡 |
| TalkBack audit | 🟡 |
| Login / cloud sync | 🚫 off |

Remove the CI pin in `.github/workflows/ci.yml` (`ref: feature/1.2.0-puzzle-timer`) once iOS merges 1.1.0 into `main`.

## Session log

| Date | Notes |
|------|-------|
| 2026-06-23 | Initial scaffold: domain, Room, 3-tab shell |
| 2026-06-23 | Photos, tags, progress, settings, IPDb import, splash — emulator verified |
| 2026-06-23 | Barcode scan, shopping mode, export share — ~90% parity |
| 2026-06-23 | Tablet: navigation rail, two-pane detail/form, stats grid, adaptive list |
| 2026-09-25 | 1.1 Analytics allowlist + session snapshot / milestones / user properties; product 1.1 deferred |
| 2026-10-28 | Domain, Room v2, and repository for iOS 1.1 landed on `claude/pensive-knuth-liq0g4`. List, form, and detail show the new fields. Unit tests passed. Emulator QA not done. Open: timer card, history UI, undo banner, stats screen, pick next, share, What's New, review prompt, drop UPCitemdb and import/export UI, JSON backup parity, version 1.1.0 (2). |
