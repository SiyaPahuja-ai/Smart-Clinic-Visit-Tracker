@echo off
setlocal
echo ===================================================
echo   Compiling Smart Clinic Visit Tracker...
echo ===================================================

:: Detect javac
set JAVAC_CMD=javac
where javac >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if exist "%USERPROFILE%\.jdk\jdk-17.0.10+7\bin\javac.exe" (
        set JAVAC_CMD="%USERPROFILE%\.jdk\jdk-17.0.10+7\bin\javac.exe"
    )
)

if not exist bin mkdir bin

echo Using: %JAVAC_CMD%
%JAVAC_CMD% -encoding UTF-8 -cp ".;lib\*" -d bin src\*.java

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Compilation completed successfully!
    echo Output classes placed in 'bin/' folder.
) else (
    echo [ERROR] Compilation failed. Please check error messages above.
)
pause
