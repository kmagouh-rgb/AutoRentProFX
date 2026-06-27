@echo off
cd /d "%~dp0"
title AutoRent Pro FX - Compile Test

where mvn >nul 2>nul
if %errorlevel%==0 (
    mvn clean compile
    pause
    exit /b
)

if exist "C:\apache-maven-3.9.16\bin\mvn.cmd" (
    "C:\apache-maven-3.9.16\bin\mvn.cmd" clean compile
    pause
    exit /b
)

if exist "C:\apache-maven-3.9.15\bin\mvn.cmd" (
    "C:\apache-maven-3.9.15\bin\mvn.cmd" clean compile
    pause
    exit /b
)

echo ERREUR: Maven introuvable.
pause
