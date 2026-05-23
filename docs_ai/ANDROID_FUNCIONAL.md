# Documentação Funcional — Android Linka

**Público-alvo:** Desenvolvedor humano e agentes de IA
**Plataforma:** Android exclusivo
**Última atualização:** 2026-05-19 (v0.8.1 — thresholds Wi-Fi por banda, MetricCards dinâmicos, DNS-03, FibraScreen loading + hint RX, DiagnosticoScreen tipo conexão real, acessibilidade)
**Mantido por:** Taisa

> Este documento responde: "O que o app Android faz, tela por tela, da perspectiva do usuário?"
> Para arquitetura interna, engines e contratos técnicos, consulte `ANDROID_TECNICO.md`.
> Fonte de verdade: dados coletados do código real (Marcelo, 2026-05-17).

---

## 1. O que é o Linka

O Linka é um app Android nativo de diagnóstico de internet doméstica. Mede velocidade, analisa Wi-Fi, DNS, latência, jitter e perda de pacotes, e entrega diagnóstico assistido por IA com ações práticas para o usuário.

**Funcionalidades principais:**
- Teste de velocidade (download, upload, latência, jitter, perda, bufferbloat)
- Diagnóstico local por engines especializados + IA via Cloudflare Worker
- Assistente conversacional Orbit (IA)
- Scan de redes Wi-Fi vizinhas e análise de topologia
- Scanner de dispositivos na rede local (ARP, mDNS)
- Monitoramento passivo em background (WorkManager)
- Leitura de dados de modem GPON Nokia (fibra óptica)
- Benchmark de DNS via DoH
- Histórico de medições com gráfico de uptime e narrativa
- Notificações de alerta configuráveis

- Permissões contextuais (localização para Wi-Fi, telefonia para dados móveis)
- Tratamento de estado offline (banners, cards, guards)
---

### Controle de Features por Build Type (FeatureFlags)

O app usa um sistema de **FeatureFlags no BuildConfig** para controlar a visibilidade de features:

- **Debug:** Todas as 33 features habilitadas (desenvolvedor testa a full stack)
- **Release:** 15 features MVP habilitadas (usuário final vê features prontas)

Verifica-se usando `io.linka.app.kotlin.FeatureFlags` em qualquer tela ou lógica. As features pós-MVP estão mapeadas em `FeatureFlags.kt` e controladas no `app/build.gradle.kts`.

**Estado em release desta entrega:**

| Feature | Flag | Estado em Release |
|---|---|---|
| FibraScreen (modem GPON) | `FEATURE_FIBRA_SCREEN` | Ativo (promovido para MVP) |
| DNS Benchmark | `FEATURE_DNS_SCREEN` | Ativo (promovido para MVP) |
| Chat Orbit / IA conversacional | `FEATURE_DIAGNOSTICO_CHAT` | Inativo (nova flag, oculto em release) |
| Diagnóstico IA (card + laudo) | `FEATURE_DIAGNOSTICO_IA` | Ativo (independente do chat) |

Consulte `ANDROID_TECNICO.md` seção 9.1 para a lista completa de flags e seus estados.


## 2. Navegação

O app usa uma `NavigationBar` inferior com **5 abas fixas**. Telas secundárias são sobrepostas sobre as abas — não são abas separadas.

```
NavigationBar (5 abas fixas)
├── [0] Início    → HomeScreen
├── [1] Velocidade → SpeedTestScreen
├── [2] Sinal     → SinalScreen
├── [3] Histórico → HistoricoScreen
└── [4] Mais      → (menu/tela de acesso a ajustes e demais funcionalidades)

Fluxos secundários (sobrepostos)
├── SpeedTest → VelocidadeScreen (execução) → ResultadoVelocidadeScreen
├── ResultadoVelocidade → DiagnosticoScreen → ChatScreen (IA) [FEATURE_DIAGNOSTICO_CHAT — inativo em release]
└── ChatScreen → volta a ResultadoVelocidadeScreen

Acessível via aba "Mais"
├── AjustesScreen
├── DispositivosScreen
└── OnboardingScreen (apenas primeira execução)
```

**Onboarding:** `OnboardingScreen` é exibida na primeira execução quando `onboarding_concluido = false` no DataStore. Após conclusão, nunca é exibida novamente.

### 2.1 ProfileAvatarButton

Presente no `navigationIcon` (lado esquerdo do TopAppBar) de **todas as 5 abas root**: Início, Velocidade, Sinal, Histórico e Mais.

