# Acceptance and approval tests

Covers the two scenario-driven test layers: Cucumber acceptance tests and
approval (golden-fixture) tests. See [`testing.md`](testing.md) for the
other test layers and enforcement gates.

## Acceptance tests (Cucumber)

- `.feature` files (Gherkin) live in `/specs/features/`, one per feature,
  each generated from the matching `/specs/intent/<slug>.md`. They are
  copied onto the test classpath at build time (`testResources` config in
  `pom.xml`) so Cucumber can discover them via `@SelectPackages("features")`.
- Step definitions live in `src/test/java/com/swiftfaze/veil/steps/`.
- `RunCucumberTest` (`src/test/java/com/swiftfaze/veil/RunCucumberTest.java`)
  is a JUnit 5 `@Suite` that includes the Cucumber engine — because its name
  matches Surefire's `*Test.java` pattern, the whole Cucumber suite runs
  under plain `mvn test` alongside the unit tests.
- Run a single scenario: `mvn test -Dcucumber.filter.name="A newly created player starts as a Warrior"`
- `specs/features/default-player-class.feature` +
  `steps/DefaultPlayerClassSteps.java` are a worked example proving this
  wiring end-to-end — copy their shape for the next real feature.
- A `.feature` file generated ahead of its implementation (e.g. an intent
  covering several areas, spec-drafted all at once but implemented one
  area at a time) should be tagged `@pending` at the top, and excluded via
  `RunCucumberTest`'s tag filter — otherwise `mvn test` fails on undefined
  steps for scenarios that don't have an implementation yet. Remove the
  tag from a file only once its step definitions exist.
- A `.feature` file with no Java code path to exercise at all — a
  build-pipeline or OS-installer concern rather than application behavior
  (e.g. `installer-mods-bundling.feature`) — is tagged `@manual-verification`
  instead, and excluded by the same tag filter. Unlike `@pending`, this
  exclusion is permanent: the file is the human-reviewed spec of record
  (per root `CLAUDE.md`'s review table), but it's never meant to gain step
  definitions, so the tag is never removed. Verification happens by
  actually exercising the described behavior manually (e.g. building and
  running an installer), not via `mvn test`/`mvn verify`.
- A step definition that drives a Swing component's keyboard interaction
  by calling `component.getActionMap().get(actionName).actionPerformed(...)`
  directly is faster to write but bypasses Swing's real focus-routing
  chain (`WHEN_FOCUSED` vs. `WHEN_ANCESTOR_OF_FOCUSED_COMPONENT`, and
  which component the OS/window manager actually has focused) — a panel
  can pass every such scenario while a real key press does nothing, or
  gets swallowed by the wrong ancestor. For any feature where keyboard
  focus crosses a window boundary or a component hierarchy the step
  definitions don't already exercise via real input, add at least one
  scenario driven through genuine key events (a `java.awt.Robot`-based
  diagnostic, or `component.dispatchEvent(new KeyEvent(...))` against the
  actual focus owner) rather than relying only on the `ActionMap`
  shortcut for every scenario.
  - **This only works with a real, OS-focused window.** Confirmed
    empirically (issue #175): a bare `dispatchEvent` against a component
    with no real, visible, focused window does nothing —
    `WHEN_IN_FOCUSED_WINDOW`/`WHEN_FOCUSED` bindings never fire. Getting
    real focus requires `new JFrame()` + `setVisible(true)` on an actual
    display, and `new JFrame()` itself throws `HeadlessException` when
    `GraphicsEnvironment.isHeadless()` — which is unconditionally true on
    this repo's actual CI (`ci.yml` runs `ubuntu-latest` with no `DISPLAY`
    and no Xvfb step). So a scenario built this way must guard itself with
    `Assumptions.assumeFalse(GraphicsEnvironment.isHeadless())` — skipped
    (not failed) in CI, but real coverage on any machine with a display
    (local dev, a future CI with Xvfb added). Worked example:
    `GamePanelRealKeyEventTest`. Adding Xvfb to CI so this runs
    unconditionally is a deliberately separate decision (cost/flakiness
    tradeoffs), not bundled into whatever change adds the guarded test.

### Troubleshooting: cascading/flaky Cucumber failures

If `UndefinedStepException` starts appearing across scenarios in
feature files unrelated to whatever you just changed — not real
assertion failures, but steps Cucumber claims don't exist — **check for
duplicate or ambiguous step definitions first**, before environment
theories (JDK/tooling version mismatches, JaCoCo instrumentation,
parallel execution). Cucumber matches step text regardless of the
Given/When/Then keyword, so two methods annotated with the same literal
step text under different keywords is always a duplicate. A duplicate
poisons the *entire* glue registry for that test run, not just the two
colliding methods — which is exactly what produces this
misleading-looks-unrelated, run-to-run-varying failure pattern (which
scenario happens to trigger the collision first, and how far the
resulting registry corruption cascades, depends on execution order).
Find it with:
```
grep -ohE '@(Given|When|Then)\("[^"]*"\)' src/test/java/com/swiftfaze/veil/steps/*.java | sed -E 's/@(Given|When|Then)\("(.*)"\)/\2/' | sort | uniq -d
```
See `.claude/workflow.md`'s Step 5 guidance for the full rule (this check
is mandatory before reporting Step 5 done, not just a debugging tip).
If the same literal step text genuinely needs different behavior
depending on whether it's a setup precondition or a later assertion,
that's a sign the text needs rewording into two distinct steps, not that
duplicate annotations on the same text are safe — see
`UiComponentFrameworkSteps.theConfirmationPopupIsShown()` for the
correct single-method pattern (guard with `if (x == null) { build it }
else { just assert }`) when the same text is genuinely reused as both a
fresh-build precondition and a later assertion.

