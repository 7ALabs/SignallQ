# Documentação Funcional — Android SignallQ

**Público-alvo:** Desenvolvedor humano e agentes de IA
**Plataforma:** Android exclusivo
**Última atualização:** 2026-06-21 (v0.16.0 — bump de versão e versionCode 46; OrbitScreen→SignallQScreen; abas da NavBar corrigidas; flags de release atualizadas; tags [VERIFICAR] resolvidas)
**Mantido por:** Taisa

> Este documento responde: "O que o app Android faz, tela por tela, da perspectiva do usuário?"
> Para arquitetura interna, engines e contratos técnicos, consulte `ANDROID_TECNICO.md`.
> Fonte de verdade: git log + código real. Última coleta: 2026-05-30.

---

## 1. O que é o SignallQ

O SignallQ é um app Android nativo de diagnóstico de internet doméstica. Mede velocidade, analisa Wi-Fi, DNS, latência, jitter e perda de pacotes, e entrega diagnóstico assistido por IA com ações práticas para o usuário.

**Funcionalidades principais:**
- Teste de velocidade (download, upload, latência, jitter, perda, bufferbloat)
- Diagnóstico local por engines especializados + IA via Cloudflare Worker
- Assistente conversacional IA (Chat IA com streaming e thinking tokens)
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

Verifica-se usando `io.veloo.app.kotlin.FeatureFlags` em qualquer tela ou lógica. As features pós-MVP estão mapeadas em `FeatureFlags.kt` e controladas no `app/build.gradle.kts`.

**Estado em release v0.16.0 (versionCode 46):**

| Feature | Flag | Debug | Release |
|---|---|---|---|
| FibraScreen (modem GPON) | `FEATURE_FIBRA_SCREEN` | true | true |
| DNS Benchmark | `FEATURE_DNS_SCREEN` | true | true |
| Chat IA conversacional (LLMChat) | `FEATURE_DIAGNOSTICO_CHAT` | true | true |
| Scanner dispositivos V2 | `FEATURE_DEVICES_SCREEN_V2` | true | false |
| LinkPulse ativo | `FEATURE_LINKPULSE_ATIVO` | true | false |

> Fonte: `app/build.gradle.kts` bloco `release`, confirmado em 2026-06-21.

Consulte `ANDROID_TECNICO.md` seção 9.1 para a lista completa de flags e seus estados.


## 2. Navegação

O app usa uma `NavigationBar` inferior com **5 abas fixas**. Telas secundárias são sobrepostas sobre as abas — não são abas separadas.

```
NavigationBar (5 abas fixas) — fonte: AppShell.kt / AppNavGraph.kt
├── [0] Início    → HomeScreen
├── [1] Velocidade → SpeedTestScreen
├── [2] Sinal     → SinalScreen
├── [3] Histórico → HistoricoScreen
└── [4] Ajustes   → AjustesScreen

Fluxos sobrepostos (Overlay stack — não são abas)
├── SpeedTest → VelocidadeScreen (execução) → ResultadoVelocidadeScreen
├── ResultadoVelocidade → DiagnosticoScreen
├── DiagnosticoScreen → LLMChatScreen [FEATURE_DIAGNOSTICO_CHAT — ativo em release v0.16.0]
├── AjustesScreen → FibraScreen [FEATURE_FIBRA_SCREEN — ativo]
├── AjustesScreen → LaudoScreen
├── ResultadoVelocidade ou DiagnosticoScreen → DispositivosScreen
└── Privacidade / Novidades (AnimatedVisibility overlay)

Especial
└── OnboardingScreen (apenas primeira execução, quando onboarding_concluido = false)
```

**Onboarding:** `OnboardingScreen` é exibida na primeira execução quando `onboarding_concluido = false` no DataStore. Após conclusão, nunca é exibida novamente.

> Nota de navegação v0.16.0: o `AppNavGraph.kt` define as rotas como `home`, `diagnostico`, `dispositivos`, `historico`, `ajustes`. A NavBar reflete labels: Início / Velocidade / Sinal / Histórico / Ajustes.

### 2.1 ProfileAvatarButton

