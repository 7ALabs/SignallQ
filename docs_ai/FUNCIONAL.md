# Documentação Funcional — SignallQ Android

- **Status:** ativo
- **Última validação:** 2026-07-16
- **Fonte de verdade:** este arquivo — substitui `docs_ai/ANDROID_FUNCIONAL.md`,
  `docs_ai/functional/AI_ASSISTANT.md`, `docs_ai/functional/CENTRAL_DE_TESTES_USER_GUIDE.md`,
  `docs_ai/functional/DIAGNOSTIC_FLOW.md`, `docs_ai/functional/DNS_FLOW.md`,
  `docs_ai/functional/FEATURES.md`, `docs_ai/functional/SCREENS_ANDROID.md`,
  `docs_ai/functional/SETTINGS.md`, `docs_ai/functional/SPEEDTEST_FLOW.md`,
  `docs_ai/functional/WIFI_FEATURES.md` (originais movidos para `docs_ai/_archive/` com nota de
  substituição). `docs_ai/functional/FEATURE_FLAGS.md` e
  `docs_ai/functional/JOGOS_TESTE_CONEXAO_SPEC.md` continuam vivos à parte (o primeiro cobre
  também a metade técnica/painel Admin fora do escopo deste documento; o segundo já está
  atualizado e serve como spec de referência do domínio Jogos).
- **Escopo:** app Android SignallQ (`7ALabs/SignallQ`, diretório `android/`) — o que o
  usuário final vê e faz. Não cobre arquitetura interna, engines internos ou contratos técnicos
  (ver `docs_ai/TECNICO.md`).
- **Responsável:** Claudete (dono do processo de documentação funcional), mantido por Camilo
  (implementação) e Rhodolfo (QA/doc)
- **Versão do app na validação:** 0.25.0 (versionCode 60) — confirmado em
  `android/gradle/libs.versions.toml`. O `.claude/CLAUDE.md` do projeto ainda cita 0.23.0/56;
  desatualizado, sinalizado à parte, não corrigido por este documento (fora do escopo de edição).

> Este documento responde: "o que o app Android faz, tela por tela, da perspectiva do usuário?"
> Fonte primária: código real em `android/app/src/main/kotlin/io/veloo/app/kotlin/` (caminho
> físico legado — o package/namespace declarado é `io.signallq.app`, ver dívida conhecida em
> `.claude/rules/higiene-e-padronizacao-repositorio.md` seção 4.1).

---

## 1. Objetivo do produto

O SignallQ é um app Android nativo de diagnóstico de internet doméstica: mede velocidade
(download, upload, latência, jitter, perda, bufferbloat), analisa Wi-Fi e rede móvel, lê dados do
modem/ONT de fibra, testa DNS, e oferece um diagnóstico assistido por IA de causa provável a partir
do resultado do teste — tudo com linguagem não técnica e veredito humano (Excelente/Bom/Regular/
Fraco), para que qualquer usuário entenda se e por que sua internet está com problema, sem precisar
interpretar números crus.

---

## 2. Navegação atual

Fonte: `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/AppShell.kt`.

**5 abas fixas na `NavigationBar`** (`tabScreenNames = listOf("home", "speedtest", "sinal_wifi", "historico", "ferramentas")`):

```
[0] Início      → HomeScreen
[1] Velocidade  → SpeedTestScreen   (aba inicial no cold start — não é Início)
[2] Sinal       → SinalScreen
[3] Histórico   → HistoricoScreen
[4] Ferramentas → FerramentasScreen
```

