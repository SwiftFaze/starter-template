#!/usr/bin/env bash
#
# PreToolUse hook (.claude/settings.json, Bash|PowerShell): blocks a mutation
# run (PIT or the acceptance mutator) that doesn't go through pit-lock.sh, so
# the one-at-a-time rule holds even when an agent forgets it.
#
# Reads the hook's JSON payload on stdin. No jq on the dev machine, so this
# matches the raw payload; the patterns need `mvn` plus a mutation goal, which
# a plain search command doesn't produce.
# Exit: 0 allow; 2 block (stderr is shown to the agent).

payload=$(cat)

printf '%s' "$payload" | grep -q 'mvn' || exit 0
printf '%s' "$payload" | grep -Eq 'mutationCoverage|AcceptanceMutator' || exit 0
printf '%s' "$payload" | grep -q 'pit-lock\.sh' && exit 0

cat >&2 <<'EOF'
Blocked: mutation runs must hold the machine-wide lock (one at a time across
all worktrees). Re-run the same command prefixed with the lock script:
  bash .claude/tools/pit-lock.sh <your mvn command>
If it exits 75, another run holds the lock: report that instead of retrying in
a loop, or set PIT_LOCK_WAIT=<seconds> to wait for it.
EOF
exit 2
