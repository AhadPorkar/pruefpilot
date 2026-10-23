#!/bin/sh
set -eu
umask 077
mkdir -p backups
name="backups/pruefpilot-$(date -u +%Y%m%dT%H%M%SZ).dump"
docker compose exec -T db pg_dump -U postgres -d pruefpilot -Fc > "$name"
if [ ! -s "$name" ]; then rm -f "$name"; echo "Backup failed" >&2; exit 1; fi
printf 'Backup created: %s\nEncrypt and move it off-host; periodically restore into an isolated environment.\n' "$name"
