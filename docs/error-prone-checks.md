# Error Prone check decisions

Every Error Prone check in the pinned `error_prone_core` (2.50.0) that its
own defaults do not already run at ERROR, with the decision this project
made for it (#219). Error Prone's default ERROR tier (186 checks) runs
unchanged and is not listed. How this gate is wired, and how to decide a
check a future Error Prone version adds:
[`testing-quality-gates.md`](testing-quality-gates.md) § "Compile-time gates".

- **ADD** — enabled with its own `-Xep:<Check>:ERROR` flag in `pom.xml`;
  a violation fails `mvn compile`.
- **EXCLUDE** — not raised. A default-on WARNING check stays at WARNING;
  an off-by-default check stays off. Only four reasons are accepted:
  **(a)** no surface in this codebase, **(b)** contradicts another enabled
  Error Prone check or PMD rule, **(c)** needs a dependency the project
  does not declare, **(d)** deprecated or a no-op. Noisy or style-only is
  not a reason.

Totals: 455 checks — 349 ADD, 106 EXCLUDE.

## Needs a new dependency (follow-up)

Excluded only because their fix needs a dependency the project doesn't
declare. Adding one is a separate decision (ask before adding a
dependency); once made, re-audit these:

- `AnnotateFormatMethod` — requires @FormatMethod from error_prone_annotations
- `CanIgnoreReturnValueSuggester` — requires @CanIgnoreReturnValue from error_prone_annotations
- `DoNotCallSuggester` — requires @DoNotCall from error_prone_annotations
- `ImmutableMemberCollection` — fix is Guava Immutable* collection types; Guava not a dependency
- `InlineMeSuggester` — requires @InlineMe from error_prone_annotations
- `Var` — requires @Var from error_prone_annotations

## All decisions

One row per check, in [`error-prone-checks.tsv`](error-prone-checks.tsv)
(columns: check, tier, decision, reason). It is data rather than prose, so it
sits outside the `docs/*.md` instruction-file line budget
(`.claude/tools/check-instruction-budget.sh`); GitHub renders it as a table.
Every ADD row has exactly one `-Xep:<Check>:ERROR` flag in `pom.xml`.
