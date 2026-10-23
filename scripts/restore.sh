#!/bin/sh
set -eu
if [ "$#" -ne 2 ] || [ "$2" != "--confirm-overwrite" ]; then
  echo "Usage: sh scripts/restore.sh backup.dump --confirm-overwrite" >&2; exit 2
fi
[ -f "$1" ] || { echo "Backup not found" >&2; exit 1; }
# Stop app writes. Restore only into a deliberately selected isolated recovery deployment.
docker compose stop app
docker compose exec -T db pg_restore -U postgres -d pruefpilot --clean --if-exists --exit-on-error < "$1"
echo "Restore complete. Inspect records, then start the app deliberately: docker compose up -d app"