Presente no `navigationIcon` (lado esquerdo do TopAppBar) de **todas as 5 abas root**: Início, Velocidade, Sinal, Histórico e Ajustes.

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

**Mockup (estado conectado em Wi-Fi, com último resultado):**

```text
╔══════════════════════════════════════╗
║  StatusBar                           ║
╠══════════════════════════════════════╣
║  [Avatar]   Início          [Perfil] ║
║         Conectado em MinhaRede       ║
╠══════════════════════════════════════╣
║                                      ║
║  [Smartphone]──[Roteador]──[Internet]║
║   Pixel 8     192.168.1.1  Claro     ║
║   192.168.1.5    Wi-Fi     177.x.x.x ║
║                                      ║
║  ┌────────────────────────────────┐  ║
║  │ Medições         Última: 15min │  ║
║  │  ↓ 240 Mbps  ↑ 48 Mbps        │  ║
║  │  Download    Upload            │  ║
║  │  ╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌ (gráfico)    │  ║
║  │                    Ver detalhe → │  ║
║  │ [   Medir velocidade agora   ] │  ║
║  └────────────────────────────────┘  ║
║                                      ║
║  ┌──────┐  ┌──────┐  ┌──────────┐   ║
║  │ DNS  │  │ Ping │  │Diagnóst. │   ║
║  └──────┘  └──────┘  └──────────┘   ║
║                                      ║
║  ┌────────────────────────────────┐  ║
║  │ WI-FI · 5 GHZ                 │  ║
║  │ MinhaRede      RSSI -58 dBm    │  ║
║  │ Canal 36 · 867 Mbps  Excelente │  ║
║  │ [WPA2]                         │  ║
║  └────────────────────────────────┘  ║
║                                      ║
╠══════════════════════════════════════╣
║ [Início][Veloc][Sinal][Hist][Ajustes]║
╚══════════════════════════════════════╝
```

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

**Mockup (estado executando — fase download):**

```text
╔══════════════════════════════════════╗
║  StatusBar                           ║
╠══════════════════════════════════════╣
║  [Avatar]   Velocidade               ║
║       Plano contratado: 300 Mbps     ║
╠══════════════════════════════════════╣
║                                      ║
║          ●───────────────●           ║
║         /  Medição 1 de 3 \          ║
║        /                   \         ║
║       ╭──────────────────────╮       ║
║      ╭│       DOWNLOAD       │╮      ║
║      ││                      ││      ║
║      ││       187.4           ││      ║
║      ││       Mbps            ││      ║
║      ╰│                      │╯      ║
║       ╰──────────────────────╯       ║
║                                      ║
║           [Cancelar]                 ║
║           2.3 MB usados              ║
║                                      ║
║  ┌──────────────────────────────┐    ║
║  │ [Rápido] [Completo] [Triplo] │    ║
║  └──────────────────────────────┘    ║
║                                      ║
╠══════════════════════════════════════╣
║ [Início][Veloc][Sinal][Hist][Ajustes]║
╚══════════════════════════════════════╝
```

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

**Mockup (modo Wi-Fi, com permissão de localização concedida):**

```text
╔══════════════════════════════════════╗
║  StatusBar                           ║
╠══════════════════════════════════════╣
║  [Avatar]      Sinal                 ║
╠══════════════════════════════════════╣
║                                      ║
║  [Todas] [2.4GHz] [5GHz] [6GHz]      ║
║                                      ║
║  ┌────────────────────────────────┐  ║
║  │ [Wifi] MinhaRede      Excelente│  ║
║  │        -58 dBm · Canal 36      │  ║
║  │        5 GHz · WPA2            │  ║
║  └────────────────────────────────┘  ║
║  ┌────────────────────────────────┐  ║
║  │ [Wifi] VizinhoA       Bom      │  ║
║  │        -65 dBm · Canal 11      │  ║
║  │        2.4 GHz · WPA2          │  ║
║  └────────────────────────────────┘  ║
║  ┌────────────────────────────────┐  ║
║  │ [Wifi] OutraRede      Regular  │  ║
║  │        -74 dBm · Canal 6       │  ║
║  │        2.4 GHz · WPA3          │  ║
║  └────────────────────────────────┘  ║
║                                      ║
║  ─── Congestionamento de canais ──   ║
║  [WifiChannelGuide — gráfico canais] ║
║                                      ║
╠══════════════════════════════════════╣
║ [Início][Veloc][Sinal][Hist][Ajustes]║
╚══════════════════════════════════════╝
```

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

