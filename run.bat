@echo off
cd /d "%~dp0"

if exist "C:\apache-maven-3.9.16\bin\mvn.cmd" (
  "C:\apache-maven-3.9.16\bin\mvn.cmd" clean javafx:run
) else if exist "C:\apache-maven-3.9.15\bin\mvn.cmd" (
  "C:\apache-maven-3.9.15\bin\mvn.cmd" clean javafx:run
) else (
  mvn clean javafx:run
)

pause
