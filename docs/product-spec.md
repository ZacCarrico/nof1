# Nof1 product spec (Android v1)

Draft for review, 2026-10-24.

Nof1 helps one person find out whether a change actually improves something they care about. It follows the structured N-of-1 trial design: periods with and without the change, in randomized order, with daily check-ins and an honest verdict at the end.

Example: project "Sleep better" → hypothesis "Screens before bed hurt my sleep" → experiment "No screens after 10pm" vs. usual routine, 6 randomized pairs of 3-day periods, with a daily morning check-in "Rate last night's sleep, 1–10".

## Decisions

| Topic | Decision |
|---|---|
| Experiment design | Structured N-of-1: baseline (A) and intervention (B) periods, a defined outcome, a fixed duration, a comparison at the end |
| Period order | Randomized pairs: each pair runs AB or BA at random |
| Check-in answers | Rating scale, yes/no, number with unit, free text |
| Results | Plain stats plus a plain-language verdict, judged against a minimum meaningful change set before starting; randomization test |
| Accounts and storage | Local-only, no sign-in; data model ready for sync later |
| AI suggestions | Included, using the user's own API key |
| Notes | Text only; photos later |
| Web | Not now; scope decided later |
| Look and feel | Minimal and smooth; nothing from the old app's UI needs to be kept |

## Proposed defaults

Product choices made to fill gaps. Any of them can change.

- **Structure:** Project (goal) → Hypotheses → Experiments → Check-ins, as before. Text notes attach to a project, hypothesis or experiment.
- **One primary outcome per experiment** drives the verdict. Secondary questions are reported descriptively only, so testing many outcomes can't produce a false "it worked".
- **Adherence:** each check-in also asks "Did you follow today's plan?" (on by default). Results report adherence but analyze every day by its assigned period (intention-to-treat), which avoids the bias of dropping hard days.
- **The plan locks at start:** once an experiment starts, its conditions, primary outcome, minimum meaningful change and schedule can't be edited (the idea behind pre-registration). Stopping early is allowed; results are then labeled "stopped early".
- **Verdict only at the end:** while an experiment runs, results show progress and averages. The verdict appears when it ends, so peeking can't bias it.
- **Home is a Today screen** showing what to do today for each running experiment and whether you've checked in.
- **Reminders belong to experiments** (check-ins and period starts). The old app's separate project and hypothesis reminders are dropped.
- **Backup and export:** your data is included in Android's own backup only when that backup is end-to-end encrypted (Android 9 and later, with a screen lock set), and in direct transfers to a new phone, so it survives a phone change. On Android 8 it isn't backed up. The AI key is never backed up. Export at any time: everything as JSON, and check-in answers as CSV for spreadsheets.
- **No analytics or crash-reporting SDKs.**
- **Identity:** keep the name Nof1 and the package `com.nof1`. Data from the old app isn't migrated.
- **Look and feel, minimal:** each screen has one main action and shows only what that action needs; details are one tap away. Plenty of space, few colors: a brand palette by default (stable colors for charts, and later for the web), with a Material You toggle in Settings. Light and dark themes.
- **Look and feel, smooth:** quick Material transitions and predictive back, nothing that blocks input, no work on the main thread, and startup and scrolling measured so they stay fast.
- **Language:** English for now, with all text in string resources so translation is possible later.
- **Devices:** Android 8.0 (API 26) and newer.

## Experiment design

The builder has four steps:

