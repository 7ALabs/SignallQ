$env:Path += ";C:\Program Files\GitHub CLI"
$repo = "7ALabs/SignallQ"
$ms = "v0.9.3 - Eficiencia"

$issues = @(
  @{f="17-speedtest-metered-network.md";   t="[dados] Speedtest detecta rede medida e limita payload";              l="severity:high,area:dados,area:ux"; m=$true},
  @{f="18-speedtest-ping-concorrente.md";  t="[dados] Reduzir ping concorrente durante throughput do speedtest";    l="severity:medium,area:dados,area:performance"; m=$true},
  @{f="19-okhttp-connection-pool-mobile.md"; t="[energia] ConnectionPool adaptativo por tipo de rede";              l="severity:medium,area:energia,area:performance"; m=$true},
  @{f="20-monitoramento-datastore-cache.md"; t="[energia] MonitoramentoWorker: combine() em vez de .first() em cascata"; l="severity:medium,area:energia,area:performance"; m=$true},
  @{f="21-monitoramento-http-timeouts.md"; t="[energia] MonitoramentoWorker: OkHttp + callTimeout + backoff";       l="severity:medium,area:energia"; m=$true},
  @{f="22-mainactivity-combine-flows.md";  t="[perf] MainActivity: agrupar Flows com combine() + distinctUntilChanged"; l="severity:medium,area:performance,area:arquitetura"; m=$true},
  @{f="23-resultado-velocidade-render.md"; t="[perf] ResultadoVelocidadeScreen: remember + LazyColumn nas listas";  l="severity:low,area:performance,area:design-system"; m=$false},
  @{f="24-okhttp-cache-dns.md";            t="[dados] Cache OkHttp para chamadas auxiliares (DNS/health)";          l="severity:low,area:dados,area:performance"; m=$false}
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
foreach ($i in $issues) {
  $body = Join-Path $root $i.f
  $args = @("issue","create","--repo",$repo,"--title",$i.t,"--label",$i.l,"--body-file",$body)
  if ($i.m) { $args += @("--milestone",$ms) }
  $url = & gh @args
  Write-Host "$($i.t) -> $url"
}
