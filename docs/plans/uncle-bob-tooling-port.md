# Plan: port Uncle Bob's tooling from Veilclj

Status: proposal, 2026-09-28. Turn into a milestone with `brainstorm-milestone`,
then delete this file. The issues are the tracking record, not this file.

Each item goes through the coder/hardener pipeline (`implement-issue`), set up
in #230.

## Why

Veilclj (`../Veilclj`) was built to try Robert C. Martin's real toolchain:
`crap4clj`, `clj-mutate`, `dependency-checker`, `deintroverter4clj` and
`uml-viewer`. The experiment worked, but the author can't read the Clojure, so
the UML view of it tells them nothing. This plan brings the workflow into the
codebase the author does understand.

## Gap analysis

What Veil already has, so none of these gets rebuilt:

| Veilclj | Veil equivalent | Verdict |
|---|---|---|
| `dependency-checker` (`bb layers`) | ArchUnit `ModuleDependencyTest` | Covered |
| `clj-mutate` (`bb mutate`) | PIT, `mutationThreshold` 51 | Covered |
| `dry4clj` (`bb dry`) | PMD CPD (`cpd-check`) | Covered |
| `test.check` property specs | jqwik + `check-jqwik-canary.sh` | Covered |
| `gate-ratchet` | `check-quality-gates.sh`, which is stricter: it also checks excludes, `@Generated` and ArchUnit ignore files | Covered |
| SCRAP (spec structure) | Full PMD catalogue (#226), including its JUnit rules | Mostly covered, so skip |
| `deintroverter` (assertions that never touch `src`) | PIT: an assertion-free test kills no mutants | Mostly covered, so skip |
| `shell-check` (the Quil shell decides nothing) | None | Deferred. There is no mechanical Java version worth building yet. The "Single answer" judgment-checklist line (`docs/clean-code-gate.md`) covers the judgment part |

What to build, in dependency order:

| # | Item | Size | Depends on |
|---|---|---|---|
| 1 | CRAP gate | M | none |
| 2 | Game event log | M | none |
| 3 | QA key-replay procedures | L | 2 |
| 4 | Acceptance (Gherkin) mutation | M | none |
| 5 | UML viewer adapter | L | 1 (metrics); PIT for the mutation overlay |

Items 1, 2 and 4 are independent and can run in parallel worktrees.

---

## 1. CRAP gate

**Problem.** PMD caps cyclomatic complexity at 8 and JaCoCo sets an 85%
coverage floor, but only for the whole bundle, never per method. So a
complexity-8 method with 0% coverage passes both gates. CRAP is the metric that
links the two:

```
CRAP(m) = CC(m)^2 * (1 - cov(m))^3 + CC(m)
```

At 100% coverage a method passes up to CC 10. At 0% coverage it passes only
up to CC 2.

**Design.**
- Add `com.swiftfaze.veil.testing.quality.CrapReport`, test scope and no new
  dependency. It reads `target/site/jacoco/jacoco.xml`: each `<method>` element
  has `COMPLEXITY` and `LINE` counters, which is all the formula needs. Use JDK
  `javax.xml`.
- It writes two outputs:
  - `target/crap/crap.txt`: a human-readable table, worst first.
  - `.metrics/crap.edn`: the same shape as Veilclj's
    (`{:entries [{:name :namespace :complexity :coverage :crap}]}`), with
    `:namespace` set to the fully qualified class name. Item 5 reads this file.
    It's written as plain text, so there's no EDN library.
- Put the threshold in a new `quality-gates.properties` (`crap.max=10`), and
  extend `check-quality-gates.sh` to ratchet it.
- Bind it to `verify` with `exec-maven-plugin`, after `jacoco:report`. It fails
  when any method exceeds `crap.max`.
- Scope follows JaCoCo's existing `<excludes>`, so there's no second exclusion
  list to keep in sync.

**Rollout.** Measure first (`--report-only`). If `develop` has methods over 10,
choose between two options:
- **(a) Baseline file:** a ratcheted allowlist that can only shrink.
- **(b) Added-lines scope, like `check-clean.sh`.**

(a) is recommended: CRAP is per method and added-lines scoping fits it poorly.

**Wiring.** Add section *N* to `check-clean.sh` as a blocking check. Add a
section to `docs/clean-code-gate.md` covering how to split a method to lower
CRAP, carried over from Veilclj's `docs/clean-code-gate.md`. That doc is at
244/250, so trim it first.

**Done when.** `mvn verify` fails on a planted uncovered CC-4 method, a
lowered `crap.max` fails the ratchet CI job, and `.metrics/crap.edn` is
written.

## 2. Game event log

**Problem.** A QA replay needs to know what the game *did*, not just what it
drew. Veilclj emits domain events such as `:screen/changed` and
`:menu/selection-changed`. Veil has no equivalent: `GameListener` only has
UI callbacks (`updatePlayer`, `toggleInventory`, ...).

**Design.**
- Add `com.swiftfaze.veil.game.event.GameEvent`, a sealed interface with records
  (`ScreenChanged(from, to)`, `MenuSelectionChanged(to)`,
  `PlayerMoved(x, y)`, `GameOver()`).
- Add `GameEventLog`, an in-memory append-only list injected where the state
  changes happen. No-op by default. The QA runner enables it with
  `-Dveil.qaLog=<path>` and writes it out as JSON Lines through Gson, which
  the project already uses.
- It stays inside the engine packages, so ArchUnit's direction holds.
- Start with the events the first QA procedures need: main menu, map movement,
  inventory toggle. Add more as later procedures need them, never in advance.

**Done when.** Unit tests show each event fires once, at the right transition,
and `-Dveil.qaLog` writes a file.

## 3. QA key-replay procedures

**Problem.** The Step 4.5 human playtest is fully manual. Veilclj's `bb qa
<slug>` replays a key script in the real window and diffs the event log
against expectations. The human then only has to check feel and rendering.

**Design.**
- Each procedure is two files, `specs/qa/<slug>.keys` and
  `specs/qa/<slug>.json`:
  - `.keys` uses Veilclj's format: one key per line, `#` comments.
  - `.json` is `{"script": …, "expect": [{event…}, …]}`. It's JSON rather than
    EDN, to avoid adding a dependency.
- `com.swiftfaze.veil.testing.qa.QaRunner` (test scope) boots the real
  `GameWindow`, waits for focus with the same technique as
  `GamePanelRealKeyEventTest.waitForFocus`, and dispatches real `KeyEvent`s
  through the focused component, so input goes through Swing's focus
  machinery. It then compares the event log (item 2) with `expect` as an
  in-order subsequence.
- Command: `mvn -q exec:java -Dexec.mainClass=…QaRunner -Dexec.args=<slug>`
  (or `--all`). It opens a window, so it's local-only: not in CI and not in
  `check-clean.sh`, the same as in Veilclj.
- Opting out: a feature with no key-driven behaviour puts
  `QA: none - <reason>` in its `Feature:` block.
- Write procedures for `main-menu`, `map-movement` and `inventory-toggle`
  first.

**Wiring.** Add the QA step to `implement-issue`. #230 left it out
because nothing existed to run yet. Add a "QA runs" section to
`docs/testing.md`.

**Done when.** `QaRunner --all` passes on `develop`, and a planted wrong
expectation fails it with a readable diff.

## 4. Acceptance (Gherkin) mutation

**Problem.** PIT mutates the code to test the unit tests. Nothing tests the
acceptance tests themselves: if a step definition ignores its `<n>` argument,
every scenario still passes. Veilclj's `bb acceptance-mutate` changes each
literal in a scenario and expects the suite to fail.

**Design.**
- `com.swiftfaze.veil.testing.aps.AcceptanceMutator` (test scope). It parses
  `.feature` files with `io.cucumber:gherkin`, which is already on the
  classpath through `cucumber-java`. Confirm with `mvn dependency:tree`; if it
  turns out to be a new direct dependency, **ask first**.
- Mutations:
  - Integers: +1 and −1.
  - Quoted strings: swap in another value used in the same file.
  - Each `Examples` row cell.
- For each mutant, run only that scenario through the Cucumber JUnit Platform
  launcher, in-process and filtered by name. A mutant that passes has
  **survived**.
- Report survivors as `file:line  original → mutant`, and exit 3 if any
  survived. Like PIT, this is a manual command for Step 6, not a CI gate.
- Runtime: in-process runs keep it at seconds per feature. Add
  `--feature <slug>` so the hardener can target only the features that
  changed.

**Done when.** A planted step that ignores its argument produces a survivor,
and every current feature reports none (or each survivor becomes an issue).

## 5. UML viewer adapter

**Problem.** The goal is Uncle Bob's live viewer, with its CRAP and mutation
colouring, running on Veil's Java code.

**Findings.** The viewer (`io.github.unclebob/uml-viewer`) is
language-neutral. Its README says: "Register another implementation with
`(graph/register! :java my-java-scanner)`", and a scanner returns
`{:classes [{:id :name :ns :stereotype}] :edges [{:from :to :kind}]}`, where
`:kind` is `:dependency` or `:implements`. Metrics come from `.metrics/`,
keyed by class `:ns`.

**Design (spike first).** There are two routes. The spike picks one.
- **(a) A Java exporter writes the IR directly (recommended).**
  `com.swiftfaze.veil.testing.uml.UmlExport` uses ArchUnit's
  `ClassFileImporter` (already a dependency) to walk `com.swiftfaze.veil..`,
  and writes `target/uml/veil.edn` with classes, `:implements` edges and
  `:dependency` edges, and `:ns` set to the class's fully qualified name. The
  viewer runs through a tiny `tools/uml/deps.edn` with its `:uml` alias.
  **Ask first:** that's a Clojure CLI toolchain in a Java repo, not a Maven
  dependency, but it's still new tooling.
- **(b) A Clojure `:java` scanner registered with the viewer.** It keeps the
  policy and generator pipeline intact, but it's Clojure code in a Java repo,
  which is the exact problem being solved.
- Metrics:
  - CRAP: `.metrics/crap.edn` from item 1.
  - Mutation: convert PIT's `mutations.xml` (enable the `XML` output format
    next to `HTML`) into the shape of Veilclj's `.metrics/mutate/`, keyed the
    same way. Check that shape against `uml-viewer`'s overlay code during the
    spike.
- Policy: `docs/uml/veil.policy.edn` with the levels from
  `docs/architecture.md` (engine packages below `ui`, `ui` below `Main`).
  The viewer draws a layer violation in red, in the same place ArchUnit
  catches it.

**Only worth it once you can read the code.** The viewer shows structure; it
doesn't explain it. Pair the first run with a walkthrough of
`docs/architecture.md` against the diagram.

**Done when.** `mvn verify` followed by the viewer command opens a diagram of
the Veil packages with CRAP colouring, and a planted `ui` → `Main` dependency
shows up red.

## Not ported

- **Jev / `single-answer`.** A Veilclj spike, and not Uncle Bob tooling.
- **`bb` as the command surface.** Maven is Veil's command surface already.
- **`update-tools`.** Dependabot and Release Please already cover dependency
  pinning here.
