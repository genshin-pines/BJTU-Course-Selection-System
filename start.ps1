param(
    [switch]$Help,
    [switch]$NoRestart,
    [switch]$SkipFrontend,
    [switch]$SkipBackend,
    [switch]$SkipDbCheck,
    [switch]$CheckOnly
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
    Write-Host "  .\start.ps1 -SkipDbCheck    Skip MySQL schema preflight checks"
    Write-Host "  .\start.ps1 -CheckOnly      Run database preflight checks only"
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

function Get-ApplicationValue {
    param([string]$Key)

    $appConfig = Join-Path $backendDir "src\main\resources\application.yml"
    if (-not (Test-Path $appConfig)) {
        return $null
    }

    $line = Get-Content $appConfig |
        Where-Object { $_ -match "^\s*$([regex]::Escape($Key))\s*:" } |
        Select-Object -First 1

    if (-not $line) {
        return $null
    }

    $value = ($line -replace "^\s*$([regex]::Escape($Key))\s*:\s*", "").Trim()
    return $value.Trim("'`"")
}

function Get-DatabaseNameFromJdbcUrl {
    param([string]$JdbcUrl)

    if ($JdbcUrl -match "jdbc:mysql://[^/]+/([^?]+)") {
        return $Matches[1]
    }
    return $null
}

function Get-MysqlScalar {
    param(
        [string]$Database,
        [string]$Username,
        [string]$Password,
        [string]$Query
    )

    $args = @("-u", $Username, "-N", "-B", "-e", $Query)
    $previousMysqlPwd = $env:MYSQL_PWD
    if ($Password) {
        $env:MYSQL_PWD = $Password
    }

    try {
        $output = & mysql @args 2>$null
        if ($LASTEXITCODE -ne 0) {
            throw "mysql command failed"
        }
    }
    finally {
        $env:MYSQL_PWD = $previousMysqlPwd
    }

    return ($output | Select-Object -First 1)
}

function Test-DatabaseSchema {
    if (($SkipBackend -and -not $CheckOnly) -or $SkipDbCheck) {
        return
    }

    Write-Section "Database Preflight"

    if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) {
        Write-Warning "mysql command was not found. Skipping automatic schema check."
        Write-Host "Manual check: make sure bjtu_review has voter_record, course_instance, review.voter_record_id, review_vote.voter_record_id, report.reporter_record_id."
        return
    }

    $jdbcUrl = Get-ApplicationValue -Key "url"
    $dbUser = Get-ApplicationValue -Key "username"
    $dbPassword = Get-ApplicationValue -Key "password"
    $dbName = Get-DatabaseNameFromJdbcUrl -JdbcUrl $jdbcUrl

    if (-not $dbName -or -not $dbUser) {
        Write-Warning "Could not parse datasource config from application.yml. Skipping schema check."
        return
    }

    $requiredTables = @(
        "student",
        "teacher",
        "course",
        "course_base",
        "course_instance",
        "course_teacher_relation",
        "voter_record",
        "review",
        "review_exam_exp",
        "review_vote",
        "report",
        "audit_log"
    )

    $requiredColumns = @(
        @{ Table = "review"; Column = "voter_record_id" },
        @{ Table = "review"; Column = "anonymous_user_key" },
        @{ Table = "review"; Column = "course_instance_id" },
        @{ Table = "review"; Column = "downvote_count" },
        @{ Table = "review_vote"; Column = "voter_record_id" },
        @{ Table = "review_vote"; Column = "vote_type" },
        @{ Table = "report"; Column = "reporter_record_id" },
        @{ Table = "review_exam_exp"; Column = "key_chapters" },
        @{ Table = "review_exam_exp"; Column = "cheat_sheet_allowed" }
    )

    $missing = New-Object System.Collections.Generic.List[string]

    foreach ($table in $requiredTables) {
        $query = "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$dbName' AND TABLE_NAME='$table';"
        $exists = Get-MysqlScalar -Database $dbName -Username $dbUser -Password $dbPassword -Query $query
        if ([int]$exists -eq 0) {
            $missing.Add("table $table")
        }
    }

    foreach ($item in $requiredColumns) {
        $query = "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='$dbName' AND TABLE_NAME='$($item.Table)' AND COLUMN_NAME='$($item.Column)';"
        $exists = Get-MysqlScalar -Database $dbName -Username $dbUser -Password $dbPassword -Query $query
        if ([int]$exists -eq 0) {
            $missing.Add("column $($item.Table).$($item.Column)")
        }
    }

    if ($missing.Count -gt 0) {
        Write-Warning "Database schema is missing required objects:"
        foreach ($item in $missing) {
            Write-Host "  - $item"
        }
        Write-Host ""
        Write-Host "Recommended migration order:"
        Write-Host "  1. backend/src/main/resources/db/schema.sql            (new database only)"
        Write-Host "  2. backend/src/main/resources/db/migration_session_id.sql"
        Write-Host "  3. backend/src/main/resources/db/migration_core_model_anonymity.sql"
        Write-Host "  4. backend/src/main/resources/db/migration_backfill_voter_record.sql"
        Write-Host "  5. backend/src/main/resources/db/migration_review_exam_exp.sql"
        Write-Host "  6. backend/src/main/resources/db/migration_review_downvote.sql"
        Write-Host ""
        Write-Host "See backend/src/main/resources/db/README.md for details."
        throw "Database schema check failed."
    }

    Write-Host "Database schema check passed for '$dbName'."
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

if (-not $SkipBackend -and -not $CheckOnly) {
    Assert-Command -CommandName "mvn" -InstallHint "Please install Maven or add it to PATH."
}
if (-not $SkipFrontend -and -not $CheckOnly) {
    Assert-Command -CommandName "npm" -InstallHint "Please install Node.js/npm or add npm to PATH."
}

Test-DatabaseSchema

if ($CheckOnly) {
    Write-Section "Check Complete"
    exit 0
}

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
