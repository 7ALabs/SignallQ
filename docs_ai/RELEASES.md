# Histórico de Releases — SignallQ Android

**Mantido por:** Rhodolfo
**Última atualização:** 2026-07-18
**Referência:** `android/CHANGELOG.md` (fonte autoritativa do histórico Android) + `git log --oneline`

> Nota de marca: o namespace/applicationId atual é **`io.signallq.app`** (renomeado de
> `io.veloo.app` em 2026-06-28; o caminho físico do código do `:app` continua
> `io/veloo/app/kotlin/`). Demais identificadores técnicos permanecem por compatibilidade
> de infra — repo `gmmattey/linka-android`, worker `linka-ai-diagnosis-worker`.

---

## v0.27.0 (versionCode 63) — 2026-07-18

**Correções pontuais e padronização de UI sobre o redesign da 0.26.0**

- TopBar padronizada: ícone por raiz de aba, 100% estática (sem reação a scroll), botão Fechar de Ajustes movido para `navigationIcon` (#1100)
- Container branco+borda para logo de operadora e arte de jogos (#1106); artwork não oficial de jogos desativado (#1097)
- Tela Sinal: texto quebrando linha na aba Móvel corrigido (#1089), indicador "Ao vivo" removido e aviso de canal consolidado (#1088)
- Histórico: medições do Monitoramento passivo excluídas da lista (#1096), scroll vertical no detalhe do teste (#1095)
- Trava em "Sessão expirada" na tela Equipamento de internet corrigida (#1090); z-order de overlays empilhados e CTA de credenciais corrigidos
- `GatewayConnectionSheet` órfã removida de Ajustes (#1099); pills DNS/Ping/Diagnóstico removidos do card Wi-Fi da Home (#1086)
- Onboarding: comunicação visual de permissão já concedida no toggle

**Publicação:** trilha internal do Play Console, via `release.yml`

---

## v0.26.0 (versionCode 61) — 2026-07-17

**Redesign Material 3 To-Be, motor de topologia unificado e monetização nativa**

- Redesign completo de UI em 8 fases (MD3 To-Be): nova navegação (Ferramentas na tab bar, Perfil via avatar), Onboarding, Início, Velocidade, Sinal, Ferramentas, Equipamento de internet, Monitoramento, Jogos, ícones Material Symbols (#939–#949, #1014)
- Motor de topologia de rede unificado substitui 3 classificadores concorrentes (Home, Sinal, Recommendation Engine) — contratos canônicos, catálogo OUI único, correlação LAN+Wi-Fi (#975–#994)
- Monetização nativa (AdMob) em 4 telas, com consentimento UMP e fallback seguro (#555)
- Diagnóstico remoto via `signallq-diagnostic-worker` + diretório de operadoras (#962–#973)
- 6 bugs de layout achados e corrigidos na rodada de QA de 2026-07-17, verificados ao vivo (#1076–#1081)
- Dezenas de fixes de qualidade, acessibilidade WCAG e consistência visual — detalhe completo em `android/CHANGELOG.md`

**Publicação:** trilha alpha do Play Console (fechada), via `gradle-play-publisher`

---

## v0.25.0 (versionCode 60) — 2026-07-10

**Recommendation Engine, avaliação nativa e equipamento local (Nokia GPON)**

- Recommendation Engine desacoplado integrado à experiência pós-diagnóstico (#807/#811–#813/#821)
- Avaliação nativa do Google Play sem atrito (#664/#806)
- Seção "Equipamento local" completa para Nokia GPON: WAN, LAN, Wi-Fi, laser da ONT (#794–#846)
- Lista de dispositivos com selo "Nome confirmado pelo roteador" via dado real (#839/#844/#856/#869)
- Publicado na trilha alpha do Play Console em 10/07/2026

---

## v0.23.0 (versionCode 56) — 2026-07-05

**Logos de operadoras, canais oficiais e instrumentação de analytics**

- Logos oficiais reais de operadoras e catálogo local de badges (SIG-292) (#472, #467)
- Exibição dos canais oficiais da operadora identificada no diagnóstico (#466)
- Instrumentação de analytics: funil principal com 7 eventos via `AnalyticsHelper` (#473) e `feature_used` em Wi-Fi, DNS, Fibra e Histórico (#469)
- Ingest de speedtest via Wi-Fi agora inclui a operadora/ISP identificada (#468)
- Correção de quebras de layout com fonte grande do sistema (#470)
- Matcher de operadora não confunde mais "Oi" com "Nio" (#465)
- Sinal › Dispositivos: fallback de fabricante por OUI do MAC quando o hostname não resolve (#394)
- Upgrade coordenado de toolchain: AGP 9.2.1, Kotlin 2.3.21, Gradle 9.4.1 (#445)

---

## v0.22.1 (versionCode 54) — 2026-07-03

**Primeira publicação na Play Console + correções de topologia**

- Primeira publicação automatizada na Play Console (trilha de teste fechado) via gradle-play-publisher no release por tag
- Roteador dual-band único não é mais classificado como mesh; aparece como "Roteador dual-band" com bandas identificadas (#356)
- Chip "Conectado" em Sinal › Wi-Fi não quebra mais com SSID longo; trunca com reticências (#355)
- Aba "Dispositivos" na barra de abas da tela Sinal não quebra mais em duas linhas (#354)

---

## v0.22.0 (versionCode 53) — 2026-06-29

**Ícone SignallQ, "Fale conosco" e otimizações**

- Item "Fale conosco" na tela de Ajustes
- Ícone do app atualizado para o símbolo SignallQ (barras de sinal roxo→azul)
- Acessibilidade TalkBack e compatibilidade com dark theme
- Baseline Profile atualizado para `io.signallq.app`
- Melhorias de desempenho: startup, consumo de bateria do worker de monitoramento e tamanho do APK

---

## v0.21.0 (versionCode 52) — 2026-06-22

**CI, ícone do app e correções do Admin Panel**

- Novo ícone do app: assets em todas as densidades + ícone adaptativo (SIG-7/SIG-8)
- Play Store asset: `play_store_512.png` adicionado ao repositório
- `signallq-privacy-worker`: novo worker Cloudflare para conformidade de privacidade
- CI — Code Quality: workflow `.github/workflows/quality.yml` com Ktlint, Detekt, testes e build APK em cada PR (SIG-28/SIG-37)
- README.md com stack, arquitetura e instruções de setup
- Admin Panel: erro 401 Overview agora exibe mensagem clara (SIG-5)
- Admin Panel: dados mockados em produção eliminados com `VITE_ENABLE_MOCKS=false` (SIG-9)
- Ktlint: formatação corrigida em ~30 arquivos do módulo `app`

---

## v0.16.0 (build 46) — 2026-06-21

**Rebranding completo para SignallQ + reorganização de documentação**

- Rebranding da identidade anterior (Linka / Veloo / Orbit) para **SignallQ** em toda a UI, copy, telas de novidades e documentação
- Superfícies de IA (antigo "Orbit") agora referenciadas como assistente SignallQ
- Reorganização da documentação: criados `docs/_archive/` e `docs_ai/_archive/` para material histórico (releases v0.9.0, relatórios, assets da marca anterior)
- `docs_ai/README.md` reescrito com índice hierárquico alinhado à árvore atual
- Identificadores técnicos preservados: package `io.veloo.app`, App ID Firebase `io.veloo.app`, repo GitHub `gmmattey/linka-android`, worker Cloudflare `linka-ai-diagnosis-worker`

---

## v0.15.1 (build 45)

**Correção mesh — nó "Roteador"**

- Trilha de rede / topologia: o nó "Roteador" em redes mesh deixava de exibir o placeholder "—" quando o dado não estava disponível; corrigido para manter o fallback "—" consistente com os demais nós

---

## v0.15.0 (build 44) — 2026-05-30

**Rebranding: Linka → Veloo**

- Identidade visual, package name e configurações Firebase atualizados
- App ID Firebase: `io.veloo.app`
- Tela de novidades v0.15.0

---

## v0.14.4 (build 43)

**Diagnóstico IA — polimento e estabilidade**

- Timeout visual com mensagem "Conectando…" durante chamada à IA
- UI de retry quando timeout é atingido
- Cleanup de lógica `setTimeout` em `DiagnosticoScreen`
- `LLMChatScreen`: TopBar Material 3 com Scaffold e insets corretos
- `LLMChatScreen`: respeita barra de status e insets do sistema
- Seção "Thinking" renderizada como expandível com animação

---

## v0.14.2 (build 41)

**ResultadoVelocidade — ações IA e operadora**

- Botão IA na tela de resultado
- Sheet de contato com operadora
- "Refazer teste" diretamente do resultado

---

## v0.14.0 (build 39)

**Redesign Diagnóstico IA — laudo + LLM**

- `DiagnosticoScreen` redesenhada: fluxo de laudo gerado por IA + assistente LLM (`LLMChatScreen`)
- Footer com 3 ações: "Tirar dúvidas", "Refazer teste", "Falar com a operadora"
- Operadoras com logo no chat
- Follow-up reutiliza contexto da conversa anterior
- Worker Cloudflare retorna texto puro (não JSON)
- Thinking tokens visíveis na UI
- Correção: crash `MetricsGrid`, topbar insets, model name

---

## v0.13.3 (build 38)

**Correções multi-plataforma**

- Fibra: auto-open corrigido
- IP público herdado Wi-Fi→Móvel corrigido
- Detecção 4G vs 5G NSA melhorada

---

## v0.13.2 (build 37)

**Sinal e rede móvel — correções**

- SSID em Android 12+ corrigido
- Home: card ISP/SIM corrigido
- Sinal Móvel redesenhado
- SpeedTest: cleanup
- Filtro de banda corrigido

---

## v0.13.1 (build 36)

**ISP info — HTTPS**

- Substituição de `ip-api.com` por `ipapi.co` (HTTPS) para resolver info ISP
- Correção de crash em redes sem IPv4 público

---

## v0.13.0 (build 35)

**Redesign UI mockup v2 — fase 1**

- Home: cards Wi-Fi e Móvel empilhados
- Sheet de rede móvel redesenhada
- SpeedTest: alinhamento ao mockup v2
- Fibra, Laudo, Privacidade, Novidades: alinhamento ao mockup v2
- SinalScreen: aba Móvel redesenhada, empty states humanizados
- SinalScreen: abas Wi-Fi e Canal alinhadas ao mockup v2
- 5G NSA via DisplayInfo + trail "Conectando"
- Filtro histórico corrigido
- TopBar contextual com SSID/operadora
- Sinal card: "Sua Conexão" destacado + redesign item de rede
- Mini-cards na Home + seletor Android/Roteador
- Chip de segurança Wi-Fi + card rede móvel dual SIM
- SpeedTest: remoção de mini-cards DNS/PING/DIAGNÓSTICO
- SpeedTest: migração de cards ("Jogar Online", "Atraso extra", "REGULAÇÃO ANATEL") para aba Velocidade
- Home: remoção do card "O QUE VOCÊ CONSEGUE FAZER"
- Robolectric + smoke tests Compose para `ChatDiagnosticoIaScreen`
- `SpeedtestPersistenceCoordinator`: race condition e `operadoraMovel null` corrigidos

---

## v0.12.0

**Chat IA — sessões persistidas e streaming**

- `ChatDiagnosticoIaScreen` com drawer, chips iniciais, banner cota e substituição do entry point anterior
- `ChatDiagnosticoIaViewModel` com 3 fluxos: diagnóstico completo, ping-only, contexto de rede sem speedtest
- Streaming e cota diária rolling 24h
- Repositories: `ChatRepository`, `CotaDiariaRepository`
- Room v10: tabelas `chat_sessions` + `chat_messages` + migration
- Deprecation do fluxo anterior, placeholder customizável, Locale moderno
- Entrada: botão "Assistente SignallQ" (ex-"Diagnóstico IA")

---

## v0.11.4 (build 30)

**Estabilidade**

- Debounce 2000ms em `onLost` para evitar flash falso de "desconectado"
- Histórico: correção double-filter MOVEL e freeze ao navegar sem resultados
- 5G NSA via `SignalStrength` em OEMs sem `nrState`

---

## v0.11.x

**Features avançadas — Fibra, DNS, Dispositivos, Onboarding**

- `FibraScreen` avançada: tela de análise completa do modem/ONT Nokia GPON
- `DnsScreen`: benchmark completo com recomendação e guia de configuração
- Dispositivos: mascarar MAC e mensagem de estado sem Wi-Fi
- Onboarding: checkbox de termos e cards de permissão por feature
- Estados vazios humanizados (telas 9.3 a 9.6)
- Estado offline: linguagem humanizada
- "Diagnóstico IA" renomeado para "Assistente SignallQ"

---

## v0.9.0

**Ping/Latência, DNS provedores BR, ExploreToolsRow**

- `PingScreen` (ModalBottomSheet): 20 amostras ICMP sobre HTTP/2, progresso real-time, 3 métricas (latência mediana, jitter, perda)
- `PingExecutor`: warmup, filtro de outliers, Dispatchers.IO
- `ExploreToolsRow`: substituição da `ExploreToolsSheet` por grid 2×N sempre visível na SpeedTestScreen
- DNS Benchmark: adição de Registro.br e CETIC.br — total de 7 provedores
- `StatusCard`: loading state "Cloudflare · Carregando…" com cor dinâmica
- Diagnóstico no grid com badge "Em breve" e 50% opacidade (flag inativa)
- Separador `·` (ponto médio) no hostname DNS

---

## v0.8.1 (baseline desta documentação)

**Thresholds Wi-Fi por banda, DNS-03, FibraScreen, acessibilidade**

- `WifiSignalQualityEngine`: thresholds RSSI distintos para 2.4GHz e 5GHz
- DNS-03: novo diagnóstico `info` para latência 51ms–150ms
- `FibraScreen`: loading dinâmico com modelo do modem, hint RX corrigido (−8 a −27 dBm)
- `DiagnosticoScreen`: tipo de conexão real enviado à IA (bug `wifi` fixo corrigido)
- `ResultadoVelocidadeScreen`: título "Resultado do teste", MetricCards de latência/jitter com cores dinâmicas por threshold
- Semântica TalkBack: `ModeSelector` e `PathConnector`
