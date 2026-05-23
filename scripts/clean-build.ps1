#Requires -Version 7
<#
.SYNOPSIS
    Limpeza segura de build cache e outputs para reconstruir do zero.

.DESCRIPTION
    Remove diretórios de build local (app/build/, .gradle/build-cache/).
    NÃO remove ~/.gradle/ (global), APKs salvos ou documentação.

.EXAMPLE
    .\scripts\clean-build.ps1
    .\scripts\clean-build.ps1 -Verbose
#>

[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')

Write-Host "=== Limpeza de Build ===" -ForegroundColor Cyan
Write-Host ""

# ── 1. Remove app/build/ ──────────────────────────────────────────────────────
$appBuild = Join-Path $repoRoot 'app\build'
if (Test-Path $appBuild) {
    Write-Host "▶ Removendo $appBuild/" -ForegroundColor Yellow
    Remove-Item -Path $appBuild -Recurse -Force
    Write-Host "  ✓ Removido" -ForegroundColor Green
}

# ── 2. Remove .gradle/build-cache/ ───────────────────────────────────────────
$buildCache = Join-Path $repoRoot '.gradle\build-cache'
if (Test-Path $buildCache) {
    Write-Host "▶ Removendo $buildCache/" -ForegroundColor Yellow
    Remove-Item -Path $buildCache -Recurse -Force
    Write-Host "  ✓ Removido" -ForegroundColor Green
}

Write-Host ""
Write-Host "✔ Limpeza concluída!" -ForegroundColor Green
Write-Host ""
Write-Host "ℹ Próxima execução de build será clean (sem cache)" -ForegroundColor Cyan
Write-Host ""
Write-Host "Nota:" -ForegroundColor Cyan
Write-Host "  ✓ ~/.gradle/ NÃO foi removido (build cache global)"
Write-Host "  ✓ builds\apk\ NÃO foi removido (APKs salvos)"
Write-Host "  ✓ Documentação NÃO foi removida"
