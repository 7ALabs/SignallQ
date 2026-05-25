# Changelog — Linka Android

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/) e este projeto adere a [Semantic Versioning](https://semver.org/).

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

---

## [Unreleased]

### Added
- **WiFi screen topology icons:** Substitui chips de texto (Roteador/Mesh/Repetidor) por ícones visuais (Router/Hub/CellTower/Lan) com cores semanticamente distintas (cinza, azul accent, laranja warning). Nó conectado exato destacado em cor accent.
- **Network grouping by SSID:** Redes de terceiros agrupadas por SSID com expand/collapse para múltiplos nós (BSSIDs). Single-BSSID networks abrem detalhe direto. SSIDs ocultos agrupados em seção "Redes ocultas". Filtragem por banda preservada.
- **Proteção de dados móveis:** Speedtest detecta rede celular medida e solicita confirmação antes de testes de 25 MB (Completo) ou 30 MB (Triplo). Modo Rápido (10 MB) executa sem aviso.
- **Preferência de dados móveis:** Novo toggle em Ajustes — "Sempre permitir testes pesados em dados móveis" — desativa o aviso para quem tem plano ilimitado.
- **Consumo mensal:** Ajustes exibe o total de dados consumidos em testes este mês, com reset automático na virada do mês.
- **UiState<T> sealed interface e StatefulScreen composable (Issue #12-A):** Novo padrao de state management. Sealed interface `UiState<T>` com estados `Loading`, `Success(data: T)`, `Empty`, `Error`. Composable `StatefulScreen` generico reduz boilerplate.
- **Migracao de PingScreenState e DiagnosticoScreenState para UiState<T> (Issue #12-B):** Refatoracao para usar `UiState<T>` com ViewModel em Coroutines, substituindo LiveData legacy.
- **Migracao de localizacaoServidor, localIp, ispInfo e publicIp para UiState<T> (Issue #12-C):** MainViewModel expoe campos de rede como `StateFlow<UiState<T>>`. Catch vazio substituido por `UiState.Error`.
- **Modifier.expandable() para toggles acessiveis (Issue #11):** Novo modificador com semantica de toggle, role acessivel, contentDescription e feedback tatil. Aplicado a 10 telas/componentes.

### Changed
- **Qualidade de código:** Eliminado uso de `!!` (not-null assertion) em código de produção. Substituído por `checkNotNull` com mensagem descritiva, elvis operator e early return conforme o contexto.
- **Injeção de dependência:** Introduzido Hilt para DI. `MainViewModel` migrado de instanciação manual (`lazy { Modulo.criar*() }`) para `@HiltViewModel` com injeção via construtor. Melhora testabilidade e ciclo de vida das dependências.

### Security
- **Network Security Config:** Substituído `usesCleartextTraffic` global por configuração declarativa. Cleartext HTTP restrito a IPs de gateway LAN (acesso a modem). Chamada `ip-api.com` migrada para HTTPS.

---

## [0.8.4] — 2026-05-19

### Correção — Detecção de 5G NSA (1)
- **MonitorTelephonyImpl**: app mostrava "4G" em redes 5G NSA porque `derivarTecnologia` usava `serviceState.toString()` para detectar `nrState=CONNECTED`, o que falha em vários OEMs. A detecção agora usa `allCellInfo` como fonte secundária — se `CellInfoNr` registrado está presente e a tecnologia derivada era "4G" ou null, exibe "5G NSA" corretamente.

---

## [0.8.3] — 2026-05-19

### Tela de Sinal — Rede Móvel (redesign completo)
- **MobileSignalCard**: redesenhado com 4 seções — header (operadora + badge de tecnologia), gauge semicircular de RSRP colorido por nível, colunas de Força e Estabilidade, card de diagnóstico com causa e ação sempre visível
- **MobileSignalCard**: textos em linguagem humana (Excelente/Bom/Regular/Fraco + Estável/Moderada/Instável) em vez de valores técnicos brutos

### Tela de Sinal — Canal Wi-Fi
- **CanalTab**: canal atual promovido para destaque (`titleLarge`) com card próprio
- **CanalTab**: card "Você está no canal ideal" (verde) exibido quando não há canal melhor disponível
- **CanalTab**: textos de recomendação reescritos em linguagem natural — "Seu canal é o 36. Melhor mudar para o 40…"
- **CanalTab**: título do card de recomendação alterado para "Troque de canal"

### Tela Histórico — Monitoramento
- **UptimeGridChart**: grid Canvas de 336 blocos substituído por lista de eventos por dia — mostra uptime %, barra de progresso e resumo de períodos offline com horário

### Tela Home — Trilha de Rede
- **NetworkPath**: nó `wifiMesh` agora exibe ícone Hub com label "Mesh" (era "Wi-Fi", genérico demais)

### Correções (5) — entregues pelo Camilo anterior (0.8.2→0.8.3)
- **HomeScreen**: `QualidadeShortcutRow` ("Diagnóstico Inteligente") agora gateado por `FeatureFlags.DIAGNOSTICO_ITERATIVO` — não aparece mais em release
- **HomeScreen**: `internetLabel` em conexão móvel retorna "Internet" fixo — evitava operadora duplicada na trilha
- **HomeScreen**: nó móvel sem IP exibe tecnologia (ex: "5G") como sublabel direto, sem "—" prefixado
- **AjustesScreen**: `PerfilEditSheet` exibe nome da operadora em conexão móvel (era "Rede móvel" fixo)
- **AjustesScreen**: item duplicado "Dados usados pelo Linka" removido — informação já coberta por "Privacidade e dados"

---

## [0.8.2] — 2026-05-19

### Correções UX/Visual (3)
- **AppShell**: navbar agora fixa — removida animação de scroll-hide que deixava espaço vazio no rodapé
- **HomeScreen**: trilha de rede (NetworkPath) agora proporcional à tela — nós distribuídos com `weight(1f)` e `SpaceEvenly` em vez de 80dp fixo
- **HomeScreen**: "Modo Gamer" renomeado para "Jogar Online"

### Melhorias (2)
- **PerfilEditSheet**: avatar agora exibe dados de conexão (ISP/Operadora, IP Público, Tipo de conexão, Localização) em vez de nome do aparelho
- **UptimeGridChart**: gráfico de monitoramento reorientado para 7 linhas (dias) × 48 colunas scrolláveis (blocos de 30min), blocos quadrados 8dp — era 7 colunas × 48 linhas achatadas

---

## [0.8.1] — 2026-05-19

### Correções UX/Visual (10)
- **FibraScreen**: modem name agora é dinâmico (era hardcoded "Nokia" para todos os usuários)
- **ResultadoVelocidadeScreen**: título corrigido para "Resultado do teste" (era "Diagnóstico IA")
- **ResultadoVelocidadeScreen**: cores das métricas de latência e jitter agora dinâmicas por valor (semáforo)
- **ResultadoVelocidadeScreen**: botão "Testar upload novamente" agora usa cor accent (não warning)
- **SinalScreen**: título TopBar corrigido para `titleLarge` (era `bodyLarge`)
- **HistoricoScreen**: título TopBar corrigido para `titleLarge` (era `bodyLarge`)
- **HomeScreen**: botão "Central de testes" (era "Central de Medição")
- **HomeScreen**: conectores da topologia de rede com `contentDescription` para TalkBack
- **AjustesScreen**: orientação adicionada na seção "Minha Conexão"
- **NovidadesScreen**: estado de erro agora tem botão "Tentar novamente"

### Acessibilidade (1)
- **SpeedTestScreen**: seletor de modo com semântica TalkBack

### Diagnóstico de Rede (5)
- **SinalScreen**: classificação de sinal Wi-Fi agora distingue 2.4GHz de 5GHz (thresholds calibrados por banda)
- **DiagnosticoScreen**: tipo de conexão enviado para IA agora é real (era `wifi` hardcoded)
- **DiagnosticoScreen**: texto do step "Gerando diagnóstico com IA…" sem duplicação com título
- **FibraScreen**: hint e limiar de RX Power alinhados a ITU-T G.984 (−27 dBm mínimo)
- **DnsDiagnosticEngine**: novo diagnóstico DNS-03 para latência 51-150ms (status info)

---

## [0.8.0] — 2026-05-18

### Features Ativadas
- **FEATURE_FIBRA_SCREEN** promovida para `true` em release — FibraScreen agora visível em produção
- **FEATURE_DNS_SCREEN** promovida para `true` em release — DNS benchmark agora visível em produção

### Flags Novas
- **FEATURE_DIAGNOSTICO_CHAT** adicionada (debug: `true`, release: `false`) — chat com IA oculto em release, disponível em debug

### Correções
- **AppShell**: lambdas de `onConectarFibra()` e `onAbrirDnsBenchmark()` agora gateadas por feature flags para evitar side-effects quando desativadas
- **ResultadoVelocidadeScreen**: botão "Testar Novamente" duplicado no rodapé removido — componente `CtaVelocidadeResultado` agora renderiza uma única vez

### Melhorias Internas
- **ResultadoVelocidadeScreen**: botão de chat protegido por `FeatureFlags.DIAGNOSTICO_CHAT`
- **AppShell**: overlays de Chat, Fibra e DNS protegidos por feature flags correspondentes
- **FeatureFlags.kt**: mapeamento de `DIAGNOSTICO_CHAT` para `BuildConfig.FEATURE_DIAGNOSTICO_CHAT`

---

## Histórico de Versões

Versões anteriores a 0.8.0 não possuem registro detalhado neste arquivo.
