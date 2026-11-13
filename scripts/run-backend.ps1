# Starts the HarvestHub backend on http://0.0.0.0:8080 (reachable from phones on the same Wi-Fi).
# Config comes from harvesthub-backend/application-local.properties (git-ignored) or environment variables.
param([int]$Port = 8080)
$ErrorActionPreference = 'Stop'
$backend = Join-Path $PSScriptRoot '..\harvesthub-backend' | Resolve-Path
$local = Join-Path $backend 'application-local.properties'
if (-not (Test-Path $local) -and -not $env:JWT_SECRET) {
    Write-Host "Missing $local. Copy application-local.properties.example and fill it in." -ForegroundColor Red
    exit 1
}
if (Test-Path $local) {
    $uri = (Select-String -Path $local -Pattern '^MONGODB_URI=(.*)$').Matches.Groups[1].Value
    if ($uri -eq 'PASTE_ATLAS_URI_HERE' -or -not $uri) {
        Write-Host "Set MONGODB_URI in $local (Atlas connection string, or the local one shown there)." -ForegroundColor Red
        exit 1
    }
    $target = if ($uri -like 'mongodb+srv://*') { 'MongoDB Atlas' } else { 'local MongoDB' }
    Write-Host "Database: $target" -ForegroundColor Cyan
}
$ip = (Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq 'Up' } | Select-Object -First 1).IPv4Address.IPAddress
Write-Host "Backend will listen on http://localhost:$Port  (phone: http://${ip}:$Port)" -ForegroundColor Green
Set-Location $backend
& .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=$Port"
