#!/usr/bin/env bash
#
# Blocks the implement-issue handoff when a changed .feature has neither a QA
# procedure (specs/qa/<slug>.keys + .json) nor a `QA: none - <reason>` line in
# its Feature: block. See specs/features/qa-key-replay.feature.
#
#   bash .claude/tools/check-qa-coverage.sh [--base <ref>]
#
# Same base logic as check-clean.sh: merge-base with develop, committed plus
# uncommitted plus untracked work.
# Exit: 0 all covered, 1 at least one offender, 2 could not run.

set -uo pipefail
cd "$(dirname "$0")/../.." || exit 2

BASE_REF="develop"
while [ $# -gt 0 ]; do
  case "$1" in
    --base) shift; BASE_REF="${1:-}" ;;
    *) echo "unknown option: $1" >&2; exit 2 ;;
  esac
  shift
done

if ! merge_base=$(git merge-base "$BASE_REF" HEAD 2>/dev/null); then
  echo "ERROR: cannot find a merge base with '$BASE_REF'. Pass --base <ref>." >&2
  exit 2
fi

changed=$( { git diff --name-only --diff-filter=d "$merge_base" -- 'specs/features/*.feature'
             git ls-files --others --exclude-standard -- 'specs/features/*.feature'; } \
           | tr '\\' '/' | sort -u)

offenders=0
for f in $changed; do
  [ -f "$f" ] || continue
  slug=$(basename "$f" .feature)
  # The Feature: block runs from the Feature: line to the first Scenario/Background/Rule.
  block=$(awk '/^[[:space:]]*(Scenario|Scenario Outline|Background|Rule)[: ]/{exit} {print}' "$f")
  if [ -f "specs/qa/$slug.keys" ] && [ -f "specs/qa/$slug.json" ]; then
    echo "OK   $slug: procedure specs/qa/$slug.keys"
  elif reason=$(printf '%s\n' "$block" | sed -n 's/^[[:space:]]*QA: none - \(.*[^[:space:]]\).*$/\1/p' | head -1) && [ -n "$reason" ]; then
    echo "OK   $slug: QA: none - $reason"
  else
    echo "FAIL $slug: add specs/qa/$slug.keys + specs/qa/$slug.json, or a 'QA: none - <reason>' line in its Feature: block" >&2
    offenders=$((offenders + 1))
  fi
done

[ -z "$changed" ] && echo "No changed .feature files vs $BASE_REF."
[ "$offenders" -eq 0 ] || exit 1
exit 0
