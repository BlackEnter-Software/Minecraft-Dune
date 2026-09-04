@echo off
setlocal EnableExtensions DisableDelayedExpansion
cd /d "%~dp0"

for /f "tokens=1,* delims==" %%A in ('findstr /B /C:"mod_version=" "gradle.properties"') do (
    set "MOD_VERSION=%%B"
)

if not defined MOD_VERSION (
    echo ERROR: Could not read mod_version from gradle.properties.
    exit /b 1
)

for /f %%A in ('powershell -NoProfile -Command "Get-Date -Format ddMMyy"') do (
    set "DATESTAMP=%%A"
)

if not defined DATESTAMP (
    echo ERROR: Could not determine the current date.
    exit /b 1
)

set "WORLD_NAME=Arrakis-dev_%MOD_VERSION%_%DATESTAMP%"
set "WORLD_PATH=%CD%\run\saves\%WORLD_NAME%"

if /I "%~1"=="--fresh" (
    if exist "%WORLD_PATH%" (
        echo Deleting existing dev world: %WORLD_NAME%
        rmdir /S /Q "%WORLD_PATH%"
        if exist "%WORLD_PATH%" (
            echo ERROR: Could not delete "%WORLD_PATH%".
            exit /b 1
        )
    )
)

rem Remove only an incomplete directory from a failed previous creation.
if exist "%WORLD_PATH%" if not exist "%WORLD_PATH%\level.dat" (
    echo Removing incomplete dev world directory: %WORLD_NAME%
    rmdir /S /Q "%WORLD_PATH%"
)

echo.
echo ========================================
echo  Minecraft Dune - Arrakis Dev World
echo ========================================
echo Version : %MOD_VERSION%
echo World   : %WORLD_NAME%
echo Seed    : 0
echo Mode    : Creative
echo Preset  : minecraftdune:arrakis_dev
echo.

if exist "%WORLD_PATH%\level.dat" (
    echo Opening existing world...
    call gradlew.bat runClient "-PdevWorldName=%WORLD_NAME%"
) else (
    echo Creating fresh world...
    call gradlew.bat runClient "-PdevWorldName=%WORLD_NAME%" "-PdevWorldCreate=true" "-PdevWorldSeed=0"
)

exit /b %ERRORLEVEL%
