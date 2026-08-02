#!/bin/sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_DIR="$APP_HOME/gradle/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/gradle-wrapper.jar"
WRAPPER_URL="https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/raw/refs/heads/main/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Gradle wrapper JAR is not present. Downloading the official NeoForge MDK copy..."
    mkdir -p "$WRAPPER_DIR"

    if command -v curl >/dev/null 2>&1; then
        curl -fL "$WRAPPER_URL" -o "$WRAPPER_JAR"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$WRAPPER_JAR" "$WRAPPER_URL"
    else
        echo "ERROR: curl or wget is required for the first run." >&2
        exit 1
    fi
fi

if [ -n "${JAVA_HOME:-}" ]; then
    JAVA_EXE="$JAVA_HOME/bin/java"
else
    JAVA_EXE="java"
fi

exec "$JAVA_EXE" "-Dorg.gradle.appname=gradlew" -classpath "" -jar "$WRAPPER_JAR" "$@"
