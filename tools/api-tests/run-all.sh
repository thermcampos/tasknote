#!/bin/bash
# Runs every script in tools/api-tests against a running API, in order:
# sign-up validations -> sign-up + sign-in (exports TOKEN) -> the rest.
# Used to exercise the API while collecting GraalVM reachability metadata.
#
# Requires: API on localhost:8585, Postgres reachable for confirm-user.sh
# (Docker), curl and jq.
#
# Usage (from anywhere): bash tools/api-tests/run-all.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT" || exit 1

# Run in this order; any other script in the folder runs afterwards.
ORDERED_SCRIPTS=(
  sign-up-validations.sh
  sign-up-and-login.sh
  create-tasks-validations.sh
  create-notes-validations.sh
  create-get-tasks.sh
  create-get-notes.sh
  fetch-home-items.sh
)

banner() {
  echo ""
  echo "############################################################"
  echo "# $1"
  echo "############################################################"
}

run_script() {
  local name=$1
  banner "$name"
  if [ "$name" = "sign-up-and-login.sh" ]; then
    # Sourced so the exported TOKEN reaches the following scripts.
    # shellcheck source=/dev/null
    . "$SCRIPT_DIR/$name"
    if [ -z "$TOKEN" ]; then
      echo ""
      echo "Sign-in did not return a token, aborting."
      exit 1
    fi
    echo ""
    echo "TOKEN exported."
  else
    bash "$SCRIPT_DIR/$name"
  fi
}

declare -A RAN=()
for name in "${ORDERED_SCRIPTS[@]}"; do
  run_script "$name"
  RAN[$name]=1
done

for path in "$SCRIPT_DIR"/*.sh; do
  name=$(basename "$path")
  if [ "$name" = "run-all.sh" ] || [ -n "${RAN[$name]}" ]; then
    continue
  fi
  run_script "$name"
done

banner "Done"