**O que exibe:**
- Foto do perfil do usuário quando `fotoUriUsuario` está definida (decodificada via `BitmapFactory` + `contentResolver`)
- Inicial do nome com gradiente accent/accentBlue quando não há foto

**Ao tocar:** abre `PerfilEditSheet` (bottom sheet de edição de perfil).

### 2.2 Scroll-aware NavBar

A `NavigationBar` reage ao scroll do conteúdo:
- **Scroll para baixo:** NavBar desliza para fora da tela (off-screen) progressivamente
- **Scroll para cima:** NavBar reaparece
- **Durante execução do speedtest:** NavBar some completamente (comportamento já existente, mantido)

A animação usa `offset { IntOffset }` + `graphicsLayer { alpha }` sincronizados com `NestedScrollConnection` no Scaffold do `AppShell`.

### 2.3 Back button em telas de fluxo

| Tela | Comportamento do botão Voltar |
|---|---|
| `VelocidadeScreen` | `ArrowBack` no navigationIcon — oculto durante `EstadoExecucaoSpeedtest.executando` |
| `ResultadoVelocidadeScreen` | `ArrowBack` no navigationIcon — sempre visível |
| `DiagnosticoScreen` | `ArrowBack` no navigationIcon — sempre visível |
| `FibraScreen` | `ArrowBack` no navigationIcon — sempre visível |

### 2.4 Confirmação de cancelamento de teste

Quando o usuário pressiona Voltar (hardware ou gesto) durante um teste em execução (`estado == executando`) no `SpeedTestScreen`:

- `BackHandler` intercepta a navegação
- Exibe `AlertDialog` com:
  - Título: "Cancelar o teste?"
  - Botão primário: "Continuar testando" (dismissar o diálogo)
  - Botão secundário: "Cancelar teste" (confirma cancelamento)

---

## 3. Primeiro Acesso — Permissões Contextuais

O app solicita permissões de forma contextualizada, apenas quando necessário, sem bloquear o usuário.

### 3.1 Permissão de Localização (PA-A)

**Cenário:** Usuário abre `SinalScreen` com rede Wi-Fi ativa e ainda não concedeu `ACCESS_FINE_LOCATION`.

**O que acontece:**
1. Um `LaunchedEffect` detecta a condição e dispara `showLocalizacaoSheet = true`
2. Exibe `ModalBottomSheet` com `PermissaoLocalizacaoContextoSheet`:
   - Ícone `LocationOn`
   - Título: "Por que precisamos da localização?"
   - Dois parágrafos explicativos
   - Botões: "Agora não" (dismissar) ou "Entendi, conceder" (solicitar)

**Comportamento:**
- Se usuário tocar "Agora não": `localizacaoSheetDismissed = true` → `LocPermissaoBanner` aparece no topo da aba WiFi (clicável para reabrir o sheet)
- Se usuário tocar "Entendi, conceder": aciona `onSolicitarPermissaoLocalizacao()` em MainActivity
  - Verifica `shouldShowRequestPermissionRationale`
  - Se negado permanentemente: abre Settings do app
  - Se ainda não decidido: exibe diálogo nativo do Android
- **Importante:** WiFi scan continua funcionando normalmente sem a permissão

### 3.2 Permissão de Telefonia (PA-B)

**Cenário:** Usuário abre `SinalScreen` com rede móvel ativa e ainda não concedeu `READ_PHONE_STATE`.

**O que acontece:**
1. Exibe `ModalBottomSheet` com `PermissaoTelefoniaContextoSheet`:
   - Ícone `CellTower`
   - Título: "Por que precisamos desta permissão?"
   - Parágrafos explicativos

**Comportamento:**
- Se usuário dismissar: exibe `MovelSemPermissaoBanner` (clicável, reabre o sheet) no lugar das métricas de sinal
- Se usuário conceder: 
  - Aciona `onSolicitarPermissaoTelefonia()` em MainActivity
  - MainActivity inicia `MonitorTelephony`
  - `movelSnapshot` deixa de ser nulo
  - `MobileSignalCard` é exibido com RSRP, RSRQ, SINR e classificação de qualidade

---

## 4. Tratamento de Estado Offline

O app exibe indicadores visuais contextualizados em 5 telas quando o dispositivo perde conectividade.

### 4.1 HomeScreen

**Novo estado:** `OfflineCard` como primeiro item condicional do LazyColumn quando `!snapshotRede.conectado`.

