#!/bin/sh
set -eu
# Runs once on an EMPTY volume. A changed .env does not rotate existing database passwords.
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set ON_ERROR_STOP=1 \
  --set owner_password="$MIGRATION_PASSWORD" --set app_password="$DATABASE_PASSWORD" <<'SQL'
CREATE ROLE pruefpilot_owner LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'owner_password';
CREATE ROLE pruefpilot_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'app_password';
ALTER DATABASE pruefpilot OWNER TO pruefpilot_owner;
ALTER SCHEMA public OWNER TO pruefpilot_owner;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT CONNECT ON DATABASE pruefpilot TO pruefpilot_app;
GRANT USAGE ON SCHEMA public TO pruefpilot_app;
ALTER DEFAULT PRIVILEGES FOR ROLE pruefpilot_owner IN SCHEMA public GRANT SELECT,INSERT,UPDATE,DELETE ON TABLES TO pruefpilot_app;
SQL
