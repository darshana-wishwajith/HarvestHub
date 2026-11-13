# Builds the Android debug app pointed at this laptop's backend, installs it on the USB-connected phone
# and launches it. Use -Production to build against https://harvesthubapp.tech, or -ApiUrl to set any URL.
param([string]$ApiUrl, [int]$Port = 8080, [switch]$Production)
$ErrorActionPreference = 'Stop'
$android = Join-Path $PSScriptRoot '..\harvesthub-android' | Resolve-Path

# --- backend URL the phone will use ---
if ($Production) {
    $ApiUrl = 'https://harvesthubapp.tech/'
} elseif (-not $ApiUrl) {
    $ip = (Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq 'Up' } |
           Select-Object -First 1).IPv4Address.IPAddress
    if (-not $ip) { Write-Host 'No active network found. Connect to Wi-Fi.' -ForegroundColor Red; exit 1 }
    $ApiUrl = "http://${ip}:$Port/"
}
if (-not $ApiUrl.EndsWith('/')) { $ApiUrl += '/' }

# --- adb ---
$adb = (Get-Command adb -ErrorAction SilentlyContinue).Source
if (-not $adb) {
    $sdk = ((Get-Content (Join-Path $android 'local.properties')) -match '^sdk.dir=' -replace '^sdk.dir=', '' -replace '\\:', ':' -replace '\\\\', '\')
    $adb = Join-Path $sdk 'platform-tools\adb.exe'
}
$devices = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\S' }
if (-not ($devices -match '\tdevice$')) {
    Write-Host 'No authorized phone found. Connect it by USB, enable USB debugging and tap Allow on the phone.' -ForegroundColor Red
    $devices | ForEach-Object { Write-Host "  $_" }
    exit 1
}

Write-Host "Building debug app -> $ApiUrl" -ForegroundColor Green
& (Join-Path $android 'gradlew.bat') -p $android assembleDebug "-PapiUrl=$ApiUrl"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$apk = Join-Path $android 'app\build\outputs\apk\debug\app-debug.apk'
Write-Host 'Installing on phone...' -ForegroundColor Green
& $adb install -r $apk
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $adb shell monkey -p com.harvesthub.mobile -c android.intent.category.LAUNCHER 1 | Out-Null
Write-Host "HarvestHub launched on the phone (backend: $ApiUrl)" -ForegroundColor Green
if (-not $Production) {
    Write-Host 'If the app cannot connect: run-backend must be running, phone on the same Wi-Fi, firewall allows port 8080.' -ForegroundColor Yellow
}
