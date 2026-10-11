# UML viewer

A local picture of Veil's class structure, drawn by Uncle Bob's
`uml-viewer`, with CRAP and mutation colouring and layer violations in red.
Local tooling only: not run in CI, and `ModuleDependencyTest` stays the gate
(see [`testing-module-dependency.md`](testing-module-dependency.md)). The spec
is `specs/features/uml-export.feature`.

## Prerequisites

- [`bb`](https://babashka.org) (Babashka) on the PATH. No `clojure` CLI is
  needed; `bb` fetches the viewer through `tools/uml/deps.edn`.
- A JDK and Maven, as for the rest of the project.

## Workflow

Run these from `tools/uml`:

- `bb export` compiles, then writes `target/uml/veil.edn` from the production
  classes (`UmlExport`) and converts PIT's report if one exists.
- `bb metrics` converts only PIT's `target/pit-reports/mutations.xml` into
  `.metrics/mutate/` (`PitMutationMetrics`).
- `bb view` opens the viewer on `target/uml/veil.edn`.

- `bb uml` runs `export` then `view`, using the colours from your last
  `mvn verify` and PIT run.
- `bb uml-fresh` first runs `mvn verify` and PIT (about 17 minutes), so
  CRAP and mutation colouring are current, then exports and views.

IntelliJ has matching run configurations: **UML viewer** and **UML viewer
(fresh tests + PIT)**.

## Where the colours come from

- The viewer walks up from the IR file to the repo root and reads `.metrics/`
  there. `.metrics/crap.edn` is written by `mvn verify` (see
  [`clean-code-gate.md`](clean-code-gate.md#crap-per-method-complexity--coverage)).
- The mutation overlay needs a PIT run first:
  `mvn org.pitest:pitest-maven:mutationCoverage` (see
  [`testing-quality-gates.md`](testing-quality-gates.md)). Without
  `mutations.xml` the export skips the overlay with a message.
- Layers come from `docs/uml/veil.policy.edn`, mirroring
  [`architecture.md`](architecture.md); the export embeds them in the IR.

## Known limits

- Layout is automatic only; classes cannot be arranged by hand, so dense
  edges are cluttered.
- Nested and anonymous classes fold into their outer class in the diagram,
  but CRAP entries for them (for example `Main$1`) are keyed by the nested
  class name, so they do not colour the folded outer class. Mutation tallies
  do fold.
