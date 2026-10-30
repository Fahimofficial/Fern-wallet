#!/usr/bin/env bash
# Wait until the given workflows have a successful `push` run for a commit, and fail as soon
# as one of them failed or was cancelled. Used by release.yml so a tag is only built from a
# main commit whose CI and security checks passed.
#
# Usage: GH_TOKEN=... GITHUB_REPOSITORY=owner/repo scripts/ci/wait_for_workflows.sh <sha> <workflow-file>...
# Env:   WAIT_TIMEOUT_SECONDS (default 2700), WAIT_INTERVAL_SECONDS (default 60)
set -euo pipefail

sha="$1"
shift
[[ "$sha" =~ ^[0-9a-f]{40}$ ]] || { echo "::error::invalid commit SHA"; exit 2; }
timeout="${WAIT_TIMEOUT_SECONDS:-2700}"
interval="${WAIT_INTERVAL_SECONDS:-60}"
deadline=$(( $(date +%s) + timeout ))

while :; do
  pending=()
  for wf in "$@"; do
    [[ "$wf" =~ ^[A-Za-z0-9._-]+\.ya?ml$ ]] || { echo "::error::invalid workflow file name: $wf"; exit 2; }
    # Most recent push run of this workflow for exactly this commit.
    if ! run="$(gh api "repos/${GITHUB_REPOSITORY}/actions/workflows/${wf}/runs?head_sha=${sha}&event=push&per_page=1" \
      --jq '.workflow_runs[0] // empty | "\(.status) \(.conclusion // "none") \(.html_url)"' 2>&1)"; then
      echo "::error title=Release blocked::Cannot read runs of $wf ($run). Does the workflow exist on the default branch?"
      exit 1
    fi
    if [[ -z "$run" ]]; then
      pending+=("$wf (no run yet)")
      continue
    fi
    read -r status conclusion url <<< "$run"
    if [[ "$status" != "completed" ]]; then
      pending+=("$wf ($status)")
    elif [[ "$conclusion" != "success" ]]; then
      echo "::error title=Release blocked::$wf concluded '$conclusion' for $sha: $url"
      echo "Fix the failure on main (or re-run it), then re-run this release workflow."
      exit 1
    else
      echo "ok: $wf succeeded ($url)"
    fi
  done
  if (( ${#pending[@]} == 0 )); then
    echo "All required workflows succeeded for $sha."
    exit 0
  fi
  if (( $(date +%s) >= deadline )); then
    echo "::error title=Release blocked::Timed out waiting for: ${pending[*]}"
    exit 1
  fi
  echo "Waiting for: ${pending[*]}"
  sleep "$interval"
done
