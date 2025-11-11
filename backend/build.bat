@echo off
echo ==========================================
echo Building MyNottingham Backend
echo ==========================================
echo.

cd %~dp0

echo Running Maven clean package...
echo.

mvn clean package

echo.
echo ==========================================
echo Build complete!
echo JAR file: target\mynottingham-backend-1.0.0.jar
echo ==========================================
echo.

pause
