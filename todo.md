# To-do

Plan for the rewrite. Product decisions and details are in [docs/product-spec.md](docs/product-spec.md).

Each milestone gets its own PR. Per claude.md, every commit message is proposed for approval before committing.

## For you

- [ ] Allow `dl.google.com` in this cloud environment's network settings (environment menu in the session title bar → Edit → Network access → Allowed domains). It hosts the Android SDK and Google's Maven repository. Until it's allowed, only the pure-Kotlin modules (M1) can be built and tested in cloud sessions.
- [ ] Lock down or delete the old Firebase project `nof1-26456`. If the rules in this repo are the deployed ones, any signed-in user, including anonymous ones, can create documents under another user's ID. v1 doesn't use Firebase.

## Android v1

### M0: Reset and foundations

- [x] Remove the old app, `.idea/`, the Firebase config, the lint baseline and the Firebase migration guide (they stay in git history)
- [x] Rename `claude.md` to `CLAUDE.md`. Claude Code looks for `CLAUDE.md`, and on case-sensitive Linux (including these cloud sessions) the lowercase file isn't loaded
- [x] Gradle 9.8.2 wrapper with a pinned checksum; version catalog
- [ ] Android app skeleton: AGP 9.4.0 with built-in Kotlin, Compose BOM 2026.09.00, compileSdk and targetSdk 37, minSdk 26, minified release builds (written; first verified by CI)
- [x] Pure-Kotlin modules `core:model` and `core:domain`. Other modules arrive with their milestones
- [x] ktlint formatting through Spotless
- [ ] Android lint with warnings as errors and no baseline (configured; first verified by CI)
- [ ] CI on GitHub Actions: formatting, build, unit tests, lint (screenshot checks come with M3)
- [x] New README

### M1: Experiment engine (pure Kotlin)

- [x] Domain models
- [x] Schedule: randomized pair order from a stored seed, periods, settling-in days, daily check-in times in the local time zone (correct across daylight-saving changes), and check-ins that describe the previous day
- [x] Results: period values, pair improvements, randomization test (exact up to 20 pairs, fixed-seed Monte Carlo above that), confidence interval, verdict rules, minimum-data rules
- [x] Unit tests, including hand-worked examples, a brute-force check of the test's definition, a coverage simulation and daylight-saving transitions

### M2: Data layer

- [ ] Convention plugins in `build-logic`, now that there will be several Android modules
- [ ] Android lint on the pure-Kotlin modules
- [ ] Room database: UUID IDs, UTC timestamps, tombstones; exported schemas and migration tests
- [ ] Repositories exposing Flows; Hilt modules
- [ ] Backup rules: back up the database only when the backup is end-to-end encrypted
- [ ] Export to JSON (everything) and CSV (check-ins)

### M3: App shell and design system

- [ ] Theme: brand palette and app icon proposal (screenshots for your review), Material You toggle, light and dark, type scale
- [ ] Adaptive navigation (bar on phones, rail on wide screens), edge-to-edge
- [ ] Type-safe navigation routes and deep links
- [ ] Screenshot tests: phone, foldable and tablet × light and dark × large text

### M4: Projects, hypotheses, notes

- [ ] Project list and detail, hypothesis detail; list and detail side by side on wide screens
- [ ] Create, edit, archive, delete with Undo
- [ ] Text notes on projects, hypotheses and experiments

### M5: Experiments and check-ins

- [ ] Experiment builder (4 steps) with period-length suggestions and a warning below 5 pairs
- [ ] Start: randomize, show the calendar, lock the plan
- [ ] Today screen
- [ ] Check-in screen for all four answer types; a missed day can be filled in until the end of the next day
- [ ] Stop early

### M6: Reminders

- [ ] Check-in reminders and period-start notices; skip the reminder when already checked in
- [ ] Reschedule after a reboot, a time or time-zone change, and an app update
- [ ] Ask for notification permission when the first experiment starts

### M7: Results

- [ ] Chart of daily answers shaded by period, with a table alternative for screen readers
- [ ] Averages, pairs, verdict with explanation, adherence, secondary outcomes, free-text answers, limitations
- [ ] Progress view while running (no verdict until the end)

### M8: AI suggestions (your own API key)

- [ ] Decide the provider(s); put the client behind an interface
- [ ] Key encrypted with an Android Keystore key, excluded from backup, redacted from logs
- [ ] Consent screen and a preview of exactly what's sent
- [ ] Hypotheses from a goal; an experiment draft from a hypothesis; review before saving
- [ ] Fake provider for debug builds and tests

### M9: Polish and release readiness

- [ ] Accessibility pass: TalkBack, 200% text, contrast, touch targets
- [ ] Minified release build, final icon, privacy text
- [ ] Baseline profile for startup performance

## Later

- [ ] Web app (scope to be decided: full app, results dashboard, or landing page)
- [ ] Accounts and cloud sync (needed if the web app shows user data)
- [ ] Photos on notes
- [ ] Import or restore from an export
- [ ] Ideas: Health Connect as an outcome source (sleep, steps), quick check-in from the notification, a home-screen widget, more than two conditions (A/B/C)