**Mockup (com histórico, filtros e gráfico):**

```text
╔══════════════════════════════════════╗
║  StatusBar                           ║
╠══════════════════════════════════════╣
║  [Avatar]  Histórico          [Export]║
╠══════════════════════════════════════╣
║                                      ║
║  ● Download  ● Upload (legenda)       ║
║  ┌────────────────────────────────┐  ║
║  │  (gráfico de linha 160dp)      │  ║
║  │   ╭──╮  ╭──────╮              │  ║
║  │──╯    ╰╯        ╰──           │  ║
║  └────────────────────────────────┘  ║
║                                      ║
║  ┌──────────┐ ┌──────────┐          ║
║  │↓ Média   │ │↑ Média   │          ║
║  │ 218 Mbps │ │ 41 Mbps  │          ║
║  └──────────┘ └──────────┘          ║
║                                      ║
║  [Todos] [Wi-Fi] [Rede móvel]        ║
║                                      ║
║  ┌────────────────────────────────┐  ║
║  │[Wifi] Hoje 14:32               │  ║
║  │  ↓ 240  ↑ 48  Latência 12 ms  │  ║
║  └────────────────────────────────┘  ║
║  ┌────────────────────────────────┐  ║
║  │[Wifi] Hoje 10:15               │  ║
║  │  ↓ 195  ↑ 42  Latência 18 ms  │  ║
║  └────────────────────────────────┘  ║
║                                      ║
╠══════════════════════════════════════╣
║ [Início][Veloc][Sinal][Hist][Ajustes]║
╚══════════════════════════════════════╝
```

**O que o usuário vê:**
- Gráfico de histórico (uptime)
- Uptime narrative: texto gerado pelo `UptimeNarrativaEngine`
- Resumo de medições

**Ações disponíveis:** visualizar histórico de medições passadas

### 5.5 AjustesScreen — Aba 4 (Ajustes)

**Composable:** `AjustesScreen.kt`

Aba de configurações. Ícone: `Settings` (outlined/filled). É a entrada direta para `AjustesScreen`. `DispositivosScreen` e telas pós-MVP são acessíveis como overlays/fluxos a partir de outros pontos de entrada, não como abas separadas.

Ver seção 6.5 para detalhamento completo da `AjustesScreen`.

---

## 6. Telas Secundárias (Fluxos Sobrepostos e Acesso via "Mais")

### 6.1 VelocidadeScreen — Execução do Teste

**Composable:** `VelocidadeScreen.kt`

**Mockup (fase upload em execução):**

```text
╔══════════════════════════════════════╗
║  StatusBar                           ║
╠══════════════════════════════════════╣
║              Medindo…                ║
╠══════════════════════════════════════╣
║                                      ║
║     São Paulo, SP · Claro Fibra      ║
║                                      ║
║          ╭────────────╮              ║
║        ╭─│   UPLOAD   │─╮            ║
║       ╭  │            │  ╮           ║
║       │  │   63.2      │  │           ║
║       │  │   Mbps      │  │           ║
║       ╰  │            │  ╯           ║
║        ╰─│            │─╯            ║
║          ╰────────────╯              ║
║                                      ║
║     ↓ 241.3 Mbps  download concluído ║
║                                      ║
║  ┌────────────────────────────────┐  ║
║  │  (MiniGrafico ao vivo — UL)    │  ║
║  └────────────────────────────────┘  ║
║                                      ║
║   [LATÊNCIA✓] [DOWN✓] [UP ●]         ║
║   Medindo a velocidade de upload…    ║
║                                      ║
║            [Cancelar]                ║
║                                      ║
╚══════════════════════════════════════╝
```

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

**Mockup (resultado completo com diagnóstico):**

