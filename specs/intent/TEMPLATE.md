# Intent: <Feature Name>

- **Slug(s):** <feature-slug> (matches `/specs/features/<feature-slug>.feature`)
  — one intent can produce more than one `.feature` file if it covers more
  than one distinct concept (e.g. a new class *and* a new biome); list
  each slug, don't bundle them into one combined file (see
  `/specs/features/README.md`).
- **Author:**
- **Date:** YYYY-MM-DD

## Status

- [ ] Intent drafted
- [ ] Spec drafted (`.feature` file) — high-risk path only, see `.claude/workflow.md`
- [ ] Approved by human — high-risk path only
- [ ] Implemented
- [ ] Manually playtested (human) — see CLAUDE.md's "Repo-specific Step 4.5"
- [ ] Acceptance tests passing
- [ ] Mutation testing passed
- [ ] Documentation updated (`docs/`, and the wiki if player-facing)

## Problem

What's broken, missing, or manual today? Why does this matter now?

## Scope

What this feature covers. Be explicit about boundaries.

- In scope:
- Out of scope:

## Actors

Who triggers this, who's affected by it (users, other systems, other teams).

## Desired behavior

Plain-language description of the target behavior — not Gherkin yet, just
enough to draft acceptance scenarios from. Cover the happy path and the
edge cases that already matter.

## Constraints / non-functional notes

Anything beyond the global budget in the workflow CLAUDE.md — performance,
security, data migration concerns, backward compatibility. Leave blank if
nothing beyond the defaults applies.

## Open questions

Anything still undecided. Flag these rather than letting the agent guess.
