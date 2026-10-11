@manual-verification
Feature: Quality-gate bypass detection (ArchUnit ignore file, @Generated usage)
  `.claude/tools/check-quality-gates.sh` (`quality-gate-ratchet.feature`, née
  `pmd-jacoco-quality-gates.feature`'s successor) already catches a lowered
  JaCoCo/PIT threshold or a newly added exclusion-list entry — every place a
  gate can be weakened by editing `pom.xml` in a way a reviewer would
  actually see in that file's diff. Issue #199 (Error Prone/NullAway) added
  a new, legitimate way to bypass a gate on purpose:
  `@SuppressWarnings("NullAway")` on a specific line, visible exactly where
  it takes effect — same shape as the three existing
  `@SuppressWarnings("PMD.ExcessiveParameterList")` uses already accepted in
  the widget classes for JDK/Swing interface signatures that mandate more
  parameters than the ceiling. This file is not about those; it is about
  the two bypasses in the same family that do NOT show the harm where the
  change is, and so were never caught by anything:

  - **An `archunit_ignore_patterns.txt` file** anywhere in the tree makes
    ArchUnit silently skip every violation matching its regex patterns.
    Nothing about the filename announces that it disables rules, and it
    does not require touching `pom.xml` or the frozen violation store at
    all.
  - **An `@Generated`-annotated class or method** in `src/main/java`. JaCoCo
    (0.8.2+) and PIT both drop anything carrying an annotation whose SIMPLE
    NAME is `Generated` (CLASS/RUNTIME retention) from the coverage
    denominator, regardless of which package the annotation comes from
    (`javax.annotation`, `jakarta.annotation`, `lombok`, or a hand-rolled
    one) — the cheapest way to raise a coverage score with no threshold or
    exclusion-list change to review.

  Both checks are delta-scoped against the base branch, matching the
  ratchet posture the rest of `check-quality-gates.sh` already uses: a
  genuinely justified use of either mechanism must still be possible, it
  just cannot land silently — the check fails loudly and a human has to
  read the diff and approve past it, with the reason stated in the PR.

  Background:
    Given `check-quality-gates.sh` resolves the base ref the same way it already does for `pom.xml` (`origin/$BASE_REF` falling back to `$BASE_REF`)
    And the current branch is not `develop` or a `release-please--branches--*` branch (both already exempt from every check in this script)

  Scenario: Adding an archunit_ignore_patterns.txt file fails the gate
    Given a change adds a file named `archunit_ignore_patterns.txt` anywhere in the tree, not present on the base ref
    When `check-quality-gates.sh` is run
    Then it reports a BLOCKING error naming that file's path
    And the script exits non-zero

  Scenario: Removing an archunit_ignore_patterns.txt file does not fail the gate
    Given the base ref carries an `archunit_ignore_patterns.txt` file and the change deletes it
    When `check-quality-gates.sh` is run
    Then no error is reported for that file
    And the script does not fail because of it

  Scenario Outline: Applying @Generated to previously unannotated code fails the gate
    Given a change in `src/main/java` adds `@<annotation>` (imported by simple name `Generated`) to a class or method that did not carry it on the base ref
    When `check-quality-gates.sh` is run
    Then it reports a BLOCKING error naming that file and the occurrence-count delta (e.g. "0 -> 1")
    And the script exits non-zero

    Examples:
      | annotation                             |
      | javax.annotation.processing.Generated  |
      | jakarta.annotation.Generated           |
      | lombok.Generated                       |

  Scenario: A fully-qualified inline @Generated reference is not the target pattern
    Given a change writes `@javax.annotation.Generated(...)` inline with no import and no line beginning with `@Generated`
    Then this is a known gap in the delta pattern (anchored to `^\s*@Generated\b`), not a scenario this check claims to catch — see Non-goals

  Scenario: @Generated usage already present on the base ref does not re-fail the gate
    Given a file already carries `@Generated` on the base ref and the change does not increase that file's occurrence count
    When `check-quality-gates.sh` is run
    Then no error is reported for that file

  Scenario: The three existing PMD.ExcessiveParameterList suppressions keep passing
    Given `RadioGroupWidget.RadioOptionBorder`, `TableWidget.AccentableCellBorder`, and `PatternFieldWidget.AllowedCharacterFilter` each carry `@SuppressWarnings("PMD.ExcessiveParameterList")` per the JDK/Swing interface exception documented in `.claude/workflow.md`'s Constraints section
    When `check-quality-gates.sh` is run against a change that does not touch those suppressions
    Then neither new check reports anything for those three classes

  # Non-goals:
  #   - A general suppression ratchet. @SuppressWarnings usage of any kind
  #     (PMD, NullAway, or otherwise) is explicitly out of scope — a
  #     suppression sits on the line it excuses and is visible in a normal
  #     diff review, which is exactly what makes it a legitimate mechanism
  #     rather than a bypass. Only the two mechanisms above, which do not
  #     show their effect at the point of change, are covered here.
  #   - Catching a fully-qualified inline `@javax.annotation.Generated(...)`
  #     reference with no import — the pattern is anchored to a line
  #     beginning with `@Generated`, per the source instruction's own
  #     suggested command. A fully-qualified inline reference is a known
  #     gap, not a silent one: `check-quality-gates.sh`'s header states the
  #     pattern precisely, so a future tightening is a visible, deliberate
  #     change to the check, not a rule anyone has to remember separately.
  #   - Detecting `@Generated` usage anywhere outside `src/main/java` — test
  #     code is not part of JaCoCo/PIT's coverage denominator, so an
  #     `@Generated` there has no gate-weakening effect to catch.
  #   - Reconciling this with `quality-gate-ratchet.feature`'s existing
  #     JaCoCo/PIT/PMD exclusion-list checks, beyond sharing the same
  #     script, base-ref resolution, and `status`/`::error::` conventions.
  #   - A new CI job. Both checks run inside the existing
  #     `quality-gate-ratchet` job in `repo-hygiene.yml`, unchanged.
  #
  # Risks:
  #   - The `@Generated` check is a per-file occurrence-count delta, not a
  #     matched-set (file:line) diff — cheaper (no base-ref checkout of
  #     `src/` beyond one `git show` per changed file), but its error
  #     message names the file and the count delta, not the specific
  #     line/annotation added. Considered sufficient: the file name is
  #     enough to find it in review, and a genuinely large multi-annotation
  #     file change would show a correspondingly large delta number.
  #   - Both checks are diff-scoped to files git reports as changed between
  #     $BASE_REV and HEAD. A change that never touches a file (so it
  #     doesn't appear in the diff) cannot be detected even if that file
  #     already carries a bypass — same limitation the rest of
  #     `check-quality-gates.sh` already accepts for its other checks.
  #
  # Open questions:
  #   - None. This was specified directly by the user as a scoped addition
  #     to the in-flight Error Prone/NullAway branch (issue #199), not
  #     derived from a separate GitHub issue.