**O que exibe:**
- Ícone `WifiOff`
- Texto explicativo: "Sem conexão de internet"
- Botão "Testar assim que voltar"

**Comportamento:**
- Ao tocar o botão: registra um `ConnectivityManager.NetworkCallback` via `DisposableEffect`
- Quando a conexão volta: auto-dispara `onNovoTeste()` (inicia speedtest automaticamente)
- O callback é limpo quando o composable sai de composição

### 4.2 SinalScreen

**Novo estado:** `OfflineBanner` adicionado no topo da tela quando `!conectado`.

**O que exibe:**
- Banner com ícone e texto: "Sem conexão — conecte-se a uma rede"

**Aplicável em:**
- Aba WiFi (se rede Wi-Fi não está disponível)
- Aba Móvel (se dados móveis não estão disponíveis)
- Ambas as seções mostram o banner conforme o estado de conectividade

### 4.3 SpeedTestScreen

**Novo comportamento:** Guard offline em `onIniciarTesteComAviso`.

**O que acontece:**
- Antes de iniciar teste, verifica `if (!conectado)` → bloqueia chamada
- Exibe indicador visual abaixo do `SpeedTestCircle`: "Sem conexão — teste indisponível"
- Botão de teste fica inativo (não clicável)

---

## 5. Telas Principais (Abas da NavigationBar)

### 5.1 HomeScreen — Aba 0 (Início)

**Composable:** `HomeScreen.kt`

**O que o usuário vê:**
- **Estado Offline (novo):** `OfflineCard` como primeiro item (se `!conectado`)
  - Ícone WifiOff, texto e botão "Testar assim que voltar"
  - Auto-dispara teste ao reconnectar
- Card de perfil: foto do usuário, nome, rede conectada
- Resumo de velocidade: última medição (download/upload)
- Lista de gateways detectados na rede
- Gráfico de histórico resumido
- Uptime narrative (texto gerado pelo engine)

**Parâmetros recebidos:** `snapshotRede`, `snapshotSpeedtest`, `history`, `ultimaMedicao`, `localIp`, `publicIp`, `ispInfo`, `gateways`, `deviceName`, `nomeUsuario`, `fotoUriUsuario`, `connectedNetwork`, `movelSnapshot`.

**Estados visuais:**
- Offline: `OfflineCard` exibido
- Conectado em Wi-Fi: exibe SSID, RSSI, banda
- Conectado em dados móveis: exibe operadora, tecnologia (4G/5G), RSRP
- Com histórico: exibe mini-gráfico e narrativa
- Sem histórico: estado vazio

### 5.2 SpeedTestScreen — Aba 1 (Velocidade)

**Composable:** `SpeedTestScreen.kt`

**O que o usuário vê:**
- `SpeedTestCircle` central animado (gauge)
- **Indicador Offline (novo):** "Sem conexão — teste indisponível" abaixo do círculo (se `!conectado`)
- **Banner plano vazio (novo):** aparece quando `planoInternet.isBlank()`: "Configure sua velocidade contratada para comparar com a ANATEL." — com ação para ir até Configurações
- `ModeSelector` com pills: rápido / completo / triplo (desativado se offline)
- `LastResultCard` com último resultado de download e upload
- `CardContextoUso`: suporte para videochamada, streaming HD, jogos, home-office
- `CardRqualAnatel`: comparação com mínimo ANATEL (40%) e normal (80%) em relação ao plano contratado. Quando resultado < 40% do plano contratado, exibe mensagem: "Abaixo de 40%: você tem direito a solicitar rescisão sem multa (ANATEL Ato 7869/2022)."
- `CardBufferbloat`: severidade (none / mild / moderate / severe)
- `CardRodadasTriplo`: expandível com 3 rodadas individuais (quando modo triplo)
- `ExploreToolsRow`: bottom sheet com acesso a DNS Benchmark (`FEATURE_DNS_SCREEN` — ativo em release) e Diagnóstico
- `StatusCard`: status de Wi-Fi, operadora e servidor

**Modos de teste:** rápido / completo / triplo (desativados em offline)

**Diálogo de confirmação:** ao iniciar com dados móveis, exibe estimativa de consumo de dados e pede confirmação antes de prosseguir.

**Estados visuais:**
- Offline: indicador visível, botão inativo
- Idle: pulse animado aguardando início
- Executando: círculo de progresso + velocidade ao vivo
- Concluído: check com resultado final

