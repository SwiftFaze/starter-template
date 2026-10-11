# Mutation testing and code-quality gates

Covers the enforcement layers on top of the test suites themselves: PIT
mutation testing, PMD/JaCoCo, and the Error Prone/NullAway compile-time
gates. See [`testing.md`](testing.md) for the test layers themselves.

## Mutation testing (workflow Step 6)

- Runner: PIT (`org.pitest:pitest-maven`, with `pitest-junit5-plugin` so it
  runs through the JUnit Platform and picks up both plain unit tests and
  Cucumber scenarios).
- Run it: `bash .claude/tools/pit-lock.sh mvn org.pitest:pitest-maven:mutationCoverage`
  — the lock allows one mutation run (PIT or the acceptance mutator) at a time
  across all worktrees, since parallel runs overload the dev machine; the
  `pit-guard.sh` hook in `.claude/settings.json` blocks a run without it. HTML report at
  `target/pit-reports/index.html` (per-package/per-class breakdowns,
  including a *line* coverage figure distinct from mutation score).
- `<targetClasses>`/`<targetTests>` in `pom.xml` scope this to classes with
  real unit tests — pure Swing view/wiring classes with no meaningful unit
  coverage (`Main`, layout-only panels, `ClassSandbox`'s UI entry point)
  are excluded rather than left to report a wall of untested mutants.
- This is the check on the unit tests themselves (CLAUDE.md's changed-file
  coverage constraint is easy to satisfy with weak assertions; mutation
  score catches that) — not a substitute for acceptance tests or the
  Step 4.5 manual playtest.
- PIT mutates `src/main` bytecode only, so test-scope tooling
  (`testing.aps`, `testing.qa`, `testing.quality`, `testing.uml`) cannot be a target, and the acceptance features have their own
  mutator: [`testing-acceptance.md`](testing-acceptance.md#acceptance-mutation).
- `<mutationThreshold>` in `pom.xml`: 51% (measured baseline 1088/2115
  mutations killed). Ratcheted by the same `check-quality-gates.sh` as the
  JaCoCo floors below. Stays a manual command, not bound to `mvn verify`.

## Code quality gates (PMD and JaCoCo)

- Both gates are bound to `mvn verify` and fail the build if violated.
- **PMD (maven-pmd-plugin)** enforces design rules via `.pmd-minimal.xml`
  (complexity, method length, parameter count — see `.claude/workflow.md`'s
  "Constraints" section for the exact current numbers, the single source of
  truth for these) plus CPD flagging duplicate code at 100+ tokens. Pure
  Swing layout/wiring classes with no branching logic are excluded (`pom.xml`'s
  PMD excludes list has the full list and rationale).
- The one narrow, already-practiced suppression exception (general rule in
  `.claude/workflow.md`'s Constraints section): a method overriding a JDK/
  library interface whose signature mandates more parameters than the
  enforced ceiling — e.g. `Border.paintBorder(...)` (6 params, see
  `RadioGroupWidget.RadioOptionBorder`/`TableWidget.AccentableCellBorder`) or
  `DocumentFilter.replace(...)` (5 params, see
  `PatternFieldWidget.AllowedCharacterFilter`) — may carry
  `@SuppressWarnings("PMD.ExcessiveParameterList")` plus a comment naming the
  interface. Parameter count only; never complexity, length, or coverage.
- **JaCoCo (jacoco-maven-plugin)** enforces coverage floors at two scopes, both
  measured from a real baseline and ratcheting up only — enforcement is
  `.claude/tools/check-quality-gates.sh`, run as `repo-hygiene.yml`'s
  `quality-gate-ratchet` job on every PR; this list is not the source of
  truth, that check is:

  | Scope | Counter | Floor | Measured baseline |
  |---|---|---|---|
  | `BUNDLE` (repo-wide) | `LINE` | 85% | 90.34% |
  | `BUNDLE` (repo-wide) | `BRANCH` | 78% | 78.98% |
  | `SOURCEFILE` (per file) | `LINE` | 35% | 35.7% (`FillLayout.java`) |
  | `SOURCEFILE` (per file) | `BRANCH` | 50% | 50.0% (`FillLayout.java`) |

  The per-file scope is `SOURCEFILE`, not `CLASS`: JaCoCo's `CLASS` element
  counts every compiled `.class` file, so a nested/anonymous inner class is
  judged as its own unit — measured on `develop`, the worst such rows are
  incidental helpers of already-tested widgets, which forces any green
  `CLASS` floor to ~0% (see `specs/intent/coverage-gate-floors-class-scope.md`
  for the full derivation). `SOURCEFILE` judges the `.java` file someone
  actually adds, needing no per-inner-class exclusion list.
  Excludes are shared with PMD's list above, plus a `$*` sibling glob per
  entry (a bare glob only matches the outer class, not its nested/anonymous
  ones) — see `pom.xml`'s `jacoco-check` execution for the full list.
- **Mutation testing** (above) also carries a ratcheted `mutationThreshold`,
  enforced by the same `check-quality-gates.sh`.
- **CRAP** (per-method complexity × coverage, `check-clean.sh` section 6) has
  its own `crap.max` and `crap-baseline.txt` ratchets in the same script —
  formula, baseline and how to lower a score: `docs/clean-code-gate.md`
  § "CRAP (per-method complexity × coverage)".

## Compile-time gates (Error Prone + NullAway)

- Wired into `maven-compiler-plugin` (`pom.xml`), bound to `mvn compile`
  directly (not `verify`, not a profile) so a violation fails the compile
  an agent already runs. JDK17 flags and a Windows fork/batch-file gotcha
  (`google/error-prone#4256`) are documented in that plugin's own `pom.xml`
  comment, not repeated here.
- Error Prone runs repo-wide: its default ERROR tier, plus every other check
  [`error-prone-checks.md`](error-prone-checks.md) marks **ADD**, each raised
  with its own `-Xep:<Check>:ERROR` flag in `pom.xml` (#219). Its data file,
  [`error-prone-checks.tsv`](error-prone-checks.tsv), has a row for every check
  not already at ERROR by default, with the reason for ADD or EXCLUDE. EXCLUDE takes one of four reasons only: no surface here,
  contradicts another enabled check or PMD rule, needs a new dependency, or
  deprecated/no-op. Noisy or style-only is not one: fix the code, never demote
  a check or add `@SuppressWarnings` for it.
- **A check added by a future Error Prone version** stays at its default
  (opt-in, so nothing turns on silently). On a version bump, list the new
  checks (`BuiltInCheckerSuppliers` in the new jar), give each a row in
  `error-prone-checks.tsv` by the same four reasons, add a flag for each ADD,
  and fix what it finds in the same change.
- NullAway runs in JSpecify mode (`-XepOpt:NullAway:JSpecifyMode=true` with
  `-XepOpt:NullAway:OnlyNullMarked=true`, not `AnnotatedPackages`).
  Coverage starts at `com.swiftfaze.veil.world` and `com.swiftfaze.veil.entities`
  (plus its `buildings`, `items`, `player`, `player.classes`, and `quests`
  subpackages), each carrying its own `@NullMarked` `package-info.java` -
  `@NullMarked` does not cascade to subpackages, so every covered package
  needs one, chosen for value (a null tile/entity field is where null
  crashes the game), not ease. **Only grows, never shrinks** - same ratchet
  as the ArchUnit store (`docs/testing-module-dependency.md`). To extend:
  add a `package-info.java` annotated `@NullMarked` to the new package, run
  `mvn compile`, fix every finding for real (annotate a genuinely-optional
  reference `@Nullable`, don't reach for a blanket
  `@SuppressWarnings("NullAway")` - that silences the whole method).
- `@Nullable` is `org.jspecify.annotations.Nullable` - use this one
  everywhere, not a per-file choice.
- Measured cost: clean `mvn compile` ~11.7s -> ~24.2s (+~12.5s) on the
  measuring machine - re-measure if it grows further as the list widens.
