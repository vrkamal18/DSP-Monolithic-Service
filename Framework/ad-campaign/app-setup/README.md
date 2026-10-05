# AdCampaign — new machine setup

Complete install guide for a brand-new user on a brand-new machine, macOS or
Windows. Follow your OS section below, then the **Common steps** everyone
runs after. A "fast path" automation script is also provided per OS if you'd
rather not type each command by hand — see [Fast path](#fast-path) at the
bottom. Either way, read this file once; the script just automates the same
steps.

## What you're installing

| Tool | Version | Why |
|---|---|---|
| JDK | 17 or newer (LTS; 21 is fine) | Builds and runs the Spring Boot app |
| PostgreSQL | 16.x | The application database |
| Git | any recent version | To clone the repository |

Maven itself is **not** required — the repo includes a wrapper (`mvnw` /
`mvnw.cmd`) that downloads the right Maven version automatically on first
use.

---

## macOS setup

### 1. Install Homebrew (if you don't already have it)

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

Follow the printed instructions to add `brew` to your shell's `PATH`, then
open a new terminal.

### 2. Install Java, PostgreSQL, and Git

```bash
brew install --cask temurin17
brew install postgresql@16 git
```

`temurin17` installs a real, `/usr/libexec/java_home`-registered JDK (no
manual symlinking needed). Confirm it's found:

```bash
java -version
```

### 3. Start PostgreSQL

```bash
brew services start postgresql@16
```

This starts PostgreSQL now and keeps it running across reboots. Confirm it's
up:

```bash
/opt/homebrew/opt/postgresql@16/bin/pg_isready
```

(If Homebrew installed to a different prefix, e.g. Intel Macs use
`/usr/local`, adjust the path — or just run `pg_isready` if `psql` is
already on your `PATH`.)

### 4. Clone the repository

```bash
git clone git@github.com:babuanandkumar/adcampaign.git
cd adcampaign/monolithic/adcampaign/Framework/ad-campaign
```

(Use the HTTPS URL instead if you haven't set up SSH keys with GitHub:
`https://github.com/babuanandkumar/adcampaign.git`.)

**All commands from here on assume you're in this `ad-campaign` directory**
(the one containing `pom.xml`).

### 5. Create the database and app role

```bash
createdb adcampaign
psql -d postgres -c "CREATE ROLE adcampaign WITH LOGIN PASSWORD 'adcampaign';"
psql -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE adcampaign TO adcampaign;"
psql -d adcampaign -c "GRANT ALL ON SCHEMA public TO adcampaign;"
psql -d adcampaign -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO adcampaign;"
psql -d adcampaign -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO adcampaign;"
```

This matches `application.properties`'s default `adcampaign` / `adcampaign`
database credentials exactly — nothing else to configure for local dev.

Now jump to [Common steps](#common-steps-macos--windows) below.

---

## Windows setup

### 1. Install Java

Download and run the Windows installer (`.msi`) from
[adoptium.net](https://adoptium.net/temurin/releases/?version=17) (choose
version 17, Windows, x64, `.msi`). During install, tick **"Set JAVA_HOME
variable"** and **"Add to PATH"**.

Or, if you have [Chocolatey](https://chocolatey.org/install) installed,
from an **Administrator** PowerShell:

```powershell
choco install temurin17 -y
```

Confirm it's found (open a **new** PowerShell window first, so the updated
`PATH` takes effect):

```powershell
java -version
```

### 2. Install PostgreSQL

Download the PostgreSQL 16 installer from
[postgresql.org/download/windows](https://www.postgresql.org/download/windows/)
and run it. During install:
- Remember the password you set for the `postgres` superuser.
- Keep the default port `5432`.
- The installer starts the PostgreSQL service automatically and adds `psql`
  to your `PATH` (under `C:\Program Files\PostgreSQL\16\bin` by default —
  if `psql` isn't recognized, open a new PowerShell window, or add that
  folder to `PATH` manually).

Or via Chocolatey (Administrator PowerShell):

```powershell
choco install postgresql16 -y --params '/Password:postgres'
```

(This sets the `postgres` superuser password to `postgres` — only use this
on a personal dev machine, never anywhere shared.)

### 3. Install Git

Download from [git-scm.com/download/win](https://git-scm.com/download/win),
or via Chocolatey:

```powershell
choco install git -y
```

### 4. Clone the repository

```powershell
git clone git@github.com:babuanandkumar/adcampaign.git
cd adcampaign\monolithic\adcampaign\Framework\ad-campaign
```

(Use `https://github.com/babuanandkumar/adcampaign.git` instead if you
haven't set up SSH keys with GitHub.)

**All commands from here on assume you're in this `ad-campaign` directory**
(the one containing `pom.xml`).

### 5. Create the database and app role

Using the `postgres` superuser password you set during install (replace
`postgres` below with yours if different):

```powershell
$env:PGPASSWORD = "postgres"
createdb -U postgres adcampaign
psql -U postgres -d postgres -c "CREATE ROLE adcampaign WITH LOGIN PASSWORD 'adcampaign';"
psql -U postgres -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE adcampaign TO adcampaign;"
psql -U postgres -d adcampaign -c "GRANT ALL ON SCHEMA public TO adcampaign;"
psql -U postgres -d adcampaign -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO adcampaign;"
psql -U postgres -d adcampaign -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO adcampaign;"
```

This matches `application.properties`'s default `adcampaign` / `adcampaign`
database credentials exactly — nothing else to configure for local dev.

---

## Common steps (macOS & Windows)

Run these from the `ad-campaign` directory (the one with `pom.xml`) on
either OS — only the wrapper script name differs.

### 6. Load the schema (and, optionally, demo data)

macOS/Linux:
```bash
psql -d adcampaign -f db/schema/run_all.sql
psql -d adcampaign -f db/seed-data/01_demo_tenant.sql   # optional demo data
```

Windows (PowerShell):
```powershell
psql -U adcampaign -d adcampaign -f db/schema/run_all.sql
psql -U adcampaign -d adcampaign -f db/seed-data/01_demo_tenant.sql   # optional demo data
```

See `../db/README.md` for exactly what the demo data contains and its login
credentials. Skip the seed-data line for a completely empty database.

### 7. Build

macOS/Linux:
```bash
./mvnw clean package -DskipTests
```

Windows:
```powershell
.\mvnw.cmd clean package -DskipTests
```

The first run downloads Maven itself plus all dependencies — expect it to
take a few minutes. Confirm it produced a jar:

macOS/Linux: `ls target/*.jar` · Windows: `dir target\*.jar`

### 8. Run

macOS/Linux:
```bash
java -jar target/ad-campaign-0.0.1-SNAPSHOT.jar
```

Windows:
```powershell
java -jar target\ad-campaign-0.0.1-SNAPSHOT.jar
```

Wait for `Started AdCampaignAppApplication` in the console output, then
leave this running in its own terminal.

### 9. Verify it's working

In a **second** terminal:

macOS/Linux:
```bash
curl -i -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"tenantName":"My First Tenant","email":"me@example.com","password":"password123"}'
```

Windows (PowerShell):
```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/register `
  -ContentType "application/json" `
  -Body '{"tenantName":"My First Tenant","email":"me@example.com","password":"password123"}'
```

A `200`/`201` response with a JWT `token` field means everything is working.
If you loaded the demo data in step 6, you can instead log in as the seeded
demo admin (`admin@acmedemo.test` / `ChangeMe123!`) via
`POST /api/v1/auth/login` with the same body shape (`email`/`password`).

Stop the app with `Ctrl+C` in its terminal when you're done.

---

## Configuration reference

Everything below is a Spring Boot property with an environment-variable
override — set these as real environment variables (not by editing
`src/main/resources/application.properties`) for anything beyond a fresh
local dev box.

| Env var | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/adcampaign` | JDBC connection string |
| `DB_USERNAME` | `adcampaign` | Database role |
| `DB_PASSWORD` | `adcampaign` | Database role password |
| `JWT_SECRET` | a dev-only placeholder | HMAC signing key for auth tokens — **must** be overridden outside local dev |
| `JWT_EXPIRATION_MINUTES` | `60` | Token lifetime |
| `ADMIN_BOOTSTRAP_EMAIL` | empty (disabled) | Grants `PLATFORM_ADMIN` to this already-registered user on startup — see `administration.PlatformAdminBootstrap` |
| `BIDSCUBE_SHARED_SECRET` | empty | Shared secret the BidsCube exchange adapter expects on inbound bid requests |
| `BIDSCUBE_TENANT_ID` | empty | The one tenant BidsCube's traffic bids against |
| `BIDSCUBE_WIN_NOTICE_BASE_URL` | empty | Base URL BidsCube calls back to report a win |

A copy of these as a template is in [`.env.example`](.env.example) — most
shells don't load `.env` files automatically, so either `export` each line
yourself or use a tool like `direnv`/`dotenv` if you prefer that workflow.

## Troubleshooting

- **`psql: command not found` (macOS)** — Homebrew's `postgresql@16` is
  "keg-only" and doesn't link onto your `PATH` automatically. Either run
  `brew link postgresql@16` or use the full path shown by
  `brew --prefix postgresql@16` (e.g.
  `/opt/homebrew/opt/postgresql@16/bin/psql`).
- **`psql` not recognized (Windows)** — open a **new** PowerShell window
  after installing (PATH changes don't apply to already-open windows), or
  add `C:\Program Files\PostgreSQL\16\bin` to your `PATH` manually.
- **`FATAL: password authentication failed for user "adcampaign"`** — the
  role wasn't created, or was created with a different password than step 5
  used. Re-run the `CREATE ROLE`/`GRANT` commands from step 5 (dropping the
  role first with `DROP ROLE adcampaign;` if it already exists with the
  wrong password).
- **Port `8080` already in use** — another process is already listening.
  Stop it, or run with a different port:
  `java -jar target/ad-campaign-0.0.1-SNAPSHOT.jar --server.port=8081`.
- **`relation "tenants" does not exist` (or similar) on startup** — the
  schema wasn't loaded. Re-run step 6.

## Fast path

Everything above, automated:
- macOS: `bash app-setup/mac/setup.sh` (run from the `ad-campaign`
  directory; it still expects Homebrew to already be installed)
- Windows: `app-setup\windows\setup.ps1` (run from an Administrator
  PowerShell, from the `ad-campaign` directory; it still expects
  Chocolatey to already be installed)

Both scripts stop short of starting the app itself — run step 8 yourself
afterward so you can see the startup log and keep it running in the
foreground.
