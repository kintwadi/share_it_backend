@echo off
echo Starting NearShare Backend with PostgreSQL Database (Local Mode)...

call "%~dp0setup.bat"

:: Force local DB for this script (even if .env points to a remote database)
set "DB_TYPE=postgres"
set "DB_URL=jdbc:postgresql://localhost:5432/nearshare"
set "DB_DRIVER=org.postgresql.Driver"
set "DB_USERNAME=postgres"
set "DB_PASSWORD=postgres"

echo.
echo Make sure PostgreSQL is running on localhost:5432
echo Database: nearshare
echo Username: %DB_USERNAME%
echo Password: ********
echo.

mvn clean -DfrontendSkip=true spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.driverClassName=org.postgresql.Driver --spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect --spring.datasource.url=%DB_URL% --spring.datasource.username=%DB_USERNAME% --spring.datasource.password=%DB_PASSWORD%"
