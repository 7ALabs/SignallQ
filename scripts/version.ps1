#Requires -Version 7
<#
.SYNOPSIS
    Gerencia versionName e versionCode em gradle/libs.versions.toml.

.DESCRIPTION
    Análogo ao dart scripts/version.dart do projeto Flutter, mas para o módulo Kotlin.
    Sempre incrementa versionCode junto com qualquer bump de versionName.

.PARAMETER Command
    show   — Exibe versão atual sem modificar nada.
    major  — Incrementa MAJOR, zera MINOR e PATCH, incrementa versionCode.
    minor  — Incrementa MINOR, zera PATCH, incrementa versionCode.
    patch  — Incrementa PATCH, incrementa versionCode.
    build  — Incrementa apenas versionCode (sem alterar versionName).
    set    — Define versão específica no formato X.Y.Z+N.

.EXAMPLE
    .\scripts\version.ps1 show
    .\scripts\version.ps1 patch
    .\scripts\version.ps1 set 1.0.0+10
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory, Position = 0)]
    [ValidateSet('show', 'major', 'minor', 'patch', 'build', 'set')]
    [string]$Command,

    [Parameter(Position = 1)]
    [string]$Version
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$tomlPath = Join-Path $repoRoot 'gradle\libs.versions.toml'

if (-not (Test-Path $tomlPath)) {
    Write-Error "Catálogo não encontrado: $tomlPath"
}

# ── Helpers ───────────────────────────────────────────────────────────────────

function Read-CurrentVersion {
    $content     = Get-Content $tomlPath -Raw
    $nameMatch   = [regex]::Match($content, 'versionName\s*=\s*"([^"]+)"')
    $codeMatch   = [regex]::Match($content, 'versionCode\s*=\s*"([^"]+)"')

    if (-not $nameMatch.Success -or -not $codeMatch.Success) {
        Write-Error "Não foi possível parsear versionName/versionCode em $tomlPath"
    }

    return @{
        Name    = $nameMatch.Groups[1].Value
        Code    = [int]$codeMatch.Groups[1].Value
        Content = $content
    }
}

function Write-Version ([string]$NewName, [int]$NewCode, [string]$Content) {
    $updated = $Content `
        -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$NewName`"" `
        -replace 'versionCode\s*=\s*"[^"]+"', "versionCode = `"$NewCode`""

    Set-Content -Path $tomlPath -Value $updated -NoNewline -Encoding UTF8
}

function Show-Version ([string]$Name, [int]$Code) {
    Write-Host "versionName : $Name" -ForegroundColor Cyan
    Write-Host "versionCode : $Code" -ForegroundColor Cyan
}

# ── Commands ──────────────────────────────────────────────────────────────────

$current = Read-CurrentVersion
$parts   = $current.Name -split '\.'

if ($parts.Count -ne 3) {
    Write-Error "versionName '$($current.Name)' não está no formato MAJOR.MINOR.PATCH"
}

[int]$major = $parts[0]
[int]$minor = $parts[1]
[int]$patch = $parts[2]
[int]$code  = $current.Code

switch ($Command) {
    'show' {
        Show-Version $current.Name $current.Code
    }

    'major' {
        $major++; $minor = 0; $patch = 0; $code++
        $newName = "$major.$minor.$patch"
        Write-Version $newName $code $current.Content
        Write-Host "Versão atualizada:" -ForegroundColor Green
        Show-Version $newName $code
    }

    'minor' {
        $minor++; $patch = 0; $code++
        $newName = "$major.$minor.$patch"
        Write-Version $newName $code $current.Content
        Write-Host "Versão atualizada:" -ForegroundColor Green
        Show-Version $newName $code
    }

    'patch' {
        $patch++; $code++
        $newName = "$major.$minor.$patch"
        Write-Version $newName $code $current.Content
        Write-Host "Versão atualizada:" -ForegroundColor Green
        Show-Version $newName $code
    }

    'build' {
        $code++
        Write-Version $current.Name $code $current.Content
        Write-Host "Versão atualizada:" -ForegroundColor Green
        Show-Version $current.Name $code
    }

    'set' {
        if (-not $Version) {
            Write-Error "Informe a versão no formato X.Y.Z+N. Exemplo: .\version.ps1 set 1.0.0+10"
        }
        $setMatch = [regex]::Match($Version, '^(\d+)\.(\d+)\.(\d+)\+(\d+)$')
        if (-not $setMatch.Success) {
            Write-Error "Formato inválido: '$Version'. Use X.Y.Z+N (ex: 1.0.0+10)."
        }
        $newName = "$($setMatch.Groups[1].Value).$($setMatch.Groups[2].Value).$($setMatch.Groups[3].Value)"
        $newCode = [int]$setMatch.Groups[4].Value
        if ($newCode -le $current.Code) {
            Write-Warning "Novo versionCode ($newCode) não é maior que o atual ($($current.Code)). versionCode deve ser estritamente crescente."
        }
        Write-Version $newName $newCode $current.Content
        Write-Host "Versão definida:" -ForegroundColor Green
        Show-Version $newName $newCode
    }
}