**Parâmetros recebidos:** `snapshotSpeedtest`, `snapshotRede`, `ispInfo`, `localizacaoServidor`, `modoSelecionado`, `onModoSelecionado`, `onIniciarTeste`, `onCancelarTeste`, `onAbrirDnsBenchmark`, `onAbrirDiagnostico`, `onVoltar`, `conectado`.

### 5.3 SinalScreen — Aba 2 (Sinal)

**Composable:** `SinalScreen.kt`

A tela detecta o tipo de conexão ativa e exibe conteúdo adaptado. O comportamento varia por modo.

**Estados offline e permissões (novo):**
- `OfflineBanner` no topo quando `!conectado`
- `PermissaoLocalizacaoContextoSheet`: exibida ao entrar em Wi-Fi sem `ACCESS_FINE_LOCATION`
  - Se dismissada: `LocPermissaoBanner` aparece no topo da aba
- `PermissaoTelefoniaContextoSheet`: exibida ao entrar em Móvel sem `READ_PHONE_STATE`
  - Se dismissada: `MovelSemPermissaoBanner` substitui as métricas de sinal

**Modo Wi-Fi (padrão):**
- `TabRow` com 4 abas: Todas / 2.4GHz / 5GHz / 6GHz
- `RedeCard` para cada rede: SSID, RSSI, canal, segurança, OUI (fabricante)
- `WifiChannelGuide`: visualização de congestionamento de canais
- `BottomSheet`: análise de topologia Wi-Fi e recomendações
- Dados exibidos por rede vizinha: SSID, BSSID, RSSI (dBm), canal, frequência (MHz), segurança, OUI do fabricante
- **Com permissão:** todas as informações disponíveis
- **Sem permissão:** scan continua, mas com `LocPermissaoBanner` para solicitar

**Classificação de sinal Wi-Fi por banda (v0.8.1):**

A classificação de qualidade do sinal distingue a banda da rede. Redes em 5GHz têm thresholds mais exigentes porque a frequência mais alta atenua mais.

| Classificação | 5GHz (dBm) | 2.4GHz (dBm) |
|---|---|---|
| Excelente | ≥ −55 | ≥ −50 |
| Bom | ≥ −65 | ≥ −60 |
| Regular | ≥ −75 | ≥ −70 |
| Fraco | < −75 | < −70 |

**Modo Móvel (4G/5G):**
- Exibe operadora e tecnologia de rede (ex.: 4G LTE, 5G NR)
- **Com permissão:** RSRP com classificação de qualidade (Excelente / Bom / Regular / Fraco), RSRQ, SINR
- **Sem permissão:** `MovelSemPermissaoBanner` (clicável, reabre o sheet de permissão)
- IP local do dispositivo

**Modo Cabo (Ethernet):**
- Estado informativo com IP local
- Sem scan de redes — não aplicável para conexão por cabo

**Modo Desconhecido/Offline:**
- Estado vazio orientando o usuário a se conectar a uma rede
- `OfflineBanner` visível

**Parâmetros recebidos:** `estadoConexao: EstadoConexao`, `movelSnapshot: MovelSnapshot?`, `localIp: String?`, `temPermissaoLocalizacao: Boolean`, `onSolicitarPermissaoLocalizacao: () -> Unit`, `temPermissaoTelefonia: Boolean`, `onSolicitarPermissaoTelefonia: () -> Unit`, `snapshotWifi`, `connectedNetwork`, callbacks para scan e refresh, `conectado: Boolean`.

> Nota: o parâmetro `isOnWifi: Boolean` foi substituído por `estadoConexao: EstadoConexao` na v0.7.3.

**Ações disponíveis:**
- Filtrar redes por banda (tabs) — apenas no modo Wi-Fi
- Atualizar scan — apenas no modo Wi-Fi
- Abrir análise de topologia (bottom sheet) — apenas no modo Wi-Fi
- Conceder permissão de localização — apenas em Wi-Fi sem permissão
- Conceder permissão de telefonia — apenas em Móvel sem permissão

### 5.4 HistoricoScreen — Aba 3 (Histórico)

**Composable:** `HistoricoScreen.kt`

**O que o usuário vê:**
- Gráfico de histórico (uptime)
- Uptime narrative: texto gerado pelo `UptimeNarrativaEngine`
- Resumo de medições

**Ações disponíveis:** visualizar histórico de medições passadas

### 5.5 Aba 4 — "Mais"

Aba de acesso a funcionalidades adicionais. Ícone: `GridView`. Dá acesso a `AjustesScreen`, `DispositivosScreen` e outras telas pós-MVP.

