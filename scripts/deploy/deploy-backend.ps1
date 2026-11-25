# Builds the backend JAR and deploys it to the production server over SSH.
# The server must already be prepared with server-setup.sh (systemd service 'harvesthub').
#
# Usage (from the repository root):
#   powershell -File scripts\deploy\deploy-backend.ps1 -HostName <server IP> -KeyPath C:\keys\harvesthub.pem
#
# The SSH key is never stored in the repository. Keep it outside the project folder.
param(
    [Parameter(Mandatory)] [string]$HostName,
    [Parameter(Mandatory)] [string]$KeyPath,
    [string]$User = 'ec2-user',
    [string]$RemoteJar = '/home/ec2-user/app.jar',
    [string]$Service = 'harvesthub',
    [int]$Port = 22
)
$ErrorActionPreference = 'Stop'

$backend = Resolve-Path (Join-Path $PSScriptRoot '..\..\harvesthub-backend')
$jar = Join-Path $backend 'target\app.jar'
$target = "$User@$HostName"
$ssh = @('-i', $KeyPath, '-p', $Port)

function Step($text, [scriptblock]$cmd) {
    Write-Host "== $text" -ForegroundColor Yellow
    & $cmd
    if ($LASTEXITCODE -ne 0) { Write-Host "FAILED: $text" -ForegroundColor Red; exit 1 }
}

if (-not (Test-Path $KeyPath)) { Write-Host "Key file not found: $KeyPath" -ForegroundColor Red; exit 1 }

Step 'Building backend' {
    & (Join-Path $backend 'mvnw.cmd') -q -f (Join-Path $backend 'pom.xml') clean package -DskipTests
}
if (-not (Test-Path $jar)) { Write-Host "JAR not found at $jar" -ForegroundColor Red; exit 1 }

$stamp = Get-Date -Format 'yyyyMMdd-HHmm'
Step 'Backing up current JAR on the server' {
    ssh @ssh $target "if [ -f $RemoteJar ]; then cp -p $RemoteJar $RemoteJar.bak-$stamp; fi"
}
Step 'Uploading new JAR' {
    scp -i $KeyPath -P $Port $jar "${target}:$RemoteJar.new"
}
Step 'Replacing JAR and restarting service' {
    ssh @ssh $target "mv $RemoteJar.new $RemoteJar && sudo systemctl restart $Service"
}

Start-Sleep -Seconds 5
ssh @ssh $target "sudo systemctl status $Service --no-pager | head -n 5"
Write-Host "Deployment to $HostName finished." -ForegroundColor Green
