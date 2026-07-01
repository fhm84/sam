#!/usr/bin/env bash
# Validates a commit message header against this repo's Conventional Commits
# convention. Used by .githooks/commit-msg (local) and the commit-lint CI job.
#
# Usage:
#   check-commit-msg.sh <path-to-commit-msg-file>   # hook mode
#   echo "$subject" | check-commit-msg.sh           # stdin mode (CI)
set -euo pipefail

if [[ $# -ge 1 ]]; then
  header=$(head -n1 "$1")
else
  IFS= read -r header
fi

# Merge/revert commits and interactive-rebase markers are exempt.
if [[ "$header" =~ ^(Merge|Revert)\  ]] || [[ "$header" =~ ^(fixup|squash)! ]]; then
  exit 0
fi

types='build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test'
pattern="^(${types})(\([a-z0-9,-]+\))?: .{1,88}\$"

if [[ ! "$header" =~ $pattern ]]; then
  cat >&2 <<EOF
Commit message does not follow this repo's Conventional Commits format.

  Expected: <type>(<scope>): <subject>
  Got:      $header

  Allowed types: build, chore, ci, docs, feat, fix, perf, refactor, revert, style, test
  Scope is optional, e.g. fix(ui): ..., test(core,api): ...
  Example: fix(ui): pin transitive deps to patch Dependabot security alerts
EOF
  exit 1
fi

if [[ "$header" == *. ]]; then
  echo "Commit subject should not end with a period: $header" >&2
  exit 1
fi