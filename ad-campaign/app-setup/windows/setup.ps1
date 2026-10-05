# Automates app-setup/README.md's Windows steps 1-7 (everything up to, but
# not including, actually running the app - see the README for that and for
# what each step does). Run from an Administrator PowerShell, from the
# ad-campaign directory (the one containing pom.xml):
#   .\app-setup\windows\setup.ps1
#
# Idempotent: safe to re-run if an earlier step failed partway through.

$ErrorActionPreference = "Stop"

if (-not (Test-Path "pom.xml")) {
    Write-Error "Run this from the ad-campaign directory (the one containing pom.xml)."
    exit 1
}

if (-not (Get-Command choco -ErrorAction SilentlyContinue)) {
    Write-Error "Chocolatey isn't installed. Install it first - see: https://chocolatey.org/install"
    exit 1
}

Write-Host "==> Installing Java, PostgreSQL, Git (skipping anything already installed)..."
# Non-fatal: choco reports (not errors) if something is already installed,
# but don't let an unexpected hiccup here abort the whole script - the
# checks further down (java on PATH, pg_isready) are the real gate.
try { choco install temurin17 -y --no-progress } catch { Write-Warning $_ }
try { choco install postgresql16 -y --no-progress --params '/Password:postgres' } catch { Write-Warning $_ }
try { choco install git -y --no-progress } catch { Write-Warning $_ }

# Refresh PATH in this session so freshly-installed tools are found without
# reopening the shell.
$env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + `
            [System.Environment]::GetEnvironmentVariable("Path", "User")

$pgBin = "C:\Program Files\PostgreSQL\16\bin"
if (Test-Path $pgBin) { $env:Path += ";$pgBin" }

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Warning "'java' isn't on PATH yet. If the choco install above failed, install it"
    Write-Warning "yourself from https://adoptium.net/temurin/releases/?version=17 then re-run this script."
}

$env:PGPASSWORD = "postgres"

Write-Host "==> Waiting for PostgreSQL to accept connections..."
$pgReady = $false
for ($i = 0; $i -lt 30; $i++) {
    try {
        & psql -U postgres -d postgres -c "SELECT 1;" | Out-Null
        if ($LASTEXITCODE -eq 0) { $pgReady = $true; break }
    } catch {}
    Start-Sleep -Seconds 1
}
if (-not $pgReady) {
    Write-Error "PostgreSQL never became reachable. Check that the postgresql-x64-16 service is running (services.msc), then re-run this script."
    exit 1
}

Write-Host "==> Creating the adcampaign database and role (safe to re-run)..."
$dbExists = (& psql -U postgres -d postgres -tc "SELECT 1 FROM pg_database WHERE datname='adcampaign'").Trim()
if ($dbExists -ne "1") {
    & createdb -U postgres adcampaign
} else {
    Write-Host "   (database 'adcampaign' already exists)"
}

$roleExists = (& psql -U postgres -d postgres -tc "SELECT 1 FROM pg_roles WHERE rolname='adcampaign'").Trim()
if ($roleExists -ne "1") {
    & psql -U postgres -d postgres -c "CREATE ROLE adcampaign WITH LOGIN PASSWORD 'adcampaign';"
}
& psql -U postgres -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE adcampaign TO adcampaign;"
& psql -U postgres -d adcampaign -c "GRANT ALL ON SCHEMA public TO adcampaign;"
& psql -U postgres -d adcampaign -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO adcampaign;"
& psql -U postgres -d adcampaign -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO adcampaign;"

Write-Host "==> Loading schema..."
& psql -U postgres -d adcampaign -f db\schema\run_all.sql

$loadSeed = Read-Host "Load optional demo data too? [y/N]"
if ($loadSeed -match '^[Yy]$') {
    & psql -U postgres -d adcampaign -f db\seed-data\01_demo_tenant.sql
    Write-Host "   Demo login: admin@acmedemo.test / ChangeMe123!"
}

Write-Host "==> Building (this downloads Maven + dependencies on first run)..."
.\mvnw.cmd clean package -DskipTests

Write-Host ""
Write-Host "Done. Next: java -jar target\ad-campaign-0.0.1-SNAPSHOT.jar"
Write-Host "See app-setup\README.md step 9 to verify it's working."
