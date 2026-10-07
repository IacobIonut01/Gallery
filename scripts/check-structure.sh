#!/usr/bin/env bash
# Structure gate for ported KMP modules (DOCS_SITE_PLAN.md §4).
#
# Rules enforced over every ported module's source tree:
#   1. One public top-level type per file (filename == type name).
#   2. No platform imports in commonMain (android.*, java.*, javax.*).
#      androidx.* is allowed — Compose is androidx on every target.
#   3. No references to the legacy `feature_node` package from ported modules.
#
# Usage:
#   scripts/check-structure.sh                 # scan every ported module
#   scripts/check-structure.sh app/core/ui ...     # scan the given module dirs
#
# Exit 0 when clean, 1 on any violation.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [ "$#" -gt 0 ]; then
  MODULES=("$@")
else
  MODULES=()
  for dir in app/core/* app/feature/* app/docs/showcase app/docs/capture; do
    [ -d "$dir" ] && MODULES+=("$dir")
  done
fi

if [ "${#MODULES[@]}" -eq 0 ]; then
  printf 'No ported modules found; nothing to check.\n'
  exit 0
fi

violations=0

# A top-level type declaration: starts at column 0, e.g.
# `class Foo`, `public sealed interface Bar`, `internal object Baz`,
# `typealias Qux = ...`. Nested/indented declarations do not match.
type_decls() {
  grep -cE '^(public |internal )?((data|sealed|enum|annotation|value|fun|open|abstract|final) )*(class|interface|object)[[:space:]]|^typealias[[:space:]]' "$1" || true
}

for module in "${MODULES[@]}"; do
  [ -d "$module" ] || continue
  while IFS= read -r -d '' file; do
    rel="${file#./}"

    count="$(type_decls "$file")"
    if [ "$count" -gt 1 ]; then
      printf 'STRUCTURE %s: %s top-level types (max 1)\n' "$rel" "$count" >&2
      violations=$((violations + 1))
    fi

    case "$file" in
      */commonMain/*)
        if grep -nE '^import (android\.|java\.|javax\.|jdk\.|sun\.|com\.android\.)' "$file"; then
          printf 'STRUCTURE %s: platform import in commonMain\n' "$rel" >&2
          violations=$((violations + 1))
        fi
        ;;
    esac

    # feature_node inside string literals is migration-compat data
    # (@SerialName discriminators, Room defaultValue JSON), not a package
    # reference — strip double-quoted strings (with escapes) before grepping.
    if sed -E 's/"([^"\\]|\\.)*"/""/g' "$file" | grep -nE 'feature_node'; then
      printf 'STRUCTURE %s: references legacy feature_node package\n' "$rel" >&2
      violations=$((violations + 1))
    fi
  done < <(find "$module" -name '*.kt' -print0 2>/dev/null)
done

if [ "$violations" -gt 0 ]; then
  printf 'check-structure: %s violation(s) in ported modules.\n' "$violations" >&2
  exit 1
fi
printf 'check-structure: clean (%s module(s) scanned).\n' "${#MODULES[@]}"
