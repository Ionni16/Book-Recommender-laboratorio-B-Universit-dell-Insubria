@echo off
setlocal
cd /d "%~dp0.."

java --module-path ".\Windows\lib" --add-modules javafx.controls,javafx.fxml -jar ClientBR.jar
pause
