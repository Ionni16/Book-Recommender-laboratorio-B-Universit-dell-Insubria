@echo off
set BASEDIR=%~dp0
set FX=%BASEDIR%Windows\lib
java --module-path "%FX%" --add-modules javafx.controls,javafx.fxml -jar "%BASEDIR%ClientBR.jar"
pause