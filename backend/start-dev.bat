@echo off
echo ==========================================
echo MyNottingham Backend - Development Mode
echo Using H2 In-Memory Database
echo ==========================================
echo.

cd %~dp0

echo Starting backend server...
echo.

mvn spring-boot:run -Dspring-boot.run.profiles=dev

pause
