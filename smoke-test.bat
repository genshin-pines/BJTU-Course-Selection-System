@echo off
setlocal

if "%~1"=="/?" (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\smoke-test.ps1" -Help
  exit /b 0
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\smoke-test.ps1" %*

if errorlevel 1 (
  echo.
  echo Smoke test failed. Please check the messages above.
  pause
  exit /b 1
)

echo.
echo Smoke test passed.
pause
