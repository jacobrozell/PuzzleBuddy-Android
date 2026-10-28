# iOS ↔ Android parity tracker

> **Dashboard:** [`STATUS.md`](STATUS.md)

**Target:** Puzzle Buddy **1.1.0** in progress on `claude/pensive-knuth-liq0g4` (iOS `feature/1.2.0-puzzle-timer` @ `1f9845c`)  
**iOS baseline:** `PuzzleBuddy` @ `1f9845c`  
**Android path:** `PuzzleBuddy-Android/`  
**Last updated:** 2026-10-28

## Legend

| Status | Meaning |
|--------|---------|
| ✅ | Shipped on Android (matches iOS lean intent) |
| 🟡 | Partial / simplified |
| ⬜ | Not started |
| 🚫 | Out of lean 1.0 scope on both platforms |

## Summary

| Area | Android |
|------|---------|
| 3-tab shell (Puzzles · Stats · Settings) | ✅ |
| Splash screen | ✅ |
| Onboarding (4 pages) + replay | ✅ |
| Local Room persistence | ✅ |
| Puzzle list (status, search, sort, filters) | ✅ |
| Add / edit / delete puzzle | ✅ |
| Photo attach (gallery + camera, JPEG compress) | ✅ |
| Tags on puzzles | ✅ |
| Half-star rating picker | ✅ |
| In-progress progress slider | ✅ |
| Puzzle detail + pace metrics + photo | ✅ |
| Collection stats | ✅ |
| Settings (appearance, demo, import, legal) | ✅ |
| IPDb CSV import | ✅ |
| Barcode scan + shopping mode | ✅ |
| Collection export (JSON/CSV share) | ✅ |
| Barcode field on puzzle form | ✅ |
| UPC barcode lookup (UPCitemdb + local cache) | ✅ |
| Firebase Analytics + Crashlytics breadcrumbs | ✅ (1.1 allowlist + session/milestone helpers) |
| Design tokens / brand background | 🟡 |
| Login / cloud sync | 🚫 (iOS 1.0 off) |
| WCAG TalkBack full audit | 🟡 (labels on key controls) |
| Tablet / landscape adaptive layout | ✅ |

**~95% lean 1.0 intent**

---

## App shell

| Feature | iOS 1.0 | Android |
|---------|---------|---------|
| Splash + tagline | ✅ | ✅ |
| Puzzles tab | ✅ | ✅ |
| Collection Stats tab | ✅ | ✅ |
| Settings tab | ✅ | ✅ |
| Teal brand accent | ✅ | ✅ |
| Brand gradient background | ✅ | 🟡 |
| Appearance (light/dark/system) | ✅ | ✅ |
| Login gate | 🚫 off | 🚫 off |

## Catalog

| Feature | iOS | Android |
|---------|-----|---------|
| Status segments | ✅ | ✅ |
| Search (name, brand, barcode, tags) | ✅ | ✅ |
| Sort + filters | ✅ | ✅ |
| Swipe to delete | ✅ | ✅ |
| Star rating on list row | ✅ | ✅ |
| FAB add puzzle | ✅ | ✅ |
| Barcode scan → quick add + lookup | ✅ | ✅ |
| Shopping mode (duplicate check) | ✅ | ✅ |
| Puzzle form (core fields) | ✅ | ✅ |
| Barcode field + scan on form | ✅ | ✅ |
| Half-star rating UI | ✅ | ✅ |
| Photo attachment | ✅ | ✅ |
| Tags field + suggestions | ✅ | ✅ |
| Progress percent (in-progress) | ✅ | ✅ |
| List thumbnails | ✅ | ✅ |

## Settings & data

| Feature | iOS | Android |
|---------|-----|---------|
| IPDb CSV import | ✅ | ✅ |
| Export JSON backup | ✅ | ✅ |
| Export IPDb CSV | ✅ | ✅ |
| Load / remove demo data | ✅ | ✅ |
| Delete all puzzles | ✅ | ✅ |
| Barcode lookup toggle | ✅ | ✅ |
| Privacy / support / a11y links | ✅ | ✅ |
| Replay onboarding | ✅ | ✅ |

## Telemetry

| Feature | iOS | Android |
|---------|-----|---------|
| Allowlisted Firebase Analytics | ✅ | ✅ |
| Crashlytics breadcrumbs (INFO+) | ✅ | ✅ |
| Non-fatal error mapping | ✅ | ✅ |
| No PII in metadata | ✅ | ✅ |
| CI-safe placeholder config | ✅ | ✅ (`google-services.json.example`) |

## Deferred (documented)

| Feature | Notes |
|---------|-------|
| iOS DesignTokens pixel match | Brand background simplified |
| On loan / Friends | Model, store, form switch, list badge, and detail rows are in. People list stays off, matching iOS. |
| Completion history edit / undo / What's New | Repository operations exist. History editor, undo banner, and What's New sheet are not in the UI yet. |
| Wishlist / Abandoned statuses | Status values, filters, and progress rules are in. Snapshot counts use the real statuses. |
| In-app timer | Semantics and repository commands exist. The detail timer card is not in the UI yet. "Timer running" shows on the list when a timer is running. |

---

## Session log

| Date | Notes |
|------|-------|
| 2026-06-23 | Initial scaffold: domain, Room, 3-tab shell |
| 2026-06-23 | Photos, tags, progress, settings, IPDb import, splash — emulator verified |
| 2026-06-23 | Barcode scan, shopping mode, export share — ~90% parity |
| 2026-06-23 | Tablet: navigation rail, two-pane detail/form, stats grid, adaptive list |
| 2026-09-25 | 1.1 Analytics allowlist + session snapshot / milestones / user properties; product 1.1 deferred |
| 2026-10-28 | iOS 1.1 domain, Room v2, and repository (photos on disk, completions, friends, timer commands, demo catalog of 13) landed on `claude/pensive-knuth-liq0g4`. List, form, and detail show the new fields. Still open: timer card, completion history UI, undo banner, stats rewrite, share collage, What's New, Play review prompt, remove UPCitemdb and import/export settings, JSON backup parity, emulator QA. |