```text
╔══════════════════════════════════════╗
║  StatusBar                           ║
╠══════════════════════════════════════╣
║  [←]    Resultado do teste   [Share] ║
╠══════════════════════════════════════╣
║                                      ║
║   Sua internet está funcionando bem  ║
║   Velocidade dentro do esperado      ║
║   [WI-FI · 5 GHz]                    ║
║                                      ║
║  ┌──────────────┐ ┌──────────────┐   ║
║  │  Download    │ │  Upload      │   ║
║  │  240.3 Mbps  │ │  48.1 Mbps   │   ║
║  └──────────────┘ └──────────────┘   ║
║  ┌──────────────┐ ┌──────────────┐   ║
║  │  Latência    │ │  Oscilação   │   ║
║  │   12 ms      │ │    4 ms      │   ║
║  └──────────────┘ └──────────────┘   ║
║  ┌──────────────┐ ┌──────────────┐   ║
║  │  Perda       │ │  Bufferbloat │   ║
║  │  0.0 %       │ │   8 ms       │   ║
║  └──────────────┘ └──────────────┘   ║
║                                      ║
║  EXPERIÊNCIA DE USO                  ║
║  ┌────────────────────────────────┐  ║
║  │ [Tv]    Streaming      Bom     │  ║
║  │─────────────────────────────── │  ║
║  │ [Game]  Gaming         Bom     │  ║
║  │─────────────────────────────── │  ║
║  │ [Cam]   Vídeo Chamada  Bom     │  ║
║  └────────────────────────────────┘  ║
║                                      ║
║  DNS: Cloudflare · 8 ms              ║
║  [Detalhes avançados              ▼] ║
║                                      ║
║  [   Testar novamente   ]            ║
║  [   Ir para o início   ]            ║
║                                      ║
╚══════════════════════════════════════╝
```

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

**Botões:** "Conversar com IA" (exibido quando `FEATURE_DIAGNOSTICO_CHAT` ativo — ativo em release v0.16.0), "Testar Upload Novamente", "Ir para o início", "Testar novamente".

**Parâmetros recebidos:** `resultado`, `snapshotDiagnostico`, `ispInfo: IspInfo?`, `onTestarNovamente`, `onIrParaHome`, `onAbrirChat`, `gemmaAvailable`.

### 6.3 DiagnosticoScreen — Diagnóstico IA com Laudo

**Composable:** `DiagnosticoScreen.kt`

**Mockup — estado de setup (seleção do que analisar):**

```text
╔══════════════════════════════════════╗
║  StatusBar                           ║
╠══════════════════════════════════════╣
║  [←]       Diagnóstico IA            ║
╠══════════════════════════════════════╣
║                                      ║
║  [✨] A IA lê os sinais da sua       ║
║       conexão e entrega diagnóstico. ║
║       Sem conversa: você escolhe     ║
║       o que medir, ela interpreta.   ║
║                                      ║
║  O QUE ANALISAR                      ║
║                                      ║
║  ┌────────────────────────────────┐  ║
║  │[▦] Velocidade          [ON ●] │  ║
║  │    Download, upload e estab.   │  ║
║  └────────────────────────────────┘  ║
║  ┌────────────────────────────────┐  ║
║  │[Wifi] Wi-Fi & Sinal    [ON ●] │  ║
║  │    Potência, canal, congest.   │  ║
║  └────────────────────────────────┘  ║
║  ┌────────────────────────────────┐  ║
║  │[↕] Latência & Bufferbloat [ON]│  ║
║  └────────────────────────────────┘  ║
║  ┌────────────────────────────────┐  ║
║  │[Tower] Modem / Fibra (GPON)   │  ║
║  └────────────────────────────────┘  ║
║  ┌────────────────────────────────┐  ║
║  │[Globe] DNS                    │  ║
║  └────────────────────────────────┘  ║
║                                      ║
╠══════════════════════════════════════╣
║  [✨  Diagnosticar conexão       ]   ║
║       Processado no dispositivo      ║
╚══════════════════════════════════════╝
```

**Mockup — estado de resultado (laudo IA):**

