# Changelog Entry — v0.9.0 (draft)

**Para copiar em:** `linka-android-kotlin/CHANGELOG.md`  
**Formato:** Keep a Changelog + SemVer  
**Status:** Pronto para merge

---

## [0.9.0] — 2026-05-20

### Added

- **Central de Testes — Grid 2×N de Ferramentas:** Nova interface unificada para acesso rápido a ferramentas de diagnóstico. Grid adaptativo com 2 colunas (N linhas), mostrando DNS Benchmark, Ping/Latência e Diagnóstico Inteligente. Cada card exibe ícone vetorial, título, descrição curta e state visual (ativo/desabilitado com badge). StatusCard integrado exibe status de conectividade (Wi-Fi/Móvel/Offline) e localização do servidor de teste em tempo real.

- **Ping / Latência — Ferramenta de medição de latência:** Executa 20 amostras ICMP sobre HTTP/2 contra Cloudflare Speed (sem payload, timeout 4s/amostra). Calcula **latência (mediana em ms)**, **jitter (desvio padrão em ms)** e **perda de pacotes (%)** com interface de progresso real-time. Estados: Idle → Executando (barra de progresso 0-100%) → Resultado (3 métricas) → Erro (com mensagem e retry). Modal ModalBottomSheet com suporte a swipe-down e botão voltar.

