# Builds the backend JAR and deploys it to the Azure VM prepared with server-setup.sh.
# Usage (from repo root):
#   powershell -File scripts\azure\deploy-azure.ps1 -HostName 20.x.x.x -KeyPath C:\keys\harvesthub-azure.pem
#   add -SetupOnly to just copy and run server-setup.sh the first time.
param(
    [Parameter(Mandatory)] [string]$HostName,
    [Parameter(Mandatory)] [string]$KeyPath,
    [string]$User = 'azureuser',
    [string]$Domain = 'harvesthubapp.tech',
    [string]$Email,
    [switch]$SetupOnly
)
$ErrorActionPreference = 'Stop'
$backend = Join-Path $PSScriptRoot '..\..\harvesthub-backend' | Resolve-Path
$target = "$User@$HostName"
$ssh = @('-i', $KeyPath, '-o', 'StrictHostKeyChecking=accept-new')

function Step($text, [scriptblock]$cmd) {
    Write-Host "== $text" -ForegroundColor Yellow
    & $cmd
    if ($LASTEXITCODE -ne 0) { Write-Host "FAILED: $text" -ForegroundColor Red; exit 1 }
}

if ($SetupOnly) {
    if (-not $Email) { Write-Host 'Pass -Email (used for the free HTTPS certificate).' -ForegroundColor Red; exit 1 }
    Step 'Copy setup script' { scp @ssh (Join-Path $PSScriptRoot 'server-setup.sh') "${target}:/tmp/server-setup.sh" }
    Step 'Run setup on server' { ssh @ssh $target "sed -i 's/\r$//' /tmp/server-setup.sh && sudo bash /tmp/server-setup.sh $Domain $Email" }
    Write-Host 'Server ready. Fill /etc/harvesthub/harvesthub.env on the server, then run this script without -SetupOnly.' -ForegroundColor Green
    exit 0
}

Step 'Build JAR' { Push-Location $backend; try { & .\mvnw.cmd -q clean package -DskipTests } finally { Pop-Location } }
$jar = Get-ChildItem (Join-Path $backend 'target') -Filter *.jar | Where-Object Name -notlike '*plain*' | Select-Object -First 1
Step 'Upload JAR' { scp @ssh $jar.FullName "${target}:/tmp/app.jar" }
Step 'Install and restart' {
    ssh @ssh $target 'sudo install -o harvesthub -g harvesthub -m 644 /tmp/app.jar /opt/harvesthub/app.jar && sudo systemctl restart harvesthub'
}
Write-Host '== Waiting for startup' -ForegroundColor Yellow
Start-Sleep 20
ssh @ssh $target 'systemctl is-active harvesthub; sudo journalctl -u harvesthub -n 15 --no-pager'
Write-Host "Check: https://$Domain/api/auth/check-availability should answer (POST)." -ForegroundColor Green
