#Requires -Version 7
<#
.SYNOPSIS
    Acorda um agent específico no Paperclip.

.DESCRIPTION
    Envia um wake event para um agent via Paperclip API.
    O agent desperta e fica pronto para receber tarefas.

.PARAMETER AgentName
    Nome ou ID do agent a acordar (claudete, claudio, camilo, caio, gema).

.PARAMETER Payload
    Dados opcionais para passar ao agent ao acordar (JSON string).

.EXAMPLE
    .\scripts\agent-wake.ps1 -AgentName claudio
    .\scripts\agent-wake.ps1 -AgentName claudete -Payload '{"task":"review-pr","prId":123}'
    .\scripts\agent-wake.ps1 -AgentName gema -Verbose
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [string]$AgentName,

    [string]$Payload,
    [string]$ConfigFile = ".env.paperclip"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')

# ── 1. Load .env.paperclip ────────────────────────────────────────────────────
$envPath = Join-Path $repoRoot $ConfigFile
if (-not (Test-Path $envPath)) {
    Write-Error "Arquivo não encontrado: $envPath"
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

# ── 2. Resolve Agent ID ────────────────────────────────────────────────────────
$agentNameUpper = $AgentName.ToUpper()
$agentIdKey = "PAPERCLIP_AGENT_$agentNameUpper"

$agentId = $env_vars[$agentIdKey]
if (-not $agentId) {
    Write-Error "Agent não encontrado em .env.paperclip: $agentIdKey"
    Write-Host "Agentes disponíveis:"
    $env_vars.Keys | Where-Object { $_ -like "PAPERCLIP_AGENT_*" } | ForEach-Object {
        Write-Host "  • $_"
    }
    exit 1
}

Write-Host "=== ACORDANDO AGENT ===" -ForegroundColor Cyan
Write-Host "Agent: $AgentName (ID: $agentId)" -ForegroundColor Yellow
Write-Host "Server: $baseUrl" -ForegroundColor Yellow
Write-Host ""

# ── 3. Build Wake Request ──────────────────────────────────────────────────────
$wakeUrl = "$baseUrl/api/agents/$agentId/wake"
$headers = @{
    "Content-Type" = "application/json"
}

$body = @{
    timestamp = [DateTime]::UtcNow.ToString("o")
} | ConvertTo-Json

if ($Payload) {
    try {
        $payloadObj = $Payload | ConvertFrom-Json
        $body = @{
            timestamp = [DateTime]::UtcNow.ToString("o")
            payload = $payloadObj
        } | ConvertTo-Json
    } catch {
        Write-Host "⚠️ Payload JSON inválido, ignorando" -ForegroundColor Yellow
    }
}

# ── 4. Send Wake Request ───────────────────────────────────────────────────────
try {
    Write-Host "▶ Enviando wake request..." -ForegroundColor Yellow
    $response = Invoke-RestMethod -Uri $wakeUrl -Method Post -Headers $headers -Body $body -TimeoutSec 30

    Write-Host ""
    Write-Host "✓ Agent acordado com sucesso!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Resposta:" -ForegroundColor Cyan
    $response | ConvertTo-Json | Write-Host

} catch {
    Write-Host ""
    Write-Host "✗ Erro ao acordar agent: $_" -ForegroundColor Red

    if ($_.Exception.Response.StatusCode -eq 404) {
        Write-Host "   Agent não encontrado. Verifique o ID em .env.paperclip" -ForegroundColor Yellow
    } elseif ($_.Exception.Response.StatusCode -eq 500) {
        Write-Host "   Erro no servidor Paperclip" -ForegroundColor Yellow
    }

    exit 1
}

Write-Host ""
Write-Host "Próximo passo: Verifique status com:" -ForegroundColor Cyan
Write-Host "  .\scripts\agent-status.ps1 -Agent $AgentName" -ForegroundColor White