```text
╔══════════════════════════════════════╗
║  StatusBar                           ║
╠══════════════════════════════════════╣
║  [←]       Diagnóstico IA   [Share]  ║
╠══════════════════════════════════════╣
║                                      ║
║  ┌────────────────────────────────┐  ║
║  │ DIAGNÓSTICO IA      BOM        │  ║
║  │ Sua conexão está performando   │  ║
║  │ dentro do esperado para o      │  ║
║  │ plano contratado.              │  ║
║  │ Confiança Alta · claude-3-5    │  ║
║  └────────────────────────────────┘  ║
║                                      ║
║  CAUSA-RAIZ IDENTIFICADA             ║
║  ┌────────────────────────────────┐  ║
║  │ [Wifi] Wi-Fi                   │  ║
║  │ Canal 36, sem congestionamento │  ║
║  └────────────────────────────────┘  ║
║                                      ║
║  ┌────────────────────────────────┐  ║
║  │ Impacto no usuário             │  ║
║  │ Streaming HD: OK               │  ║
║  │ Gaming: OK                     │  ║
║  └────────────────────────────────┘  ║
║                                      ║
║  O QUE FAZER · EM ORDEM              ║
║  ┌────────────────────────────────┐  ║
║  │ 1. Manter canal atual          │  ║
║  │    Baixa prioridade            │  ║
║  └────────────────────────────────┘  ║
║                                      ║
║  [Métricas da análise           ▼]   ║
║                                      ║
╠══════════════════════════════════════╣
║ [Tirar dúvidas] [Refazer] [Operadora]║
╚══════════════════════════════════════╝
```

**Trigger:** a partir de `ResultadoVelocidadeScreen` ou via `ExploreToolsRow` no SpeedTest.

**O que o usuário vê (v0.14.0+):**
- Laudo gerado por IA com análise da conexão
- Cards dinâmicos de resultado por engine: ícone, `status badge` (OK / INFO / ATTENTION / CRITICAL), mensagem e recomendação
- Footer com 3 ações: "Tirar dúvidas" (abre `LLMChatScreen`), "Refazer teste", "Falar com a operadora"
- Timeout visual com mensagem "Conectando…" durante chamada à IA (v0.14.4)
- UI de retry quando timeout é atingido (v0.14.4)

**Estados:** Idle / Executando (loader) / Conectando (timeout visual) / Concluído / Timeout (retry)

**Contexto enviado à IA:** tipo de conexão real (`wifi`, `movel`, `ethernet`). Bug de enviar `wifi` fixo foi corrigido na v0.8.1.

**Parâmetros recebidos:** `snapshotDiagnostico`, `resultado`, callbacks para iniciar, selecionar chips, enviar contexto, abrir chat, refazer teste, contato operadora.

### 6.4 LLMChatScreen — Chat IA com Streaming

**Composable:** `LLMChatScreen.kt`

**Mockup (conversa em andamento com thinking expansível):**

```text
╔══════════════════════════════════════╗
║  StatusBar                           ║
╠══════════════════════════════════════╣
║  [←]          SignallQ          [Novo]  ║
║           ● Assistente de conexão    ║
╠══════════════════════════════════════╣
║                                      ║
║                 ┌──────────────────┐ ║
║                 │ Minha velocidade │ ║
║                 │ caiu ontem?      │ ║
║                 └──────────────────┘ ║
║  ┌──────────────────────────────┐    ║
║  │ [Thinking ▼]                 │    ║
║  │ Analisando os dados de       │    ║
║  │ velocidade do histórico…     │    ║
║  └──────────────────────────────┘    ║
║  ┌──────────────────────────────┐    ║
║  │ Sim, identifiquei uma queda  │    ║
║  │ ontem às 19h. Sua velocidade │    ║
║  │ caiu de 240 para 38 Mbps.    │    ║
║  │ Isso coincide com horário de │    ║
║  │ pico da sua operadora.       │    ║
║  └──────────────────────────────┘    ║
║                 ┌──────────────────┐ ║
║                 │ O que eu faço?   │ ║
║                 └──────────────────┘ ║
║  ┌──────────────────────────────┐    ║
║  │ [●●●  digitando…]            │    ║
║  └──────────────────────────────┘    ║
║                                      ║
║  [Chip: Testar agora] [Chip: Ajuda]  ║
╠══════════════════════════════════════╣
║  ┌──────────────────────────┐ [Send] ║
║  │ Digite sua pergunta…     │        ║
║  └──────────────────────────┘        ║
╚══════════════════════════════════════╝
```