> **Mudança relevante (GH#930, fase 1 do plano MD3 To-Be):** "Ajustes" deixou de ser aba. A 5ª aba
> agora é **Ferramentas** — um hub de atalhos. Configurações (perfil, provedor, tema, dados
> móveis, dados locais) viraram um overlay chamado **Perfil**, acessado pelo avatar no canto
> superior esquerdo de qualquer uma das 5 telas-aba, não mais por uma aba própria.
>
> **Cold start:** o app abre direto na aba **Velocidade** (índice 1), nunca em Início — decisão de
> produto registrada em GH#376/#381, substitui o comportamento anterior (abria em Início).

**Overlays empilhados** (pilha `overlayStack`, não são abas):

| Overlay | Tela | Aberto a partir de |
|---|---|---|
| `ResultadoVelocidade` | `ResultadoVelocidadeScreen` | Automático ao concluir um teste de velocidade |
| `Laudo` | `LaudoScreen` | Ferramentas, Perfil/Ajustes, ou botão no resultado |
| `Ping` | `PingScreen` | Ferramentas |
| `Dns` | `DnsScreen` | Ferramentas, SpeedTestScreen |
| `Dispositivos` | `DispositivosScreen` | Ferramentas |
| `Fibra` / `EquipamentoInternet` | `EquipamentoInternetScreen` | Ferramentas, nó do gateway na Início |
| `Jogos` | `JogosScreen` | Ferramentas |
| `Perfil` | `AjustesScreen` (reorganizada em 6 seções) | Avatar no TopBar de qualquer aba |
| `Privacidade` | `PrivacidadeScreen` | Dentro do overlay Perfil |
| `Novidades` | `NovidadesScreen` | Dentro do overlay Perfil |

Sheets modais adicionais (não empilhados no `overlayStack`, controlados por flag local):
`PerfilEditSheet`, `DadosLocaisSheet`, `MonitoramentoSheet`, `GatewayConnectionSheet`,
`AnaliseDetalhadaBottomSheet` (diagnóstico IA por problema relatado).

**Onboarding:** `OnboardingScreen` exibida apenas na primeira execução (`onboarding_concluido =
false` no DataStore); inclui overlay interno de `TermosDeUsoScreen`. Nunca reaparece após concluído.

### 2.1 Divergência corrigida nesta consolidação

A documentação anterior (`ANDROID_FUNCIONAL.md`, `SCREENS_ANDROID.md`, `.claude/CLAUDE.md`)
descrevia as 5 abas como Início/Velocidade/Sinal/Histórico/**Ajustes**, e ainda citava
`DiagnosticoScreen`, `SignallQScreen` e `LLMChatScreen` como telas ativas. Nenhuma das três existe
mais no código (busca real por `*Diagnostico*Screen*`/`*SignallQScreen*`/`*LLMChat*` não retorna
resultado). A 5ª aba real é **Ferramentas**; Ajustes é overlay "Perfil". Ver seção 6 para onde o
diagnóstico por IA foi parar.

---

## 3. Telas e bottom sheets

Inventário real (`android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/`):

| Tela | Tipo | Observação |
|---|---|---|
| `HomeScreen` | Aba 0 | — |
| `SpeedTestScreen` | Aba 1 | aba inicial no cold start |
| `SinalScreen` | Aba 2 | — |
| `HistoricoScreen` | Aba 3 | — |
| `FerramentasScreen` | Aba 4 | hub de atalhos, substitui a antiga aba Ajustes |
| `VelocidadeScreen` | Overlay de execução | full-screen durante o teste |
| `ResultadoVelocidadeScreen` | Overlay | contém o diagnóstico IA inline (`AnalisadorEntryRow`) |
| `AjustesScreen` | Overlay "Perfil" | reorganizada em 6 seções (perfil, provedor, monitoramento, modem, tema, dados) |
| `DispositivosScreen` | Overlay | scanner de rede local |
| `EquipamentoInternetScreen` | Overlay | leitura do modem/ONT (fibra) |
| `LaudoScreen` | Overlay | laudo técnico completo |
| `PingScreen` | Overlay | teste isolado de latência |
| `DnsScreen` | Overlay | benchmark DNS |
| `JogosScreen` | Overlay | teste de conexão para jogos |
| `PrivacidadeScreen` | Overlay (dentro de Perfil) | política/LGPD |
| `NovidadesScreen` | Overlay (dentro de Perfil) | changelog |
| `OnboardingScreen` | Especial | primeira execução |
| `TermosDeUsoScreen` | Overlay interno do Onboarding | — |
| `MinhaConexaoScreen` | **Órfã** | existe no diretório mas não é referenciada em nenhum `AppShell.kt`/tela ativa. Candidata a código morto — não removida aqui por não ser o escopo desta tarefa; confirmar antes de apagar (regra de higiene do repositório, seção 11). |

**Telas removidas em versões anteriores e não mais existentes** (confirmado por busca no código —
não confundir com documentação antiga que ainda as cita): `DiagnosticoScreen`, `SignallQScreen`
(ex-`OrbitScreen`), `LLMChatScreen`, `ChatDiagnosticoIaScreen`, `SignallQPulseScreen` (ex-
`LinkaPulseScreen`). O diagnóstico assistido por IA e o "chat" foram substituídos por um fluxo sem
conversa contínua — ver seção 4.2.

---

## 4. Funcionalidades por domínio

### 4.1 Velocidade (Speedtest)

**Telas:** `SpeedTestScreen` (aba 1, pré-execução) → `VelocidadeScreen` (execução, overlay
full-screen) → `ResultadoVelocidadeScreen` (overlay).

- **3 modos:** rápido / completo / triplo, selecionados por `ModeSelector` (pills). Modo triplo
  expõe `CardRodadasTriplo` com as 3 rodadas individuais.
- **Fases medidas em ordem:** LATÊNCIA (ping) → DOWN (download Mbps ao vivo) → UP (upload Mbps ao
  vivo) → CONCLUÍDO. `GaugeCircular` central mostra progresso + fase + valor ao vivo;
  `MiniGrafico` plota os pontos ao vivo; `PillsFase` marca cada fase concluída; haptics disparam na
  transição entre fases.
- **Dados móveis:** ao iniciar teste em rede móvel, `ForaDoWifiDialog` pede confirmação com
  estimativa de consumo antes de prosseguir (gate único — GH#516 evita pedir confirmação duas
  vezes).
- **Cancelamento:** `BackHandler` intercepta back durante execução com diálogo "Cancelar o teste?"
  ("Continuar testando" / "Cancelar teste").
- **Resultado (`ResultadoVelocidadeScreen`):**
  - Grade A/B/C/D/? com cor por severidade
  - Cards: download, upload, latência, jitter, perda de pacotes, bufferbloat (severidade
    none/mild/moderate/severe)
  - Chip de contaminação de teste, quando detectado
  - Seção "Experiência de uso": vereditos Streaming / Gaming / Vídeo Chamada
    (good/acceptable/poor)
  - Comparação ANATEL RQUAL: mínimo 40% e normal 80% do plano contratado (Ato 7869/2022 — abaixo
    de 40% mostra aviso de direito a rescisão sem multa)
  - DNS Info (provedor + latência), Detalhes Avançados expansíveis (pico DL/UL, latência com
    carga, estabilidade)
  - **`AnalisadorEntryRow`** — entrada do diagnóstico IA por problema relatado (ver seção 4.2)
  - **`RecommendationCard`** — recomendação gerada pelo `coreRecommendation` (engine de
    recomendação por tags de diagnóstico), quando há uma decisão disponível
  - **`OperadoraContactCard`** — quando o diagnóstico aponta causa no ISP, mostra botões de SAC/
    WhatsApp da operadora identificada (catálogo local + resolução remota); fallback Anatel (1331)
  - Botão de compartilhamento no TopBar: gera bitmap 1080×600 com as métricas principais e a cor
    de fundo variando por severidade
  - Botões: "Testar Upload Novamente", "Ir para o início", "Testar novamente"
- **Persistência:** todo resultado salvo automaticamente em Room (`MedicaoEntity`, tabela
  `medicao`) — inclui os vereditos de uso, flag de contaminação, tipo de conexão e origem
  (`fonte = "android"`).

**Severidade de bufferbloat:** none < 5ms · mild 5–30ms · moderate 30–100ms · severe > 100ms.

### 4.2 Diagnóstico assistido por IA

Não existe mais uma tela de chat dedicada. O diagnóstico por IA acontece **inline dentro de
`ResultadoVelocidadeScreen`**, via `AnalisadorEntryRow` (GH#931 fase 2 MD3):

**Fluxo duplo:**
1. **Laudo automático** ("Diagnóstico geral da sua conexão") — diagnóstico Motor remoto (`signallq-diagnostic-worker`, timeout 42s, GH#962) chamado automaticamente com o resultado do speedtest, sem ação do usuário. Fallback automático para motor local em caso de timeout/erro.
2. **Análise por problema específico** ("Análise do seu problema") — usuário descreve em texto livre em `AnaliseDetalhadaBottomSheet`, IA Gemini/Qwen analisa aquele sintoma específico com timeout remoto 42s (GH#969).

**Estados de cada análise:**
- **Inativo:** card "Analisar meu problema com IA" — convite a descrever.
- **Analisando:** spinner + "Analisando seu problema…".
- **Resultado:** "Diagnóstico" ou "Análise do seu problema" + ações recomendadas (`AiAcaoRecomendada`).
- **Erro:** "Falha — toque para tentar novamente".

É uma interação de turno único por análise (usuário descreve → IA responde), não uma conversa
contínua com histórico multi-turno como nas versões anteriores do app.

Paralelamente, o `RecommendationCard` (motor `coreRecommendation`) mostra uma recomendação
determinística derivada das tags do diagnóstico local, independente da análise por IA — as duas
coexistem no resultado.

> **Nota de precedência sobre `.claude/CLAUDE.md`:** o CLAUDE.md do projeto descreve o
> `coreRecommendation` como "ainda não integrado a nenhuma feature/UI" — desatualizado. O código
> real (`ResultadoVelocidadeScreen.kt`, import `io.signallq.app.core.recommendation.RecommendationDecision`)
> mostra que já está integrado ao resultado do speedtest. Reportar essa correção separadamente ao
> dono do CLAUDE.md (fora do escopo de edição deste documento).

### 4.3 Wi-Fi

**Tela:** `SinalScreen` (aba 2), modo Wi-Fi.

- `TabRow` com 4 filtros de banda: Todas / 2.4GHz / 5GHz / 6GHz
- `RedeCard` por rede vizinha: SSID (ou "Rede oculta"), BSSID, RSSI (dBm), canal, frequência (MHz),
  segurança, OUI (fabricante), largura de canal
- `WifiChannelGuide`: visualização de congestionamento de canais
- Bottom sheet de análise de topologia (roteador / mesh / repetidor) com recomendações
- **Classificação de sinal por banda** (5GHz é mais exigente que 2.4GHz, pois atenua mais):

  | Classificação | 5GHz (dBm) | 2.4GHz (dBm) |
  |---|---|---|
  | Excelente | ≥ −55 | ≥ −50 |
  | Bom | ≥ −65 | ≥ −60 |
  | Regular | ≥ −75 | ≥ −70 |
  | Fraco | < −75 | < −70 |

- **Permissão de localização** (`ACCESS_FINE_LOCATION`, obrigatória para `WifiInfo`/`ScanResult` a
  partir da API 29): solicitada contextualmente ao entrar na aba Sinal em Wi-Fi sem a permissão —
  ver seção 5. Sem ela o scan continua rodando, mas com dados degradados.
- **Rate limiting de scan (API 28+):** Android limita `startScan()` a 4 chamadas/2min em
  foreground; em throttling, o scanner usa o último `scanResults` em cache como fallback.
- **6GHz:** detectado quando `frequenciaMhz >= 5925`; exige hardware Wi-Fi 6E, ausente na maioria
  dos aparelhos mid-range — nesse caso simplesmente não aparecem redes nessa banda, sem erro.

**Modo Móvel (4G/5G):** operadora, tecnologia (4G LTE/5G NR), RSRP com classificação de qualidade,
RSRQ, SINR — exige `READ_PHONE_STATE` (seção 5). IP local sempre exibido.

**Modo Cabo (Ethernet):** estado informativo com IP local, sem scan (não aplicável).

### 4.4 Dispositivos

**Tela:** `DispositivosScreen` (overlay via Ferramentas).

- Descoberta de dispositivos na rede local (ARP + mDNS)
- Classificação por OUI (fabricante) e serviços mDNS
- Apelidos customizáveis por MAC, persistidos em Room (`ApelidoDispositivoEntity`, tabela
  `apelido_dispositivo`)
- `OfflineBanner` no topo quando sem conectividade
- Estados: Loading / Lista / Vazio / Offline / Erro

### 4.5 DNS

**Tela:** `DnsScreen` (overlay via Ferramentas ou `SpeedTestScreen`). Módulo `:featureDns`, feature
real confirmada em `android/feature/dns/src/main/kotlin/io/veloo/app/kotlin/feature/dns/`:
`BenchmarkDns.kt`, `BenchmarkDnsDoh.kt`, `AvaliadorCoerenciaDns.kt`, `OrientadorConfiguracaoDns.kt`,
`SnapshotBenchmarkDns.kt`.

- É um **benchmark comparativo via DoH** (DNS over HTTPS) contra uma lista fixa de servidores
  públicos — não é uma tela de configuração de DNS do sistema Android.
- Servidores testados: Cloudflare, Google, Quad9, OpenDNS, AdGuard, e dois nacionais (Registro.br,
  CETIC.br).
- Resultado: ranking por latência com grades A (≤15ms) / B (≤30ms) / C (≤50ms) / D (>50ms); badges
  de "atual" e "recomendado".
- Ao final, exibe orientação (não ação automática) de como configurar DNS privado no Android e no
  roteador — o app não altera DNS do sistema.
- `OrientadorConfiguracaoDns` sugere o melhor servidor detectado; `AvaliadorCoerenciaDns` avalia
  coerência do DNS atual.

> **Correção sobre o doc anterior (`DNS_FLOW.md`):** o documento anterior descrevia (com nota
> própria de "inferential path, precisa validação humana") um fluxo de troca de DNS do sistema via
> possível VPN service, arquivos e módulos inventados (`DnsViewModel.kt`, `AppSettingsDataSource.kt`
> em `coreDatastore`) que não existem no código real. Conteúdo integralmente reescrito acima a
> partir do módulo real `:featureDns` e da tela `DnsScreen.kt`.

### 4.6 Equipamento de internet / Fibra (GPON)

**Tela:** `EquipamentoInternetScreen` (overlay `Fibra`/`EquipamentoInternet`) — substitui o antigo
`FibraModemScreen` (Nokia-only) por uma composição por capacidade do equipamento (GH#934, fase 5
do plano MD3 To-Be). Nokia GPON continua sendo o único provider real implementado hoje.

- Status GPON: up/down, potência Rx/Tx (dBm — faixa ideal −8 a −27 dBm, ITU-T G.984), temperatura,
  corrente do laser, voltagem, número serial, modo de operação
- Status WAN: IP, máscara, gateway; status PPP quando aplicável
- Alerta de Double NAT/CGNAT (`NatStatus`, reaproveitado do diagnóstico de topologia)
- Detecção automática do gateway IP; formulário de conexão (host, usuário, senha); toggle
  "manter conectado" com sessão vinculada ao BSSID atual (evita reautenticar toda vez que o
  usuário reentra na mesma rede)
- Acessível também pelo "nó do gateway" na tela Início (mesmo destino, dois entry points)

### 4.7 Histórico

**Tela:** `HistoricoScreen` (aba 3).

- Gráfico de histórico (uptime), narrativa textual gerada por engine
- Filtros por tipo de conexão e por operadora
- Resumo de medições (médias)

### 4.8 Ajustes / Perfil

**Tela:** `AjustesScreen`, acessada como overlay "Perfil" pelo avatar no TopBar (não é mais aba).
Reorganizada (GH#936, fase 7 MD3) em seções por responsabilidade, recebidas via state objects
dedicados no código (`AjustesPerfilState`, `AjustesProvedorState`, `AjustesMonitoramentoState`,
`AjustesModemState`, `AjustesDadosMoveisState`):

| Seção | Conteúdo |
|---|---|
| Perfil | nome, foto (`PerfilEditSheet`), nome do dispositivo, versão do app |
| Provedor | operadora (detecção automática + confirmação via banner), plano contratado (Mbps),
  estado/cidade (IBGE), velocidade contratada down/up |
| Monitoramento | agora um sheet único (`MonitoramentoSheet`) — ativo/inativo, análise avançada,
  4 notificações individuais (latência, DNS, RSSI, sem internet). Acessado tanto daqui quanto do
  atalho "Monitoramento" no hub Ferramentas — mesma fonte, sem duplicar toggles |
| Modem | host, usuário, senha, "permanecer conectado", link para o detalhe do equipamento |
| Tema | Sistema / Claro / Escuro |
| Dados | limite de alerta de velocidade (Mbps), limpar histórico, apagar dados locais, resetar
  app (`DadosLocaisSheet`) |
| Dados móveis | permitir teste completo em rede móvel (heavy), consumo estimado do mês |
| Links | Privacidade (`PrivacidadeScreen`), Novidades (`NovidadesScreen`), atalho para Histórico e
  Laudo |

> **Correção sobre o doc anterior (`SETTINGS.md`):** o documento anterior era inteiramente
> especulativo ("*inferential path*" em quase toda linha, módulos/arquivos inventados como
> `SettingsScreen.kt`, `SettingsViewModel.kt`, `NotificationPreferencesDataSource.kt`). O conteúdo
> acima vem da leitura direta de `AjustesScreen.kt` e de `AppShell.kt` (bloco `Overlay.Perfil`).

### 4.9 Ferramentas (hub)

**Tela:** `FerramentasScreen` (aba 4, substitui a antiga aba Ajustes). Lista de atalhos, cada um
navega para um overlay:

| Item | Descrição exibida | Destino |
|---|---|---|
| Dispositivos | "Quem está na sua rede" | `DispositivosScreen` |
| Equipamento de internet | "Status do modem/ONT da operadora" | `EquipamentoInternetScreen` |
| Ping | "Teste de latência para um endereço" | `PingScreen` |
| DNS | "Compare servidores e troque o seu" | `DnsScreen` |
| Laudo | "Laudo técnico completo da sua conexão" | `LaudoScreen` |
| Monitoramento | "Análise avançada e alertas em segundo plano" | `MonitoramentoSheet` |
| Jogos | "Games multiplayer e dicas para PS5, Xbox e PC" | `JogosScreen` |

### 4.10 Ping

**Tela:** `PingScreen` (overlay via Ferramentas ou `SpeedTestScreen`).

- Teste de latência isolado, ~20 segundos, roda silenciosamente com barra de progresso
- Resultado: latência (ms), jitter (ms), perda de pacotes (%)
- Referência de leitura: latência boa < 50ms / ruim > 150ms; jitter bom < 10ms / ruim > 30ms; perda
  boa 0% / ruim > 2%

### 4.11 Jogos — teste de conexão

**Tela:** `JogosScreen` (overlay via Ferramentas). Feature completa e recente (issue #935,
implementada em 2026-07-14) — spec detalhada mantida separadamente em
`docs_ai/functional/JOGOS_TESTE_CONEXAO_SPEC.md` (não duplicada aqui). Resumo:

- Fluxo de 5 etapas: escolher plataforma (PC/PS5/Xbox Series) → escolher jogo (16 jogos no
  catálogo inicial, busca) → executar teste nomeado pelo jogo → progresso (10–15s, sem jargão
  técnico) → resultado único (veredito + latência/jitter/perda/estabilidade/região testada)
- 4 perfis de sensibilidade com thresholds próprios (competitivo extremo, competitivo, esporte
  competitivo, multiplayer moderado); prioridade de avaliação: perda > jitter > estabilidade >
  latência > qualidade Wi-Fi > velocidade
- Estratégia de endpoint: `REGIONAL_ESTIMATE` via Worker Cloudflare dedicado
  (`game-latency-probe-worker`, reaproveita `PingExecutor` de `:featureSpeedtest`) é a única
  implementada; `PROVIDER_NETWORK` (rede oficial Riot/Valve) cai automaticamente em estimativa
  regional com aviso explícito — nunca inventa dado de rede não medido
- **Limitações conhecidas (declaradas na spec):** sem detecção real de rede Riot/Valve;
  "estabilidade" não é uma quarta dimensão pontuada própria (tratada via jitter); bufferbloat não é
  medido neste fluxo; histórico por jogo não implementado (era opcional no MVP)

### 4.12 Central de testes (nomenclatura anterior)

O nome de produto "Central de Testes" (usado no antigo `CENTRAL_DE_TESTES_USER_GUIDE.md`) descrevia
um agrupamento de Ping + DNS + "Diagnóstico Inteligente (em breve)" dentro da aba Velocidade. Essa
superfície foi substituída pelo hub **Ferramentas** (seção 4.9) — os mesmos testes (Ping, DNS)
continuam existindo, mas roteados pela aba Ferramentas ou por atalhos dentro de Velocidade/Início,
não por uma aba ou seção própria chamada "Central de Testes". O placeholder "Diagnóstico
Inteligente — em breve" não existe mais: o diagnóstico por IA já está implementado (seção 4.2).

---

## 5. Permissões contextuais

O app pede permissões de forma contextualizada, nunca bloqueante.

### 5.1 Localização (`ACCESS_FINE_LOCATION`)

**Gatilho:** entrar na aba Sinal com Wi-Fi ativo, sem a permissão concedida.

1. `PermissaoLocalizacaoContextoSheet` (bottom sheet): ícone, "Por que precisamos da localização?",
   dois parágrafos explicativos, botões "Agora não" / "Entendi, conceder"
2. Se dispensada: `LocPermissaoBanner` fica no topo da aba, clicável para reabrir o sheet
3. Se concedida: verifica `shouldShowRequestPermissionRationale`; se negada permanentemente, abre
   Configurações do app; senão, mostra o diálogo nativo do Android
4. O scan de Wi-Fi continua funcionando sem a permissão (com dados degradados)

### 5.2 Telefonia (`READ_PHONE_STATE`)

**Gatilho:** entrar na aba Sinal com rede móvel ativa, sem a permissão concedida.

1. `PermissaoTelefoniaContextoSheet`: mesmo padrão de sheet, "Por que precisamos desta permissão?"
2. Se dispensada: `MovelSemPermissaoBanner` substitui as métricas de sinal (clicável, reabre o
   sheet)
3. Se concedida: inicia o monitor de telefonia; card de sinal móvel completo passa a ser exibido

**Regra geral:** nenhuma permissão bloqueia o uso do app — apenas oculta os dados que dependem dela.

---

## 6. Estados e mensagens ao usuário

- **Offline:** `OfflineBanner`/`OfflineCard` aparece em Início, Sinal, SpeedTest e Dispositivos
  quando `!conectado`. Em Início, monitora `ConnectivityManager.NetworkCallback` e dispara um teste
  automático assim que a conexão volta. Em SpeedTest, desativa o botão de iniciar teste.
- **Especificidade de conexão nas mensagens:** o app sempre diferencia Wi-Fi / Móvel / Ethernet /
  Desconhecido nas mensagens e cards, nunca trata todos como genéricos.
- **Confirmação de dados móveis:** `ForaDoWifiDialog` antes de iniciar teste em rede móvel.
- **Confirmação de cancelamento de teste:** diálogo "Cancelar o teste?" ao pressionar voltar durante
  execução.
- **Métrica sempre com veredito humano:** toda métrica crua (dBm, ms, Mbps) vem acompanhada de
  classificação (Excelente/Bom/Regular/Fraco/Forte) — não-negociável de design, ver
  `.claude/CLAUDE.md` seção Design System.

---

## 7. Ativo em debug vs. release

Fonte: `android/app/build.gradle.kts` (blocos `debug` e `release`) e
`android/app/src/main/kotlin/io/veloo/app/kotlin/FeatureFlags.kt` — revalidado nesta consolidação,
diverge da tabela antiga do `.claude/CLAUDE.md`/docs anteriores em um ponto crítico: **não existe
mais a flag `FEATURE_DIAGNOSTICO_CHAT`** (o chat livre foi removido do produto).

**Debug:** todas as flags `true` — build de desenvolvimento expõe tudo.

**Release — flags ativas (visíveis ao usuário final):** `FEATURE_SPEEDTEST`,
`FEATURE_DIAGNOSTICO_LOCAL`, `FEATURE_DIAGNOSTICO_IA` (card + laudo — diagnóstico inline da seção
4.2, não chat), `FEATURE_WIFI_ANALISE`, `FEATURE_REDE_MOVEL_ANALISE`, `FEATURE_HISTORICO`,
`FEATURE_LAUDO_PDF`, `FEATURE_ONBOARDING`, `FEATURE_PERMISSOES_CONTEXTO`,
`FEATURE_ESTADO_OFFLINE`, `FEATURE_SETTINGS_MVP`, `FEATURE_PRIVACIDADE_TELA`,
`FEATURE_NOVIDADES_TELA`, `FEATURE_FIBRA_SCREEN`.

**Release — flags inativas (só em debug):** `FEATURE_DEVICES_SCREEN_V2`, `FEATURE_LINKPULSE_ATIVO`,
`FEATURE_NOTIFICACAO_INLINE`, `FEATURE_LINKPULSE_CHAT`, `FEATURE_WIDGET`,
`FEATURE_QUICK_SETTINGS_TILE`, `FEATURE_PROVA_REAL_COMPLETO`, `FEATURE_DIAGNOSTICO_ITERATIVO`,
`FEATURE_TRACEROUTE`, `FEATURE_TELEPHONY_AVANCADO`, `FEATURE_MAPA_CALOR_WIFI`,
`FEATURE_AGENDAMENTO_TESTES`, `FEATURE_LINKASYNC`, `FEATURE_BACKUP_LOCAL`,
`FEATURE_CONTRIBUICAO_ANONIMA`, `FEATURE_RATE_US`, `FEATURE_ACESSIBILIDADE`.

> Jogos (`JogosScreen`) e DNS Benchmark não têm flag própria de build type — estão sempre
> presentes, controlados apenas pela navegação real (aba Ferramentas). Regras de build/flag
> puramente técnicas (como ativar uma flag nova) ficam em `docs_ai/functional/FEATURE_FLAGS.md` e
> em `docs_ai/TECNICO.md` — fora do escopo funcional deste documento.

---

## 8. Limitações conhecidas

- **`MinhaConexaoScreen`** existe no diretório de telas mas não está ligada a nenhuma navegação
  real — possível código morto, não confirmado nesta tarefa (ver seção 3).
- **Jogos:** sem detecção real de rede oficial Riot/Valve (`PROVIDER_NETWORK`); sem bufferbloat
  medido no fluxo; sem histórico por jogo.
- **Diagnóstico IA:** é uma análise de turno único por descrição do problema, não uma conversa
  contínua — quem espera o "chat" das versões anteriores do app não vai encontrá-lo.
- **Wi-Fi:** taxa PHY e label MIMO ainda não exibidos na UI (bloqueados por falta de API pública
  direta no SDK Android para MIMO); identificação de nó mesh sujeita a falso-positivo em redes
  tri-band com BSSID diferente por banda.
- **DNS:** o app nunca altera o DNS do sistema — apenas orienta a configuração manual.
- **Caminho físico legado:** todo o código desta camada ainda mora fisicamente em
  `io/veloo/app/kotlin/...` apesar do package declarado ser `io.signallq.app` — não é um problema
  funcional para o usuário, mas afeta qualquer agente navegando o código (ver
  `.claude/rules/higiene-e-padronizacao-repositorio.md` seção 4.1).
