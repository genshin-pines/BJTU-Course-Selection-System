@echo off
setlocal

if "%~1"=="/?" (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1" -Help
  exit /b 0
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1" %*

if errorlevel 1 (
  echo.
  echo Startup failed. Please check the message above or logs in "%~dp0logs".
  pause
  exit /b 1
)

echo.
echo Startup command finished. Services are running in the background.
pause
