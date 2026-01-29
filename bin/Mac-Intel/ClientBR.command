#!/bin/sh
cd "$(dirname "$0")/.."

MP="$PWD/Mac-Intel/lib"
CP="$PWD/ClientBR.jar"

java --module-path "$MP" --add-modules javafx.controls,javafx.fxml -jar "$CP"
