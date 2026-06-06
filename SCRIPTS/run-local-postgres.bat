@echo off
echo Starting Vicinity24 Backend with PostgreSQL Database (Local Mode)...

set "ROOT_DIR=%~dp0.."
pushd "%ROOT_DIR%" >nul

call "%~dp0setup.bat"

:: Force local DB for this script (even if .env points to a remote database)
set "DB_TYPE=postgres"
set "DB_URL=jdbc:postgresql://localhost:5432/Vicinity24"
set "DB_DRIVER=org.postgresql.Driver"
set "DB_USERNAME=postgres"
set "DB_PASSWORD=postgres"
set "FRONTEND_BASE_URL=http://localhost:4200"



echo.
echo Make sure the configured database is reachable
set "DB_URL_SAFE=%DB_URL%"
for /f "tokens=1* delims=@" %%A in ("%DB_URL_SAFE%") do if not "%%B"=="" set "DB_URL_SAFE=%%B"
echo Database URL: %DB_URL_SAFE%
echo Username: %DB_USERNAME%
echo Password: ********
echo.

mvn -DfrontendSkip=true spring-boot:run

popd >nul
