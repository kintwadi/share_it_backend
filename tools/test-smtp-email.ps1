$ErrorActionPreference = 'Stop'

$email = 'chskoop@gmaill.com'
$baseUrl = 'http://127.0.0.1:8081'
$registerUrl = "$baseUrl/api/v1/auth/register"
$forgotUrl = "$baseUrl/api/v1/auth/forgot-password"

function Read-ResponseBody($response) {
  try {
    if (-not $response) { return '' }
    $stream = $response.GetResponseStream()
    if (-not $stream) { return '' }
    $reader = New-Object System.IO.StreamReader($stream)
    $text = $reader.ReadToEnd()
    $reader.Close()
    return $text
  } catch {
    return ''
  }
}

Write-Output "Testing SMTP email flow to: $email"

$registerBody = @{
  name = 'SMTP Test User'
  email = $email
  password = 'password123'
  phone = ''
  address = ''
  avatarUrl = ''
  lat = 0.0
  lng = 0.0
} | ConvertTo-Json

try {
  $res = Invoke-WebRequest -UseBasicParsing -Uri $registerUrl -Method Post -ContentType 'application/json' -Body $registerBody
  Write-Output "REGISTER: HTTP $($res.StatusCode)"
  if ($res.Content) { Write-Output $res.Content }
  Write-Output "If signup email verification is enabled, a verification email should have been sent via JavaMailSender."
  exit 0
} catch {
  $resp = $_.Exception.Response
  $status = if ($resp) { [int]$resp.StatusCode } else { 0 }
  $body = Read-ResponseBody $resp
  Write-Output "REGISTER: HTTP $status"
  if ($body) { Write-Output $body }
  if ($body -match '"error"\s*:\s*"email_exists"' -or $body -match 'email_exists') {
    Write-Output "Email already exists. Triggering forgot-password to send an email to the existing user (response is always 200)."
  } else {
    Write-Output "Register failed. Trying forgot-password anyway (will send only if user exists)."
  }
}

$forgotBody = @{ email = $email } | ConvertTo-Json
$res2 = Invoke-WebRequest -UseBasicParsing -Uri $forgotUrl -Method Post -ContentType 'application/json' -Body $forgotBody
Write-Output "FORGOT-PASSWORD: HTTP $($res2.StatusCode)"
Write-Output "If the account exists, a password reset email should have been sent via JavaMailSender."

