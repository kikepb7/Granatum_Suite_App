#!/usr/bin/env bash
# Copies the backend's API contract into docs/openapi.json, pinned to one backend commit
# (constitution principle VI). The app never edits the copy by hand: to pick up a contract
# change, run this with the new commit and review the diff like any other code change.
#
#   scripts/sync-openapi.sh <backend-commit-sha>
#
# The commit must already be on GitHub. A branch name is rejected on purpose: the pin has to
# say exactly which contract the app was built against, and a branch keeps moving.

set -euo pipefail

repo="kikepb7/Granatum_Suite_Backend"
ref="${1:-}"

if [[ ! "$ref" =~ ^[0-9a-f]{40}$ ]]; then
  echo "usage: $0 <full 40-character backend commit sha>" >&2
  exit 1
fi

root="$(cd "$(dirname "$0")/.." && pwd)"
target="$root/docs/openapi.json"
pin="$root/docs/openapi.pin"
tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT

curl --fail --silent --show-error --location \
  "https://raw.githubusercontent.com/$repo/$ref/docs/openapi.json" -o "$tmp"

# Refuse anything that is not an OpenAPI document, so a 200 with an HTML error page or an
# empty body never lands as the contract.
python3 - "$tmp" <<'EOF'
import json, sys
doc = json.load(open(sys.argv[1]))
assert str(doc.get("openapi", "")).startswith("3."), "not an OpenAPI 3 document"
assert doc.get("paths"), "the document has no paths"
EOF

mkdir -p "$root/docs"
mv "$tmp" "$target"
trap - EXIT
printf '%s\n' "$ref" > "$pin"

echo "docs/openapi.json updated to $repo@${ref:0:7}"
