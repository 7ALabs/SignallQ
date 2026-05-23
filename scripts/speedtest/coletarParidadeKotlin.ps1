param(
    [string]$PackageName = "io.linka.app.kotlin",
    [string]$FlutterJsonDir = "tmp_logs\sessao_v3.3.3+12_20260507_1224",
    [string]$FlutterSqlite = "",
    [ValidateSet("all", "fast", "complete")]
    [string]$Modo = "all",
    [int]$JanelaSegundos = 900,
    [switch]$NaoExecutarComparacao
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
        throw "adb nao encontrado. Configure ANDROID_HOME/ANDROID_SDK_ROOT ou ajuste o script."
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

function Export-DbFile {
    param(
        [string]$Adb,
        [string]$PackageName,
        [string]$RemoteName,
        [string]$LocalPath
    )
    try {
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = $Adb
        $psi.Arguments = "exec-out `"run-as $PackageName cat databases/$RemoteName`""
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

        if ((Test-Path $LocalPath) -and ((Get-Item $LocalPath).Length -gt 0) -and $proc.ExitCode -eq 0) {
            return $true
        }
    } catch {
        # segue fluxo; alguns arquivos podem nao existir (wal/shm)
    }
    if (Test-Path $LocalPath) {
        Remove-Item $LocalPath -Force -ErrorAction SilentlyContinue
    }
    return $false
}

$adb = Resolve-Adb
$python = Resolve-Python

$deviceCount = Get-ConnectedDeviceCount -Adb $adb
if ($deviceCount -le 0) {
    throw "nenhum dispositivo conectado no adb."
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$saidaDir = Join-Path "tmp_logs" "paridade_kotlin_$timestamp"
New-Item -ItemType Directory -Path $saidaDir -Force | Out-Null

$dbMain = Join-Path $saidaDir "linkaKotlin.db"
$dbWal = Join-Path $saidaDir "linkaKotlin.db-wal"
$dbShm = Join-Path $saidaDir "linkaKotlin.db-shm"

$okMain = Export-DbFile -Adb $adb -PackageName $PackageName -RemoteName "linkaKotlin.db" -LocalPath $dbMain
$okWal = Export-DbFile -Adb $adb -PackageName $PackageName -RemoteName "linkaKotlin.db-wal" -LocalPath $dbWal
$okShm = Export-DbFile -Adb $adb -PackageName $PackageName -RemoteName "linkaKotlin.db-shm" -LocalPath $dbShm

if (-not $okMain) {
    throw "falha ao exportar linkaKotlin.db via run-as. Verifique packageName e build debuggable."
}

Write-Output "base Kotlin exportada: $dbMain"
if ($okWal) { Write-Output "wal exportado: $dbWal" }
if ($okShm) { Write-Output "shm exportado: $dbShm" }

if ($NaoExecutarComparacao) {
    Write-Output "comparacao nao executada por opcao."
    exit 0
}

$comparador = "scripts\speedtest\comparadorParidadeFlutterKotlin.py"
if (-not (Test-Path $comparador)) {
    throw "comparador nao encontrado: $comparador"
}

$args = @($comparador, "--kotlin-sqlite", $dbMain, "--modo", $Modo, "--janela-segundos", $JanelaSegundos.ToString())

if ($FlutterJsonDir -and (Test-Path $FlutterJsonDir)) {
    $args += @("--flutter-json-dir", $FlutterJsonDir)
}

if ($FlutterSqlite -and (Test-Path $FlutterSqlite)) {
    $args += @("--flutter-sqlite", $FlutterSqlite)
}

if (-not ($args -contains "--flutter-json-dir") -and -not ($args -contains "--flutter-sqlite")) {
    throw "informe uma fonte Flutter valida em -FlutterJsonDir ou -FlutterSqlite."
}

& $python $args