**Flag de controle:** `FEATURE_DIAGNOSTICO_CHAT` — **ativa em release v0.16.0** (promovida para release no build.gradle.kts). Visível para o usuário final.

**Trigger:** botão "Tirar dúvidas" no footer da `DiagnosticoScreen` (a partir de v0.14.0) ou botão "Conversar com IA" em `ResultadoVelocidadeScreen`.

**O que o usuário vê (v0.14.x+):**
- Bolhas de mensagem do usuário e da IA
- Seção "Thinking" expansível com animação — exibe tokens de raciocínio da IA quando disponíveis
- Nome e ícone do modelo de IA no rodapé
- Operadoras com logo (banco interno de logos por nome de ISP)
- Follow-up reutiliza contexto da conversa anterior

**API da IA:** Worker Cloudflare — retorna texto puro (não streaming JSON).

**Estados da sessão:** Idle / Thinking / AwaitingInput / Error / Timeout (com UI de retry)

**Sessão persistida:** `chat_sessions` + `chat_messages` no Room (v10). Cota diária rolling 24h via `CotaDiariaRepository`.

**Ações disponíveis:**
- Enviar mensagem de texto livre
- Expandir/recolher seção "Thinking"
- Retomar conversa (follow-up com contexto)
- Voltar para tela anterior

### 6.4-legacy ChatDiagnosticoIaScreen (v0.12.0)

**Composable:** `ChatDiagnosticoIaScreen.kt`

Introduzido na v0.12.0 como Chat IA com drawer, chips iniciais e cota diária. Substituído funcionalmente pelo `LLMChatScreen` na v0.14.0 (redesign completo). A flag `FEATURE_DIAGNOSTICO_CHAT` controla ambos os fluxos.

**3 fluxos de diagnóstico suportados:** diagnóstico completo, ping-only, contexto de rede sem speedtest.

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

**Mockup (lista de dispositivos detectados):**

```text
╔══════════════════════════════════════╗
║  StatusBar                           ║
╠══════════════════════════════════════╣
║        [Devices] Dispositivos        ║
╠══════════════════════════════════════╣
║                                      ║
║  ─── GATEWAY ───                     ║
║  ┌────────────────────────────────┐  ║
║  │ [Router] TP-Link AC1750        │  ║
║  │          192.168.1.1           │  ║
║  │          AA:BB:CC:DD:EE:FF     │  ║
║  └────────────────────────────────┘  ║
║                                      ║
║  ─── DISPOSITIVOS (4) ───            ║
║  ┌────────────────────────────────┐  ║
║  │ [Phone] Pixel 8 (este dispos.) │  ║
║  │         192.168.1.5            │  ║
║  └────────────────────────────────┘  ║
║  ┌────────────────────────────────┐  ║
║  │ [Laptop] MacBook Pro           │  ║
║  │          192.168.1.10          │  ║
║  │          Apple Inc.            │  ║
║  └────────────────────────────────┘  ║
║  ┌────────────────────────────────┐  ║
║  │ [Bulb] Smart TV (apelido)      │  ║
║  │        192.168.1.22            │  ║
║  │        Samsung Electronics     │  ║
║  └────────────────────────────────┘  ║
║  ┌────────────────────────────────┐  ║
║  │ [Device] Dispositivo           │  ║
║  │          192.168.1.31          │  ║
║  └────────────────────────────────┘  ║
║                                      ║
╚══════════════════════════════════════╝
```

**Acesso:** overlay empilhado sobre as abas — não é uma aba da NavBar. Acessível via `AjustesScreen` ou outros pontos de navegação internos.

**O que o usuário vê:**
- `OfflineBanner` no topo quando `!conectado`
- Lista de dispositivos detectados na rede local
- Por dispositivo: nome/apelido, IP, MAC, fabricante (OUI), tipo, serviços mDNS

**Ações disponíveis:**
- Dar apelido a um dispositivo (salvo na tabela `apelido_dispositivo` do Room)
- Atualizar lista (refresh)

