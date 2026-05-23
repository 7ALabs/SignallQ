#Requires -Version 7
<#
.SYNOPSIS
    Verifica status de todos os agents no Paperclip.

.DESCRIPTION
    Conecta ao Paperclip (http://127.0.0.1:3100) e exibe:
    - Status de cada agent (running, idle, error)
    - Modelo configurado
    - Último heartbeat
    - Erros, se houver

.EXAMPLE
    .\scripts\agent-status.ps1
    .\scripts\agent-status.ps1 -Agent claudete
    .\scripts\agent-status.ps1 -Verbose
#>

[CmdletBinding()]
param(
    [string]$Agent,                    # Agent específico (optional)
    [string]$ConfigFile = ".env.paperclip"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')

# ── 1. Load .env.paperclip ────────────────────────────────────────────────────
$envPath = Join-Path $repoRoot $ConfigFile
if (-not (Test-Path $envPath)) {
    Write-Error "❌ Arquivo não encontrado: $envPath"
    Write-Host "   Crie .env.paperclip na raiz do projeto"
    exit 1
}

$env_content = Get-Content $envPath | Where-Object { $_ -and -not $_.StartsWith('#') }
$env_vars = @{}
foreach ($line in $env_content) {
    if ($line -match '^\s*([^=]+)=(.*)$') {
        $key = $Matches[1].Trim()
        $value = $Matches[2].Trim()
        $env_vars[$key] = $value
    }
}

$paperclipHost = $env_vars['PAPERCLIP_HOST'] ?? '127.0.0.1'
$paperclipPort = $env_vars['PAPERCLIP_PORT'] ?? '3100'
$baseUrl = "http://$paperclipHost`:$paperclipPort"

Write-Host "=== PAPERCLIP AGENT STATUS ===" -ForegroundColor Cyan
Write-Host "Server: $baseUrl" -ForegroundColor Yellow
Write-Host ""

# ── 2. Test Connection ─────────────────────────────────────────────────────────
try {
    $healthCheck = Invoke-RestMethod -Uri "$baseUrl/api/health" -Method Get -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✓ Conexão com Paperclip: OK" -ForegroundColor Green
} catch {
    Write-Host "✗ Não consegui conectar ao Paperclip em $baseUrl" -ForegroundColor Red
    Write-Host "  Verifique se Paperclip está rodando:" -ForegroundColor Yellow
    Write-Host "  http://127.0.0.1:3100" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# ── 3. Fetch Agents ────────────────────────────────────────────────────────────
try {
    $agents = Invoke-RestMethod -Uri "$baseUrl/api/agents" -Method Get -TimeoutSec 10
} catch {
    Write-Host "✗ Erro ao buscar agentes: $_" -ForegroundColor Red
    exit 1
}

if (-not $agents -or $agents.Count -eq 0) {
    Write-Host "⚠️ Nenhum agent encontrado no Paperclip" -ForegroundColor Yellow
    exit 0
}

# ── 4. Filter by Agent Name (optional) ─────────────────────────────────────────
if ($Agent) {
    $agents = $agents | Where-Object { $_.name -like "*$Agent*" -or $_.id -like "*$Agent*" }
    if (-not $agents) {
        Write-Host "⚠️ Nenhum agent encontrado com nome: $Agent" -ForegroundColor Yellow
        exit 0
    }
}

# ── 5. Display Status ──────────────────────────────────────────────────────────
Write-Host "👥 AGENTS:" -ForegroundColor Cyan
Write-Host ""

$errorCount = 0
$runningCount = 0
$idleCount = 0

foreach ($agent in $agents) {
    $name = $agent.name ?? "Unknown"
    $id = $agent.id ?? "Unknown"
    $status = $agent.status ?? "unknown"
    $model = $agent.adapterConfig?.model ?? "(default)"
    $adapter = $agent.adapterType ?? "unknown"
    $lastHeartbeat = $agent.lastHeartbeat ?? "never"
    $error = $agent.lastError ?? $null

    # Status icon
    $statusIcon = switch ($status) {
        "running" { "🟢"; $runningCount++ }
        "idle" { "🔵"; $idleCount++ }
        "error" { "🔴"; $errorCount++ }
        default { "⚪" }
    }

    Write-Host "$statusIcon $name" -ForegroundColor $(
        if ($status -eq "error") { "Red" }
        elseif ($status -eq "running") { "Green" }
        else { "Cyan" }
    )
    Write-Host "   ID: $id"
    Write-Host "   Status: $status"
    Write-Host "   Adapter: $adapter"
    Write-Host "   Model: $model"
    Write-Host "   Last Heartbeat: $lastHeartbeat"

    if ($error) {
        Write-Host "   ❌ Error: $error" -ForegroundColor Red
    }

    Write-Host ""
}

# ── 6. Summary ─────────────────────────────────────────────────────────────────
Write-Host "════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "📊 RESUMO:" -ForegroundColor Cyan
Write-Host "  🟢 Running: $runningCount"
Write-Host "  🔵 Idle: $idleCount"
Write-Host "  🔴 Error: $errorCount"
Write-Host "  ━━━━━━━━"
Write-Host "  Total: $($agents.Count)"
Write-Host "════════════════════════════════════════════" -ForegroundColor Cyan

# Exit code based on errors
if ($errorCount -gt 0) {
    exit 1
} else {
    exit 0
}
