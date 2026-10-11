@manual-verification
Feature: PIT mutation-testing threshold enforcement
  `mvn org.pitest:pitest-maven:mutationCoverage` (`.claude/workflow.md`
  Step 6) is the check on the unit tests themselves, since agent-authored
  tests are never human-reviewed. Until now the goal has been purely
  advisory: it produces an HTML report, but nothing fails when the
  mutation score drops. This feature gives the goal a committed
  `mutationThreshold`, so a falling score fails the goal instead of sitting
  unnoticed in a report nobody re-opens.

  This does not make PIT a CI gate — the `mutationCoverage` goal stays a
  manual command with no execution bound to a Maven lifecycle phase, the
  same as before (`pmd-jacoco-quality-gates.feature`'s Non-goals already
  disclaim moving it into CI; that boundary is unchanged here). The only
  change is that a human or agent who does run it now gets a real pass/fail
  instead of a report they have to read numbers out of by hand. Whether a
  scheduled CI run of `mutation-testing.yml` should also enforce this
  threshold is an explicit follow-up, not decided by this feature.

  Background:
    Given `pom.xml`'s pitest-maven plugin has a committed `mutationThreshold`

  Scenario: A mutation score at or above the threshold passes
    Given the mutation score across PIT's configured target classes is at least the configured mutationThreshold
    When `mvn org.pitest:pitest-maven:mutationCoverage` is run
    Then the goal succeeds

  Scenario: A mutation score below the threshold fails the goal
    Given the mutation score across PIT's configured target classes is below the configured mutationThreshold
    When `mvn org.pitest:pitest-maven:mutationCoverage` is run
    Then the goal fails, reporting the actual score against the configured threshold
    And the HTML report is still produced under `target/pit-reports/`

  Scenario: The mutationCoverage goal is still not bound to any build lifecycle phase
    Given `pom.xml`'s pitest-maven plugin configuration
    Then it has no `<execution>` bound to a lifecycle phase
    And `mvn verify` does not invoke `mutationCoverage` as a side effect

  # Non-goals:
  #   - Binding mutationCoverage to a lifecycle phase or otherwise making it
  #     run automatically on every `mvn verify`/`mvn test` — stays a manual
  #     command; see pmd-jacoco-quality-gates.feature's Non-goals for the
  #     original decision this preserves.
  #   - Enforcing this threshold on a schedule in `mutation-testing.yml` —
  #     explicit follow-up per the source issue, not decided here.
  #   - Reconciling PIT's <excludedClasses>/<targetClasses> lists with the
  #     PMD/JaCoCo exclusion lists — out of scope per the source issue.
  #   - Raising or otherwise changing JaCoCo's coverage floors — covered by
  #     pmd-jacoco-quality-gates.feature instead.
  #
  # Risks:
  #   - The threshold is set from a measured baseline run against PIT's
  #     current <targetClasses>/<excludedClasses> lists in pom.xml. If those
  #     lists change later (classes added/removed from targeting) without
  #     re-measuring, the threshold could end up stricter or laxer than
  #     intended relative to the new target set.
  #
  # Open questions:
  #   - None — the source issue (GitHub #196) has no open questions, and
  #     drafting this spec surfaced no design fork beyond the measured-value
  #     question already noted as a Risk above.