## Acceptance mutation

PIT mutates production code; nothing else tests the `.feature` files. A step
definition that ignores its `{int}` argument passes every scenario. The
acceptance mutator changes each literal in a scenario and expects that scenario
to fail. Spec: `specs/features/acceptance-mutation.feature`. It is test-scope
(`com.swiftfaze.veil.testing.aps`), a manual Step 6 command for the hardener
like PIT, not a CI gate.

```
bash .claude/tools/pit-lock.sh mvn -q exec:java -Dexec.classpathScope=test \
  -Dexec.mainClass=com.swiftfaze.veil.testing.aps.AcceptanceMutator \
  -Dexec.args="--feature <slug>"
```

- **When:** hardener, Step 6, once per changed or added `.feature`
  (`--feature <slug>`, no `.feature` suffix). Omit `--feature` only for a
  whole-suite baseline; runtime grows with literals x scenario cost.
- **Mutations:** integers +1/-1, quoted strings swapped for another quoted
  value in the same file, each `Examples` cell on its own row. Features and
  scenarios tagged `@pending` or `@manual-verification` are skipped and listed.
- **Exit codes:** `0` no survivors; `3` a mutant survived; `2` something could
  not be checked (unknown slug, scenario failing unmutated, run found no test).
  `2` wins over `3`.
- **Reading survivors:** `SURVIVED  <file>:<line>  <original> → <mutant>` means
  the scenario still passed with that literal changed, so a step (or an
  assertion) ignores it. Fix the step definition to use the value, or the
  scenario to assert it; a survivor that is genuinely equivalent is named as
  such in the report, not left silent.
- **Do not pass `-Dcucumber.features`.** The engine lets that property
  override every selector, so each nested run would execute those features
  instead of the mutant; the runner throws if it is set.
- Nested runs use their own glue package and thread, and the mutator's own
  fixtures live in `src/test/resources/aps-fixtures/` (outside `features/`),
  so they never join the real suite.

## Approval tests

Approval tests compare rendered output (the ASCII glyph grid) against committed
baseline fixtures. They catch rendering regressions — camera viewport edges,
building footprints, entity layering, glyph grid content — that humans tend to
miss during playtesting but that can be checked automatically.

### How approval tests work

- **Fixture location:** `src/test/resources/approved/<scenario-name>.approved.txt`
- **Mismatch file:** `src/test/resources/approved/<scenario-name>.received.txt`
  (created only if the actual output differs from the approved fixture)
- **Behavior:** `ApprovalCheck.verify()` renders a scene to a `char[][]` grid,
  converts it to text (rows joined by `\n`), and compares against the committed
  fixture. On match, any stale `.received.txt` is cleaned up and the test passes.
  On mismatch, `.received.txt` is written, an assertion failure shows both grids
  for side-by-side inspection, and the test fails.
- **Re-approval:** After validating that a `.received.txt` is correct (e.g.
  because a fixture was deliberately updated), promote it to `.approved.txt`:
  ```
  mvn compile exec:java -Dexec.mainClass=com.swiftfaze.veil.testing.approval.ApprovalReapprove
  ```
  This command scans for all `.received.txt` files in the approved fixtures
  directory, replaces their `.approved.txt` siblings with the received content,
  and deletes the `.received.txt` files. It is never run automatically — re-approval
  is always explicit. After running it, `mvn test` will pass if the new fixtures
  are correct.
- **Line endings:** Approved fixtures must use LF (not CRLF) for byte-exact
  comparison across platforms. This is enforced in `.gitattributes`:
  ```
  src/test/resources/approved/*.txt text eol=lf
  ```
  When you commit an approved fixture, Git normalizes its line endings to LF
  regardless of the OS's default.

### Writing approval test scenarios

Use the patterns from `specs/features/approval-tests-glyph-grid.feature`:
- Build a small `WorldScene` with hand-built test-double tiles (not real mod
  content — see `WorldSceneTest` for the pattern).
- Create a `Camera` with the viewport size you want to test.
- Call `scene.renderToGrid(new Viewport(camera.getX(), camera.getY(),
  camera.getViewportWidth(), camera.getViewportHeight()), entities)` to produce
  the glyph grid. `renderToGrid` takes a `Viewport` (a small record in
  `com.swiftfaze.veil.world`) and a `List<? extends PositionedGlyph>` rather
  than `Camera`/`DrawableAsciiEntity` directly, so the seam has no dependency
  back on the root `com.swiftfaze.veil` package — see
  `docs/testing-module-dependency.md`.
- Convert the `char[][]` to a text fixture: join each row into a line, rows
  into a single string with `\n` between them.
- Call `ApprovalCheck.verify(scenarioName, gridAsText)` to compare and
  record the result.