1. **What you'll change:** the intervention (B) name and instructions, and the baseline (A) name ("Usual routine" by default) and instructions.
2. **What you'll measure:** the primary outcome question (rating scale, number with unit, or yes/no; free text can't be primary), whether higher or lower is better, and the minimum meaningful change ("a change of 1 point matters to me"). Optional secondary questions of any type. The adherence question is on by default.
3. **Schedule:** daily check-in time; whether a check-in describes today or the previous day (e.g., a morning check-in about last night's sleep); period length; settling-in days; number of pairs; start date. The builder shows the total duration.
4. **Review and start:** the app randomizes the order within each pair, shows the calendar and locks the plan.

**Period length guidance.** The builder asks how quickly the change takes effect and wears off, then suggests a starting design. The numbers below are suggested starting points, not established standards, and all are adjustable.

| Takes effect and wears off | Period length | Settling-in days | Pairs | Total |
|---|---|---|---|---|
| Within a day (caffeine timing, a nap) | 1 day | 0 | 10 | 20 days |
| Within a few days (bedtime routine) | 3 days | 1 | 6 | 36 days |
| Over about a week (diet change, supplement) | 7 days | 2 | 5 | 70 days |

**Settling-in days:** the first days of each period follow that period's plan but aren't analyzed, so effects from the previous period can fade (an "analytic washout").

**At least 5 pairs** are needed for a verdict. With K pairs there are only 2^K possible orderings, and with 4 or fewer the test can't rule out chance at the 5% level, however clear the data looks. The builder warns below 5 pairs, and such experiments get descriptive results only.

## Running an experiment

- Today shows each running experiment: today's condition and instructions ("No screens after 10pm, period 3 of 12, day 2 of 3"), check-in status, and when the next period starts.
- A check-in answers the experiment's questions for one study day. A missed day can be filled in until the end of the next day; after that it stays missing. Answers can be edited during that window.
- Stopping early ends the experiment, and results use complete pairs only.

## Results and verdict

The analysis uses the primary outcome. Settling-in days are excluded, and each check-in counts toward the period of its study day.

1. **Period value:** the average answer (rating or number), or the share of "yes" days (yes/no). A period counts if at least half of its analyzed days, rounded up, have an answer.
2. **Pair improvement:** B's value minus A's value, with the sign flipped when lower is better. Pairs with a missing period are left out, and the results say so.
3. **Effect estimate:** the average pair improvement.
4. **Randomization test:** if the change had no effect, each pair could just as well have run in the opposite order, which only flips the sign of that pair's improvement. The 2^K equally likely orderings give the exact distribution of the estimate under "no effect". Inverting the test gives a confidence interval for the effect, computed from the ordered averages of subsets of pair improvements and assuming the change shifts answers by a roughly constant amount. Neither step assumes normal or independent daily data, only that the order really was randomized. The computation is exact up to 20 pairs; above that it uses a fixed-seed Monte Carlo approximation.
5. **Verdict,** based on the confidence interval (at least 90%) and the minimum meaningful change (MMC). Rules are checked top to bottom, and the first match wins:

| Verdict | Rule |
|---|---|
| Inconclusive | Fewer than 5 complete pairs |
| Helped | Interval entirely above 0, and estimate ≥ MMC |
| Helped a little | Interval entirely above 0, but estimate < MMC |
| Made it worse | Interval entirely below 0 |
| No meaningful effect | Interval entirely between −MMC and +MMC |
| Inconclusive | Anything else |

The results screen also shows:
- a chart of daily answers shaded by period, with a table alternative for screen readers
- per-period averages and how many pairs favored B
- adherence by condition
- secondary outcomes, and free-text answers grouped by period
- notes on the timeline
- limitations: you knew which period you were in, so expectations can affect self-ratings, and results apply to you under the conditions you tested.

## Notifications

- A daily check-in reminder per running experiment at its check-in time. It's skipped if you've already checked in, and tapping it opens the check-in.
- A period-start notice on the first day of each period ("Starting today: No screens after 10pm, through Fri Oct 31"), at 8:00 by default and adjustable in Settings.
- Notification permission is requested when you start your first experiment, not at first launch.
- Reminders use inexact alarms, so no special alarm permission is needed, and they can arrive a few minutes late. They're rescheduled after a reboot, a time or time-zone change, and an app update.

## AI suggestions (your own API key)

- Suggest hypotheses for a project's goal, and draft an experiment for a hypothesis (conditions, primary outcome question and type, a suggested minimum meaningful change, period length). You review and edit before anything is saved.
- Suggestions only run when you tap the button. Before sending, the app shows exactly what will be sent (the project goal or hypothesis text, never check-in data) and to which provider.
- The key is encrypted with an Android Keystore key, excluded from backups and device transfers, and never logged.
- Provider: open question, see below.

## Navigation and screens

- **Top level:** Today, Projects, Settings. A bottom navigation bar on phones; a navigation rail on tablets, unfolded foldables and in landscape.
- **Projects:** list → project (goal, hypotheses, notes) → hypothesis (statement, experiments with their verdicts, notes) → experiment (plan, calendar, check-ins, results, notes). On wide screens the list and the selected item show side by side.
- **Flows:** new project, new hypothesis (typed or AI-suggested), experiment builder, check-in, note editor.
- **Settings:** reminders, appearance (theme, Material You), AI suggestions (provider, key), your data (export, delete everything), about and privacy.
- **Archive and delete:** archiving hides an item and can be undone later. Deleting asks for confirmation and offers Undo.

Layouts follow Material 3 and Android's adaptive layout guidance: window size classes, the canonical list-detail layout, edge-to-edge, 48dp touch targets, screen reader labels, and text that scales to 200%.

## Privacy and security

- Nof1 has no account and no server. Its only network traffic is AI suggestions, when you request them, sent straight to your provider. Android's own backup is covered under "Backup and export" above.
- No cleartext traffic, minified release builds, and no debug UI or verbose logging in release.
- Export uses the system file picker, so no storage permission is needed. In the CSV, text that a spreadsheet could run as a formula is prefixed with an apostrophe.

## Data model (sync-ready)

- IDs are UUIDs, and created and updated times are stored in UTC, so adding sync later doesn't need ID mapping.
- Deleting removes the data. A deletion log keeps only the type and ID of what was deleted, so a future sync can delete it on other devices too. Undo works by deleting only once the Undo message has gone.
- Check-ins store the study date, the moment they were recorded and the time zone.
- Experiments store the realized period sequence and the randomization seed.
- Room schemas are exported and every schema change ships with a migration. The database is never wiped on upgrade.

## Open questions

| Question | Needed by |
|---|---|
| AI provider(s) for user keys (the old app used OpenAI) | M8 |
| Visual identity: a palette and icon will be proposed with screenshots | M3 |
| Publish on Google Play? Affects the privacy policy, Play's policy requirements for health apps, and release signing | M9 |
| Web app scope | After Android v1 |
