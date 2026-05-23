#Requires -Version 7
<#
.SYNOPSIS
    Faz handoff de tarefa entre agents no Paperclip.

.DESCRIPTION
    Delegado um task/issue de um agent para outro via Paperclip API.
    Segue as políticas de contexto e escalation definidas em OPERATIONAL.md.

.PARAMETER FromAgent
    Agent que está delegando (claudete, claudio, camilo, etc).

.PARAMETER ToAgent
    Agent que receberá a tarefa.

.PARAMETER TaskId
    ID da task/issue a delegar.

.PARAMETER Summary
    Resumo compacto da tarefa (máx 200 tokens).

.PARAMETER Blocker
    Se a tarefa está bloqueada, liste o motivo.

.EXAMPLE
    .\scripts\agent-delegate.ps1 -FromAgent claudio -ToAgent camilo -TaskId "LINKA-123" -Summary "Implementar autenticação"
    .\scripts\agent-delegate.ps1 -FromAgent claudio -ToAgent gema -TaskId "LINKA-125" -Summary "Documentar release" -Verbose
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [string]$FromAgent,

    [Parameter(Mandatory=$true)]
    [string]$ToAgent,

    [Parameter(Mandatory=$true)]
    [string]$TaskId,

    [Parameter(Mandatory=$true)]
    [string]$Summary,

    [string]$Blocker,
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

# ── 2. Resolve Agent IDs ───────────────────────────────────────────────────────
function Resolve-AgentId {
    param([string]$Name)
    $key = "PAPERCLIP_AGENT_$($Name.ToUpper())"
    $id = $env_vars[$key]
    if (-not $id) {
        throw "Agent não encontrado: $Name"
    }
    return $id
}

$fromAgentId = Resolve-AgentId $FromAgent
$toAgentId = Resolve-AgentId $ToAgent

Write-Host "=== DELEGANDO TAREFA ===" -ForegroundColor Cyan
Write-Host "De: $FromAgent → Para: $ToAgent" -ForegroundColor Yellow
Write-Host "Task: $TaskId" -ForegroundColor Yellow
Write-Host "Summary: $Summary" -ForegroundColor White
if ($Blocker) {
    Write-Host "Blocker: $Blocker" -ForegroundColor Red
}
Write-Host ""

# ── 3. Validate Summary Length ─────────────────────────────────────────────────
$summaryLength = $Summary.Length
if ($summaryLength -gt 500) {
    Write-Host "⚠️ Aviso: Summary é muito longo ($summaryLength chars, máx 500)" -ForegroundColor Yellow
    Write-Host "   Por favor, resuma a tarefa para máximo 500 caracteres" -ForegroundColor Yellow
    exit 1
}

# ── 4. Build Delegation Payload ────────────────────────────────────────────────
$delegationPayload = @{
    taskId = $TaskId
    fromAgentId = $fromAgentId
    toAgentId = $toAgentId
    timestamp = [DateTime]::UtcNow.ToString("o")
    context = @{
        summary = $Summary
        blocker = if ($Blocker) { $Blocker } else { $null }
    }
} | ConvertTo-Json

# ── 5. Send Delegation Request ─────────────────────────────────────────────────
$delegateUrl = "$baseUrl/api/agents/$toAgentId/delegate"
$headers = @{
    "Content-Type" = "application/json"
}

try {
    Write-Host "▶ Enviando delegação..." -ForegroundColor Yellow
    $response = Invoke-RestMethod -Uri $delegateUrl -Method Post -Headers $headers -Body $delegationPayload -TimeoutSec 30

    Write-Host ""
    Write-Host "✓ Delegação enviada com sucesso!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Resposta:" -ForegroundColor Cyan
    $response | ConvertTo-Json | Write-Host

} catch {
    Write-Host ""
    Write-Host "✗ Erro ao delegar: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Próxima ação:" -ForegroundColor Cyan
Write-Host "  1. Monitorar status: .\scripts\agent-status.ps1 -Agent $ToAgent" -ForegroundColor White
Write-Host "  2. Verificar task em Paperclip UI: http://127.0.0.1:3100" -ForegroundColor White
