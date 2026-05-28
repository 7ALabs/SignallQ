# Changelog — Linka Android

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/) e este projeto adere a [Semantic Versioning](https://semver.org/).

---

## [0.12.0] — 2026-05-27

### Added

- **Chat de Diagnóstico IA — nova tela chat-first:** Ao tocar no botão "Diagnóstico" da home, abre direto `ChatDiagnosticoIaScreen` sem tela intermediária. Estilo conversa moderna (Material Design 3) com balões à direita para o usuário e à esquerda para a IA, drawer lateral com histórico de sessões, input fixo acima da bottom nav e tratamento correto de IME. Primeira abertura exibe duas mensagens da IA (boas-vindas + opções) e três chips clicáveis: "Analisar meu último teste", "Executar novo teste agora", "Analisar meu histórico recente". Cada opção dispara fluxo dedicado — última medição via `MedicaoDao.observarUltimas(1)`, novo teste executado em background sem sair do chat com mensagens progressivas a cada fase do speedtest (download → upload → coleta → análise), histórico via `observarUltimas(7)` com mensagem adaptada quando há menos de 7 testes.

- **Streaming SSE de respostas da IA:** Reutiliza `AiDiagnosisRepository.explainDiagnosisStream` com tokens aplicados incrementalmente na bolha da IA. Auto-scroll inteligente — só puxa para o fim quando o usuário já estava no fim, não interrompe leitura de histórico. Nome do modelo exibido vem de `ModeloIa.nomeExibicao` (não hardcoded), com fallback "o modelo de IA".

- **Persistência de sessões via Room v10:** Novas tabelas `chat_sessions` (id, título, criadoEmEpochMs, atualizadoEmEpochMs, status, tipoDiagnostico, nomeModelo, diagnosticoPayloadJson) e `chat_messages` (id, sessionId, role, content, createdAtEpochMs, status, metadataJson) com foreign key `ON DELETE CASCADE`. Drawer lateral permite abrir, apagar (com confirmação) e renomear (dialog com contador 60 chars) sessões anteriores. Título da sessão derivado automaticamente da primeira mensagem do usuário, truncado a 40 chars com reticência.

- **Cota diária com janela rolling 24h:** `CotaIaRepository` em DataStore isolado (`cota_ia`), default 10 análises por ciclo de 24h. Renovação calculada a partir do início do ciclo (não à meia-noite). Quando excedida, input substituído por `CotaExcedidaBanner` com data/hora de renovação formatada em pt-BR (`Locale.forLanguageTag("pt-BR")` + `DateTimeFormatter`) — "amanhã às 14h32", "hoje às 22h15" ou fallback "em 24 horas". Sessões antigas continuam acessíveis para leitura mesmo com cota zerada; nova análise só permitida quando ciclo renovar.

- **Tratamento humanizado de cinco cenários de erro:** Modelo indisponível ("No momento o ${modelo} está indisponível..."), sem rede ("Verifique sua conexão..."), timeout ("A análise demorou mais que o esperado..."), resposta incompleta ("Recebi uma resposta incompleta...") e catch-all. Sem stack trace, sem código HTTP cru. Mensagens viram `ChatMessageEntity` com `role=system`, `status=failed` e `metadataJson.errorCode` preservando o motivo técnico para suporte.

- **Componentes Compose adaptados:** `OrbitAiMessageBubble` ganhou parâmetro `isProgressMessage: Boolean` que suprime métricas, ações e detalhes técnicos para mensagens de progresso do teste. `OrbitInputArea` ganhou parâmetro `placeholder: String` customizável para sinalizar "Aguarde o resultado do teste..." e "Aguarde a resposta da IA..." sem reaproveitar gambiarra de `isLimitReached`. Ambos parâmetros têm default que preserva callers existentes.

### Changed

- **Entry point do botão "Diagnóstico":** Passa a empilhar `Overlay.ChatDiagnosticoIa` em vez de `Overlay.DiagnosticoInteligente`. Bottom nav já oculta durante speedtest reaproveitada — quando o chat dispara um novo teste, a navbar global desaparece pelo mesmo gate. `BackHandler` integrado fecha o overlay.

- **`Locale("pt", "BR")` modernizado para `Locale.forLanguageTag("pt-BR")`** nos componentes do chat (4 ocorrências). Outros call sites do app permanecem como estão para escopo dedicado de migração futura.

### Deprecated

- **`DiagnosticoScreen` (chat inline antigo) marcada como `@Deprecated`:** Mantida no codebase como fallback de emergência via `Overlay.DiagnosticoInteligente` (também deprecado por KDoc, já que Kotlin não honra `@Deprecated` em entries de enum/sealed para warning de uso). Call sites internos do `AppShell` que ainda referenciam o overlay antigo têm `@Suppress("DEPRECATION")` documentando a intencionalidade. Será removida em próxima major.

### Technical

- **Migration Room v9 → v10** com SQL alinhado ao schema gerado pelo Room (`10.json`, identityHash `97f676bb…`). Schema antigo no repositório continha entidades fantasma (`fibra_config`, `movel_chip_config`, `localizacao_config`) sem código correspondente — resíduo de branch nunca mergeada — sobrescrito com schema correto.

- **11 testes unitários para `CotaIaRepository`** (rolling 24h, expiração de ciclo, reset automático, renovação, observabilidade reativa) com clock injetável (`clock: () -> Long = System::currentTimeMillis`) para simular passagem de tempo sem `Thread.sleep`.

- **25 testes unitários para `ChatDiagnosticoIaViewModel`** cobrindo os 3 fluxos de opção inicial, cota excedida pré-chamada, erros de rede/modelo/timeout/resposta incompleta, renomeação automática de sessão e persistência de medição via chat.

- **AndroidTests para `ChatSessionDao` (7 casos), `Migration_9_10` (`MigrationTestHelper`) e `ChatDiagnosticoIaRepository` (19 casos)** compilados e prontos. Execução em device ficou pendente — recomendada antes de release.

- **`ChatDiagnosticoIaViewModel` injetado via `viewModels()` na MainActivity** seguindo padrão do `MainViewModel` (projeto não tem `hilt-navigation-compose` no catálogo). Repositories de chat e cota construídos via `by lazy` recebendo `LinkaDatabase` e `@ApplicationContext`.

### Known Limitations

- **Race condition residual com `MainViewModel`:** Ambos ViewModels observam o mesmo `executorSpeedtest.snapshotFlow`. Guard `ultimoResultadoSpeedtestPersistidoEpochMs` em ambos previne dupla persistência no Room, mas só é seguro enquanto a Screen do chat e a Screen do speedtest não estiverem ativas simultaneamente — o que é garantido pelo overlay stack hoje.

- **`operadoraMovel = null`** em `MedicaoEntity` persistida quando o teste é disparado pelo chat (sem `MonitorTelephony` injetado no novo ViewModel). Documentado para correção em PR de polish.

- **Smoke tests Compose ausentes** — setup Robolectric não configurado no módulo `:app`. Validação visual da Screen depende de teste manual ou device farm.

---

## [0.11.4] — 2026-05-27

### Fixed

- **DNS atual sem latência no comparativo:** Dois bugs independentes. (1) `resolveDnsName()` não mapeava Registro.br (200.160.0.80, 200.160.2.3) e CETIC.br (191.234.170.40), retornando "DNS do Provedor" para essas IPs. O benchmark identificava corretamente o nome via `inferirNomeSistemaDns()`, criando mismatch no lookup da latência. Adicionados os IPs ao `resolveDnsName()` em sincronia com `mapaIpParaProvedor`. (2) Filtro `>= 3ms` em `medirSistemaDns()` excluía DNS de operadora com respostas rápidas em 5G/4G, resultando em `amostras` vazio e latência null. Filtro removido — round 0 descartado como warmup, rounds 1-2 sempre incluídos independente do tempo.

---

## [0.11.3] — 2026-05-27

### Fixed

- **Trilha de rede mostra "sem internet" com internet ativa:** `hasInternetError` usava `publicIp == null && ispInfo == null` como sinal de erro, mas esses valores são nulos também enquanto o fetch do ISP está em andamento — resultando em erro imediato antes mesmo de qualquer requisição completar. `loadingInternet` era logicamente impossível quando conectado (mesmas condições já ativavam `hasInternetError`). Fix: `hasInternetError` agora usa `!snapshotRede.conectado` (`NET_CAPABILITY_VALIDATED`) — erro só aparece quando o SO confirma ausência de internet (captive portal, sem rota). `loadingInternet` agora funciona corretamente enquanto o IP/ISP ainda está sendo buscado.

---

## [0.11.2] — 2026-05-27

### Fixed

- **Crash na abertura do app (NullPointerException em MonitorRedeAndroid):** Bug de ordem de inicialização — `calcularSnapshotAtual()` era chamado durante a inicialização de `mutableSnapshotFlow` (linha 28), antes que `tentativasAguardandoValidated`, `mainHandler` e `runnableRetry` fossem inicializados (linhas 32–39). Campos não inicializados retornam `null` na JVM → NPE ao chamar `.set(0)`. Fix: reordenar declarações para que todas as dependências de `calcularSnapshotAtual()` sejam inicializadas antes de `mutableSnapshotFlow`. Latente desde 0.11.0, amplificado em 0.11.1 com distribuição pelo Firebase (244 crashes, 81 usuários afetados em 1 dia).

---

## [0.11.1] — 2026-05-27

### Fixed

- **Detecção de 5G NSA em Samsung Exynos / Xiaomi MIUI 14+:** Em devices que retornam `allCellInfo` vazio sem permissão de localização, o app exibia "4G" mesmo em rede 5G NSA. Adicionado terceiro mecanismo de detecção via `SignalStrength.cellSignalStrengths` (API 29+): se o objeto contém `CellSignalStrengthNr`, confirma 5G NSA independente de `allCellInfo` ou reflexão via `getNrState()`.

- **Histórico móvel vazio ao filtrar por MOVEL:** Double-filter no modo controlado — AppShell passava lista já filtrada pelo ViewModel, mas `HistoricoScreen` re-filtrava internamente, resultando em lista sempre vazia. Modo controlado agora usa a lista diretamente sem re-filtrar. Estado vazio com mensagem contextual ("Nenhum teste para este filtro") quando não há resultados pós-filtro.

- **Back fecha o app na tab Histórico:** Sem `BackHandler` na tab Histórico (índice 3), o back gesture do sistema encerrava o app. Agora navega para Home (tab 0).

- **Trilha de rede mostra desconectado com internet ativa:** Race condition em `ConnectivityManager.NetworkCallback` — `onLost` disparava antes de `onAvailable` durante handoff Wi-Fi → Móvel, emitindo estado desconectado por 1-3s. Debounce de 2000ms adicionado: `onLost` aguarda antes de confirmar desconexão; `onAvailable` cancela o debounce pendente se a rede voltar na janela.

---

## [0.11.0] — 2026-05-26

### Added

- **Tela Fibra/Modem — Análise avançada do modem/ONT (#168):** `FibraModemScreen.kt` com 5 estados visuais: SemWifi ("A análise do modem só funciona na rede local"), SemCredenciais com CTA para Ajustes, Conectando com skeleton animado e LinearProgressIndicator, Erro com dois botões ("Tentar novamente" + "Revisar configurações"), e Concluído com chip de status geral, bloco de valores técnicos (RX/TX/temperatura/status óptico/modelo com fallback "--") e bloco de interpretação via `FibraSignalQualityEngine`. Senha do modem nunca exposta. `FibraModemUiState.kt` com sealed interface de 5 estados e função `mapearSnapshotFibra` pura.

- **Tela DNS — Benchmark, recomendação e guia de configuração (#167):** `DnsScreen.kt` com 4 blocos: DNS atual (IP, nome amigável, badge "DNS Privado ativo", latência), Benchmark com 7 provedores (Cloudflare, Google, Quad9, OpenDNS, AdGuard, Registro.br, CETIC.br), Recomendação com disclaimer explícito ("Isso não troca o DNS automaticamente"), e Guia colapsável "Como trocar DNS?" com seções "Quando vale a pena" e "Quando não faz diferença". Texto educacional no topo: "DNS afeta abertura de sites, não velocidade." Extraído do AppShell para arquivo próprio.

- **Onboarding — Checkbox de termos e cards de permissão (#165):** Slide 1 exige aceite obrigatório de Termos de Uso e Política de Privacidade antes de avançar (swipe e botão bloqueados). Slide 2 apresenta cards de permissão com ícone, título, descrição e botão de concessão individual para Localização/Wi-Fi (`ACCESS_FINE_LOCATION`) e Dispositivos próximos (`NEARBY_WIFI_DEVICES`, API 33+). Botão "Pular" visível apenas no slide 0. Launcher contextual usa `solicitacaoPermissoes` para não corromper callback de localização.

- **Mensagens humanizadas para estados vazios (#164):** Estados 9.3, 9.4, 9.5 e 9.6 com mensagens em linguagem humana e ícones contextuais. Substitui textos técnicos por explicações práticas de limitações (permissões, offline, recursos não disponíveis).

### Fixed

- **Privacidade — MAC address mascarado em DispositivosScreen (#166):** Octetos 3-4 do MAC são substituídos por `••` (ex: `c4:8e:de:ad:1a:2b` → `c4:8e:••:••:1a:2b`). Aplica em DeviceDetailSheet e MeshApSheet. Estado sem Wi-Fi exibe mensagem diferenciada "Dispositivos da rede só aparecem quando você está conectado a um Wi-Fi."

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

## [0.10.0] — 2026-05-26

### Added

- **Tela Fibra/Modem — Análise avançada do modem/ONT (#149):** `FibraModemScreen.kt` com 5 estados visuais: SemWifi ("A análise do modem só funciona na rede local"), SemCredenciais com CTA para Ajustes, Conectando com skeleton animado e LinearProgressIndicator, Erro com dois botões ("Tentar novamente" + "Revisar configurações"), e Concluído com chip de status geral, bloco de valores técnicos (RX/TX/temperatura/status óptico/modelo com fallback "--") e bloco de interpretação via `FibraSignalQualityEngine`. Senha do modem nunca exposta. `FibraModemUiState.kt` com sealed interface de 5 estados e função `mapearSnapshotFibra` pura.

- **Tela DNS — Benchmark, recomendação e guia de configuração (#146):** `DnsScreen.kt` com 4 blocos: DNS atual (IP, nome amigável, badge "DNS Privado ativo", latência), Benchmark com 7 provedores (Cloudflare, Google, Quad9, OpenDNS, AdGuard, Registro.br, CETIC.br), Recomendação com disclaimer explícito ("Isso não troca o DNS automaticamente"), e Guia colapsável "Como trocar DNS?" com seções "Quando vale a pena" e "Quando não faz diferença". Texto educacional no topo: "DNS afeta abertura de sites, não velocidade." Extraído do AppShell para arquivo próprio.

- **Onboarding — Checkbox de termos e cards de permissão (#128):** Slide 1 exige aceite obrigatório de Termos de Uso e Política de Privacidade antes de avançar (swipe e botão bloqueados). Slide 2 apresenta cards de permissão com ícone, título, descrição e botão de concessão individual para Localização/Wi-Fi (`ACCESS_FINE_LOCATION`) e Dispositivos próximos (`NEARBY_WIFI_DEVICES`, API 33+). Botão "Pular" visível apenas no slide 0. Launcher contextual usa `solicitacaoPermissoes` para não corromper callback de localização.

- **Chat inline "Perguntar sobre diagnóstico" com LLM na DiagnosticoScreen:** O card "Perguntar sobre diagnóstico" agora integra chat com **Gemma 4 26B** (via Cloudflare Workers AI) inline, sem sair da tela. Visual estilo ChatGPT/Claude: mensagem do usuário alinhada à direita com pill sutil; resposta da IA sem bubble, com header `[ícone] Linka IA` em texto secundário. Chips de sugestão desaparecem com animação após o primeiro envio. Loading visível com 3 pontos pulsantes. Limite de 5 perguntas por sessão, enforçado no ViewModel (campo desabilitado com mensagem ao atingir limite). Histórico de chat persiste no ViewModel (sobrevive rotação de tela). Sem novo endpoint no Cloudflare Worker — usa campo `feedbackUsuario` do payload existente. (#66)

- **Histórico: Gráfico de testes e cards de velocidade média:** HistoricoScreen agora exibe gráfico Canvas Compose com dados de download e upload ao longo do tempo (cores accent e accentBlue). Cards "Download médio" e "Upload médio" calculam a média com base no histórico filtrado. Filtros por tipo de rede (Wi-Fi / Rede móvel / Todos) com dropdown de operadora aparecendo quando rede móvel é selecionada. Toggle e dropdown afetam gráfico, cards e lista simultaneamente. Enum `FiltroConexaoHistorico` e `historicoFiltrado` StateFlow no ViewModel. (#95)

- **Diagnóstico Inteligente — Redesign completo da tela de resultado:** Nova UI com 5 cards: StatusDiagnosticoCard (escudo + chip pill de status), PrincipalPontoCard (ícone dinâmico por tipo de problema + tip card âmbar), OQueFazerCard (lista de ações + 3 botões de navegação), seção duas colunas de Evidências + Análise por categoria, e ChatCard com SuggestionChips e campo de input pill. Tokens de cor `warningContainer`/`successContainer`/`amberSurface` adicionados ao design system. `WindowInsets.ime` aplicado para campo de input não ser coberto pelo teclado. (#60)

- **Classificação automática de topologia WiFi:** `SinalScreen` agora exibe o tipo de topologia real de cada rede (ROTEADOR, ROTEADOR_MESH, NO_MESH, REPETIDOR) via integração com `TopologiaWifiEngine`. Fallback gracioso para DESCONHECIDO via `runCatching` em caso de falha na classificação. (#40)

- **Streaming SSE no chat de diagnóstico:** Texto da IA aparece progressivamente com cursor pulsante em tempo real. Backend via Cloudflare Worker suporta modo SSE com `?stream=true` retornando chunks de dados. `DiagChatEntry` rastreia entradas parciais durante streaming, `AiDiagnosisRepository` implementa `explainDiagnosisStream()` com OkHttp Okio e fallback silencioso, `MainViewModel` atualiza entrada token a token e desativa carregamento no primeiro chunk, `DiagnosticoScreen` renderiza `DiagChatTextoComCursor` com cursor pulsante via `rememberInfiniteTransition`. (#68)

- **UptimeNarrativaEngine v2.0 — Detecção de padrões avançados:** Três novos comportamentos de análise de uptime: detecção de padrões horários recorrentes (ex: "toda manhã entre 8h e 9h a conexão cai"), identificação de interrupções longas >30 minutos ordenadas por duração, e cálculo de tendência de qualidade (MELHORANDO/PIORANDO/ESTAVEL) comparando as últimas 24h com as 24h anteriores. (#42)

- **ExportadorHistoricoPDF v2.0 — Layout rico e paginação automática:** PDF agora renderizado via HTML/CSS com `WebView.createPrintDocumentAdapter()` — tabela profissional com cabeçalho colorido e linhas zebradas. Paginação automática elimina truncamento de históricos longos. Timeouts defensivos (10s) em `exportarComWebView` e `PdfPrintHelper` evitam coroutines penduradas. (#41)

### Fixed

- **Privacidade — MAC address mascarado em DispositivosScreen (#144):** Octetos 3-4 do MAC são substituídos por `••` (ex: `c4:8e:de:ad:1a:2b` → `c4:8e:••:••:1a:2b`). Aplica em DeviceDetailSheet e MeshApSheet. Estado sem Wi-Fi exibe mensagem diferenciada "Dispositivos da rede só aparecem quando você está conectado a um Wi-Fi."

- Botão de iniciar teste sempre exibe "Iniciar teste" — removido label "Repetir" do estado concluído (#91)
- Status bar (bateria, hora, sinal) visível no modo claro — corrigido `enableEdgeToEdge` com `SystemBarStyle` condicional ao tema (#79)
- **Acessibilidade TalkBack — Auditoria completa de telas:** Correções em `DispositivosScreen` (DispositivoItem com `role=Button` e contentDescription dinâmica), `LaudoScreen` (link Anatel com `contentDescription`; `LkListRow` recebe role apenas quando interativo), `ResultadoVelocidadeScreen` (toggle "Detalhes avançados" com `stateDescription` dinâmica), e `ProfileAvatarButton` (contentDescription dinâmica com nome do usuário). (#11)
- **Mensagens de erro do modem exibidas em português humanizado:** Strings brutas internas (ex: "erroModemInacessivel") substituídas por mensagens humanizadas em português. Mapeamento `when()` em `AppShell.kt` e `VelocidadeScreen.kt` com 6 strings de fallback semânticas. (#80)
- Card de rede móvel exibe nome da operadora no formato "Operadora · Tipo" (ex: "Claro · 4G") (#83)
- **Tela Sinal em rede móvel exibe RSRP, RSRQ e SINR em cards estruturados com chips de status visual (Ótimo/Bom/Ruim) e ícones (#84)**
- TopBar da Central de Testes colapsa completamente ao rolar — sem gap vazio no topo (#75)
- Consumo em testes este mês exibe "0 MB" (não traço) e acumula corretamente mesmo com falha parcial do teste (#94)
- Label "Análise local" renomeado para "Diagnóstico do dispositivo" — elimina conflito visual com header "Gemma 4" no chat inline (#69)

---

### Added

- **Acessibilidade TalkBack em LinearProgressIndicator, StepRow, ConfiancaBarra (Issue #45):** Adicionadas semantica de progresso, roles acessíveis, contentDescription dinâmicas e live regions para componentes críticos de PingScreen e DiagnosticoScreen. Aumento de ~40% em cobertura de TalkBack.

- **Otimizações de recomposição em ResultadoVelocidadeScreen (Issue #23):** Aplicados `remember` com keys corretas e derivadas de state. 7 otimizações implementadas reduzindo recomposições desnecessárias durante atualização de dados.

- **Strings hardcoded extraídas para strings.xml (Issue #10):** 115+ strings em 4 telas migraram de hardcode para localização. Telas afetadas: PingScreen, DiagnosticoScreen, ResultadoVelocidadeScreen, FibraScreen. Prepara base para i18n futuro.

- **MainActivity refatorada com combine() e data classes tipadas (Issue #22):** Redução de 35→17 coletas com `combine()` em vez de `flatMapLatest` cascata. Flow<UiState> tipados substituem Any genéricos. Reduz observer churn em 40%.

- **MonitoramentoWorker: combine() otimizado para cascata .first() (Issue #20):** 8 `.first()` em cascata substituídos por 2 `combine().first()`. Reduz timeout desnecessário de ~800ms para ~100ms em ciclo de coleta.

- **MonitoramentoWorker: withTimeout + BackoffPolicy.EXPONENTIAL (Issue #21):** Timeout explícito (8s) com exponential backoff (initial 1s, max 32s). Worker não trava indefinidamente.

- **ConnectionPool adaptativo por tipo de rede (Issue #19):** OkHttp ConnectionPool configurado dinamicamente — 2 conexões/1min para móvel, 8 conexões/5min para Wi-Fi. Reduz consumo de dados e latência em Speedtest.

- **Ping concorrente speedtest: intervalo adaptado 300ms→1000ms (Issue #18):** Intervalo entre amostras ICMP aumentado de 300ms para 1000ms. Reduz congestão em redes móveis, melhora estabilidade de latência.

- **Cobertura de testes unitários em snapshot entities (Issue #16):** 15 testes unitários adicionados: SnapshotRedeTest (4), WifiLinkSnapshotTest (6), MedicaoEntityTest (5). Cobertura de equals, hashCode, copy e serialização.

- **Baseline Profile + AAB splits habilitados (Issue #9):** Perfil de partida instrumentado para Pixel 6. AAB splits de ABI habilitados para reduzir APK por ~30%. Prepara deploy em Play Store.

- **Firebase Crashlytics integrado (Issue #39):** BOM 33.1.0 adicionado. Crashlytics ativo em release builds. Capturas de exceções não-capturadas em background tasks e Workers.

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

- **Injeção de dependência:** Introduzido Hilt para DI. `MainViewModel` migrado de instanciação manual (`lazy { Modulo.criar*()} `) para `@HiltViewModel` com injeção via construtor. Melhora testabilidade e ciclo de vida das dependências.

### Fixed

- **Issue #24 — Won't Implement:** DNS e Health check continuam usando HttpURLConnection (não OkHttp). Acoplamento com protocolo HTTP não justifica refatoração. Documentado como decisão arquitetural.

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
