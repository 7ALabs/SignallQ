#Requires -Version 7
<#
.SYNOPSIS
    Valida que o ambiente está corretamente configurado para build do SignallQ Android Kotlin.

.DESCRIPTION
    Verifica Java, Android SDK, ADB, Node.js, npm, Python, Gradle e outras ferramentas.

.EXAMPLE
    .\scripts\check-env.ps1
    .\scripts\check-env.ps1 -Verbose
#>

[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')

Write-Host "=== Environment Validation ===" -ForegroundColor Cyan
Write-Host ""

$issues = @()
$warnings = @()

# ── 1. Java/JDK ──────────────────────────────────────────────────────────────
Write-Host "☕ Java/JDK:" -ForegroundColor Yellow
try {
    $javaVersion = & java -version 2>&1
    Write-Host "✓ $($javaVersion[0])" -ForegroundColor Green
    $javaPath = (Get-Command java -ErrorAction SilentlyContinue).Source
    Write-Host "  Path: $javaPath"
} catch {
    Write-Host "✗ Java não encontrado" -ForegroundColor Red
    $issues += "Java/JDK não encontrado. Instale uma JDK compatível e configure JAVA_HOME."
}

# ── 2. Android SDK ────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "🤖 Android SDK:" -ForegroundColor Yellow
$androidSdk = $env:ANDROID_HOME
if ([string]::IsNullOrEmpty($androidSdk)) {
    $androidSdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (-not (Test-Path $androidSdk)) {
        Write-Host "✗ ANDROID_HOME não definido e SDK padrão não encontrado em $androidSdk" -ForegroundColor Red
        $issues += "Android SDK não configurado."
    }
}

if (Test-Path $androidSdk) {
    Write-Host "✓ Encontrado: $androidSdk" -ForegroundColor Green
    $platforms = @(Get-ChildItem "$androidSdk\platforms" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name)
    $buildTools = @(Get-ChildItem "$androidSdk\build-tools" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name | Sort-Object -Descending)

    if ($platforms) { Write-Host "  Plataformas: $($platforms -join ', ')" }
    if ($buildTools) { Write-Host "  Build-tools: $($buildTools[0])" }
} else {
    Write-Host "✗ Android SDK não encontrado em $androidSdk" -ForegroundColor Red
    $issues += "Android SDK não configurado."
}

# ── 3. ADB ───────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "📱 ADB (Android Debug Bridge):" -ForegroundColor Yellow
try {
    $adbVersion = & adb version 2>&1 | Select-Object -First 1
    Write-Host "✓ $adbVersion" -ForegroundColor Green
    $adbPath = (Get-Command adb -ErrorAction SilentlyContinue).Source
    Write-Host "  Path: $adbPath"
} catch {
    Write-Host "✗ ADB não encontrado" -ForegroundColor Red
    $issues += "ADB não encontrado. Configure Android SDK e platform-tools no PATH."
}

# ── 4. Java Home Validation ───────────────────────────────────────────────────
Write-Host ""
Write-Host "⚙ JAVA_HOME:" -ForegroundColor Yellow
$javaHome = $env:JAVA_HOME
if ([string]::IsNullOrEmpty($javaHome)) {
    Write-Host "⚠ JAVA_HOME não definido" -ForegroundColor Yellow
    $warnings += "JAVA_HOME não definido. Configure em Variáveis de Ambiente do Windows."
} else {
    Write-Host "  $javaHome" -ForegroundColor Cyan
    if (-not (Test-Path $javaHome)) {
        Write-Host "⚠ JAVA_HOME aponta para caminho não existente" -ForegroundColor Yellow
        $warnings += "JAVA_HOME aponta para caminho inexistente: $javaHome"
    }
    # Check for discrepancy
    if ($javaHome -like "*jdk-21*" -and $javaVersion -like "*17*") {
        Write-Host "⚠ JAVA_HOME aponta para jdk-21, mas jdk-17 está em uso" -ForegroundColor Yellow
        $warnings += "Discrepância: JAVA_HOME aponta para jdk-21, mas jdk-17 está ativo."
    }
}

# ── 5. Node.js ───────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "⬢ Node.js:" -ForegroundColor Yellow
try {
    $nodeVersion = & node --version 2>&1
    Write-Host "✓ Node: $nodeVersion" -ForegroundColor Green
    $nodePath = (Get-Command node -ErrorAction SilentlyContinue).Source
    Write-Host "  Path: $nodePath"
} catch {
    Write-Host "⚠ Node.js não encontrado (opcional)" -ForegroundColor Yellow
    $warnings += "Node.js não encontrado (opcional para SignallQ Kotlin)."
}

# ── 6. npm ───────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "📦 npm:" -ForegroundColor Yellow
try {
    $npmVersion = & npm --version 2>&1
    Write-Host "✓ npm: v$npmVersion" -ForegroundColor Green
} catch {
    Write-Host "⚠ npm não encontrado (opcional)" -ForegroundColor Yellow
}

# ── 7. Python ────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "🐍 Python:" -ForegroundColor Yellow
try {
    $pythonVersion = & python --version 2>&1
    Write-Host "✓ $pythonVersion" -ForegroundColor Green
} catch {
    Write-Host "⚠ Python não encontrado (opcional)" -ForegroundColor Yellow
}

# ── 8. Gradle Wrapper ────────────────────────────────────────────────────────
Write-Host ""
Write-Host "🔨 Gradle:" -ForegroundColor Yellow
$gradlewPath = Join-Path $repoRoot 'android\gradlew.bat'
if (Test-Path $gradlewPath) {
    Write-Host "✓ gradlew.bat encontrado" -ForegroundColor Green
    Write-Host "  Path: $gradlewPath"
} else {
    Write-Host "✗ gradlew.bat não encontrado em $gradlewPath" -ForegroundColor Red
    $issues += "Gradle wrapper não encontrado. Verifique se está no diretório correto."
}

# ── 9. Flutter (Legado) ──────────────────────────────────────────────────────
Write-Host ""
Write-Host "📱 Flutter (Legado):" -ForegroundColor Yellow
try {
    $flutterVersion = & flutter --version 2>&1 | Select-Object -First 1
    Write-Host "⚠ $flutterVersion (LEGADO, não usar para novos features)" -ForegroundColor Yellow
} catch {
    Write-Host "ℹ Flutter não encontrado (esperado - stack primário é Kotlin)" -ForegroundColor Cyan
}

# ── 10. Paperclip ────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "📋 Paperclip (Agent Orchestration):" -ForegroundColor Yellow
try {
    $paperclipHealth = Invoke-RestMethod -Uri "http://127.0.0.1:3100/api/health" -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✓ Paperclip respondendo em http://127.0.0.1:3100" -ForegroundColor Green

    $agents = Invoke-RestMethod -Uri "http://127.0.0.1:3100/api/agents" -TimeoutSec 5 -ErrorAction Stop
    $agentCount = if ($agents -is [array]) { $agents.Count } else { 1 }
    Write-Host "  Agents encontrados: $agentCount"

} catch {
    Write-Host "⚠ Paperclip não está respondendo" -ForegroundColor Yellow
    $warnings += "Paperclip não respondendo. Verifique se está rodando."
}

# ── 11. .env.paperclip ───────────────────────────────────────────────────────
Write-Host ""
Write-Host "🔧 .env.paperclip:" -ForegroundColor Yellow
if (Test-Path ".env.paperclip") {
    Write-Host "✓ Encontrado" -ForegroundColor Green
} else {
    Write-Host "⚠ .env.paperclip não encontrado" -ForegroundColor Yellow
    $warnings += ".env.paperclip não encontrado. Scripts de agent não funcionarão."
}

# ── Summary ───────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "════════════════════════════════════════════" -ForegroundColor Cyan

if ($issues.Count -eq 0 -and $warnings.Count -eq 0) {
    Write-Host "✅ Status: OK — Ambiente pronto para build" -ForegroundColor Green
    exit 0
} elseif ($issues.Count -eq 0) {
    Write-Host "⚠ Status: AVISOS — Ambiente funcional, mas com alertas" -ForegroundColor Yellow
    Write-Host ""
    foreach ($warning in $warnings) {
        Write-Host "  ⚠ $warning" -ForegroundColor Yellow
    }
    exit 0
} else {
    Write-Host "❌ Status: ERRO — Problemas encontrados" -ForegroundColor Red
    Write-Host ""
    foreach ($issue in $issues) {
        Write-Host "  ❌ $issue" -ForegroundColor Red
    }
    exit 1
}
