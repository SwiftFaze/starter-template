Feature: CRAP gate links per-method complexity to per-method coverage
  PMD caps cyclomatic complexity at 8 and JaCoCo sets an 85% coverage floor,
  but the coverage floor is bundle-wide. A complexity-8 method at 0% coverage
  passes both. CRAP is the metric that links the two, per method:

    CRAP(m) = CC(m)^2 * (1 - cov(m))^3 + CC(m)

  At 100% coverage a method passes up to CC 10; at 0% only up to CC 2.

  `com.swiftfaze.veil.testing.quality.CrapReport` (test scope, JDK `javax.xml`,
  no new dependency) reads `target/site/jacoco/jacoco.xml`, using each
  `<method>`'s `COMPLEXITY` and `LINE` counters, and writes
  `target/crap/crap.txt` (worst first) and `.metrics/crap.edn` in Veilclj's
  shape (`{:entries [{:name :namespace :complexity :coverage :crap}]}`,
  `:namespace` = fully qualified class name) for the UML viewer
  (uml-export.feature). It is bound to `verify` after `jacoco:report` and fails
  when any method exceeds `crap.max` in `quality-gates.properties`.

  Scope follows the `jacoco-check` `<excludes>` in `pom.xml`: that list is the
  only exclusion list, and `CrapReport` reads it rather than keeping a second
  one.

  The gate goes live with methods already over the limit. They sit in
  `crap-baseline.txt` (beside `quality-gates.properties`), one
  `<fully.qualified.Class>#<method> <score>` per line. A baselined method
  passes while its score is at or below its recorded score. The baseline
  only shrinks: `check-quality-gates.sh` fails a PR that adds an entry or
  raises a recorded score, and the gate fails on a stale entry, so a fixed
  method must be removed.

  The five ratchet scenarios exercise `check-quality-gates.sh`, a CI shell
  script with no Java code path, so they are tagged `@manual-verification`
  like quality-gate-ratchet.feature and verified by running the script.

  QA: none - build tooling, no keyboard input
  Covers: the report, the gate, the baseline, the `crap.max` and baseline
  ratchets in `check-quality-gates.sh`, and the `check-clean.sh` section.
  Supersedes: nothing. The complexity and coverage floors in
  pmd-jacoco-quality-gates.feature stay as they are.
  Out of scope: branch coverage (line coverage only, as in Veilclj), and
  fixing the 9 methods over the limit on `develop` when the gate lands
  (they are baselined and sorted later).

  Background:
    Given `crap.max` is 10 in `quality-gates.properties`

  Scenario Outline: CRAP is computed from complexity and line coverage
    Given a JaCoCo report with a method of complexity <cc> and <covered> of <lines> lines covered
    When the CRAP report runs
    Then that method's CRAP score is <crap>

    Examples:
      | cc | covered | lines | crap |
      | 1  | 0       | 4     | 2.0  |
      | 4  | 0       | 10    | 20.0 |
      | 4  | 10      | 10    | 4.0  |
      | 10 | 10      | 10    | 10.0 |
      | 6  | 5       | 10    | 10.5 |

  Scenario: A codebase with every method at or under the limit passes
    Given a JaCoCo report where every method scores at most 10
    When the CRAP report runs in gate mode
    Then it passes
    And `target/crap/crap.txt` lists every method, worst first

  Scenario: An uncovered complexity-4 method fails the gate
    Given a JaCoCo report containing an uncovered method of complexity 4
    When the CRAP report runs in gate mode
    Then it fails
    And the failure names the method's class and name, its complexity, coverage and CRAP score, and the limit

  Scenario: A method exactly at the limit passes
    Given a JaCoCo report containing a fully covered method of complexity 10
    When the CRAP report runs in gate mode
    Then it passes

  Scenario: Report-only mode never fails
    Given a JaCoCo report containing an uncovered method of complexity 4
    When the CRAP report runs with `--report-only`
    Then it passes
    And the method still appears in `target/crap/crap.txt` above the limit

  Scenario: Classes excluded from JaCoCo's check are not scored
    Given the `jacoco-check` `<excludes>` contains `**/Main.class` and `**/Main$*.class`
    And a JaCoCo report containing an uncovered complexity-6 method in `Main` and one in `Main$1`
    When the CRAP report runs in gate mode
    Then it passes
    And neither method appears in either output file

  Scenario: The metrics file is written in the viewer's shape
    Given a JaCoCo report with method `move` of complexity 2, fully covered, in `com.swiftfaze.veil.entities.Player`
    When the CRAP report runs
    Then `.metrics/crap.edn` contains an entry with `:name` "move", `:namespace` "com.swiftfaze.veil.entities.Player", `:complexity` 2, `:coverage` 100 and `:crap` 2.0

  Scenario: A missing JaCoCo report fails loudly
    Given no `target/site/jacoco/jacoco.xml` exists
    When the CRAP report runs
    Then it fails, naming the missing file
    And it does not write an empty report that passes

  Scenario: Unreadable JaCoCo excludes fail loudly
    Given `pom.xml` has no `jacoco-check` execution with an `<excludes>` list
    When the CRAP report runs
    Then it fails, saying it could not find the exclusion list

  Scenario: A baselined method at or under its recorded score passes
    Given `crap-baseline.txt` records `com.swiftfaze.veil.ui.TableWidget#moveLeft 13.3`
    And a JaCoCo report where `TableWidget#moveLeft` scores 13.3
    When the CRAP report runs in gate mode
    Then it passes
    And `target/crap/crap.txt` marks the method as baselined

  Scenario: A baselined method that gets worse fails
    Given `crap-baseline.txt` records `com.swiftfaze.veil.ui.TableWidget#moveLeft 13.3`
    And a JaCoCo report where `TableWidget#moveLeft` scores 20.0
    When the CRAP report runs in gate mode
    Then it fails, naming the method, its baselined score and its new score

  Scenario: A stale baseline entry fails until it is removed
    Given `crap-baseline.txt` records `com.swiftfaze.veil.ui.TableWidget#moveLeft 13.3`
    And a JaCoCo report where `TableWidget#moveLeft` now scores 4.0
    When the CRAP report runs in gate mode
    Then it fails, saying the method is now within the limit and its baseline entry must be removed

  Scenario: A baseline entry for a method that no longer exists fails
    Given `crap-baseline.txt` records a method that is not in the JaCoCo report
    When the CRAP report runs in gate mode
    Then it fails, naming the entry and saying it must be removed

  @manual-verification
  Scenario: Adding a baseline entry fails the ratchet
    Given a PR that adds a line to `crap-baseline.txt`
    When `check-quality-gates.sh` runs
    Then it fails, naming the added entry
    And the failure message states that a baseline entry is a gate weakening and needs a stated reason in the PR

  @manual-verification
  Scenario: Raising a recorded baseline score fails the ratchet
    Given a PR that raises a method's score in `crap-baseline.txt`
    When `check-quality-gates.sh` runs
    Then it fails, naming the method, its base-branch score and the proposed score

  @manual-verification
  Scenario: Removing a baseline entry passes the ratchet
    Given a PR that removes a line from `crap-baseline.txt`
    When `check-quality-gates.sh` runs
    Then it passes

  @manual-verification
  Scenario: Raising `crap.max` fails the ratchet
    Given a PR that raises `crap.max` above its value on the base branch
    When `check-quality-gates.sh` runs
    Then it fails, naming `crap.max`, its base-branch value and the proposed value

  @manual-verification
  Scenario: Lowering `crap.max` passes the ratchet
    Given a PR that lowers `crap.max` below its value on the base branch
    When `check-quality-gates.sh` runs
    Then it passes

  # Non-goals:
  #   - Replacing PMD's complexity cap or JaCoCo's bundle floor. CRAP sits
  #     beside them.
  #   - A second exclusion list. Scope is JaCoCo's, read from pom.xml.
  #   - Branch coverage in the formula.
  #
  # Risks:
  #   - Reading excludes out of pom.xml couples CrapReport to the POM's
  #     shape. It must fail loudly when it can't find the list (scenario
  #     above), never score everything or nothing silently.
  #   - Direction of the ratchet is the reverse of the coverage floors:
  #     *raising* crap.max is the weakening. check-quality-gates.sh must not
  #     reuse the "lower is weaker" comparison.
  #   - Lambdas and synthetic methods appear in jacoco.xml as `lambda$x$0`
  #     etc. They are scored like any method; the report names them as
  #     JaCoCo does.
  #
  #   - Baseline keys are class#method, and overloads share a name. If two
  #     overloads are baselined, the entry must hold the worse score, or the
  #     key must include JaCoCo's method descriptor.
  #
  # Open questions:
  #   - None. Rollout settled as a shrink-only baseline (intent,
  #     Clarifications).
