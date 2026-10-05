#!/usr/bin/env bash
# Automates app-setup/README.md's macOS steps 1-7 (everything up to, but not
# including, actually running the app — see the README for that and for
# what each step does). Run from the `ad-campaign` directory (the one
# containing pom.xml):
#   bash app-setup/mac/setup.sh
#
# Idempotent: safe to re-run if an earlier step failed partway through.

set -euo pipefail

if [ ! -f "pom.xml" ]; then
    echo "Run this from the ad-campaign directory (the one containing pom.xml)." >&2
    exit 1
fi

if ! command -v brew >/dev/null 2>&1; then
    echo "Homebrew isn't installed. Install it first - see:" >&2
    echo "  https://brew.sh" >&2
    exit 1
fi

echo "==> Installing Java, PostgreSQL, Git (skipping anything already installed)..."
# The Java cask install can require an interactive sudo password and may
# fail in a non-interactive shell; that's non-fatal here (mvnw further down
# will fail with a clear error if no JDK ends up on PATH at all).
brew install --cask temurin17 || true
brew install postgresql@16 git || true

if ! command -v java >/dev/null 2>&1; then
    echo "WARNING: 'java' isn't on PATH yet. If the cask install above failed" >&2
    echo "(e.g. needed a sudo password this shell couldn't prompt for)," >&2
    echo "install it yourself from https://adoptium.net/temurin/releases/?version=17" >&2
    echo "then re-run this script." >&2
fi

PG_BIN="$(brew --prefix postgresql@16)/bin"

echo "==> Starting PostgreSQL..."
# Don't let this abort the script: it errors if the service is already
# bootstrapped, or if another PostgreSQL (e.g. Postgres.app) is already
# running — either way, the pg_isready wait loop below is the real check.
brew services start postgresql@16 || true

echo "==> Waiting for PostgreSQL to accept connections..."
PG_READY=0
for i in $(seq 1 30); do
    if "$PG_BIN/pg_isready" >/dev/null 2>&1 || pg_isready >/dev/null 2>&1; then
        PG_READY=1
        break
    fi
    sleep 1
done
if [ "$PG_READY" -ne 1 ]; then
    echo "PostgreSQL never became reachable on localhost:5432. If you already" >&2
    echo "run Postgres another way (e.g. Postgres.app), make sure it's started," >&2
    echo "then re-run this script." >&2
    exit 1
fi

# Prefer the Homebrew postgresql@16 client tools, but fall back to whatever
# psql/createdb are already on PATH (e.g. if another PostgreSQL install's
# client tools shadow them — same binaries work fine against either server
# as long as something is listening on 5432).
PSQL="$PG_BIN/psql";     [ -x "$PSQL" ]     || PSQL="psql"
CREATEDB="$PG_BIN/createdb"; [ -x "$CREATEDB" ] || CREATEDB="createdb"

echo "==> Creating the adcampaign database and role (safe to re-run)..."
"$CREATEDB" adcampaign 2>/dev/null || echo "   (database 'adcampaign' already exists)"
"$PSQL" -d postgres -tc "SELECT 1 FROM pg_roles WHERE rolname='adcampaign'" | grep -q 1 \
    || "$PSQL" -d postgres -c "CREATE ROLE adcampaign WITH LOGIN PASSWORD 'adcampaign';"
"$PSQL" -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE adcampaign TO adcampaign;"
"$PSQL" -d adcampaign -c "GRANT ALL ON SCHEMA public TO adcampaign;"
"$PSQL" -d adcampaign -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO adcampaign;"
"$PSQL" -d adcampaign -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO adcampaign;"

echo "==> Loading schema..."
"$PSQL" -d adcampaign -f db/schema/run_all.sql

read -rp "Load optional demo data too? [y/N] " LOAD_SEED
if [[ "$LOAD_SEED" =~ ^[Yy]$ ]]; then
    "$PSQL" -d adcampaign -f db/seed-data/01_demo_tenant.sql
    echo "   Demo login: admin@acmedemo.test / ChangeMe123!"
fi

echo "==> Building (this downloads Maven + dependencies on first run)..."
./mvnw clean package -DskipTests

echo
echo "Done. Next: java -jar target/ad-campaign-0.0.1-SNAPSHOT.jar"
echo "See app-setup/README.md step 9 to verify it's working."
