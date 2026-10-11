@manual-verification
Feature: SpotBugs + fb-contrib — bytecode dataflow gate PMD cannot provide
  PMD is an AST analyzer: it reasons about syntax shape, which is why every
  existing PMD-driven gate (`pmd-jacoco-quality-gates.feature`,
  `pmd-parameter-count-gate.feature`) is a smell detector — long methods,
  poor names, duplicated literals, swallowed catches. It cannot answer "can
  this reference be null on some path", because that needs dataflow over
  compiled bytecode, which PMD does not do. An entire class of *defect*, as
  opposed to smell, has passed every existing gate untouched:
  `EI_EXPOSE_REP`/`EI_EXPOSE_REP2` (a tile grid or stat map handed out by
  reference — a live aliasing bug that looks like clean code),
  `equals`/`hashCode` contract violations (relevant the moment an entity or
  tile key lands in a `HashMap`), null-deref paths through branches no test
  exercises, non-transient non-serializable fields on `Serializable` Swing
  components, ignored return values, self-assignment, and comparison of
  unrelated types.

  This feature adds `spotbugs-maven-plugin` with the `fb-contrib` plugin —
  several hundred further detectors weighted toward the collection/loop/
  inheritance misuse that accumulates in a growing codebase — run at `Max`
  effort. Find Security Bugs is explicitly excluded: Veil is an offline
  single-player desktop game with no network surface and no untrusted input
  beyond local mod JSON, so that detector set would be almost entirely
  inapplicable noise.

  Existing findings on `develop` are not fixed by this change; they are
  scoped out the same way `.pmd-clean-code.xml` findings already are —
  `check-clean.sh` judges only lines a change adds, so `develop` is green on
  arrival without a separate baseline file to maintain. This is a new
  concept `pmd-jacoco-quality-gates.feature` explicitly named out of scope
  ("SpotBugs, SonarQube, or any static analysis tool beyond PMD") — this
  file is that follow-up, not a duplicate.

  Background:
    Given `spotbugs-maven-plugin` runs with the `fb-contrib` plugin at `Max` effort
    And Find Security Bugs is not among the loaded detectors
    And `check-clean.sh` parses the SpotBugs XML report the same way it already parses `target/pmd.xml`, diff-scoped to lines a change adds against `develop`

  Scenario: A clean checkout of develop is green
    Given a clean checkout of `develop` with this change applied
    When `mvn verify` is run
    Then the build succeeds
    And pre-existing SpotBugs/fb-contrib findings elsewhere in the codebase do not fail the build

  Scenario: A newly introduced EI_EXPOSE_REP on changed lines fails the gate
    Given a change adds a method that returns a mutable array or collection field directly
    When `check-clean.sh` is run
    Then it reports a BLOCKING `EI_EXPOSE_REP` (or `EI_EXPOSE_REP2`) finding with file and line
    And the gate does not pass

  Scenario Outline: High-precision dataflow findings on changed lines block the gate
    Given a change on its added lines introduces <finding>
    When `check-clean.sh` is run
    Then it reports a BLOCKING finding for that rule with file and line

    Examples:
      | finding                                                          |
      | an `equals` override with no matching `hashCode` override        |
      | a null-deref reachable on some path with no test covering it     |
      | a non-transient non-serializable field on a `Serializable` class |
      | an ignored return value from a method whose result must be used  |
      | a self-assignment                                                |
      | a comparison of unrelated types                                  |

  Scenario: Lower-precision fb-contrib findings on changed lines are advisory, not blocking
    Given a change on its added lines introduces a lower-precision fb-contrib heuristic finding
    When `check-clean.sh` is run
    Then it reports the finding as ADVISORY with file and line
    And the gate still passes on that finding alone
    And the finding must be dispositioned in the completion report, per the existing PMD advisory convention

  Scenario: A finding outside the changed lines does not block the gate
    Given `develop` carries a pre-existing SpotBugs/fb-contrib finding untouched by the current change
    When `check-clean.sh` is run
    Then that finding is not reported
    And the gate does not fail because of it

  Scenario: Find Security Bugs findings never appear
    Given a change introduces a pattern Find Security Bugs would normally flag
    When `mvn verify` and `check-clean.sh` are run
    Then no Find Security Bugs finding is reported by either

  Scenario: The documentation lands with the gate
    Given this change is complete
    Then `docs/clean-code-gate.md`'s coverage table gains a row for SpotBugs/fb-contrib, the same shape as row 9 (ArchUnit)
    And the row states the confidence threshold chosen and the finding counts measured at each candidate threshold
    And `docs/clean-code-gate.md` or the PR description states the added `mvn verify` (or profile) wall-clock time
    And the PR description states whether SpotBugs binds to `verify` directly or to the `clean-code` profile, and why, based on the measured wall-clock time

  # Non-goals:
  #   - Find Security Bugs, or any detector category built for a networked/
  #     web-facing application — Veil has no such surface.
  #   - Fixing existing SpotBugs/fb-contrib findings on develop. Diff-scoping
  #     through check-clean.sh (see Background) is the mechanism that keeps
  #     develop green without needing a separate baseline file or a
  #     fix-everything pass.
  #   - Reconciling or unifying this gate with the PMD/JaCoCo/PIT exclusion
  #     lists quality-gate-ratchet.feature already tracks — out of scope per
  #     issue #197; a future issue can fold SpotBugs's exclusion filter into
  #     that ratchet if desired.
  #   - Any change to the confidence/effort settings after this lands —
  #     thresholds are dials per docs/clean-code-gate.md's closing section,
  #     changed in a later PR with its own measurement, not implied here.
  #
  # Risks:
  #   - SpotBugs at Max effort is the slowest tool proposed in this
  #     milestone. If it adds more than ~60s to mvn verify, it must bind to
  #     the clean-code profile check-clean.sh invokes instead of to verify
  #     directly, mirroring how .pmd-clean-code.xml is already kept off the
  #     verify-bound execution for the same reason (pom.xml's profile
  #     comment). The measured number, not a guess, decides which.
  #   - A blocking/advisory misclassification either trains agents to ignore
  #     the gate (too much advisory noise blocking nothing) or produces
  #     false-positive build failures (a heuristic finding treated as
  #     high-precision). The split must follow the same precision standard
  #     .pmd-clean-code.xml's ADVISORY_RULES comment already documents, not
  #     a fresh judgment call per rule.
  #
  # Open questions:
  #   - None outstanding. The one open question from specs/intent/
  #     spotbugs-fb-contrib.md (approval to add the new build-time
  #     dependency) was resolved by the user before this spec was drafted;
  #     see that file's Clarifications section. The baseline mechanism and
  #     verify-vs-profile binding were auto-decided there for the same
  #     reason they appear as Risks/Non-goals here rather than as blocking
  #     questions: both are settled by following an existing pattern or by
  #     a measurement taken during implementation, not by human judgment.
