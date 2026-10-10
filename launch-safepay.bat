@echo off
setlocal enabledelayedexpansion
title SafePay AI-Powered Fraud Detection System

echo ==============================================================================
echo              SafePay -- AI-Powered Fraud Detection System
echo                      Desktop GUI Application Launcher
echo ==============================================================================
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
echo [*] Starting SafePay Spring Boot Backend in a separate window...
start "SafePay Server" cmd /c "mvnw.cmd spring-boot:run"
echo [*] Waiting 8 seconds for server initialization...
timeout /t 8 /nobreak >nul
goto launch_client

:launch_client
echo.
echo [*] Launching SafePay Desktop GUI (Event Dispatch Thread)...
call mvnw.cmd exec:java -Dexec.mainClass="com.frauddetection.frauddetection.gui.FraudDetectionGuiApp"
goto end

:launch_server
echo.
echo [*] Launching SafePay Spring Boot Server on http://127.0.0.1:8080...
call mvnw.cmd spring-boot:run
goto end

:end
echo.
echo SafePay process finished.
pause
