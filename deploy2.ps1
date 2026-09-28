$ErrorActionPreference = "Stop"

$remoteUser = "ec2-user"
$remoteHost = "13.228.18.33"
$sshPort = 22
$jarName = "app.jar"
$remoteJarPath = "/home/ec2-user/app.jar"
$remoteServiceName = "harvesthub"

$projectRoot = $PSScriptRoot
$localJarPath = Join-Path -Path $projectRoot -ChildPath "harvesthub-backend\target\$jarName"
if (-not (Test-Path $localJarPath)) {
    $localJarPath = Join-Path -Path $projectRoot -ChildPath "target\$jarName"
}
$pemKeyPath = Join-Path -Path $projectRoot -ChildPath "harvesthub_key_pair.pem"

function Invoke-Checked {
    param([string]$Description, [scriptblock]$Command)
    Write-Host "--- $Description ---" -ForegroundColor Yellow
    & $Command
    if ($LASTEXITCODE -ne 0) {
        Write-Host "FAILED: $Description (exit code $LASTEXITCODE)" -ForegroundColor Red
        exit 1
    }
    Write-Host "$Description - OK" -ForegroundColor Green
}

Set-Location $projectRoot
Write-Host "Working directory set to: $projectRoot" -ForegroundColor Cyan

if (-not (Test-Path $pemKeyPath)) {
    Write-Host "Error: Key file not found at $pemKeyPath" -ForegroundColor Red
    exit 1
}

Write-Host "--- Step 1: Stopping remote service $remoteServiceName ---" -ForegroundColor Yellow
ssh -i $pemKeyPath -p $sshPort "${remoteUser}@${remoteHost}" "sudo systemctl stop $remoteServiceName"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Could not stop remote service. It may not exist yet or SSH failed - continuing." -ForegroundColor Yellow
}

Start-Sleep -Seconds 2

Invoke-Checked "Step 2: Building the project with Maven" {
    & ".\harvesthub-backend\mvnw.cmd" -f ".\harvesthub-backend\pom.xml" clean package -DskipTests
}

if (-not (Test-Path $localJarPath)) {
    Write-Host "Error: JAR file not found at $localJarPath. Build may have failed." -ForegroundColor Red
    exit 1
}

Invoke-Checked "Step 3: Copying $jarName to the remote server" {
    scp -i $pemKeyPath -P $sshPort "$localJarPath" "${remoteUser}@${remoteHost}:${remoteJarPath}"
}

Invoke-Checked "Step 4: Starting the $remoteServiceName service" {
    ssh -i $pemKeyPath -p $sshPort "${remoteUser}@${remoteHost}" "sudo systemctl start $remoteServiceName"
}

Write-Host "Waiting 5 seconds before checking service status..." -ForegroundColor Cyan
Start-Sleep -Seconds 5
Write-Host "--- Final Service Status ---" -ForegroundColor Yellow
ssh -i $pemKeyPath -p $sshPort "${remoteUser}@${remoteHost}" "sudo systemctl status $remoteServiceName --no-pager"

Write-Host "Deployment to $remoteHost finished successfully" -ForegroundColor Magenta