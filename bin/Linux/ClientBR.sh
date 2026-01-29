#!/bin/sh
cd "$(dirname "$0")/.."

MP="$PWD/Linux/lib"
CP="$PWD/ClientBR.jar"

java --module-path "$MP" --add-modules javafx.controls,javafx.fxml -jar "$CP"
