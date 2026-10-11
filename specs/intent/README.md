# Intent docs

One `<feature-slug>.md` file per feature, written by a human (or derived from a GitHub issue via the `spec-intent`
skill) before implementation starts. This is the source of *why* a feature exists and what it must do.

See `.claude/workflow.md` for the high-risk vs. standard path split: on the high-risk path
(auth/payments/data-integrity/public API),
`/specs/features/<feature-slug>.feature` is generated from this doc and reviewed/approved before any code is written. On
the standard path, this doc still gets written first, but feeds directly into an agile implementation loop instead — no
fully-approved `.feature` file is required before code exists.

Intent is not written once and frozen: clarifying answers gathered while drafting or implementing get appended back
here.

Copy `TEMPLATE.md` to `<feature-slug>.md` to start a new one.

These docs are local scratch, not version-controlled: `.gitignore` excludes everything in this directory except
`README.md` and `TEMPLATE.md` themselves, so a `<feature-slug>.md` never gets committed. Unlike
`specs/features/*.feature` — which stays a real, executable check via Cucumber — an intent doc has no equivalent once a
feature is built: the code itself, and its `.feature` file where one exists, are what actually persist. Every
previously-committed intent doc (and this README's old Index of them) was deleted in a repo cleanup (2026-09-05) once
that was made explicit, since none of it was needed to persist past the feature it fed.
