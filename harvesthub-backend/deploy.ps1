# Stop on any error
$ErrorActionPreference = "Stop"

$SERVER_IP = "152.42.164.221"
$SSH_USER = "root"

# 1. Build the project using Maven
Write-Host "--- Step 1: Building JAR (Skipping Tests) ---" -ForegroundColor Cyan
.\mvnw.cmd clean package -DskipTests

# 2. Check if build was successful
$JAR_PATH = "target\app.jar"
if (-not (Test-Path $JAR_PATH)) {
    Write-Host "Error: Build failed or app.jar not found." -ForegroundColor Red
    exit 1
}

# 3. Upload to server
Write-Host "--- Step 2: Uploading to Server ---" -ForegroundColor Cyan
# Using -o IdentitiesOnly=yes to ensure it uses the key we just authorized
scp -o IdentitiesOnly=yes -P 22 "$JAR_PATH" "${SSH_USER}@${SERVER_IP}:/root/app.jar"

# 4. Restart the remote service
Write-Host "--- Step 3: Restarting Remote Service ---" -ForegroundColor Cyan
ssh -o IdentitiesOnly=yes "${SSH_USER}@${SERVER_IP}" "systemctl restart harvesthub"

Write-Host "--- Deployment Successfully Completed! ---" -ForegroundColor Green
