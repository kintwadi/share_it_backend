@echo off
echo Starting NearShare Backend with PostgreSQL Database (Local Mode)...

call "%~dp0setup.bat"

echo.
echo Make sure PostgreSQL is running on localhost:5432
echo Database: nearshare
echo Username: %DB_USERNAME%
echo Password: %DB_PASSWORD%
echo.

mvn clean -DfrontendSkip=true spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.driverClassName=org.postgresql.Driver --spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect --spring.datasource.url=%DB_URL% --spring.datasource.username=%DB_USERNAME% --spring.datasource.password=%DB_PASSWORD%"
