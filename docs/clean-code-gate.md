# The Clean Code gate

One command, run identically by the agent doing the work and the agent
checking it:

```
bash .claude/tools/check-clean.sh
```

## Why it exists

The recurring failure is a subagent reporting "done" and the orchestrator
bouncing it for failing tests, smells, or Clean Code violations. That loop is
expensive because the two sides were checking *different things* — the subagent
checked whatever it remembered from its handoff prompt, the orchestrator checked
whatever it thought to look for. Same script, same rules, same output closes the
gap by construction: there is nothing the orchestrator can reject the work for
that the subagent could not have seen first.

This is `CLAUDE.md`'s "if it is mechanically checkable, write the check, not the
rule" applied to `uncle-bob-craft`. The prose skill still owns the judgment;
everything a tool can decide has moved here.

## Scope: added lines, not whole files

By default the gate judges **only the lines your change added**, measured
against the merge-base with `develop`, including uncommitted and untracked
work. Two reasons:

- The rules in `.pmd-clean-code.xml` find ~1000 violations across the existing
  codebase. Binding them to `mvn verify` would have turned `develop` red the day
  they landed, and a gate that is red on arrival teaches everyone to ignore it —
  the same reasoning as the budget baselines in `docs/instruction-files.md`.
- You are accountable for the lines you wrote, not for the file you happened to
  open.

New files are included even before `git add`, because an agent's brand-new class
is untracked and `git diff` cannot see it.

## Running it

**Subagent, before reporting a task finished** (mandatory, see
`.claude/workflow.md` Step 4) **and orchestrator, verifying a subagent's
report** — the identical command, from the same branch (a worktree: run it
there, or pass `--base <the branch point>`):

```
bash .claude/tools/check-clean.sh
```

Exit 0 means the mechanical checks pass. It does **not** mean you are done: the
run ends by printing a judgment checklist you still owe answers to.

During the edit loop, `--fast` skips `mvn verify` for a quicker signal. A
`--fast` run is never sufficient to report a task finished, and the script says
so in its own output.

Orchestrator, what to check in the report:

1. Exit status is 0.
2. Every **advisory** finding the script listed has a disposition in the report
   — fixed, or one line saying why it is correct as written.
3. Every **judgment checklist** line has a PASS/FAIL *and evidence naming a
   file, function, or test*. "All good" is not an answer, and a checklist
   returned without evidence is the same signal as a skipped step.

`implement-issue`'s "Verifying what comes back" still applies: a report is not evidence. The
point of this tool is that verifying it costs one command.

Other flags: `--all` (whole repo, for baselining — not the gate), `--files`
(whole changed files rather than added lines), `--base <ref>`.

## What is checked, and how far it automates

"Automatable" means a tool decides pass/fail with no human reading the code.

