@echo off
title Hotel Management System - Member 2
cd /d "%~dp0"
echo ============================================================
echo   HOTEL MANAGEMENT SYSTEM - MEMBER 2
echo   Reservations, Guests & Billing
echo ============================================================
echo.
echo Launching Hotel Management System...
echo (Do not close this console window while the application is running)
echo.

java -cp "bin;lib/mysql-connector-j-26.7.0.jar" Main

if errorlevel 1 (
    echo.
    echo ============================================================
    echo Recompiling project and retrying...
    echo ============================================================
    javac -cp "lib/mysql-connector-j-26.7.0.jar" -d bin src/database/*.java src/model/*.java src/dao/*.java src/ui/*.java src/Main.java
    java -cp "bin;lib/mysql-connector-j-26.7.0.jar" Main
)

pause
