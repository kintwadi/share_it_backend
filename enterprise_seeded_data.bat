@echo off
setlocal

if "%BASE_URL%"=="" set BASE_URL=http://localhost:8080
if "%RESET%"=="" set RESET=true
if "%LIMIT%"=="" set LIMIT=80

set URL=%BASE_URL%/api/enterprise/seeded-data/load?reset=%RESET%^&limit=%LIMIT%

curl -sS -X POST "%URL%" -H "Content-Type: application/json"
echo.

endlocal

