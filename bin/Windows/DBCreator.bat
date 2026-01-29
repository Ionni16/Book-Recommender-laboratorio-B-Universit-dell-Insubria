@echo off
setlocal
cd /d "%~dp0.."

java -jar DBCreator.jar
pause
