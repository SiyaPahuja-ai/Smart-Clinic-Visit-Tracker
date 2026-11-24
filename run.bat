@echo off
setlocal
echo ===================================================
echo   Starting Smart Clinic Visit Tracker...
echo ===================================================

:: Detect java
set JAVA_CMD=java
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if exist "%USERPROFILE%\.jdk\jdk-17.0.10+7\bin\java.exe" (
        set JAVA_CMD="%USERPROFILE%\.jdk\jdk-17.0.10+7\bin\java.exe"
    )
)

%JAVA_CMD% -cp "bin;lib\*" Main

pause