**Estados visuais:** Loading / Lista com dispositivos / Vazio / Offline / Erro

### 6.7 FibraScreen — Modem GPON

**Composable:** `FibraModemScreen.kt`

**Mockup (estado concluído com dados GPON):**

```text
╔══════════════════════════════════════╗
║  StatusBar                           ║
╠══════════════════════════════════════╣
║ [←]  Sua internet por fibra  [Atual] ║
║      Modem conectado pela operadora  ║
╠══════════════════════════════════════╣
║                                      ║
║  ─── STATUS GPON ───                 ║
║  ┌────────────────────────────────┐  ║
║  │ [Signal] Potência Rx           │  ║
║  │          -18.5 dBm  Excelente  │  ║
║  │          Ideal: -8 a -27 dBm   │  ║
║  └────────────────────────────────┘  ║
║  ┌────────────────────────────────┐  ║
║  │ [Signal] Potência Tx           │  ║
║  │          2.3 dBm               │  ║
║  └────────────────────────────────┘  ║
║  ┌──────────────┐ ┌──────────────┐   ║
║  │ Temperatura  │ │ Corrente     │   ║
║  │ 42°C         │ │ 6.8 mA       │   ║
║  └──────────────┘ └──────────────┘   ║
║                                      ║
║  ─── STATUS WAN ───                  ║
║  ┌────────────────────────────────┐  ║
║  │ IP: 177.xxx.xxx.xxx            │  ║
║  │ Máscara: 255.255.255.0         │  ║
║  │ Gateway: 192.168.1.1           │  ║
║  └────────────────────────────────┘  ║
║                                      ║
║  ─── DISPOSITIVO ───                 ║
║  Nokia G-010G-P · SN: GPON12345      ║
║                                      ║
╚══════════════════════════════════════╝
```

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

### 6.8 SignallQScreen — Chat IA Diagnóstico (ex-OrbitScreen)

**Composable:** `SignallQScreen.kt`

`OrbitScreen.kt` foi renomeada para `SignallQScreen.kt` durante o rebranding (v0.15.0+). É o ponto de entrada da experiência de IA diagnóstica conversacional autônoma do SignallQ.

**O que faz:** exibe o símbolo animado SignallQ e gerencia o fluxo de chat diagnóstico com estados `Idle`, `Collecting`, `Thinking`, `Analyzing`, `AwaitingChipSelection`, `AwaitingAnswer`, `Result`, `Erro`. Componentes internos renomeados de `Orbit*` para `SignallQ*` (ex.: `SignallQTopBar`, `SignallQInputArea`, `SignallQUserMessageBubble`, `SignallQAiMessageBubble`, `SignallQThinkingBubble`, `SignallQWelcomeState`, `SignallQInlineQuestion`).

**Distinção com `LLMChatScreen`:** `SignallQScreen` é o chat diagnóstico autônomo com chips contextuais e pipeline de engines (`SignallQOrchestrator`). `LLMChatScreen` é o chat livre controlado por `FEATURE_DIAGNOSTICO_CHAT`. Ambos coexistem em v0.16.0.

### 6.9 LinkaPulseScreen

**Composable:** `LinkaPulseScreen.kt`

**Flag de controle:** `FEATURE_LINKPULSE_ATIVO` — **inativo em release v0.16.0**. Visível apenas em builds debug.

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
   └── [opcional] "Conversar com IA" → LLMChatScreen [FEATURE_DIAGNOSTICO_CHAT — ativo em release v0.16.0]
```

**Resultado salvo automaticamente em Room** (`MedicaoEntity`) com todos os campos medidos.

---

## 9. Fluxo Principal: Diagnóstico

```
1. SpeedTestScreen → ExploreToolsRow → DiagnosticoScreen
   OU ResultadoVelocidadeScreen → DiagnosticoScreen
