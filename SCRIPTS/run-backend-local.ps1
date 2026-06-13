$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$javaHome = 'C:\Program Files\Android\Android Studio\jbr'
$env:JAVA_HOME = $javaHome

foreach ($raw in Get-Content (Join-Path $repoRoot '.env')) {
  $line = $raw.Trim()
  if (-not $line -or $line.StartsWith('#')) {
    continue
  }

  $idx = $line.IndexOf('=')
  if ($idx -lt 1) {
    continue
  }

  $key = $line.Substring(0, $idx).Trim()
  $value = $line.Substring($idx + 1).Trim()

  if (
    ($value.StartsWith('"') -and $value.EndsWith('"')) -or
    ($value.StartsWith("'") -and $value.EndsWith("'"))
  ) {
    $value = $value.Substring(1, $value.Length - 2)
  }

  Set-Item -Path ("Env:" + $key) -Value $value
}

& mvn spring-boot:run
exit $LASTEXITCODE
