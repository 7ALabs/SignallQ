$env:Path += ";C:\Program Files\GitHub CLI"
$repo = "7ALabs/linka-android"
$ms = "v0.9.2 - Hardening"

$issues = @(
  @{f="01-rotacionar-keystore.md";        t="[seg] Rotacionar keystore e remover key.properties do versionamento"; l="severity:critical,area:seguranca"},
  @{f="02-network-security-config.md";    t="[seg] Substituir usesCleartextTraffic global por Network Security Config"; l="severity:critical,area:seguranca,area:android"},
  @{f="03-introduzir-hilt.md";            t="[arq] Introduzir Hilt e migrar DI manual do MainViewModel"; l="severity:high,area:arquitetura"},
  @{f="04-erradicar-bang-bang.md";        t="[arq] Erradicar uso de !! em codigo de producao"; l="severity:high,area:arquitetura,area:qualidade"},
  @{f="05-detekt-ktlint-ci.md";           t="[qual] Configurar Detekt + Ktlint + workflow CI"; l="severity:medium,area:qualidade,area:scripts"},
  @{f="06-logger-timber.md";              t="[arq] Introduzir Logger abstrato (Timber) substituindo Log.* direto"; l="severity:medium,area:arquitetura"},
  @{f="07-dispatchers-explicitos.md";     t="[arq] Definir Dispatchers explicitos em repositorios IO/DB"; l="severity:medium,area:arquitetura"},
  @{f="08-allowbackup-extraction-rules.md"; t="[seg] Restringir allowBackup e definir dataExtractionRules"; l="severity:medium,area:seguranca,area:android"},
  @{f="09-baseline-profile-splits.md";    t="[perf] Gerar Baseline Profile e habilitar splits AAB"; l="severity:medium,area:android"},
  @{f="10-strings-i18n.md";               t="[ux] Mover strings UI hardcoded para strings.xml e preparar i18n"; l="severity:medium,area:ux,area:design-system"},
  @{f="11-acessibilidade.md";             t="[ux] Auditoria de acessibilidade (contentDescription, TalkBack)"; l="severity:medium,area:ux"},
  @{f="12-uistate-padrao.md";             t="[ux] Padronizar UiState<T> selado (Loading/Success/Empty/Error)"; l="severity:medium,area:ux,area:arquitetura"},
  @{f="13-auditar-permissoes.md";         t="[android] Auditar permissoes do AndroidManifest contra uso real"; l="severity:medium,area:android,area:seguranca"},
  @{f="14-consolidar-docs-adrs.md";       t="[docs] Consolidar duplicacoes docs/ vs docs_ai/ e criar ADRs"; l="severity:medium,area:docs"},
  @{f="15-todos-orfaos.md";               t="[qual] Auditar e remover TODOs orfaos ou converter em issues vinculadas"; l="severity:low,area:qualidade"},
  @{f="16-cobertura-testes-core.md";      t="[qual] Elevar cobertura de testes em core* (database/network/datastore)"; l="severity:low,area:qualidade,area:arquitetura"}
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$urls = @()
foreach ($i in $issues) {
  $body = Join-Path $root $i.f
  $args = @("issue","create","--repo",$repo,"--title",$i.t,"--label",$i.l,"--body-file",$body)
  if ($i.l -match "critical|high") { $args += @("--milestone",$ms) }
  $url = & gh @args
  Write-Host "$($i.t) -> $url"
  $urls += $url
}
$urls | Set-Content (Join-Path $root "_created.txt")
