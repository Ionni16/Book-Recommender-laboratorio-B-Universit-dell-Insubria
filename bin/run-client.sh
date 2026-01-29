#!/bin/sh
BASEDIR="$(cd "$(dirname "$0")" && pwd)"
OS="$(uname)"
ARCH="$(uname -m)"

if [ "$OS" = "Darwin" ] && [ "$ARCH" = "arm64" ]; then
  FX="$BASEDIR/Mac-aarch64/lib"
elif [ "$OS" = "Darwin" ]; then
  FX="$BASEDIR/Mac-Intel/lib"
elif [ "$OS" = "Linux" ]; then
  FX="$BASEDIR/Linux/lib"
else
  echo "Unsupported OS: $OS"
  exit 1
fi

java --module-path "$FX" --add-modules javafx.controls,javafx.fxml -jar "$BASEDIR/ClientBR.jar"