---

## 6. Telas Secundárias (Fluxos Sobrepostos e Acesso via "Mais")

### 6.1 VelocidadeScreen — Execução do Teste

**Composable:** `VelocidadeScreen.kt`

**Trigger:** automático ao iniciar um teste via `SpeedTestScreen`.

**O que o usuário vê:**
- `GaugeCircular` central: progresso global + fase atual + velocidade em Mbps
- `MiniGrafico`: gráfico ao vivo de pontos de velocidade (`PontoAoVivo`)
- `PillsFase`: status de cada fase com checkmark ao concluir (LATÊNCIA / DOWN / UP / CONCLUÍDO)
- `LinhaServidor`: localização do servidor + nome do ISP
- `ErroContent`: botões "Testar Novamente" e "Cancelar" (visível apenas em caso de erro)

**Transições:** haptics entre fases.

**Botão Voltar:** `ArrowBack` no navigationIcon — oculto durante `EstadoExecucaoSpeedtest.executando`.

**Parâmetros recebidos:** `snapshot`, `localizacaoServidor`, `ispInfo`, `onCancelar`, `onReiniciar`.

### 6.2 ResultadoVelocidadeScreen — Resultado do Teste

**Composable:** `ResultadoVelocidadeScreen.kt`

**Trigger:** automático após conclusão do teste.

**Título da tela (v0.8.1):** "Resultado do teste"

**Botão Voltar:** `ArrowBack` no navigationIcon — sempre visível.

**Layout em ordem de exibição:**
1. Grade circle: classificação A / B / C / D / ? com cor correspondente
2. Título e mensagem de diagnóstico
3. Cards de download e upload (Mbps)
4. Cards de latência e jitter — cores dinâmicas por threshold (v0.8.1):
   - **Latência:** < 20ms = verde, < 60ms = amarelo, ≥ 60ms = vermelho
   - **Jitter:** < 10ms = verde, < 30ms = amarelo, ≥ 30ms = vermelho
5. Chip de contaminação (se teste foi contaminado)
6. Cards de perda de pacotes e bufferbloat
7. Seção EXPERIÊNCIA DE USO: vereditos para Streaming, Gaming e Vídeo Chamada (good / acceptable / poor)
8. DNS Info: provedor + latência
9. Detalhes Avançados (expansível): pico DL/UL, latência com carga, estabilidade
10. `RecomendacaoCard`: ação baseada no diagnóstico
11. `OperadoraContactCard`: exibido quando o diagnóstico identifica o problema no ISP (`categoria == "isp"`). Detecta a operadora pelo nome do ISP e exibe botões de SAC (abre discador) e WhatsApp (quando disponível). Fallback para Anatel (1331) quando a operadora não é identificada. Base com 16 ISPs mapeados.

**Botão de compartilhamento (TopAppBar):** ícone Share no cabeçalho da tela. Ao tocar, gera um bitmap 1080×600px com download, upload, latência, jitter, headline do diagnóstico e data/hora. A cor de fundo varia por severidade do diagnóstico (verde / amarelo / vermelho / neutro escuro). Compartilha via share sheet nativo do Android. Exibe spinner enquanto o bitmap é gerado.

**Botões:** "Conversar com IA" (exibido apenas quando `FEATURE_DIAGNOSTICO_CHAT` ativo — inativo em release), "Testar Upload Novamente", "Ir para o início", "Testar novamente".

**Parâmetros recebidos:** `resultado`, `snapshotDiagnostico`, `ispInfo: IspInfo?`, `onTestarNovamente`, `onIrParaHome`, `onAbrirChat`, `gemmaAvailable`.

### 6.3 DiagnosticoScreen — Diagnóstico Detalhado

**Composable:** `DiagnosticoScreen.kt`

**Trigger:** a partir de `ResultadoVelocidadeScreen` ou via `ExploreToolsRow` no SpeedTest.

**O que o usuário vê:**
- Cards dinâmicos de resultado por engine: ícone, `status badge` (OK / INFO / ATTENTION / CRITICAL), mensagem e recomendação

**Estados:** Idle / Executando (loader) / Concluído

**Parâmetros recebidos:** `snapshotDiagnostico`, `resultado`, callbacks para iniciar, selecionar chips, enviar contexto.

**Contexto enviado à IA (v0.8.1):** o tipo de conexão incluído no contexto enviado ao Worker Cloudflare é o tipo real detectado (`wifi`, `movel`, `ethernet`). Versões anteriores enviavam `wifi` fixo independentemente da conexão ativa.