- **DNS Benchmark — Provedores ISP brasileiros:** Adicionados 2 novos resolvedores públicos brasileiros ao benchmark de DNS:
  - **Registro.br** (https://dns.registro.br/query) — Gerido por Fapesp, mantém registros .br, ideal para latência em SP
  - **CETIC.br** (https://resolver.cetic.br/dns-query) — Centro de Estudos e Tecnologia em Informação e Comunicação, resolver público nacional de baixa latência
  - Benchmark agora testa 7 provedores total (era 5: Cloudflare, Google, Quad9, OpenDNS, AdGuard)

- **StatusCard — Loading state visual:** Card de status do servidor exibe "Cloudflare · Carregando…" enquanto localizacaoServidor é null. Ícone e texto com cor dinâmica (textSecondary = carregado, textTertiary = carregando). Sem breaking change, fallback para valor anterior se API falhar.

### Fixed

- **Diagnóstico Inteligente — Desabilitado sem confusão:** Feature agora aparece no grid com badge "Em breve" e 50% opacidade visual, tornando explícito que está em desenvolvimento. Card não é clicável (nenhum side-effect). Evita confusão de usuário (FEATURE_DIAGNOSTICO_CHAT = false em release). Será reabilitado quando flag mudar para true nas próximas versões.

### Changed

- **DNS Benchmark — Separador visual:** Hostname de conexão agora usa separador ponto médio `·` em vez de hífen `-`. Exemplos:
  - Antes: "Cloudflare-carregando…", "Cloudflare-", "Cloudflare-São Paulo, BR"
  - Depois: "Cloudflare · Carregando…", "Cloudflare ·", "Cloudflare · São Paulo, BR"
  - Melhora legibilidade (ponto médio é separador tipográfico padrão)

- **ExploreToolsSheet → ExploreToolsRow:** Layout de ferramentas passou de bottom sheet vertical (acionada por botão) para grid 2×N em área principal da tela SpeedTest. Menos toque necessário, mais visual, melhor para descoberta (grid sempre visível).

- **SpeedTestScreen — Exports e callback:** PingScreen integrada como ModalBottomSheet. Callback `onAbrirPing: () -> Unit` adicionado a `SpeedTestScreen`, gerenciado por `MainViewModel`. Nenhuma breaking change em assinatura existente (todos callbacks opcionais).

### Technical

- **New: io.linka.app.kotlin.feature.speedtest.PingExecutor** (object, ~150 LOC)
  - Companion object: OkHttpClient singleton com HTTP/2, 4s timeouts, User-Agent Desktop, Cache-Control: no-store
  - `suspend fun executar(count: Int = 20, onProgresso: (Int) -> Unit): PingResultado`
  - Algoritmo: 1ª amostra descartada (warmup), 2-20 válidas. Filtra outliers (≤3× mediana). Calcula mediana (latência), stdDev (jitter), perda%.
  - Coroutine-safe: usa Dispatchers.IO, withContext()

- **New: io.linka.app.kotlin.feature.speedtest.PingResultado** (data class)
  ```kotlin
  data class PingResultado(
    val latenciaMs: Double,
    val jitterMs: Double,
    val perdaPercentual: Double,
    val amostras: Int,
  )
  ```

- **New: io.linka.app.kotlin.ui.screen.PingScreenViewModel** (class, ~40 LOC)
  - MutableStateFlow<PingScreenState> internal
  - `suspend fun executarPing()` — dispara executor, atualiza estado
  - `fun resetar()` — volta para Idle
  - Sealed interface PingScreenState: Idle, Executando(progresso: Int), Resultado(resultado), Erro(mensagem)

- **New: io.linka.app.kotlin.ui.screen.PingScreen** (Composable, ~200 LOC)
  - ModalBottomSheet com temas LkTokens (bgCard, border, textPrimary, etc.)
  - Estados renderizados: Idle (botão "Iniciar"), Executando (LinearProgressIndicator), Resultado (Column de 3 métricas), Erro (texto + botão retry)
  - Padrão Screen/ViewModel separado, StateFlow.collectAsState()

- **Modified: io.linka.app.kotlin.feature.dns.BenchmarkDnsDoh**
  - Linhas 51-59: adicionados 2 provedores ao `provedoresPublicos` list
  - Nenhuma mudança em assinatura ou algoritmo, apenas mais 2 loops no forEach (7 vs 5)
  - Logging atualizado (continua com TAG = "LinkaDnsBenchmark")

- **Modified: io.linka.app.kotlin.ui.screen.SpeedTestScreen**
  - Função `ExploreToolsRow` (novo composable, ~60 LOC) substitui acesso anterior a `ExploreToolsSheet`
  - Composable `FerramentaCard` (novo, ~50 LOC) — padrão reutilizável com icon, título, descrição, badge, disabled state
  - `StatusCard` expandida (novo ~40 LOC adicional) — agora com servidor "Cloudflare · Carregando…"
  - Callback `onAbrirPing: () -> Unit` adicionado a `SpeedTestScreen` signature (parâmetro opcional com default {})

- **Temas aplicados consistently:**
  - LkTokens: bgCard, border, textPrimary, textSecondary, textTertiary
  - LkSpacing: sm (8dp), md (12dp), lg (16dp)
  - LkRadius: card (12dp)
  - Material3: LinearProgressIndicator, ModalBottomSheet, RoundedCornerShape, Modifier.clip/border/background

- **Acessibilidade:**
  - FerramentaCard: Icon + role=Button, Text com contentDescription
  - PingScreen: estados com descrição (Idle = "Ping ocioso", Executando = "Ping em progresso X%")
  - StatusCard: ícones com tint dinâmica (textSecondary = ativo, textTertiary = carregando)

### Tests

- **io.linka.app.kotlin.feature.speedtest.PingExecutorTest** (novo)
  - `testExecutarComAmostraValida()` — 5 amostras, resultado.latenciaMs > 0, jitter ≥ 0, perda 0-100%
  - `testProgressoCallback()` — callback reporta progresso 1 a count
  - `testTimeoutHandling()` — sem conexão, gera Erro

- **io.linka.app.kotlin.ui.screen.PingScreenTest** (novo)
  - `testIdleState()` — renderiza botão "Iniciar"
  - `testExecutandoState()` — LinearProgressIndicator visível
  - `testResultadoState()` — 3 métricas exibidas (latência, jitter, perda)
  - `testErroState()` — mensagem erro + botão retry

- **io.linka.app.kotlin.feature.dns.BenchmarkDnsDohTest** (expansão)
  - `testProveedoresIncluemBrasileiros()` — verifica Registro.br + CETIC.br na lista

- **UI Snapshot Tests** (temas, spacing, radius)
  - FerramentaCard (ativo/desabilitado com badge)
  - StatusCard (conectado/carregando/offline)

### Versioning

**De:** 0.8.5  
**Para:** 0.9.0 (minor bump)  
**Justificativa:** Feature nova (Ping) + providers novos (DNS BR) + redesign visual grid (feature parity + UI enhancement)

### Breaking Changes

Nenhuma. Todos callbacks são opcionais com defaults.

### Migration Guide

Nenhuma migração necessária.

### Known Issues & Future Work

- **Interrupção de teste:** User não pode "cancel" durante execução (implementar com backHandler futuro)
- **Histórico de testes:** Não há persistência de resultados passados (considerar para v0.10)
- **DNS + Diagnóstico integrados:** Próxima feature = recomendação automática (ex: "Troque para Registro.br")

