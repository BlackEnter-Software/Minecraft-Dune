@echo off
setlocal
if "%~1"=="" goto launch
if /I "%~1"=="--fresh" (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\Start-ArrakisDev.ps1" -Fresh
    exit /b
)
echo Usage: run-arrakis-dev.bat [--fresh]
exit /b 1
:launch
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\Start-ArrakisDev.ps1"
exit /b
