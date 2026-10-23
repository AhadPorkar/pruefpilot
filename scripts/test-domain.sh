#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT HUP INT TERM
javac -d "$OUT" "$ROOT"/backend/src/main/java/com/ahadporkar/pruefpilot/domain/*.java "$ROOT/backend/src/main/java/com/ahadporkar/pruefpilot/auth/LoginLimiter.java" "$ROOT/scripts/DomainSmoke.java"
java -cp "$OUT" DomainSmoke
