param(
    [switch]$Help,
    [string]$BaseUrl = "http://localhost:8889/api",
    [string]$StudentNo = "2022111111",
    [string]$StudentPassword = "123456",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "123456",
    [switch]$IncludeWriteChecks
)

$ErrorActionPreference = "Stop"

if ($Help) {
    Write-Host "BJTU Course Review System smoke test"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\smoke-test.bat"
    Write-Host "  .\scripts\smoke-test.ps1"
    Write-Host "  .\scripts\smoke-test.ps1 -BaseUrl http://localhost:8889/api"
    Write-Host "  .\scripts\smoke-test.ps1 -IncludeWriteChecks"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -BaseUrl              Backend API base URL. Default: http://localhost:8889/api"
    Write-Host "  -StudentNo            Student account. Default: 2022111111"
    Write-Host "  -StudentPassword      Student password. Default: 123456"
    Write-Host "  -AdminUsername        Admin account. Default: admin"
    Write-Host "  -AdminPassword        Admin password. Default: 123456"
    Write-Host "  -IncludeWriteChecks   Also test vote toggling. Off by default."
    exit 0
}

$script:Passed = 0
$script:Failed = 0

function Write-Step {
    param([string]$Text)
    Write-Host ""
    Write-Host "== $Text =="
}

function Pass {
    param([string]$Text)
    $script:Passed++
    Write-Host "[PASS] $Text" -ForegroundColor Green
}

function Fail {
    param(
        [string]$Text,
        [string]$Detail
    )
    $script:Failed++
    Write-Host "[FAIL] $Text" -ForegroundColor Red
    if ($Detail) {
        Write-Host "       $Detail" -ForegroundColor DarkRed
    }
}

function Invoke-Api {
    param(
        [string]$Method = "GET",
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = $null
    )

    $headers = @{}
    if ($Token) {
        $headers.Authorization = "Bearer $Token"
    }

    $uri = "$BaseUrl$Path"
    $params = @{
        Method = $Method
        Uri = $uri
        Headers = $headers
        ContentType = "application/json; charset=utf-8"
    }

    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 8)
    }

    $response = Invoke-RestMethod @params
    if ($response.code -ne 200) {
        throw "API returned code=$($response.code), message=$($response.message)"
    }
    return $response.data
}

function Test-Step {
    param(
        [string]$Name,
        [scriptblock]$Action
    )

    try {
        & $Action
        Pass $Name
    }
    catch {
        Fail $Name $_.Exception.Message
    }
}

Write-Host "BJTU Course Review System smoke test"
Write-Host "Base URL: $BaseUrl"

$course = $null
$reviews = @()
$studentToken = $null
$adminToken = $null

Write-Step "Public Course APIs"

Test-Step "course search returns records" {
    $result = Invoke-Api -Path "/course/search?page=1&size=5"
    if (-not $result.records -or $result.records.Count -eq 0) {
        throw "No course records returned."
    }
    $script:course = $result.records[0]
}

Test-Step "course detail loads" {
    if (-not $script:course) {
        throw "No course selected from search result."
    }
    if ($script:course.courseInstanceId) {
        $detail = Invoke-Api -Path "/course/instances/$($script:course.courseInstanceId)/detail"
    }
    else {
        $detail = Invoke-Api -Path "/course/$($script:course.id)"
    }
    if (-not $detail.courseName) {
        throw "Course detail did not include courseName."
    }
}

Test-Step "review list loads" {
    if (-not $script:course) {
        throw "No course selected from search result."
    }
    if ($script:course.courseInstanceId) {
        $script:reviews = @(Invoke-Api -Path "/review/instance/$($script:course.courseInstanceId)")
    }
    else {
        $script:reviews = @(Invoke-Api -Path "/review/course/$($script:course.id)")
    }
}

Write-Step "Student Auth"

Test-Step "student login succeeds" {
    $login = Invoke-Api -Method "POST" -Path "/auth/login" -Body @{
        studentNo = $StudentNo
        password = $StudentPassword
    }
    if (-not $login.token) {
        throw "Student login did not return token."
    }
    $script:studentToken = $login.token
}

Test-Step "student session verifies" {
    $session = Invoke-Api -Path "/auth/session" -Token $script:studentToken
    if ($session.role -ne "STUDENT") {
        throw "Expected STUDENT session, got $($session.role)."
    }
}

Test-Step "student vote-state endpoints load" {
    if (-not $script:course.courseInstanceId) {
        throw "Selected course has no courseInstanceId."
    }
    Invoke-Api -Path "/review/instance/$($script:course.courseInstanceId)/liked" -Token $script:studentToken | Out-Null
    Invoke-Api -Path "/review/instance/$($script:course.courseInstanceId)/downvoted" -Token $script:studentToken | Out-Null
}

if ($IncludeWriteChecks) {
    Write-Step "Optional Write APIs"

    Test-Step "like/downvote toggle on first published review" {
        if (-not $script:reviews -or $script:reviews.Count -eq 0) {
            throw "No reviews available to test voting."
        }
        $reviewId = $script:reviews[0].id
        $like = Invoke-Api -Method "POST" -Path "/review/like/$reviewId" -Token $script:studentToken
        if ($null -eq $like.likeCount -or $null -eq $like.downvoteCount) {
            throw "Vote result did not include counts."
        }
        Invoke-Api -Method "POST" -Path "/review/like/$reviewId" -Token $script:studentToken | Out-Null
    }
}

Write-Step "Admin Auth And Governance APIs"

Test-Step "admin login succeeds" {
    $login = Invoke-Api -Method "POST" -Path "/auth/admin/login" -Body @{
        username = $AdminUsername
        password = $AdminPassword
    }
    if (-not $login.token) {
        throw "Admin login did not return token."
    }
    $script:adminToken = $login.token
}

Test-Step "admin session verifies" {
    $session = Invoke-Api -Path "/auth/session" -Token $script:adminToken
    if ($session.role -ne "ADMIN") {
        throw "Expected ADMIN session, got $($session.role)."
    }
}

Test-Step "admin pending reviews loads" {
    Invoke-Api -Path "/admin/reviews/pending" -Token $script:adminToken | Out-Null
}

Test-Step "admin reports loads" {
    Invoke-Api -Path "/admin/reports" -Token $script:adminToken | Out-Null
}

Test-Step "admin audit logs loads" {
    Invoke-Api -Path "/admin/audit-logs" -Token $script:adminToken | Out-Null
}

Write-Step "Summary"
Write-Host "Passed: $script:Passed"
Write-Host "Failed: $script:Failed"

if ($script:Failed -gt 0) {
    exit 1
}

exit 0
