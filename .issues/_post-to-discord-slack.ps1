$DiscordWebhook = $env:DISCORD_WEBHOOK_LINKA
if (-not $DiscordWebhook) {
  Write-Host "Erro: DISCORD_WEBHOOK_LINKA não está configurada em variáveis de ambiente ou .env" -ForegroundColor Red
  Write-Host "Configure o webhook em .env (copie .env.example como referência)" -ForegroundColor Yellow
  exit 1
}

$SlackChannelId = "C0B4NSGSK1D"

# Mapeamento de issues por sprint
$sprints = @{
  "Sprint 1: Segurança Crítica" = @(
    @{ num = 1; title = "Rotacionar Keystore"; agent = "Rodrigo + Gema"; severity = "🔴 CRITICAL" }
    @{ num = 2; title = "Network Security Config"; agent = "Rodrigo + Gema"; severity = "🔴 CRITICAL" }
  )

  "Sprint 2: Arquitetura & Qualidade" = @(
    @{ num = 3; title = "Introduzir Hilt"; agent = "Camilo"; severity = "🟠 HIGH" }
    @{ num = 4; title = "Erradicar !! Operators"; agent = "Camilo"; severity = "🟠 HIGH" }
    @{ num = 5; title = "Detekt + Ktlint + CI"; agent = "Marina + Camilo"; severity = "🟡 MEDIUM" }
    @{ num = 16; title = "Elevar Cobertura em core*"; agent = "Marina"; severity = "🟡 MEDIUM" }
  )

  "Sprint 3: Logging & Dispatchers" = @(
    @{ num = 6; title = "Logger Abstrato com Timber"; agent = "Camilo"; severity = "🟡 MEDIUM" }
    @{ num = 7; title = "Dispatchers Explícitos"; agent = "Camilo"; severity = "🟡 MEDIUM" }
    @{ num = 8; title = "allowBackup + dataExtractionRules"; agent = "Rodrigo"; severity = "🟡 MEDIUM" }
  )

  "Sprint 4: Performance & Otimizações" = @(
    @{ num = 17; title = "Detecção de Rede Medida"; agent = "Brás"; severity = "🟡 MEDIUM" }
    @{ num = 18; title = "Ping Concorrente Relaxado"; agent = "Brás"; severity = "🟡 MEDIUM" }
    @{ num = 19; title = "ConnectionPool Adaptativo"; agent = "Camilo"; severity = "🟡 MEDIUM" }
    @{ num = 20; title = "Monitoramento com combine()"; agent = "Camilo"; severity = "🟡 MEDIUM" }
    @{ num = 21; title = "HTTP Timeouts Global"; agent = "Camilo"; severity = "🟡 MEDIUM" }
    @{ num = 22; title = "MainActivity com combine()"; agent = "Camilo"; severity = "🟡 MEDIUM" }
    @{ num = 23; title = "ResultadoVelocidadeScreen Render"; agent = "Claudete"; severity = "🟡 MEDIUM" }
    @{ num = 24; title = "OkHttp DNS/Health Caching"; agent = "Camilo"; severity = "🟡 MEDIUM" }
  )

  "Sprint 5: Design System & UX" = @(
    @{ num = 10; title = "Mover Strings para i18n"; agent = "Claudete"; severity = "🟡 MEDIUM" }
    @{ num = 11; title = "Auditoria de Acessibilidade"; agent = "Claudete"; severity = "🟡 MEDIUM" }
    @{ num = 12; title = "UiState Padrão Selado"; agent = "Gema + Claudete"; severity = "🟡 MEDIUM" }
    @{ num = 13; title = "Auditar Permissões"; agent = "Rodrigo"; severity = "🟡 MEDIUM" }
    @{ num = 14; title = "Consolidar Docs + ADRs"; agent = "Gema"; severity = "🟡 MEDIUM" }
    @{ num = 15; title = "TODOs Órfãos"; agent = "Gema"; severity = "🟢 LOW" }
  )
}

function Post-ToDiscord($title, $issues) {
  $content = @"
# $title
**Semana**: Veja detalhes das issues abaixo

"@

  foreach ($issue in $issues) {
    $content += @"
**$($issue.severity) #$($issue.num)**: $($issue.title)
👤 Agente: $($issue.agent)
🔗 GitHub: https://github.com/7ALabs/SignallQ/issues/$($issue.num)

"@
  }

  $payload = @{
    content = $content
    username = "LINKA Auditor Bot"
  } | ConvertTo-Json

  Invoke-WebRequest -Uri $DiscordWebhook -Method Post -ContentType "application/json" -Body $payload | Out-Null
}

Write-Host "Postando 24 issues para Discord (5 threads por sprint)..." -ForegroundColor Green

foreach ($sprintName in $sprints.Keys) {
  Write-Host "  → $sprintName..." -ForegroundColor Yellow
  Post-ToDiscord $sprintName $sprints[$sprintName]
  Start-Sleep -Milliseconds 500
}

Write-Host ""
Write-Host "✅ Todas as 24 issues postadas no Discord!" -ForegroundColor Green
Write-Host "📍 Canal: https://discord.com/channels/... (LINKA)" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  Slack: Usar conector MCP (Slack não tem webhook configurado)" -ForegroundColor Yellow
