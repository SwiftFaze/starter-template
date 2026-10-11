#!/usr/bin/env bash
#
# Runs a mutation-testing command (PIT or the acceptance mutator) under one
# machine-wide lock, shared by every worktree of this repo. Two mutation runs at
# once overload the dev machine, so parallel tickets (run-milestone skill) must
# take turns. pit-guard.sh blocks any mutation run that bypasses this script.
#
#   bash .claude/tools/pit-lock.sh mvn org.pitest:pitest-maven:mutationCoverage
#   PIT_LOCK_WAIT=600 bash .claude/tools/pit-lock.sh <command...>
#
# PIT_LOCK_WAIT: seconds to wait for a held lock (default 0: fail at once).
# A lock whose owning process is gone (killed run) is reclaimed automatically.
# Exit: the command's own exit code; 75 lock held by a live run; 64 bad usage.

set -uo pipefail

[ "$#" -gt 0 ] || { echo "usage: pit-lock.sh <command...>" >&2; exit 64; }

common_dir=$(git rev-parse --git-common-dir) || exit 64
LOCK="$(cd "$common_dir" && pwd)/veil-mutation.lock"
WAIT="${PIT_LOCK_WAIT:-0}"

owner_alive() {
  local pid
  # No owner file yet: the holder is between its mkdir and its write.
  [ -f "$LOCK/owner" ] || return 0
  pid=$(sed -n 's/^pid=//p' "$LOCK/owner")
  [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null
}

acquire() {
  mkdir "$LOCK" 2>/dev/null || return 1
  printf 'pid=%s\nworktree=%s\nstarted=%s\ncommand=%s\n' \
    "$$" "$(pwd)" "$(date '+%Y-%m-%d %H:%M:%S')" "$*" > "$LOCK/owner"
}

waited=0
until acquire "$@"; do
  if ! owner_alive; then
    echo "pit-lock: reclaiming stale lock (owner process is gone)" >&2
    rm -rf "$LOCK"
    continue
  fi
  if [ "$waited" -ge "$WAIT" ]; then
    echo "pit-lock: another mutation run holds the lock:" >&2
    sed 's/^/  /' "$LOCK/owner" >&2
    echo "Only one mutation run at a time; retry when it finishes." >&2
    exit 75
  fi
  sleep 15
  waited=$((waited + 15))
done

trap 'rm -rf "$LOCK"' EXIT INT TERM
"$@"
