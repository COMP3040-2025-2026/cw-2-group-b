@echo off
echo ==========================================
echo Maven Installation Helper for Windows
echo ==========================================
echo.

echo Step 1: Downloading Maven...
echo Please visit: https://maven.apache.org/download.cgi
echo Download: apache-maven-3.9.6-bin.zip
echo.

echo Step 2: Extract to C:\Program Files\Apache\maven
echo.

echo Step 3: Add to System PATH
echo This script will help you configure environment variables
echo.

pause

echo.
echo Opening Maven download page...
start https://maven.apache.org/download.cgi

echo.
echo After downloading, please:
echo 1. Extract apache-maven-3.9.6-bin.zip
echo 2. Move the extracted folder to: C:\Program Files\Apache\maven
echo 3. Run this script again to configure environment variables
echo.

set /p downloaded="Have you downloaded and extracted Maven? (y/n): "

if /i "%downloaded%"=="y" (
    echo.
    echo Checking Maven location...

    if exist "C:\Program Files\Apache\maven\bin\mvn.cmd" (
        echo Maven found at C:\Program Files\Apache\maven
        echo.
        echo Now we need to add Maven to PATH
        echo.
        echo Please follow these steps manually:
        echo 1. Right-click on 'This PC' or 'My Computer'
        echo 2. Click 'Properties'
        echo 3. Click 'Advanced system settings'
        echo 4. Click 'Environment Variables'
        echo 5. Under 'System variables', click 'New':
        echo    - Variable name: MAVEN_HOME
        echo    - Variable value: C:\Program Files\Apache\maven
        echo 6. Find 'Path' in System variables, click 'Edit'
        echo 7. Click 'New' and add: %%MAVEN_HOME%%\bin
        echo 8. Click OK on all windows
        echo 9. Close and reopen Command Prompt
        echo 10. Run: mvn --version
        echo.

        echo Opening System Properties for you...
        start sysdm.cpl
    ) else (
        echo ERROR: Maven not found at C:\Program Files\Apache\maven
        echo Please extract Maven to that location first.
    )
) else (
    echo Please download and extract Maven first, then run this script again.
)

echo.
pause
