# Testing

Three test layers, run at different points, kept in separate file-naming
conventions so Maven can tell them apart automatically. Enforcement layers
on top of them (mutation testing, PMD/JaCoCo, Error Prone/NullAway, the
ArchUnit module-dependency gate) live in their own files below — this file
stays the short index once those grew past a single-file instruction budget.

## Unit tests

- Location: `src/test/java/**/*Test.java`
- Runner: Surefire, bound to `mvn test`
- Fast, no real I/O — the norm for new production code.
- Run a single test: `mvn test -Dtest=PlayerTest#movingRightIncreasesX`
- Property-based testing with jqwik — see [`testing-property-based.md`](testing-property-based.md).

## Acceptance and approval tests

Cucumber `.feature` scenarios and golden-fixture (glyph-grid) rendering
tests — see [`testing-acceptance.md`](testing-acceptance.md).

## Integration tests

- Location: `src/test/java/**/*It.java` (`It`, not `IT`: Error Prone `IdentifierName` treats acronyms as words; `pom.xml` points Failsafe at this suffix)
- Runner: Failsafe, bound to `integration-test`/`verify` — **not** run by
  plain `mvn test`. Run them with `mvn verify`.
- Reserved for tests that need real I/O or cross-class wiring that unit
  tests shouldn't pay for on every run (e.g. `ModLoaderIt`, which loads
  actual mod content off disk instead of mocking the file read).
- Run a single integration test: `mvn verify -Dit.test=ModLoaderIt`

## QA runs

Local-only key replay: `QaRunner` boots the real game, presses the keys of a
procedure as real key events and checks the game event log. It needs a desktop
and window focus, so it is not in CI or `check-clean.sh`. It narrows the Step
4.5 playtest to feel and rendering; it does not replace it.

- **Procedure** = `specs/qa/<slug>.keys` (one `KeyEvent.VK_` name per line, `#`
  comments) plus `specs/qa/<slug>.json`
  (`{"script": "<slug>.keys", "expect": [{event}, ...]}`); examples in
  `specs/qa/`. Event types are in `specs/features/game-event-log.feature`.
- **Run one:** `mvn -q test-compile exec:java -Dexec.classpathScope=test
  -Dexec.mainClass=com.swiftfaze.veil.testing.qa.QaRunner "-Dexec.args=<slug>"`;
  use `"-Dexec.args=--all"` for every procedure. One PASS/FAIL line each; exit
  non-zero if any failed.
- **Matching:** `expect` must appear in the recorded log as an in-order
  subsequence; other events in between are fine.
- **Reading a failure:** the diff names the first expected event that was not
  matched (`#n of m`) and every event recorded after the last match. A wrong
  key name or missing file fails before any window opens.
- **Opt-out:** a feature with no key-driven behaviour says `QA: none - <reason>`
  in its `Feature:` block. `bash .claude/tools/check-qa-coverage.sh` blocks a
  changed `.feature` that has neither a procedure nor that line.
- Spec and rationale: `specs/features/qa-key-replay.feature`.

## Everything together

`mvn verify` runs all three layers: unit tests and the Cucumber suite via
Surefire, then integration tests via Failsafe.

## Enforcement gates

- [`testing-quality-gates.md`](testing-quality-gates.md) — mutation testing
  (PIT), PMD/JaCoCo, and the Error Prone/NullAway compile-time gates.
- [`testing-module-dependency.md`](testing-module-dependency.md) — the
  ArchUnit module-dependency and package-cycle gate.
