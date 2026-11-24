@echo off
setlocal
echo ===================================================
echo   Starting Smart Clinic Web Dashboard...
echo ===================================================

:: Detect java
set JAVA_CMD=java
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if exist "%USERPROFILE%\.jdk\jdk-17.0.10+7\bin\java.exe" (
        set JAVA_CMD="%USERPROFILE%\.jdk\jdk-17.0.10+7\bin\java.exe"
    )
)

:: Detect javac
set JAVAC_CMD=javac
where javac >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if exist "%USERPROFILE%\.jdk\jdk-17.0.10+7\bin\javac.exe" (
        set JAVAC_CMD="%USERPROFILE%\.jdk\jdk-17.0.10+7\bin\javac.exe"
    )
)

if not exist bin mkdir bin

echo [1/2] Compiling Java backend...
%JAVAC_CMD% -encoding UTF-8 -cp ".;lib\*" -d bin src\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo [2/2] Launching Web Server on http://localhost:8080/ ...
start "" http://localhost:8080/
%JAVA_CMD% -cp "bin;lib\*" WebServer

pause