2. DiagnosticOrchestrator executa engines em sequência
3. DiagnosticDecisionEngine consolida resultado final
4. DiagnosticoScreen exibe laudo + cards por engine (OK/INFO/ATTENTION/CRITICAL)
5. [opcional] LLMChatScreen para diálogo de refinamento com IA (via "Tirar dúvidas")
```

---

## 10. Fluxo Principal: Chat IA

> **Disponibilidade:** `FEATURE_DIAGNOSTICO_CHAT` — **ativo em release v0.16.0**. Este fluxo está visível para o usuário final em produção.

```
1. LLMChatScreen iniciada via:
   - Botão "Tirar dúvidas" no footer da DiagnosticoScreen (v0.14.0+)
   - Botão "Conversar com IA" em ResultadoVelocidadeScreen
2. Contexto da rede + resultado do diagnóstico enviado ao Worker Cloudflare
3. LLMChatScreen exibe resposta em streaming
   ├── Seção "Thinking" expansível (tokens de raciocínio visíveis)
   └── Operadora com logo quando identificada
4. Usuário continua a conversa (follow-up reutiliza contexto)
5. Sessão persistida em Room (chat_sessions + chat_messages)
```

> **Fluxo legado (v0.12.0–v0.13.x):** `ChatDiagnosticoIaScreen` com drawer e chips iniciais. Substituído pelo redesign LLMChat na v0.14.0.

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

## 13. Features Dependentes de APIs Nativas Android

| Feature | Dependência Android |
|---|---|
| Scan de redes Wi-Fi (SSID, RSSI, canal) | `WifiManager` |
| Scan de dispositivos na rede | ARP + mDNS nativo |
| Monitoramento passivo em background | `WorkManager` |
| Leitura de dados da ONT GPON | HTTP local ao modem |
| Sinal de dados móveis (RSRP, RSRQ, SINR) | `TelephonyManager` |
| Detecção 5G NSA via DisplayInfo / SignalStrength | `DisplayInfo`, `SignalStrength` |
| Gráfico de uptime com medições passivas | Histórico do monitor |
| Notificações de alerta de rede | `NotificationManager` + background |
| Permissões contextuais (localização, telefonia) | Runtime permissions Android 6+ |
| Detecção automática de offline | `ConnectivityManager.NetworkCallback` |

---

## 14. Features Entregues desde v0.8.1

Resumo das principais entregas por versão. Ver `docs_ai/RELEASES.md` para histórico completo.

| Versão | Feature |
|---|---|
| v0.9.0 | PingScreen (20 amostras ICMP sobre HTTP/2), ExploreToolsRow (grid visível), DNS BR (Registro.br + CETIC.br) |
| v0.11.x | Fibra avançada (tela de análise do modem/ONT), DNS benchmark completo, Dispositivos: mascarar MAC, Onboarding com checkbox de termos e cards de permissão, estados vazios humanizados |
| v0.12.0 | ChatDiagnosticoIaScreen — Chat IA com drawer, chips, cota diária rolling 24h, streaming, Room v10 |
| v0.13.0 | Redesign mockup v2 (Home, Sinal, SpeedTest, Fibra, Laudo), TopBar contextual com SSID/operadora, chip segurança Wi-Fi, card rede móvel dual SIM, mini-cards na Home, seletor Android/Roteador |
| v0.13.x | 5G NSA via DisplayInfo + fallback SignalStrength, sheet de rede móvel redesenhada, SSID Android 12+, IP público Wi-Fi→Móvel, ipapi.co (HTTPS) substituindo ip-api.com |
| v0.14.0 | Redesign Diagnóstico IA: fluxo laudo + LLMChat, footer "Tirar dúvidas / Refazer / Operadora", operadoras com logo |
| v0.14.2 | Botão IA, sheet operadora e "Refazer teste" em ResultadoVelocidadeScreen |
| v0.14.4 | Timeout visual "Conectando…" + UI retry no Diagnóstico IA, LLMChatScreen insets/TopBar corretos, thinking expansível |
| v0.15.0 | Rebranding Veloo → SignallQ, package name `io.veloo.app` (técnico), identidade visual, OrbitScreen→SignallQScreen, Orbit*→SignallQ* componentes |
| v0.16.0 | versionCode 46; FEATURE_DIAGNOSTICO_CHAT promovida para release; DEVICES_SCREEN_V2 permanece off em release; LINKPULSE_ATIVO off em release |
