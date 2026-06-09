$root = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "=========================================="
Write-Host "  BJTU Course Review System v0.1"
Write-Host "  Backend:  http://localhost:8889"
Write-Host "  Frontend: http://localhost:8081"
Write-Host "=========================================="

# Kill old Java
Write-Host "Cleaning up old processes..."
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force

# Start backend
Write-Host "Starting backend..."
$backendDir = Join-Path $root "backend"
Start-Process cmd -ArgumentList "/k", "cd /d `"$backendDir`" && mvn spring-boot:run" -WindowStyle Normal

# Wait
Start-Sleep -Seconds 10

# Start frontend
Write-Host "Starting frontend..."
$frontendDir = Join-Path $root "frontend"
Start-Process cmd -ArgumentList "/k", "cd /d `"$frontendDir`" && npm run dev" -WindowStyle Normal

Write-Host ""
Write-Host "All started! Press any key to close this window..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
