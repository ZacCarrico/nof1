# Nof1

An Android app for running your own N-of-1 experiments: find out whether a change actually improves
something you care about.

You set a goal, write down a hypothesis, and test it with an experiment that alternates baseline and
intervention periods in randomized order. A short daily check-in records how it went. At the end,
Nof1 compares the periods and gives a plain-language verdict: helped, made it worse, no meaningful
effect, or inconclusive.

> **Status:** being rewritten from scratch. The experiment engine (scheduling and results) is done;
> the app UI comes next. See [todo.md](todo.md) for the plan and
> [docs/product-spec.md](docs/product-spec.md) for how the app works.

## Project structure

| Module | Contents |
|---|---|
| `app` | The Android app |
| `core:model` | Data types: projects, hypotheses, experiments, check-ins, notes (pure Kotlin) |
| `core:domain` | Experiment scheduling, reminders and results analysis (pure Kotlin) |

More modules (database, design system, features) arrive with their milestones.

## Building

Requirements: JDK 17 or newer, and the Android SDK with API level 37 (a recent Android Studio
installs it).

```bash
./gradlew build          # build, unit tests, lint and formatting checks
./gradlew spotlessApply  # format Kotlin files
./gradlew :core:domain:test
```

## How results are computed

Each pair of periods yields one improvement: the intervention period's average minus the baseline
period's. Because the order within each pair is randomized, a randomization test gives an exact
p-value and confidence interval without assuming normally distributed or independent daily data.
The verdict compares that interval with the smallest change you said would matter, which you set
before the experiment starts. The details are in
[docs/product-spec.md](docs/product-spec.md#results-and-verdict) and
[`RandomizationTest.kt`](core/domain/src/main/kotlin/com/nof1/core/domain/analysis/RandomizationTest.kt).
