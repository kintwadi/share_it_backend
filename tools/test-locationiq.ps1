$ErrorActionPreference = 'Stop'

$port = $env:PORT
if (-not $port) { $port = '8081' }
$baseUrl = "http://127.0.0.1:$port"
$reverseUrl = "$baseUrl/api/v1/location/reverse?lat=50.8503&lng=4.3517"
$autoUrl = "$baseUrl/api/v1/location/autocomplete?q=Munich&countryCodes=de&limit=3"

function Try-Get($url) {
  try {
    $res = Invoke-WebRequest -UseBasicParsing -Uri $url -Method Get -Headers @{ 'Accept' = 'application/json' }
    Write-Output "GET $url"
    Write-Output "HTTP $($res.StatusCode)"
    if ($res.Content) { Write-Output $res.Content }
    Write-Output ""
  } catch {
    $resp = $_.Exception.Response
    $status = if ($resp) { [int]$resp.StatusCode } else { 0 }
    Write-Output "GET $url"
    Write-Output "HTTP $status"
    if ($resp) {
      try {
        $stream = $resp.GetResponseStream()
        if ($stream) {
          $reader = New-Object System.IO.StreamReader($stream)
          $text = $reader.ReadToEnd()
          $reader.Close()
          if ($text) { Write-Output $text }
        }
      } catch { }
    }
    Write-Output ""
  }
}

Try-Get $reverseUrl
Try-Get $autoUrl
