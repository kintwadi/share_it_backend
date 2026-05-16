@echo off
setlocal EnableExtensions EnableDelayedExpansion
echo Starting NearShare Backend with SQLite Database (Local Mode)...

call "%~dp0setup.bat"

set "SQLITE_ENV_FILE=%~dp0local_database"
if exist "%SQLITE_ENV_FILE%" (
  for /f "usebackq eol=# delims=" %%L in ("%SQLITE_ENV_FILE%") do call :SetEnv "%%L"
)

if "%DB_TYPE%"=="" set "DB_TYPE=sqlite"
if "%DB_URL%"=="" set "DB_URL=jdbc:sqlite:./nearshare.sqlite"
if "%DB_USERNAME%"=="" set "DB_USERNAME=sqlite_user"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=sqlite_password_123"
if "%DB_DRIVER%"=="" set "DB_DRIVER=org.sqlite.JDBC"

echo.
echo SQLite DB URL: %DB_URL%
echo.

mvn -DfrontendSkip=true spring-boot:run

exit /b 0

:SetEnv
set "LINE=%~1"
if "%LINE%"=="" exit /b 0
for /f "tokens=1* delims==" %%A in ("%LINE%") do (
  set "KEY=%%A"
  set "VAL=%%B"
)
for /f "tokens=* delims= " %%K in ("%KEY%") do set "KEY=%%K"
for /f "tokens=* delims= " %%V in ("%VAL%") do set "VAL=%%V"
if not "%KEY%"=="" (
  if not "%VAL%"=="" (
    if "%VAL:~0,1%"=="\"" if "%VAL:~-1%"=="\"" set "VAL=%VAL:~1,-1%"
  )
  set "%KEY%=%VAL%"
)
exit /b 0
