@echo off
cd /d "%~dp0"
title AutoRent Pro FX - Error Log

if exist error.txt del error.txt

where mvn >nul 2>nul
if %errorlevel%==0 (
    mvn clean javafx:run > error.txt 2>&1
    notepad error.txt
    exit /b
)

if exist "C:\apache-maven-3.9.16\bin\mvn.cmd" (
    "C:\apache-maven-3.9.16\bin\mvn.cmd" clean javafx:run > error.txt 2>&1
    notepad error.txt
    exit /b
)

if exist "C:\apache-maven-3.9.15\bin\mvn.cmd" (
    "C:\apache-maven-3.9.15\bin\mvn.cmd" clean javafx:run > error.txt 2>&1
    notepad error.txt
    exit /b
)

echo Maven introuvable. > error.txt
notepad error.txt
