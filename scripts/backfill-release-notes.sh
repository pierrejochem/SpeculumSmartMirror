#!/usr/bin/env bash
# Rewrite existing GitHub Release bodies with git-cliff notes (Conventional
# Commits, see cliff.toml). One-off cleanup of releases created before the
# changelog job existed. Idempotent — safe to re-run.
#
# Needs a token with `repo` scope:
#   GITHUB_TOKEN=ghp_xxx ./scripts/backfill-release-notes.sh
# Dry run (print, don't PATCH):
#   DRY_RUN=1 ./scripts/backfill-release-notes.sh
set -euo pipefail

REPO="pierrejochem/SpeculumSmartMirror"
CONFIG="$(dirname "$0")/../cliff.toml"
: "${GITHUB_TOKEN:?set GITHUB_TOKEN (repo scope)}"
DRY_RUN="${DRY_RUN:-}"

cliff() { npx -y git-cliff@latest --config "$CONFIG" --strip all "$@" 2>/dev/null || true; }

# Tags oldest -> newest so each range is prev..tag.
mapfile -t TAGS < <(git tag --sort=v:refname | grep -E '^v[0-9]')

prev=""
for tag in "${TAGS[@]}"; do
  range="${prev:+$prev..}$tag"
  # Generate the section, drop the redundant "## x — date" heading line.
  body="$(cliff "$range" | sed '/^## /d' | sed '/./,$!d')"
  prev="$tag"

  if [[ -z "${body//[$' \t\n']/}" ]]; then
    echo "skip $tag (no changelog-worthy commits)"
    continue
  fi

  if [[ -n "$DRY_RUN" ]]; then
    echo "=== $tag ==="; echo "$body"; echo; continue
  fi

  # Resolve the release id for this tag.
  id="$(curl -fsSL -H "Authorization: Bearer $GITHUB_TOKEN" \
        "https://api.github.com/repos/$REPO/releases/tags/$tag" \
        | python3 -c 'import json,sys; print(json.load(sys.stdin).get("id",""))')"
  if [[ -z "$id" ]]; then echo "skip $tag (no release)"; continue; fi

  payload="$(python3 -c 'import json,sys; print(json.dumps({"body": sys.stdin.read()}))' <<<"$body")"
  curl -fsSL -X PATCH -H "Authorization: Bearer $GITHUB_TOKEN" \
    -H "Accept: application/vnd.github+json" \
    "https://api.github.com/repos/$REPO/releases/$id" \
    -d "$payload" >/dev/null
  echo "updated $tag (release $id)"
done
