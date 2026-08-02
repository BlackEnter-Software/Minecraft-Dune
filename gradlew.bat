\
    @echo off
    setlocal

    set "APP_HOME=%~dp0"
    set "WRAPPER_DIR=%APP_HOME%gradle\wrapper"
    set "WRAPPER_JAR=%WRAPPER_DIR%\gradle-wrapper.jar"
    set "WRAPPER_URL=https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/raw/refs/heads/main/gradle/wrapper/gradle-wrapper.jar"

    if not exist "%WRAPPER_JAR%" (
        echo Gradle wrapper JAR is not present. Downloading the official NeoForge MDK copy...
        if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"

        powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
          "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue'; Invoke-WebRequest -UseBasicParsing -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"

        if errorlevel 1 (
            echo.
            echo ERROR: Failed to download the Gradle wrapper JAR.
            echo Download it manually from:
            echo %WRAPPER_URL%
            exit /b 1
        )
    )

    if defined JAVA_HOME (
        set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
    ) else (
        set "JAVA_EXE=java.exe"
    )

    "%JAVA_EXE%" -version >NUL 2>&1
    if errorlevel 1 (
        echo ERROR: Java 21 was not found. Set JAVA_HOME to a Java 21 JDK.
        exit /b 1
    )

    "%JAVA_EXE%" "-Dorg.gradle.appname=gradlew" -classpath "" -jar "%WRAPPER_JAR%" %*
    exit /b %ERRORLEVEL%
