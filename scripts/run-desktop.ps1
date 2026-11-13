# Starts the JavaFX officer/admin portal against a backend (default: the local one on port 8080).
# Use -Production to connect to https://harvesthubapp.tech instead.
param([string]$ApiUrl = 'http://localhost:8080', [switch]$Production)
$ErrorActionPreference = 'Stop'
if ($Production) { $ApiUrl = 'https://harvesthubapp.tech' }
$desktop = Join-Path $PSScriptRoot '..\harvesthub-desktop' | Resolve-Path
try {
    Invoke-WebRequest -Uri "$ApiUrl/api/auth/login" -Method Get -TimeoutSec 3 -UseBasicParsing | Out-Null
} catch {
    if (-not $_.Exception.Response) {
        Write-Host "Backend not reachable at $ApiUrl. Start it first: scripts\run-backend.ps1" -ForegroundColor Yellow
    }
}
Write-Host "Desktop app -> $ApiUrl" -ForegroundColor Green
$env:HARVESTHUB_BACKEND_URL = $ApiUrl
Set-Location $desktop
& .\mvnw.cmd javafx:run
