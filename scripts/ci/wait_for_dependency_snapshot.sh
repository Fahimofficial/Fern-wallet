#!/usr/bin/env bash
# Wait until GitHub has a dependency snapshot for both commits of a pull request.
#
# Gradle dependencies reach the dependency graph only through GitHub's automatic dependency
# submission, which runs as a separate job a few minutes after each push. If dependency review
# runs before that snapshot is processed it compares only manifest files (e.g. workflows) and
# passes, which silently skips every Gradle dependency. Fail instead of passing in that case.
#
# Usage: GH_TOKEN=... GITHUB_REPOSITORY=owner/repo scripts/ci/wait_for_dependency_snapshot.sh <base-sha> <head-sha>
# Env:   WAIT_TIMEOUT_SECONDS (default 900), WAIT_INTERVAL_SECONDS (default 20)
set -euo pipefail

base="$1"
head="$2"
for sha in "$base" "$head"; do
  [[ "$sha" =~ ^[0-9a-f]{40}$ ]] || { echo "::error::invalid commit SHA: $sha"; exit 2; }
done
timeout="${WAIT_TIMEOUT_SECONDS:-900}"
interval="${WAIT_INTERVAL_SECONDS:-20}"
deadline=$(( $(date +%s) + timeout ))
response="$(mktemp)"
trap 'rm -f "$response"' EXIT

while :; do
  if ! gh api --include "repos/${GITHUB_REPOSITORY}/dependency-graph/compare/${base}...${head}" > "$response" 2>&1; then
    echo "::error title=Dependency review::Cannot compare dependencies of $base...$head:"
    head -n 5 "$response"
    exit 1
  fi
  # Headers end at the first empty line. The warnings header is base64-encoded when present.
  warnings="$(sed -e 's/\r$//' -e '/^$/q' "$response" \
    | sed -n 's/^[Xx]-[Gg]ithub-[Dd]ependency-[Gg]raph-[Ss]napshot-[Ww]arnings:[[:space:]]*//p')"
  if [[ -z "$warnings" ]]; then
    echo "Dependency snapshots for $base and $head are available."
    exit 0
  fi
  decoded="$(printf '%s' "$warnings" | base64 -d 2> /dev/null || printf '%s' "$warnings")"
  if (( $(date +%s) >= deadline )); then
    echo "::error title=Dependency review::No Gradle dependency snapshot after ${timeout}s: ${decoded}"
    echo "Check that 'Automatic dependency submission' is enabled (Settings → Code security) and that its"
    echo "'submit-gradle' job succeeded for this commit, then re-run this job."
    exit 1
  fi
  echo "Waiting for the dependency snapshot: ${decoded}"
  sleep "$interval"
done
