$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$envFile = Join-Path $root '.env'

if (Test-Path $envFile) {
  Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if (-not $line) { return }
    if ($line.StartsWith('#')) { return }
    $idx = $line.IndexOf('=')
    if ($idx -lt 1) { return }
    $key = $line.Substring(0, $idx).Trim()
    $val = $line.Substring($idx + 1).Trim()
    if ($val.StartsWith('"') -and $val.EndsWith('"') -and $val.Length -ge 2) {
      $val = $val.Substring(1, $val.Length - 2)
    }
    if ($key) {
      [System.Environment]::SetEnvironmentVariable($key, $val, 'Process')
    }
  }
}

if (-not $env:DB_URL) { $env:DB_URL = 'jdbc:sqlite:./nearshare.sqlite' }
if (-not $env:PORT) { $env:PORT = '8081' }
if (-not $env:SSL_ENABLED) { $env:SSL_ENABLED = 'false' }
if (-not $env:SETTINGS_HTTP_ENABLED) { $env:SETTINGS_HTTP_ENABLED = 'false' }

$dbUrlSafe = $env:DB_URL
if ($dbUrlSafe -match '@') { $dbUrlSafe = $dbUrlSafe.Split('@')[-1] }

Write-Host "Starting NearShare Backend (Postgres mode)..." -ForegroundColor Cyan
Write-Host "DB_URL: $dbUrlSafe"
Write-Host "DB_USERNAME: $env:DB_USERNAME"
Write-Host "PORT: $env:PORT"

Set-Location $root
mvn -DfrontendSkip=true spring-boot:run

