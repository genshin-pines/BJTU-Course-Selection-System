param(
    [string]$Server = "101.32.222.213",
    [string]$User = "root"
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$Parent = Split-Path $ProjectRoot -Parent
$ProjectName = Split-Path $ProjectRoot -Leaf
$ArchiveName = "bjtu-review-upload.tar.gz"
$ArchivePath = Join-Path $env:TEMP $ArchiveName

if (Test-Path $ArchivePath) {
    Remove-Item $ArchivePath -Force
}

Write-Host "Creating deployment archive..."
tar -czf $ArchivePath `
    --exclude ".git" `
    --exclude "backend/target" `
    --exclude "frontend/node_modules" `
    --exclude "frontend/dist" `
    -C $Parent $ProjectName
if ($LASTEXITCODE -ne 0) {
    throw "Archive creation failed."
}

Write-Host "Uploading archive to ${User}@${Server}:~/ ..."
Push-Location $Parent
try {
    scp $ArchivePath "${User}@${Server}:~/$ArchiveName"
    if ($LASTEXITCODE -ne 0) {
        throw "Upload failed. Check the server username/password and try again."
    }
}
finally {
    Pop-Location
}

Write-Host "Starting remote setup. You may be asked for your server password again."
ssh -t "${User}@${Server}" "sudo rm -rf /home/$User/$ProjectName && tar -xzf ~/$ArchiveName -C ~/ && rm -f ~/$ArchiveName && cd ~/$ProjectName && sed -i 's/\r$//' deploy/setup-ubuntu.sh && sudo bash deploy/setup-ubuntu.sh"
if ($LASTEXITCODE -ne 0) {
    throw "Remote setup failed. Check the error above."
}
