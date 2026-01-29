#!/bin/sh
cd "$(dirname "$0")/.."

MP="$PWD/Mac-aarch64/lib"
CP="$PWD/ClientBR.jar"

java --module-path "$MP" --add-modules javafx.controls,javafx.fxml -jar "$CP"
