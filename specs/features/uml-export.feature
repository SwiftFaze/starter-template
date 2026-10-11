@manual-verification
Feature: UML export shows Veil's structure with CRAP and mutation colouring
  The goal is Uncle Bob's live viewer (`io.github.unclebob/uml-viewer`), with
  its CRAP and mutation colouring, running on Veil's Java code rather than
  Veilclj's Clojure, which the author can't read.

  The viewer is language-neutral: it takes Veilclj's hierarchical IR,
  `{:hierarchical true :title :prefix :edge-kinds :omit-edges :omit :levels
  :order :classes :edges}`, where each class is `{:id :name :ns :stereotype
  :level}` and each edge `{:from :to :kind :violating}` (`:kind` is
  `:dependency` or `:implements`; `:level` and `:violating` are set only when
  the policy ranks the packages involved). It reads its levels from the IR,
  not from the policy file, so the export embeds them. Metrics are read from
  `.metrics/`, keyed by class `:ns`.

  `com.swiftfaze.veil.testing.uml.UmlExport` (test scope) walks
  `com.swiftfaze.veil..` with ArchUnit's `ClassFileImporter` (already a
  dependency) and writes `target/uml/veil.edn`, with `:ns` set to each
  class's fully qualified name. Metrics come from `.metrics/crap.edn`
  (crap-gate.feature) and from PIT's `mutations.xml` (PIT gains the `XML`
  output format beside `HTML`), converted to Veilclj's `.metrics/mutate/`
  shape. `docs/uml/veil.policy.edn` encodes the layers from
  `docs/architecture.md` (engine below `ui`, `ui` below `Main`), so the viewer
  draws a layer violation in red where ArchUnit would catch it. `sandbox`
  shares the `ui` level, being dev-only UI tooling.

  This file is `@manual-verification`: the end result is a diagram in an
  external viewer, checked by opening it.

  QA: none - build tooling, no keyboard input
  Covers: the export, the mutation-metrics conversion, the policy file, and
  the viewer command.
  Supersedes: nothing. ArchUnit's `ModuleDependencyTest` stays the gate; the
  viewer is a picture of it.
  Out of scope: a Clojure `:java` scanner registered with the viewer (route
  b), and running the viewer in CI.

  Scenario: The export lists every Veil class once
    Given a compiled Veil build
    When UmlExport runs
    Then `target/uml/veil.edn` has one class entry per top-level class under `com.swiftfaze.veil`
    And each entry's `:ns` is the class's fully qualified name

  Scenario: Interface implementation becomes an `:implements` edge
    Given `PopupToggleListener` implements `GameListener`
    When UmlExport runs
    Then there is an `:implements` edge from `PopupToggleListener` to `GameListener`

  Scenario: A field, parameter or call dependency becomes a `:dependency` edge
    Given `Main` references `GamePanel`
    When UmlExport runs
    Then there is a `:dependency` edge from `Main` to `GamePanel`

  Scenario: Classes outside Veil's packages are not exported
    Given Veil classes that depend on `java.util` and `javax.swing` types
    When UmlExport runs
    Then no class or edge names a `java.` or `javax.` type

  Scenario: PIT mutation results are converted to the viewer's shape
    Given a PIT run that wrote `target/pit-reports/mutations.xml`
    When the mutation-metrics conversion runs
    Then `.metrics/mutate/` contains results keyed by fully qualified class name, in the shape uml-viewer's overlay reads

  Scenario: A missing PIT report skips the mutation overlay, not the diagram
    Given no `mutations.xml` exists
    When the export and viewer run
    Then the diagram opens with CRAP colouring and no mutation colouring
    And the export says the mutation report was missing

  Scenario: The viewer opens with CRAP colouring
    Given `mvn verify` has written `.metrics/crap.edn`
    When the author runs the viewer command
    Then a diagram of the Veil packages opens
    And classes are coloured by their worst method's CRAP score

  Scenario: A planted layer violation shows red
    Given a planted dependency from a `ui` class to `Main`
    When UmlExport and the viewer run
    Then that edge is drawn red as a layer violation

  # Non-goals:
  #   - Explaining the code. The viewer shows structure; pair the first run
  #     with a walkthrough of docs/architecture.md against the diagram.
  #   - A new layering rule. The policy file mirrors ArchUnit, never adds to it.
  #
  # Risks:
  #   - The .metrics/mutate/ shape is Veilclj's, keyed for Clojure
  #     namespaces. The spike must check it against uml-viewer's overlay code
  #     before writing the converter.
  #   - Nested and anonymous classes (Main$1): exporting them clutters the
  #     diagram; folding them into their outer class matches how
  #     crap.edn keys by class.
  #
  # Open questions:
  #   - None. Route (a) and the local-only Clojure CLI toolchain in
  #     tools/uml/ are approved (intent, Clarifications).
