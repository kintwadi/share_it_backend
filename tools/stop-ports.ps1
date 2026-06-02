$ErrorActionPreference = 'SilentlyContinue'

param(
  [int[]] $Ports = @(8081, 8082)
)

$listeners = @()
try {
  $listeners = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $Ports -contains $_.LocalPort }
} catch {
  $listeners = @()
}

if (-not $listeners -or $listeners.Count -eq 0) {
  Write-Output ("No LISTEN processes found on ports: " + ($Ports -join ', '))
  exit 0
}

Write-Output "Listeners found:"
$listeners | Sort-Object LocalPort | Select-Object LocalPort, OwningProcess

$pids = $listeners | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($pid in $pids) {
  $proc = Get-Process -Id $pid -ErrorAction SilentlyContinue
  if ($proc) {
    Write-Output ("Stopping PID " + $pid + " (" + $proc.ProcessName + ")")
  } else {
    Write-Output ("Stopping PID " + $pid)
  }
  try {
    Stop-Process -Id $pid -Force -ErrorAction Stop
    Start-Sleep -Milliseconds 200
    $still = Get-Process -Id $pid -ErrorAction SilentlyContinue
    if ($still) {
      Write-Output ("Failed to stop PID " + $pid)
    } else {
      Write-Output ("Stopped PID " + $pid)
    }
  } catch {
    Write-Output ("Failed to stop PID " + $pid + ": " + $_.Exception.Message)
  }
}

Write-Output "Done."
