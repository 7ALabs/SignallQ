#Requires -Version 7
<#
.SYNOPSIS
    Gera APK debug do projeto Linka Android e salva em builds\apk\debug\<versionName>\.

.DESCRIPTION
    Lê versionName e versionCode de gradle/libs.versions.toml, executa o assembleDebug
    via Gradle wrapper, copia o APK gerado para a pasta oficial com nome padronizado.

.EXAMPLE
    .\scripts\build-apk-debug.ps1
    .\scripts\build-apk-debug.ps1 -Verbose
#>

[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$tomlPath = Join-Path $repoRoot 'gradle\libs.versions.toml'
$gradlew  = Join-Path $repoRoot 'gradlew.bat'

# ── 1. Lê versão do catálogo ──────────────────────────────────────────────────
if (-not (Test-Path $tomlPath)) {
    Write-Error "Catálogo não encontrado: $tomlPath"
}

$toml        = Get-Content $tomlPath -Raw
$versionName = [regex]::Match($toml, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
$versionCode = [regex]::Match($toml, 'versionCode\s*=\s*"([^"]+)"').Groups[1].Value

if (-not $versionName -or -not $versionCode) {
    Write-Error "Não foi possível extrair versionName/versionCode de $tomlPath"
}

Write-Host "=== LINKA Kotlin — Build Debug ===" -ForegroundColor Cyan
Write-Host "  versionName : $versionName"
Write-Host "  versionCode : $versionCode"
Write-Host ""

# ── 2. Executa o build ────────────────────────────────────────────────────────
Write-Host "▶ Executando: gradlew :app:assembleDebug" -ForegroundColor Yellow
Push-Location $repoRoot
try {
    & $gradlew :app:assembleDebug
    if ($LASTEXITCODE -ne 0) { Write-Error "Gradle falhou com código $LASTEXITCODE" }
} finally {
    Pop-Location
}

# ── 3. Localiza o APK gerado ──────────────────────────────────────────────────
$apkSrc = Join-Path $repoRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apkSrc)) {
    Write-Error "APK não encontrado em $apkSrc — verifique a saída do Gradle."
}

# ── 4. Copia para pasta oficial com nome padronizado ──────────────────────────
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$destDir  = Join-Path $repoRoot "builds\apk\debug\$versionName"
$destFile = Join-Path $destDir "linka-android-v$versionName+$versionCode-debug-$timestamp.apk"

New-Item -ItemType Directory -Path $destDir -Force | Out-Null
Copy-Item -Path $apkSrc -Destination $destFile -Force

$sizeMb = [math]::Round((Get-Item $destFile).Length / 1MB, 1)

Write-Host ""
Write-Host "✔ APK gerado com sucesso!" -ForegroundColor Green
Write-Host "  Arquivo  : $destFile"
Write-Host "  Tamanho  : ${sizeMb} MB"
Write-Host ""
Write-Host "Próximos passos:" -ForegroundColor Cyan
Write-Host "  1. Instalar no dispositivo: adb install -r `"$destFile`""
Write-Host "  2. Conferir versão: aapt dump badging `"$destFile`" | findstr version"
