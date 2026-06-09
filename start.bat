@echo off
start "Backend" /d "%~dp0backend" cmd /k "mvn spring-boot:run"
start "Frontend" /d "%~dp0frontend" cmd /k "npm run dev"
