$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$sdkTools = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools'
$nodeDir = 'C:\Program Files\nodejs'
$javaHome = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = $nodeDir + ';' + $sdkTools + ';' + $env:Path
$env:JAVA_HOME = $javaHome
$env:ORG_GRADLE_JAVA_HOME = $javaHome

$envFile = Join-Path $repoRoot 'public\env.js'
$distEnv = Join-Path $repoRoot 'dist\share-it-client\browser\env.js'
$original = Get-Content -Raw $envFile
$remoteEnv = @"
window.__env = window.__env || {};
window.__env.API_URL = "https://vicinity24api.com/api/v1";
window.__env.TENANT_HEADER_NAME = "X-Tenant-ID";
window.__env.TENANT_ID = "";
"@

try {
  Set-Content -Path $envFile -Value $remoteEnv -NoNewline

  & 'C:\Program Files\nodejs\npm.cmd' run build
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

  Copy-Item $envFile $distEnv -Force

  & 'C:\Program Files\nodejs\npx.cmd' cap sync android
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

  & 'C:\Program Files\nodejs\npx.cmd' cap run android --no-sync
  exit $LASTEXITCODE
}
finally {
  Set-Content -Path $envFile -Value $original -NoNewline
}
