@echo off
cd /d "%~dp0"
title AutoRent Pro FX

echo ==========================================
echo AutoRent Pro FX - RUN
echo ==========================================
echo.

where mvn >nul 2>nul
if %errorlevel%==0 (
    echo Maven trouve dans PATH.
    mvn clean javafx:run
    pause
    exit /b
)

if exist "C:\apache-maven-3.9.16\bin\mvn.cmd" (
    echo Maven trouve: C:\apache-maven-3.9.16
    "C:\apache-maven-3.9.16\bin\mvn.cmd" clean javafx:run
    pause
    exit /b
)

if exist "C:\apache-maven-3.9.15\bin\mvn.cmd" (
    echo Maven trouve: C:\apache-maven-3.9.15
    "C:\apache-maven-3.9.15\bin\mvn.cmd" clean javafx:run
    pause
    exit /b
)

echo ERREUR: Maven introuvable.
echo Installe Apache Maven ou ajoute Maven au PATH.
pause
