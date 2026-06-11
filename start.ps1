param(
    [switch]$Help,
    [switch]$NoRestart,
    [switch]$SkipFrontend,
    [switch]$SkipBackend
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $root "backend"
$frontendDir = Join-Path $root "frontend"
$logDir = Join-Path $root "logs"
$backendPort = 8889
$frontendPort = 8081

if ($Help) {
    Write-Host "BJTU Course Review System startup script"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\start.ps1                 Stop old owners of ports 8889/8081, then start backend and frontend"
    Write-Host "  .\start.ps1 -NoRestart      Keep existing processes and start only missing services"
    Write-Host "  .\start.ps1 -SkipFrontend   Start backend only"
    Write-Host "  .\start.ps1 -SkipBackend    Start frontend only"
    Write-Host ""
    Write-Host "Batch wrapper:"
    Write-Host "  start.bat"
    exit 0
}

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Write-Section {
    param([string]$Text)
    Write-Host ""
    Write-Host "== $Text =="
}

function Assert-Command {
    param(
        [string]$CommandName,
        [string]$InstallHint
    )

    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        throw "Missing command '$CommandName'. $InstallHint"
    }
}

function Get-PortProcessIds {
    param([int]$Port)

    if (Get-Command Get-NetTCPConnection -ErrorAction SilentlyContinue) {
        return @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess -Unique)
    }

    return @(
        netstat -ano |
            Select-String -Pattern "[:.]$Port\s" |
            ForEach-Object {
                $parts = $_.Line.Trim() -split "\s+"
                if ($parts.Length -ge 5 -and $parts[3] -eq "LISTENING") {
                    [int]$parts[4]
                }
            } |
            Select-Object -Unique
    )
}

function Stop-PortOwners {
    param(
        [int]$Port,
        [string]$Name
    )

    $pids = @(Get-PortProcessIds -Port $Port | Where-Object { $_ -and $_ -ne $PID })
    if ($pids.Count -eq 0) {
        Write-Host "$Name port $Port is free."
        return
    }

    foreach ($processId in $pids) {
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if ($process) {
            Write-Host "Stopping $Name port owner: PID $processId ($($process.ProcessName))"
            Stop-Process -Id $processId -Force
        }
    }
}

function Wait-Port {
    param(
        [int]$Port,
        [string]$Name,
        [int]$TimeoutSeconds = 60
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if ((Get-PortProcessIds -Port $Port).Count -gt 0) {
            Write-Host "$Name is listening on port $Port."
            return $true
        }
        Start-Sleep -Seconds 1
    }

    Write-Warning "$Name did not start listening on port $Port within $TimeoutSeconds seconds."
    return $false
}

function Start-Backend {
    $stdout = Join-Path $logDir "backend.out.log"
    $stderr = Join-Path $logDir "backend.err.log"

    Remove-Item -Force -ErrorAction SilentlyContinue $stdout, $stderr
    Write-Host "Starting backend with Maven..."
    Start-Process -FilePath "cmd.exe" `
        -ArgumentList "/c", "mvn spring-boot:run" `
        -WorkingDirectory $backendDir `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -WindowStyle Hidden `
        -PassThru | Out-Null
}

function Start-Frontend {
    $stdout = Join-Path $logDir "frontend.out.log"
    $stderr = Join-Path $logDir "frontend.err.log"

    if (-not (Test-Path (Join-Path $frontendDir "node_modules"))) {
        Write-Host "node_modules not found. Running npm install first..."
        Push-Location $frontendDir
        try {
            npm install
        }
        finally {
            Pop-Location
        }
    }

    Remove-Item -Force -ErrorAction SilentlyContinue $stdout, $stderr
    Write-Host "Starting frontend with Vite..."
    Start-Process -FilePath "cmd.exe" `
        -ArgumentList "/c", "npm run dev -- --host 0.0.0.0" `
        -WorkingDirectory $frontendDir `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -WindowStyle Hidden `
        -PassThru | Out-Null
}

Write-Host "=========================================="
Write-Host "  BJTU Course Review System"
Write-Host "  Backend:  http://localhost:$backendPort"
Write-Host "  Frontend: http://localhost:$frontendPort"
Write-Host "  Logs:     $logDir"
Write-Host "=========================================="

Assert-Command -CommandName "mvn" -InstallHint "Please install Maven or add it to PATH."
Assert-Command -CommandName "npm" -InstallHint "Please install Node.js/npm or add npm to PATH."

if (-not $NoRestart) {
    Write-Section "Cleaning Ports"
    if (-not $SkipBackend) {
        Stop-PortOwners -Port $backendPort -Name "Backend"
    }
    if (-not $SkipFrontend) {
        Stop-PortOwners -Port $frontendPort -Name "Frontend"
    }
}
else {
    Write-Section "Keeping Existing Processes"
    Write-Host "NoRestart is enabled. Existing port owners will not be stopped."
}

if (-not $SkipBackend) {
    Write-Section "Backend"
    if ((Get-PortProcessIds -Port $backendPort).Count -eq 0) {
        Start-Backend
    }
    else {
        Write-Host "Backend port $backendPort is already occupied. Skipping backend start."
    }
    Wait-Port -Port $backendPort -Name "Backend" -TimeoutSeconds 60 | Out-Null
}

if (-not $SkipFrontend) {
    Write-Section "Frontend"
    if ((Get-PortProcessIds -Port $frontendPort).Count -eq 0) {
        Start-Frontend
    }
    else {
        Write-Host "Frontend port $frontendPort is already occupied. Skipping frontend start."
    }
    Wait-Port -Port $frontendPort -Name "Frontend" -TimeoutSeconds 30 | Out-Null
}

Write-Section "Ready"
Write-Host "Open frontend: http://localhost:$frontendPort"
Write-Host "Backend API:   http://localhost:$backendPort"
Write-Host "Backend logs:  $(Join-Path $logDir 'backend.out.log')"
Write-Host "Backend errors:$(Join-Path $logDir 'backend.err.log')"
Write-Host "Frontend logs: $(Join-Path $logDir 'frontend.out.log')"
Write-Host "Frontend errors:$(Join-Path $logDir 'frontend.err.log')"