### 6.4 ChatScreen — Orbit IA Conversacional

**Composable:** `ChatScreen.kt`

**Flag de controle:** `FEATURE_DIAGNOSTICO_CHAT` — **inativa em release**. O botão "Conversar com IA" e o overlay `ChatScreen` não são exibidos para o usuário final em builds de produção. Visíveis apenas em debug.

**Trigger:** botão "Conversar com IA" em `ResultadoVelocidadeScreen` (exibido apenas quando `FEATURE_DIAGNOSTICO_CHAT` está ativo).

**O que o usuário vê:**
- `OrbitUserMessageBubble`: bolha de mensagem do usuário
- `OrbitThinkingBubble`: animação de "pensando"
- `OrbitAiMessageBubble`: resposta da IA em markdown
- `OrbitInlineQuestion`: chips de resposta rápida
- `OrbitInputArea`: campo de texto + botão de envio
- `AiModelFooter`: informação do modelo de IA usado
- `LinkaIaHeader`: cabeçalho da sessão

**API da IA:** `https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev`

**Estados da sessão:** Idle / Thinking / AwaitingInput / Error

**Parâmetros recebidos:** `uiState`, `onNavigateBack`, `onIniciarOrbit`, `onResetOrbit`, `onSelecionarChip`, `onResponderPergunta`, `onEnviarMensagemTexto`.

**Ações disponíveis:**
- Enviar mensagem de texto livre
- Selecionar chip de resposta
- Resetar sessão
- Voltar para `ResultadoVelocidadeScreen`

### 6.5 AjustesScreen — Configurações

**Composable:** `AjustesScreen.kt`

**TopAppBar:** título "Configurações".

**Estrutura:** `LazyColumn` organizado em 4 seções fixas + 1 seção condicional:

| Seção | Conteúdo |
|---|---|
| Minha conexão | Operadora (`ProvedorSheet`), plano contratado (numérico, máx 4 dígitos), Estado (dropdown 27 UFs), Cidade (autocomplete via IBGE API) |
| Aparência | Tema: Sistema / Claro / Escuro |
| Histórico e dados | Toggle monitoramento passivo, notificações individuais (latência, DNS, RSSI, sem internet), alerta de velocidade (Mbps) |
| Informações | Link para PrivacidadeScreen, link para NovidadesScreen, versão do app |
| Avançado | Análise avançada (toggle), Fibra — modem Nokia (host, usuário, senha, manter conectado) |

**Seção "Avançado"** é exibida condicionalmente quando pelo menos um dos flags estiver ativo: `BuildConfig.FEATURE_FIBRA_SCREEN`, `BuildConfig.FEATURE_DNS_SCREEN` ou `BuildConfig.FEATURE_LINKPULSE_ATIVO`. Como `FEATURE_FIBRA_SCREEN` e `FEATURE_DNS_SCREEN` são MVP (ativos em release), a seção Avançado é sempre exibida a partir desta entrega.

**Banner ISP (inline no LazyColumn):**
- Aparece quando: `!ispConfirmado && ispDetectado != null && operadora.isBlank()`
- Exibe o ISP detectado automaticamente e oferece duas ações:
  - "Confirmar" — salva o ISP detectado como operadora e marca `ispConfirmado = true`
  - "Ignorar" — marca `ispConfirmado = true` sem salvar operadora
- Após qualquer ação, o banner some

**Campo Plano de Internet:**
- Aceita apenas dígitos (máx 4 caracteres), ex.: "300", "1000"
- Representa velocidade contratada em Mbps

**Campo Região:**
- Substituído por seleção estruturada em dois passos:
  1. Dropdown de Estado: 27 UFs fixas no código
  2. Autocomplete de Cidade: lista carregada da IBGE API (`https://servicodados.ibge.gov.br/api/v1/localidades/estados/$uf/municipios`) com cache in-memory por UF

**Perfil:** editável via `PerfilEditSheet` (acessível pelo `ProfileAvatarButton` em todas as abas root, não diretamente em AjustesScreen).

### 6.6 DispositivosScreen — Scanner de Rede Local

**Composable:** `DispositivosScreen.kt`

**Acesso:** aba "Mais" (índice 4) — não está mais na TabBar principal.

**O que o usuário vê:**
- `OfflineBanner` no topo quando `!conectado`
- Lista de dispositivos detectados na rede local
- Por dispositivo: nome/apelido, IP, MAC, fabricante (OUI), tipo, serviços mDNS