| # | Concern | Automation | Enforced by |
|---|---|---|---|
| 1 | Method length (>40), cyclomatic complexity (>8), parameters (>4) | Full | `.pmd-minimal.xml`, repo-wide at `mvn verify` |
| 1 | Nesting depth (>3), cognitive complexity (>15), NPath (>200) | Full | `.pmd-clean-code.xml` |
| 2 | SRP — method/field/public counts, fan-out, God class | **Partial** — proxies for cohesion, not proofs | `.pmd-clean-code.xml` (advisory) + checklist |
| 3 | Naming conventions, short/long names, linguistic (`isX` returns boolean) | Full | `.pmd-clean-code.xml` |
| 3 | Abbreviations | **Partial** — closed deny-list | `VeilAbbreviatedName` (advisory) |
| 3 | Intent-revealing names | **Judgment** | Checklist |
| 4 | Duplicated blocks (50+ tokens), repeated literals | Full | CPD + `AvoidDuplicateLiterals` |
| 5 | Commented-out code, TODO/FIXME, empty bodies, oversized comments | Full | script + PMD |
| 5 | Comments explaining *what* instead of *why* | **Judgment** | Checklist |
| 6 | Missing asserts, assert count, `@Test` misuse, control flow in tests | Full / partial | `.pmd-clean-code.xml` |
| 6 | FIRST, AAA structure, one reason to fail | **Judgment** | Checklist |
| 7 | Unused fields, methods, locals, parameters, assignments, imports | Full | `.pmd-clean-code.xml` |
| 8 | Swallowed/broad catches, lost stack traces, `printStackTrace`, `println` | Full | `.pmd-clean-code.xml` |
| 8 | Magic numbers and strings | **Partial** — literals are legitimate in a renderer | `VeilMagicNumber` (advisory) |
| 9 | Between-package structure — cycle freedom, public mutable static state, instantiation confined to composition roots | Full (frozen baseline) | `ModuleDependencyTest` (ArchUnit, `FreezingArchRule`) |
| 10 | Exposed mutable references, equals/hashCode contract, null-deref paths | **Full** (high-precision, blocking) | SpotBugs: `EI_EXPOSE_REP`, `EI_EXPOSE_REP2`, `HE_EQUALS_USE_HASHCODE`, `NP_*` family, `EC_UNRELATED_TYPES` |
| 10 | Non-transient non-serializable fields on Serializable, ignored return values, self-assignment | **Full** (high-precision, blocking) | SpotBugs: `SE_BAD_FIELD`, `RV_RETURN_VALUE_IGNORED*`, `SA_SELF_ASSIGNMENT` |
| 10 | Collection/loop/inheritance misuse (exception softening, literal string comparison, overridable calls in constructors, etc.) | **Partial** — heuristic proxies, not proofs | fb-contrib detectors (advisory) |
| 11 | Full PMD 7 catalogue (#215) — 196 native rules the ad hoc set above never covered: reassignment, redundant constructs, exception/clone/finalize misuse, thread-safety, string/collection performance, `MutableStaticState`, `DataClass`, `LawOfDemeter` | Full | `.pmd-clean-code.xml` §9 |
| 12 | Vendored jPinpoint rules (#215) — equals/hashCode consistency, regex recompilation, per-call allocation, suppression hygiene | Full | `.pmd-clean-code.xml` §10 (Apache 2.0, `PMD-jPinpoint-rules`) |
| 13 | Default-charset and default-locale JDK calls (`String.format`, `toLowerCase`, `getBytes`, `FileReader`, etc.) and deprecated JDK API use | Full | forbiddenapis `jdk-unsafe` + `jdk-deprecated`, repo-wide (main + test) at `mvn verify` |

Test-quality rules run only against `src/test`; SRP rules only against
`src/main`. `AvoidInstantiatingObjectsInLoops` (row 10, performance) is scoped
to the render path (`GamePanel`, `PatternFieldWidget`) rather than repo-wide —
see "Known carve-outs" below. The script routes all of these by file path.

### Blocking vs advisory

**Blocking** rules are high-precision: a hit is a defect. They fail the gate.

**Advisory** rules are heuristic proxies — a 2D renderer legitimately contains
numbers, a tile coordinate is legitimately called `x`, and a class can pass
every count and still do two things. They print with `file:line` and must each
be dispositioned in the completion report, but they do not fail the gate on
their own.

Advisory is not "ignorable". An advisory finding with no disposition is an
incomplete report.

## CRAP (per-method complexity × coverage)

Row 1's caps are independent: a CC-8 method at 0% coverage passes both.
`CrapReport` (test scope, `check-clean.sh` section 6,
`specs/features/crap-gate.feature`) links them per method:

    CRAP(m) = CC(m)^2 * (1 - cov(m))^3 + CC(m)

At 100% coverage a method passes up to CC 10 (`crap.max`,
`quality-gates.properties`); at 0% only up to CC 2. `target/crap/crap.txt`
lists every method worst-first with its complexity, coverage and score.

9 methods were already over the limit when the gate landed; they're
baselined in `crap-baseline.txt` (`<FQCN>#<method> <score>`) and pass at or
under their recorded score. Shrink-only, ratcheted like the other gates'
floors — see `docs/testing-quality-gates.md`.

**To lower a score:** decomposition moves complexity to the extracted method,
it doesn't remove it — CRAP falls only when a branch is actually eliminated
or the method gains real coverage. A split helps only when the *caller's*
complexity also drops, e.g. a CC-8 method becoming a CC-2 orchestrator plus a
fully-covered CC-6 helper.

## Known carve-outs, and why

Each of these was measured against this repo, not assumed:

- **Find Security Bugs is excluded from SpotBugs.** Veil is an offline single-player desktop game with no network surface and no untrusted input beyond local mod JSON. That detector set (SQL injection, XSS, command injection, insecure cryptography, etc.) is built for web applications and would report almost entirely inapplicable noise. Only the core SpotBugs + fb-contrib detectors are loaded.
- **SpotBugs `threshold` is `Medium`, not `High`.** Measured at `effort=Max`:
  `High` misses `EI_EXPOSE_REP`/`EI_EXPOSE_REP2` entirely (0 of 75 pre-existing
  instances); `Medium` catches all 75 plus 269 total findings; `Low` adds only
  lower-precision noise on top of `Medium`, no more `EI_EXPOSE_REP`/`REP2`
  instances. `Medium` is the smallest threshold that still reports every rule
  `check-clean.sh` classifies as blocking.
- **`x y z w h dx dy dw dh cx cy g g2 e id`** are exempt from the short-name
  rule — measured clearer than any longer name at their sites (tile
  coordinates, `Graphics2D`, `ActionEvent`). `r`/`c` are still flagged; their
  sites were genuine "what is this?" locals.
- **`logger` / `log`** are exempt from the constant-name pattern — the slf4j
  convention the repo already follows.
- **`VeilNoLogicInTests` is advisory**, not blocking. It fires on
  `NoDuplicateStepDefinitionsTest`, a structural test that legitimately loops.
- **Assert ceiling is 3, not 1.** A strict 1 flags legitimate "assert the value
  AND that it was persisted" pairs. The checklist enforces the real rule — one
  *concept* per test.
- **`GenericsNaming` and `AvoidLosingExceptionInformation` are absent.** PMD
  7.17 reports both as scheduled for removal in PMD 8.
- **PMD bumped from 7.17.0 to 7.27.0 (#215).** 18 of the 196 native rules the
  full-catalogue audit found only exist from PMD 7.2x onward. Overridden via
  `pmd.version` in `pom.xml`; also shifted `ExcessiveParameterList`'s
  `minimum` back to minimum-to-trigger (`>=`), so `.pmd-minimal.xml` moved
  4→5 to keep the same effective threshold — see that file's own comment.
- **Not yet in any released PMD, so absent:** `OnDemandImport`,
  `TypeNameMismatch`, `CStyleArrayDeclaration`, `LongLiteralEndingWithLowercaseL`.
- **`AvoidInstantiatingObjectsInLoops` is scoped to the render path**
  (`GamePanel`, `PatternFieldWidget` — the only classes overriding
  `paintComponent`/`paint`), not repo-wide: allocation in a ~60fps paint loop
  is a real defect, in a cold loop (JSON building, mod loading) it is not.
  `check-clean.sh`'s `RENDER_SCOPED_RULES` routes this like `TEST_ONLY`/
  `MAIN_ONLY` route test-quality and SRP rules; repo-wide count measured
  before narrowing was 27 violations across 9 files, none in either scoped
  file — see `impacts.md`.

## Suppressions

`@SuppressWarnings("PMD...")` on an added line fails the gate. "Fixed" means
decomposed. The single documented exception is `ExcessiveParameterList` on a
method overriding a JDK/library interface whose signature mandates 5+
parameters — see `docs/testing-quality-gates.md` § "Code quality gates".

If a rule is genuinely wrong for your case, **stop and report the blocker**.
Do not suppress it, and do not report the task done with a caveat.

## The judgment checklist

Printed by the script when the mechanical checks pass. These cannot be
automated; they are the part `uncle-bob-craft` still owns. Answer every line
with PASS or FAIL **and one clause of evidence naming a file, function or
test**.

**SLAP — one level of abstraction per function.** FAIL if any function you
added both calls named steps *and* does its own detail work (index arithmetic,
string building, null checks) in the same body.
*Pass looks like:* `renderRow` calls `drawGlyph`/`drawBorder` and contains no
arithmetic of its own. *Fail looks like:* `renderRow` calls `drawGlyph` and also
computes `x * TILE + offset - 1` inline.

**SRP — one sentence, no "and".** FAIL if describing any class you touched needs
"and" to be accurate.
*Fail looks like:* "parses the mod JSON **and** registers the entities".

**Naming — understandable without the implementation.** FAIL if a reader must
open the body to know what a name holds or does, or if any name needed a comment
to explain it. Renaming is the fix, not commenting.

**Why-not-what comments.** FAIL if any comment you added restates the code below
it. A comment earns its place by explaining a decision, a constraint, or a
non-obvious consequence.
*Fail looks like:* `// increment the counter` above `counter++`.
*Pass looks like:* `// PMD's minimum is strictly-greater-than, verified 2026-09-06`.

**Test intent — one reason to fail.** FAIL if any test you added could fail for
two unrelated reasons, or if its name does not say which reason.
*Fail looks like:* `testPlayer()`. *Pass looks like:*
`movingRightIncreasesX()`.

**AAA structure.** FAIL if arrange/act/assert are not visibly in that order, or
an assertion appears before the act.

**No new debt.** FAIL if you added a code path that exists only to make a test
pass, or an abstraction with a single caller added "for later".

<!-- added 2026-09-28: ArchUnit sees only the direction of a dependency, not a rule worked out twice -->
**Single answer — the UI translates, it doesn't decide.** For each thing a
changed `com.swiftfaze.veil.ui` class shows or decides, name the engine method
that supplies it. FAIL if UI code re-derives an answer the engine already owns
(is this tile walkable, which menu item is next): every arrow still points the
right way, but the rule now exists twice and the copies drift. Fix by calling
the engine, not by moving the duplicate. Also FAIL for the converse: an engine
method only tests call while the UI reimplements it.

## Thresholds are dials

Every number here is a dial set against this codebase, not a law. If you change
one, change it in `.pmd-clean-code.xml` **and** in the table above in the same
commit, and say why in the commit message. Do not loosen a threshold because one
change did not fit under it — that is the decomposition signal working.
