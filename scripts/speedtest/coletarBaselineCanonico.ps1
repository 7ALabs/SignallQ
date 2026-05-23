param(
    [string]$PackageName = "br.com.linka.speedtest",
    [string]$SaidaBaseDir = "tmp_logs",
    [string]$PrefixoJson = "canonico"
)

$ErrorActionPreference = "Stop"

function Resolve-Adb {
    $candidatos = @(
        "$env:ANDROID_HOME\platform-tools\adb.exe",
        "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe",
        "E:\DevTools\Android\sdk\platform-tools\adb.exe",
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    ) | Where-Object { $_ -and (Test-Path $_) }
    if (-not $candidatos -or $candidatos.Count -eq 0) {
        throw "adb nao encontrado."
    }
    return $candidatos[0]
}

function Resolve-Python {
    $candidatos = @(
        "C:\Users\luizg\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe",
        "python.exe"
    ) | Where-Object { $_ -and (Get-Command $_ -ErrorAction SilentlyContinue) }
    if (-not $candidatos -or $candidatos.Count -eq 0) {
        throw "python nao encontrado."
    }
    return $candidatos[0]
}

function Get-ConnectedDeviceCount {
    param([string]$Adb)
    $linhas = & $Adb devices
    $ativos = $linhas | Select-String -Pattern "device$" | ForEach-Object { $_.Line }
    return ($ativos | Measure-Object).Count
}

function Export-FileFromRunAs {
    param(
        [string]$Adb,
        [string]$PackageName,
        [string]$RemotePath,
        [string]$LocalPath
    )
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $Adb
    $psi.Arguments = "exec-out run-as $PackageName cat `"$RemotePath`""
    $psi.RedirectStandardOutput = $true
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true

    $proc = New-Object System.Diagnostics.Process
    $proc.StartInfo = $psi
    [void]$proc.Start()

    $fs = [System.IO.File]::Open($LocalPath, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
    try {
        $proc.StandardOutput.BaseStream.CopyTo($fs)
    } finally {
        $fs.Dispose()
    }
    $proc.WaitForExit()

    if ($proc.ExitCode -ne 0) {
        throw "falha ao exportar '$RemotePath' (exit=$($proc.ExitCode))"
    }
}

function List-LevelDbFilesFromRunAs {
    param(
        [string]$Adb,
        [string]$PackageName
    )
    $remoteDir = "app_webview/Default/Local Storage/leveldb"
    $linhas = & $Adb shell "run-as $PackageName ls '$remoteDir'"
    return $linhas |
        ForEach-Object { $_.ToString().Trim() } |
        Where-Object { $_ -and $_ -notmatch "^ls: " }
}

$adb = Resolve-Adb
$python = Resolve-Python

if ((Get-ConnectedDeviceCount -Adb $adb) -le 0) {
    throw "nenhum dispositivo conectado no adb."
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$saidaRaiz = Join-Path $SaidaBaseDir "baseline_canonico_$timestamp"
$leveldbDir = Join-Path $saidaRaiz "leveldb"
$jsonDir = Join-Path $saidaRaiz "json"
New-Item -ItemType Directory -Path $leveldbDir -Force | Out-Null
New-Item -ItemType Directory -Path $jsonDir -Force | Out-Null

$arquivos = List-LevelDbFilesFromRunAs -Adb $adb -PackageName $PackageName
if (-not $arquivos -or $arquivos.Count -eq 0) {
    throw "nenhum arquivo leveldb encontrado via run-as."
}

foreach ($arquivo in $arquivos) {
    $remote = "app_webview/Default/Local Storage/leveldb/$arquivo"
    $local = Join-Path $leveldbDir $arquivo
    try {
        Export-FileFromRunAs -Adb $adb -PackageName $PackageName -RemotePath $remote -LocalPath $local
    } catch {
        if (Test-Path $local) { Remove-Item $local -Force -ErrorAction SilentlyContinue }
    }
}

$extrator = "scripts\speedtest\extrairHistoricoCanonicoWebview.py"
if (-not (Test-Path $extrator)) {
    throw "extrator nao encontrado: $extrator"
}

& $python $extrator --leveldb-dir $leveldbDir --saida-dir $jsonDir --prefixo $PrefixoJson

Write-Output "baseline canonico coletado em: $saidaRaiz"
Write-Output "json canonico: $jsonDir"