**Ações disponíveis:**
- Dar apelido a um dispositivo (salvo na tabela `apelido_dispositivo` do Room)
- Atualizar lista (refresh)

**Estados visuais:** Loading / Lista com dispositivos / Vazio / Offline / Erro

### 6.7 FibraScreen — Modem GPON

**Composable:** `FibraScreen.kt`

**Flag de controle:** `FEATURE_FIBRA_SCREEN` — **ativa em release** (promovida para MVP nesta entrega). Visível para o usuário final.

**Trigger:** Ajustes → seção Avançado → Fibra → conectar modem.

**O que o usuário vê (quando conectado):**
- Status GPON: up/down, potência Rx (dBm), potência Tx (dBm), temperatura (°C), corrente do laser (mA), voltagem, número serial, modo de operação
- Status WAN: IP, máscara, gateway
- Status PPP (se aplicável)
- Informações do dispositivo (modelo da ONT)
- Gateway IP detectado automaticamente

**Texto de loading (v0.8.1):** dinâmico — exibe o modelo do modem detectado (`deviceInfo.model`) quando disponível (ex.: "Conectando ao Nokia G-010G-P…"), ou "Conectando ao modem…" como fallback quando o modelo ainda não foi identificado.

**Campo RX Power (v0.8.1):** hint corrigido para "Ideal: −8 a −27 dBm" conforme padrão ITU-T G.984 (GPON). Versões anteriores exibiam faixa incorreta.

**Botão Voltar:** `ArrowBack` no navigationIcon — sempre visível.

**Ações disponíveis:** conectar/reconectar, salvar configuração.

### 6.8 OrbitScreen

**Composable:** `OrbitScreen.kt`

Exibe o símbolo animado de Orbit. Ponto de entrada da experiência de IA conversacional autônoma.

### 6.9 LinkaPulseScreen

**Composable:** `LinkaPulseScreen.kt`

Dashboard de monitoramento contínuo. Exibe o símbolo LinkaPulse animado com status do monitoramento passivo.

### 6.10 LaudoScreen

**Composable:** `LaudoScreen.kt`

Relatório visual completo do diagnóstico. Acesso via Ajustes → "Gerar Laudo".

### 6.11 OnboardingScreen

**Composable:** `OnboardingScreen.kt`

Fluxo de primeiro uso com slides de boas-vindas. Exibido uma única vez.

### 6.12 PrivacidadeScreen

**Composable:** `ui/screen/PrivacidadeScreen.kt`

**Acesso:** AjustesScreen → seção "Informações" → "Privacidade".

**Apresentação:** tela full-screen com overlay via `AnimatedVisibility`. Fecha com `BackHandler` (hardware back ou gesto) via `AppShell`.

**Conteúdo em 4 blocos:**
1. "O que coletamos" — dados que o app armazena localmente
2. "O que NÃO coletamos" — confirmação explícita do que é excluído
3. "Retenção e controle" — como o usuário controla seus dados
4. "Solicitação de exclusão" — como solicitar remoção de dados

**Controle de exibição:** `BuildConfig.FEATURE_PRIVACIDADE_TELA` (flag MVP — ativa em debug e release).

### 6.13 NovidadesScreen

**Composable:** `ui/screen/NovidadesScreen.kt`

**Acesso:** AjustesScreen → seção "Informações" → "Novidades".

**Apresentação:** tela full-screen com overlay via `AnimatedVisibility`. Fecha com `BackHandler` via `AppShell`.

**Conteúdo:** lista de versões com changelog, lida do arquivo `app/src/main/assets/changelog.json`. Versões presentes: v0.7.0, v0.6.0, v0.5.0.

**Marcação de leitura:** DataStore key `ultimaVersaoVista` — armazena a versão mais recente vista pelo usuário. O app pode usar este valor para exibir badge ou destaque na entrada do menu.

**Controle de exibição:** `BuildConfig.FEATURE_NOVIDADES_TELA` (flag MVP — ativa em debug e release).

---

## 7. Monitoramento Passivo

**Ativação:** Toggle "Monitoramento ativo" em Ajustes.

**O que faz em background (WorkManager):**
- Mede latência periodicamente via HTTP
- Mede tempo de resolução DNS
- Coleta RSSI atual do Wi-Fi
- Persiste medições no Room (`connectionType = "monitor"`, sem download/upload)
- Aplica histerese para notificações (evita spam)

**Notificações configuráveis individualmente:**

