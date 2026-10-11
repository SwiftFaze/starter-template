Feature: Acceptance mutation tests the acceptance tests
  PIT mutates the code to test the unit tests. Nothing tests the acceptance
  tests themselves: if a step definition ignores its `<n>` argument, every
  scenario still passes. Veilclj's `bb acceptance-mutate` changes each
  literal in a scenario and expects the suite to fail.

  `com.swiftfaze.veil.testing.aps.AcceptanceMutator` (test scope) parses
  `.feature` files with `io.cucumber:gherkin`, already on the classpath
  transitively through `cucumber-java`. For each mutant it runs only that
  scenario, in-process, through the Cucumber JUnit Platform launcher filtered
  by name. A mutant whose scenario still passes has **survived**.

  Mutations:
    - Integers: +1 and -1.
    - Quoted strings: swapped for another quoted value used in the same file.
    - Each `Examples` row cell, mutated by the same rules.

  Survivors are reported as `file:line  original → mutant`, and the command
  exits 3 if any survived. Like PIT, it is a manual Step 6 command for the
  hardener, not a CI gate. `--feature <slug>` targets one feature.

  QA: none - build tooling, no keyboard input
  Covers: mutant generation, the per-mutant run, survivor reporting, exit
  codes, and `--feature`.
  Supersedes: nothing. PIT (pit-mutation-threshold.feature) still owns
  mutation of production code.
  Out of scope: a CI gate or ratcheted threshold, mutating step
  definitions or keywords, and features tagged `@manual-verification` or
  `@pending` (they never run, so every mutant would "survive").

  Scenario: A step that ignores its argument produces a survivor
    Given a fixture feature with a scenario "Player gains <n> gold" using the integer 5
    And its step definition ignores the integer
    When the acceptance mutator runs on that feature
    Then it reports survivors for 5 → 6 and 5 → 4 at the literal's file and line
    And it exits 3

  Scenario: A step that uses its argument kills every mutant
    Given a fixture feature whose step definitions check every literal
    When the acceptance mutator runs on that feature
    Then it reports no survivors
    And it exits 0

  Scenario: A quoted string is swapped for another value from the same file
    Given a fixture feature using the quoted strings "Warrior" and "Mage"
    When the acceptance mutator generates mutants
    Then "Warrior" is mutated to "Mage" and "Mage" to "Warrior"

  Scenario: A quoted string with no other value in the file is not mutated
    Given a fixture feature whose only quoted string is "Warrior"
    When the acceptance mutator generates mutants
    Then no string mutant is generated for "Warrior"
    And the report says it was skipped for having no alternative value

  Scenario: Each Examples cell is mutated on its own row
    Given a fixture Scenario Outline with an Examples row "| 2 | 3 |"
    When the acceptance mutator generates mutants
    Then it generates mutants for 2 and for 3 separately, each running only that Examples row

  Scenario: Only the mutated scenario runs
    Given a fixture feature with two scenarios
    When the acceptance mutator runs a mutant of the first scenario
    Then only the first scenario is executed for that mutant

  Scenario: `--feature` limits the run to one feature
    Given fixture features `alpha` and `beta`
    When the acceptance mutator runs with `--feature alpha`
    Then only mutants from `alpha.feature` are run

  Scenario: An unknown `--feature` slug fails
    When the acceptance mutator runs with `--feature does-not-exist`
    Then it fails, naming the slug and the features directory searched

  Scenario: Manual-verification and pending features are skipped
    Given a fixture feature tagged `@manual-verification` and one tagged `@pending`
    When the acceptance mutator runs on all features
    Then neither feature is mutated
    And the report lists both as skipped with their tag

  Scenario: A scenario that already fails unmutated is reported, not mutated
    Given a fixture feature whose scenario fails before any mutation
    When the acceptance mutator runs on that feature
    Then it reports the scenario as failing on the original
    And it generates no mutants for it
    And it exits with a code other than 0 and 3

  # Non-goals:
  #   - A CI gate or a threshold ratchet. Manual Step 6 only, like PIT.
  #   - Mutating Gherkin keywords, step text or data tables beyond Examples.
  #
  # Risks:
  #   - Running Cucumber in-process from inside a Cucumber scenario (these
  #     step definitions) may clash on glue and SharedScenarioContext state.
  #     Fixture features and glue must be isolated from the real suite's.
  #   - Runtime grows with literals x scenario cost; --feature is how the
  #     hardener keeps it to the changed features.
  #
  # First run on the existing suite: survivors are not fixed in this branch.
  # One GitHub issue is filed per affected feature file (added to the VEIL
  # board), listing that file's survivors.
  #
  # Open questions:
  #   - None (intent, Clarifications).
