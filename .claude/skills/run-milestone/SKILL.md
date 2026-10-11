---
name: run-milestone
description: Orchestrate a whole VEIL milestone end to end — plan its open issues by dependency, then run spec writers and coders in parallel worktrees while exactly one hardener runs at a time (mutation testing is machine-bound), surfacing a playtest per issue as each one is ready and opening one PR per issue. Use when the user hands over a milestone number to build; not for a single issue (spec-intent-auto / implement-issue), resuming one issue (resume-issue) or shipping a finished milestone (close-milestone).
---

Milestone-scale wrapper around the per-issue pipeline. It owns **scheduling**
only: which issue moves to which phase, and when. Each phase is still the
existing skill or agent, unchanged — `spec-intent`, `spec-feature`,
`spec-intent-auto`'s rules for auto-decisions and playtest placement, and
`implement-issue`'s dispatching, verification and escalation. Follow those;
don't restate them here.

Input: a milestone number or title (if missing, ask). Optional `--coders N`:
how many spec/coder agents may run at once (default 2).

## Ground rules

- **Run in the main session.** Subagents can't dispatch agents, so the
  orchestrator can't be one. Keep your own context small: every phase's work
  runs in a background agent, and you hold only state and verdicts. Tail
  command output (`-q`, `tail -40`), don't read full logs.
- **Never switch this checkout's branch or touch its working tree** — another
  session may be using it. Every issue lives in its own worktree,
  `.claude/worktrees/<slug>` (`spec-intent`'s parallel variant). Create
  worktrees yourself, never via `isolation: "worktree"`.
- **Concurrency.** At most N spec/coder agents running. **Exactly one hardener
  at a time**, milestone-wide: start the next only after the previous one's
  verification is done. `.claude/tools/pit-lock.sh` (enforced by the
  `pit-guard.sh` hook) backs this up mechanically. A hardener reporting exit 75
  from the lock means the schedule broke: find the other run, don't retry.
- **One desktop.** `QaRunner` drives real keyboard input: run it yourself,
  one at a time, never while the user is playtesting.
- **Same autonomy as `spec-intent-auto`:** no confirmation between mechanical
  steps. Questions are auto-decided and surfaced at that issue's playtest,
  never asked mid-run.

## State file

`specs/intent/milestone-<n>.md` (gitignored), one row per issue:

```
| #  | slug | deps | phase | spec sha | coder sha | hardener sha | PR | notes |
```

Phases: `blocked` → `ready` → `spec` → `coding` → `harden-queue` →
`hardening` → `playtest` → `pr-open` → `merged`; plus `skipped` (with reason)
and `stuck` (with the blocker). Write the row on **every** transition, before
dispatching the next thing; it's the only thing that survives a dead session.

**If the file already exists, this is a resume.** Reconcile each row against
reality the way `resume-issue` does (branch, commits, PR state, agents still
running) and continue from what the repo shows, not what the file claims.

## Step 1 — Plan

1. `gh issue list --repo SwiftFaze/Veil --milestone "<title>" --state all --json number,title,body,labels,state`.
   Closed issues are `merged`. An issue that already has a branch or PR gets
   adopted at its real phase, not restarted.
2. Classify risk per `spec-intent-auto` Step 0. A high-risk issue is `skipped`
   ("high-risk: run spec-intent + spec-feature by hand").
3. Dependencies: "depends on / blocked by / after #N" in the body, plus
   obvious build order (a core restructure before what's built on it). An
   issue is `ready` only when every dependency inside the milestone is
   **merged into develop**: branches stack on `develop`, never on each
   other. Ties break on the board's Priority, then issue number.
4. Print the plan (order, deps, skips) in one table, and start right away —
   the user interrupts if the order is wrong.

## Step 2 — Spec (per ready issue, up to N running)

1. **Yourself, one issue at a time** (concurrent `git worktree add` and branch
   linking race on git config): `spec-intent` Steps 2-4, parallel variant —
   branch, worktree, board → In progress.
2. **Background `general-purpose` agent, `model: "sonnet"`**, working only in
   the worktree: `spec-intent` Step 5 (intent doc), then `spec-feature` Step
   2 (`.feature`), resolving questions per `spec-intent-auto` Step 2 but
   without asking — a real question is recorded as `A (auto-decided, REAL):`.
   It commits the `.feature` (`docs: add <slug> acceptance spec`) and
   reports the sha plus every auto-decision.
3. **Verify**: the `.feature` is committed, one concept per file
   (`CLAUDE.md`), and its scenarios match the issue's scope. A miss goes back
   to the same agent via `SendMessage`.

## Step 3 — Coder (up to N running)

Dispatch `coder` in the background, in the worktree, briefed per
`implement-issue` "Dispatching", including the spec's auto-decisions. When
it returns, verify per `implement-issue` "After the coder", including QA.
Run `mvn verify` in the background, not as a blocking call, so other
agents' results keep flowing in. Pass → `harden-queue`.

## Step 4 — Hardener (exactly one, FIFO)

When nothing is `hardening`, take the oldest `harden-queue` row: dispatch
`hardener` in the background with the coder's sha, in that worktree, and tell
it to run every mutation command through `pit-lock.sh`. On return, run all of
`implement-issue`'s hardener checks yourself. The slot stays busy until they
pass. A hand-back to the coder sends the row back to `coding` (resume the
original coder), and frees the slot.

## Step 5 — Playtest, as each issue gets there

Don't block the pipeline on it. Post one message per issue, in
`spec-intent-auto` Step 5's format, with the worktree path:
`cd .claude/worktrees/<slug> && mvn compile exec:java`. Put the
`A (auto-decided, REAL):` entries first, since a wrong one means rework.
Keep scheduling everything else while you wait. When the user answers:

- **Looks good** → `spec-intent-auto` Step 6 (status checklist, push, PR
  with `Closes #N` and the playtest in their words). Row → `pr-open`.
- **Bug** → resume the original coder with it; the row goes back through
  hardening and playtest.

## Step 6 — Merges unblock the next wave

Merging stays the human's (`CLAUDE.md`: squash). On each turn, check
`pr-open` rows (`gh pr view`). A merged one → `merged`; recompute `blocked`
rows, and start any newly `ready` ones off a fresh `develop`. After each
merge, Release Please pushes a release commit to `develop` and leaves the next
open PR BEHIND: tell the user to wait for that commit and update the branch
before merging the next one.

## Each turn, before ending it

Fill every free slot (spec/coder up to N, hardener up to 1), update the state
file, then print a status table (issue, phase, waiting on whom). End the turn
only when everything left is a running agent or a human action.

## Done

Every row is `merged` or `skipped`. Report the skipped ones and why, then
point to `close-milestone`. Remove the worktrees of merged issues
(`git worktree remove`) only after asking.