| Tipo | Condição |
|---|---|
| Latência alta | Latência persistentemente elevada |
| DNS lento | DNS do provedor mais lento que alternativas |
| Sinal Wi-Fi fraco | RSSI abaixo do limiar |
| Sem internet | Sem conectividade |

---

## 8. Fluxo Principal: Speedtest

```
1. Usuário toca "Iniciar Teste" em SpeedTestScreen
   └── Se dados móveis: diálogo de confirmação com estimativa de consumo
2. VelocidadeScreen: gauge animado em tempo real
   ├── Fase LATÊNCIA (ping)
   ├── Fase DOWN (download Mbps ao vivo)
   ├── Fase UP (upload Mbps ao vivo)
   └── CONCLUÍDO: haptic + checkmarks
3. ResultadoVelocidadeScreen: resultado completo
   ├── Grade A/B/C/D
   ├── Métricas: DL, UL, latência, jitter, perda, bufferbloat
   ├── Vereditos de uso (Streaming, Gaming, Vídeo Chamada)
   └── [opcional] "Conversar com IA" → ChatScreen
```

**Resultado salvo automaticamente em Room** (`MedicaoEntity`) com todos os campos medidos.

---

## 9. Fluxo Principal: Diagnóstico

```
1. SpeedTestScreen → ExploreToolsRow → DiagnosticoScreen
   OU ResultadoVelocidadeScreen → DiagnosticoScreen
2. DiagnosticOrchestrator executa engines em sequência
3. DiagnosticDecisionEngine consolida resultado final
4. DiagnosticoScreen exibe cards por engine (OK/INFO/ATTENTION/CRITICAL)
5. [opcional] ChatScreen para diálogo de refinamento com Orbit IA
```

---

## 10. Fluxo Principal: Orbit IA

> **Disponibilidade:** `FEATURE_DIAGNOSTICO_CHAT` — **inativo em release**. Este fluxo está oculto para o usuário final em produção. Visível apenas em builds debug.

```
1. ChatScreen iniciada via ResultadoVelocidadeScreen
2. OrbitOrchestrator orquestra:
   ├── Coleta dados da rede atual
   ├── Speedtest silencioso (sem abrir VelocidadeScreen)
   └── Envio ao Worker Cloudflare (Gemma 4 26B)
3. ChatScreen exibe resposta em markdown via OrbitAiMessageBubble
4. DynamicQuestionEngine gera perguntas contextuais
5. Usuário responde via chips ou texto livre
6. ContextAccumulator acumula respostas para refinamento
```

---

## 11. Fluxo Principal: Wi-Fi

```
1. SinalScreen: TabRow filtra por banda (Todas / 2.4GHz / 5GHz / 6GHz)
2. WifiChannelGuide: visualiza congestionamento de canais
3. TopologiaWifiEngine classifica dispositivos:
   ├── ROTEADOR_MESH (OUI mesh + nó principal)
   ├── NO_MESH (OUI mesh + nó secundário)
   ├── ROTEADOR (OUI ISP ou SSID único)
   └── REPETIDOR (múltiplos BSSIDs + OUI diferente + sinal mais fraco)
4. BottomSheet: análise de topologia e recomendações
```

---

## 12. Acessibilidade

### 12.1 Semântica TalkBack (v0.8.1)

| Componente | Tela | Semântica adicionada |
|---|---|---|
| `ModeSelector` (pills rápido/completo/triplo) | `SpeedTestScreen` | Roles e labels descritivos para leitura por TalkBack |
| `PathConnector` (linha de conexão visual) | `HomeScreen` | Marcado como elemento decorativo (sem semântica de conteúdo) — não lido pelo TalkBack |

---

## 13. Features Exclusivas Android (sem equivalente no PWA)

| Feature | Dependência Android |
|---|---|
| Scan de redes Wi-Fi (SSID, RSSI, canal) | `WifiManager` |
| Scan de dispositivos na rede | ARP + mDNS nativo |
| Monitoramento passivo em background | `WorkManager` |
| Leitura de dados da ONT GPON | HTTP local ao modem |
| Sinal de dados móveis (RSRP, RSRQ, SINR) | `TelephonyManager` |
| Gráfico de uptime com medições passivas | Histórico do monitor |
| Notificações de alerta de rede | `NotificationManager` + background |
| Permissões contextuais (localização, telefonia) | Runtime permissions Android 6+ |
| Detecção automática de offline | `ConnectivityManager.NetworkCallback` |
