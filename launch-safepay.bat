@echo off
setlocal enabledelayedexpansion
title SafePay AI-Powered Fraud Detection System

if "%SERVER_PORT%"=="" (
    set SERVER_PORT=8081
)
if "%SAFEPAY_API_URL%"=="" (
    set SAFEPAY_API_URL=http://localhost:%SERVER_PORT%
)
if "%DB_USER%"=="" (
    set DB_USER=root
)
if "%DB_URL%"=="" (
    set DB_URL=jdbc:mysql://localhost:3306/fraud_detection
)

echo ==============================================================================
echo              SafePay -- AI-Powered Fraud Detection System
echo                      Desktop GUI Application Launcher
echo ==============================================================================
echo [*] Configured Port: %SERVER_PORT%
echo [*] Configured API Base URL: %SAFEPAY_API_URL%
echo.

if "%1"=="client" goto launch_client
if "%1"=="server" goto launch_server
if "%1"=="both" goto launch_both

echo Please select launch option:
echo   [1] Launch SafePay Desktop GUI Client (Swing)
echo   [2] Launch SafePay Spring Boot Backend Server
echo   [3] Launch Both (Backend in background window + Desktop GUI)
echo.
set /p OPTION="Enter choice [1-3] (default: 1): "
if "%OPTION%"=="" set OPTION=1

if "%OPTION%"=="1" goto launch_client
if "%OPTION%"=="2" goto launch_server
if "%OPTION%"=="3" goto launch_both
goto launch_client

:launch_both
echo.
set ACTIVE_PROFILE=
if "!DB_PASSWORD!"=="" (
    echo [*] Note: DB_PASSWORD environment variable is not currently set.
    set /p DB_PASSWORD="[*] Enter MySQL password for !DB_USER! (press Enter for H2 demo database): "
    if "!DB_PASSWORD!"=="" (
        echo [*] Launching with in-memory H2 database (MySQL compatibility mode)...
        set ACTIVE_PROFILE=h2
    )
)
echo [*] Starting SafePay Spring Boot Backend on %SAFEPAY_API_URL% in a separate window...
if "!ACTIVE_PROFILE!"=="h2" (
    start "SafePay Server" cmd /c "set SERVER_PORT=!SERVER_PORT! && mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=!SERVER_PORT! -Dspring-boot.run.profiles=h2"
) else (
    start "SafePay Server" cmd /c "set DB_PASSWORD=!DB_PASSWORD! && set DB_USER=!DB_USER! && set DB_URL=!DB_URL! && set SERVER_PORT=!SERVER_PORT! && mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=!SERVER_PORT!"
)
echo [*] Waiting 8 seconds for server initialization...
timeout /t 8 /nobreak >nul
goto launch_client

:launch_client
echo.
echo [*] Launching SafePay Desktop GUI connecting to %SAFEPAY_API_URL%...
call mvnw.cmd exec:java -Dexec.mainClass="com.frauddetection.frauddetection.gui.FraudDetectionGuiApp" -Dexec.args="%SAFEPAY_API_URL%"
goto end

:launch_server
echo.
set ACTIVE_PROFILE=
if "!DB_PASSWORD!"=="" (
    echo [*] Note: DB_PASSWORD environment variable is not currently set.
    set /p DB_PASSWORD="[*] Enter MySQL password for !DB_USER! (press Enter for H2 demo database): "
    if "!DB_PASSWORD!"=="" (
        echo [*] Launching with in-memory H2 database (MySQL compatibility mode)...
        set ACTIVE_PROFILE=h2
    )
)
echo [*] Launching SafePay Spring Boot Server on %SAFEPAY_API_URL%...
if "!ACTIVE_PROFILE!"=="h2" (
    call mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=%SERVER_PORT% -Dspring-boot.run.profiles=h2
) else (
    call mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=%SERVER_PORT%
)
goto end

:end
echo.
echo SafePay process finished.
pause
