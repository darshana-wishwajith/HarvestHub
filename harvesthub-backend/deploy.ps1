# =======================================================================================
# DEPLOYMENT SCRIPT FOR HARVESTHUB BACKEND TO DIGITALOCEAN
#
# This script automates the deployment using a robust stop -> build -> copy -> start sequence.
# =======================================================================================

# --- Configuration ---
$ErrorActionPreference = "Stop" # Exit the script on any error

$remoteUser = "root"
$remoteHost = "152.42.164.221"
$sshPort = 22
$jarName = "app.jar"
$remoteJarPath = "/root/app.jar"
$remoteServiceName = "harvesthub"

$projectRoot = $PSScriptRoot
$localJarPath = Join-Path -Path $projectRoot -ChildPath "target\$jarName"

# --- Main Script ---
Set-Location $projectRoot
Write-Host "Working directory set to: $projectRoot" -ForegroundColor Cyan

# 1. Stop the remote service
Write-Host "--- Step 1: Stopping remote service '$remoteServiceName' ---" -ForegroundColor Yellow
try {
    ssh -p $sshPort "${remoteUser}@${remoteHost}" "systemctl stop $remoteServiceName"
    Write-Host "Service '$remoteServiceName' stopped successfully on the remote server." -ForegroundColor Green
} catch {
    Write-Host "Could not stop remote service. It might already be stopped. Continuing..." -ForegroundColor Yellow
}

# Add a small delay to ensure the process fully terminates
Start-Sleep -Seconds 2

# 2. Build the project locally
Write-Host "--- Step 2: Building the project with Maven ---" -ForegroundColor Yellow
if (-not (Test-Path "./mvnw.cmd")) {
    Write-Host "Error: 'mvnw.cmd' not found. Make sure you are in the project root." -ForegroundColor Red
    exit 1
}
./mvnw.cmd clean package -DskipTests
Write-Host "Maven build successful." -ForegroundColor Green

# 3. Copy the new JAR file to the remote server
Write-Host "--- Step 3: Copying '$jarName' to the remote server ---" -ForegroundColor Yellow
if (-not (Test-Path $localJarPath)) {
    Write-Host "Error: JAR file not found at '$localJarPath'. Build may have failed." -ForegroundColor Red
    exit 1
}
scp -P $sshPort "$localJarPath" "${remoteUser}@${remoteHost}:${remoteJarPath}"
Write-Host "JAR file copied successfully." -ForegroundColor Green

# 4. Start the remote service
Write-Host "--- Step 4: Starting the '$remoteServiceName' service on the server ---" -ForegroundColor Yellow
ssh -p $sshPort "${remoteUser}@${remoteHost}" "systemctl start $remoteServiceName"
Write-Host "Service started successfully." -ForegroundColor Green

# 5. Check the status of the service
Write-Host "Waiting 5 seconds before checking service status..." -ForegroundColor Cyan
Start-Sleep -Seconds 5
Write-Host "--- Final Service Status ---" -ForegroundColor Yellow
ssh -p $sshPort "${remoteUser}@${remoteHost}" "systemctl status $remoteServiceName --no-pager"

Write-Host "--- Deployment to $remoteHost finished successfully! ---" -ForegroundColor Magenta
