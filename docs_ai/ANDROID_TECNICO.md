# Documentação Técnica — Android Linka

**Público-alvo:** Desenvolvedor humano e agentes de IA
**Plataforma:** Android exclusivo — Kotlin, Jetpack Compose, Material Design 3
**Última atualização:** 2026-05-19 (v0.8.1 — thresholds Wi-Fi por banda, DNS-03, FibraScreen loading dinâmico + hint RX, DiagnosticoScreen tipo conexão real, semântica TalkBack)
**Mantido por:** Taisa

> Este documento descreve a arquitetura interna, módulos, camadas de dados, engines de diagnóstico e contratos do app Android Linka.
> Fonte de verdade: dados coletados do código real (Marcelo, 2026-05-17).
> Para funcionalidades da perspectiva do usuário, consulte `ANDROID_FUNCIONAL.md`.

---

## 1. Stack e Versões

| Tecnologia | Versão | Função |
|---|---|---|
| Kotlin | 2.2.20 | Linguagem principal |
| Jetpack Compose | plugin 2.2.20 | UI declarativa |
| Material Design 3 | — | Sistema de design |
| Room | kapt | Persistência local (SQLite) |
| DataStore Preferences | — | Preferências do usuário |
| Kotlin Coroutines | — | Operações assíncronas |
| OkHttp | — | HTTP (speedtest, IA, fibra) |
| Android Gradle Plugin | 8.11.1 | Build system |
| compileSdk | 35 | SDK de compilação |
| minSdk | 28 | Android 9 (Pie) |
| targetSdk | 35 | Target SDK |
| JVM target | 17 | Bytecode Java |

**Plugins do módulo `:app`:** `com.android.application 8.11.1`, `kotlin.android 2.2.20`, `kotlin.plugin.compose 2.2.20`, `kapt 2.2.20`

**Injeção de dependência:** manual por construtor. Sem Hilt, sem Koin. Cada módulo expõe um objeto `*Modulo.kt` com funções fábrica estáticas.

---

## 2. Módulos (15 total)

Declarados em `settings.gradle.kts`. O número correto é **15** — o CLAUDE.md do workspace mencionava 16 incorretamente.

| Módulo | Namespace | Camada | Plugins | Dependências notáveis | Responsabilidade |
|---|---|---|---|---|---|
| `:app` | io.linka.app.kotlin | app | android-application, kotlin-android, kotlin.plugin.compose, kapt | todos os core* e feature*, compose, lifecycle, material3 | Entry point, MainActivity, MainViewModel, AppShell, telas, orchestrators, componentes UI globais |
| `:coreNetwork` | io.linka.app.kotlin.core.network | core | android-library, kotlin-android | androidx.core.ktx, kotlinx.coroutines | Monitor de conectividade (ConnectivityManager.NetworkCallback), SnapshotRede, WifiLinkSnapshot, medição de RTT do gateway |
| `:coreDatabase` | io.linka.app.kotlin.core.database | core | android-library, kotlin-android, kapt | androidx.room.runtime, androidx.room.ktx | Persistência SQLite via Room — LinkaDatabase, DAOs, entidades |
| `:coreDatastore` | io.linka.app.kotlin.core.datastore | core | android-library, kotlin-android | androidx.datastore.preferences, kotlinx.coroutines | Preferências do usuário via DataStore — PreferenciasAppRepository |
| `:corePermissions` | io.linka.app.kotlin.core.permissions | core | android-library, kotlin-android | — | Gerenciamento de permissões de rede em runtime |
| `:coreTelephony` | io.linka.app.kotlin.core.telephony | core | android-library, kotlin-android | — | Monitoramento de rede móvel via TelephonyManager — MovelSnapshot |
| `:featureHome` | io.linka.app.kotlin.feature.home | feature | android-library, kotlin-android | — | Módulo mínimo — FeatureHomeModulo.kt; HomeScreen reside em :app |
| `:featureWifi` | io.linka.app.kotlin.feature.wifi | feature | android-library, kotlin-android | :coreNetwork | Scan de redes Wi-Fi, TopologiaWifiEngine, MontarResumoWifiUseCase, MeshOuiDatabase |
| `:featureDevices` | io.linka.app.kotlin.feature.devices | feature | android-library, kotlin-android | — | Descoberta de dispositivos (ARP, mDNS, port scan), OuiDatabase, ClassificadorDispositivoRede |
| `:featureDns` | io.linka.app.kotlin.feature.dns | feature | android-library, kotlin-android | — | Benchmark de DNS via DoH — BenchmarkDnsDoh, AvaliadorCoerenciaDns |
| `:featureSpeedtest` | io.linka.app.kotlin.feature.speedtest | feature | android-library, kotlin-android | okhttp | Execução do speedtest — ExecutorSpeedtest, SnapshotExecucaoSpeedtest, ResultadoSpeedtest |
| `:featureDiagnostico` | io.linka.app.kotlin.feature.diagnostico | feature | android-library, kotlin-android | :featureFibra, okhttp, org.json:json | Engines de diagnóstico local, DiagnosticOrchestrator, integração IA, Orbit/Chat |
| `:featureFibra` | io.linka.app.kotlin.feature.fibra | feature | android-library, kotlin-android | — | Leitura de dados da ONT GPON Nokia — NokiaModemClient, ClassificadorSaudeGpon |
| `:featureHistory` | io.linka.app.kotlin.feature.history | feature | android-library, kotlin-android | — | Histórico de medições, UptimeChartUseCase, UptimeNarrativaEngine, exportação CSV/PDF |
| `:featureSettings` | io.linka.app.kotlin.feature.settings | feature | android-library, kotlin-android | — | Módulo mínimo — FeatureSettingsModulo.kt; AjustesScreen reside em :app |

---

## 3. Arquitetura MVVM

### 3.1 Camadas

```
UI (Composables — telas e componentes)
    ↑ StateFlow.collectAsStateWithLifecycle()
MainViewModel (AndroidViewModel — único ViewModel raiz)
    ↑ lazy singletons instanciados manualmente
Serviços / Repositórios / Engines / Use Cases
    ↑ Room / DataStore / APIs Android / OkHttp
```

**Fluxo unidirecional de dados:** evento da UI → função no ViewModel → atualiza StateFlow → recomposição da UI.

**Injeção:** cada serviço é instanciado como `lazy` dentro do `MainViewModel`. Não há Hilt nem Koin.

### 3.2 Entry Points

| Arquivo | Papel |
|---|---|
| `LinkaApplication.kt` | Application — inicialização do app |
| `MainActivity.kt` | Activity única — `setContent { LinkaTheme { AppShell(...) } }` |
| `MainViewModel.kt` | ViewModel raiz — instancia todos os serviços como lazy singletons |
| `AppShell.kt` | Shell do app — NavigationBar de 5 abas, fluxos secundários sobrepostos |
| `LinkaTheme.kt` | Tema MD3 com ColorScheme customizado |

### 3.3 MainViewModel — Lazy Singletons

Instâncias criadas sob demanda (lazy) dentro do MainViewModel:

| Singleton | Tipo | Observação |
|---|---|---|
| `bancoDados` | LinkaDatabase | Room database |
| `preferenciasAppRepository` | PreferenciasAppRepository | DataStore |
| `monitorRede` | MonitorRede | ConnectivityManager.NetworkCallback |
| `gerenciadorPermissoes` | GerenciadorPermissoesRede | Permissões de rede |
| `scannerDispositivos` | ScannerDispositivos | ARP + mDNS |
| `benchmarkDns` | BenchmarkDns | DoH benchmark |
| `executorSpeedtest` | ExecutorSpeedtest | Teste de velocidade |
| `scannerRedesWifi` | ScannerRedesWifi | Scan de Wi-Fi |
| `diagnosticOrchestrator` | DiagnosticOrchestrator | Engines de diagnóstico |
| `executorFibra` | ExecutorFibra | Leitura GPON |
| `monitorTelephony` | — | Só ativa em móvel + permissão concedida |
| `orbitOrchestrator` | OrbitOrchestrator | Fluxo Orbit/Chat |

### 3.4 MainViewModel — StateFlows expostos à UI

| StateFlow | Tipo | Descrição |
|---|---|---|
| `snapshotRede` | SnapshotRede | Estado da rede em tempo real |
| `snapshotSpeedtest` | SnapshotExecucaoSpeedtest | Estado do speedtest |
| `snapshotDns` | SnapshotBenchmarkDns | Estado do benchmark DNS |
| `snapshotDevices` | SnapshotScanDispositivos | Estado do scan de dispositivos |
| `snapshotWifi` | SnapshotScanWifi | Estado do scan Wi-Fi |
| `snapshotFibra` | SnapshotFibra | Estado da leitura do modem |
| `snapshotDiagnostico` | — | Estado do diagnóstico |
| `movelSnapshot` | MovelSnapshot | Dados de sinal móvel |
| `orbitUiStateFlow` | OrbitUiState | Estado da sessão Orbit |
| `apelidos` | Map<String, String?> | Apelidos de dispositivos por MAC |
| `onboardingConcluido` | Boolean | Se onboarding foi completado |
| `gemmaAvailable` | Boolean | Se Gemma local está disponível |
| `resumoHistorico` | ResumoHistorico | Resumo agregado do histórico |
| `localIp` | String? | IP local do dispositivo |
| `publicIp` | String? | IP público |
| `ispInfo` | IspInfo? | Dados do ISP |
| `gateways` | List | Gateways detectados |
| `history` | List | Histórico para gráfico |
| `historico` | List | Lista completa de medições |
| `localizacaoServidor` | String? | Localização do servidor de speedtest |
| `blocoUptime` | — | Bloco de uptime para gráfico |
| `narrativaUptime` | String? | Narrativa textual de uptime |

### 3.5 MainViewModel — Métodos principais

| Método | Descrição |
|---|---|
| `reiniciarSuite(modo)` | Inicia o teste de velocidade no modo especificado |
| `cancelar()` | Cancela o teste em andamento |
| `dispararBenchmarkDns()` | Inicia benchmark de DNS |
| `refreshDispositivos()` | Atualiza scan de dispositivos |
| `refreshSinal()` | Atualiza scan de redes Wi-Fi |
| `reconectarFibra()` | Reconecta ao modem GPON |
| `iniciarDiagnostico()` | Executa diagnóstico local |
| `iniciarMonitorTelefoniaSeMovel()` | Ativa monitor de telefonia (condicional) |
| `atualizarMonitoramento(ativo)` | Liga/desliga monitoramento passivo |
| `verificarDisponibilidadeGemma()` | Verifica se Gemma local está disponível |
| `marcarOnboardingConcluido()` | Marca onboarding como concluído no DataStore |
| `confirmarIspDetectado(operadora: String)` | Salva o ISP detectado como `operadora` e define `ispConfirmado = true` no DataStore (AJ-B) |
| `dispensarBannerIsp()` | Define `ispConfirmado = true` sem salvar operadora — o banner some sem persistir o ISP detectado (AJ-B) |
| `salvarEstadoCidade(estadoUf: String, cidadeNome: String)` | Persiste UF e município selecionados no DataStore (AJ-C) |
| `salvarUltimaVersaoVista(versao: String)` | Persiste a versão do changelog mais recente vista pelo usuário no DataStore (AJ-F) |

---

## 4. Room — Persistência Local

**Banco:** `LinkaDatabase` — módulo `:coreDatabase`

### 4.1 MedicaoEntity (tabela: `medicao`)

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | String (PK) | UUID único |
| `timestampEpochMs` | Long | Timestamp da medição em epoch ms |
| `connectionType` | String | Tipo: `wifi`, `movel`, `ethernet`, `monitor` |
| `contaminado` | Boolean | Teste descartado/contaminado |
| `speedtestMode` | String? | Modo: `complete`, `ping_only` |
| `specVersion` | String? | Versão do spec do speedtest |
| `downloadMbps` | Double? | Download em Mbps (null em medições do monitor) |
| `uploadMbps` | Double? | Upload em Mbps (null em medições do monitor) |
| `latencyMs` | Double? | Latência em ms |
| `jitterMs` | Double? | Jitter em ms |
| `perdaPercentual` | Double? | Perda de pacotes em % |
| `bufferbloatMs` | Double? | Bufferbloat em ms |
| `packetLossSource` | String? | Fonte da perda: `"download"` ou `"upload"` |
| `vereditoStreaming` | String? | Veredito: `"good"`, `"acceptable"` ou `"poor"` |
| `vereditoGamer` | String? | Veredito: `"good"`, `"acceptable"` ou `"poor"` |
| `vereditoVideoChamada` | String? | Veredito: `"good"`, `"acceptable"` ou `"poor"` |
| `gargaloPrimario` | String? | Gargalo identificado |
| `fonte` | String? | `"web"`, `"android"` ou `"pwa"` |

> Medições com `connectionType = "monitor"` têm `downloadMbps` e `uploadMbps` nulos — o monitoramento passivo só mede latência.

### 4.2 ApelidoDispositivoEntity (tabela: `apelido_dispositivo`)

| Campo | Tipo | Descrição |
|---|---|---|
| `mac` | String (PK) | Endereço MAC do dispositivo |
| `apelido` | String? | Nome definido pelo usuário. `null` = registrado sem apelido (suprime notificação de novo dispositivo) |

---

## 5. DataStore — Preferências do Usuário

**Arquivo:** `PreferenciasAppRepository.kt` — módulo `:coreDatastore`

### Boolean

| Chave | Padrão | Descrição |
|---|---|---|
| `monitoramentoAtivo` | false | Monitoramento passivo em background |
| `modemPermanecerConectado` | false | Manter sessão ativa no modem GPON |
| `analiseAvancada` | false | Modo de análise técnica avançada |
| `onboarding_concluido` | false | Se o onboarding foi completado |
| `alerta_latencia_ativo` | false | Estado de histerese — latência |
| `alerta_dns_ativo` | false | Estado de histerese — DNS |
| `alerta_rssi_ativo` | false | Estado de histerese — RSSI |
| `alerta_sem_internet_ativo` | false | Estado de histerese — sem internet |
| `notificacao_latencia_ativa` | — | Notificações de latência habilitadas |
| `notificacao_dns_ativa` | — | Notificações de DNS lento habilitadas |
| `notificacao_rssi_ativa` | — | Notificações de RSSI fraco habilitadas |
| `notificacao_sem_internet_ativa` | — | Notificações de sem internet habilitadas |

### Boolean

Chaves adicionadas no bloco AJ-B:

| Chave | Padrão | Descrição |
|---|---|---|
| `ispConfirmado` | false | Se o usuário já confirmou ou dispensou o banner de ISP detectado |

### String

| Chave | Padrão | Descrição |
|---|---|---|
| `modemHost` | — | IP/hostname do modem GPON |
| `modemUsername` | `"userAdmin"` | Usuário do modem |
| `modemPassword` | `""` | Senha do modem |
| `temaSelecionado` | `"sistema"` | Tema: `"sistema"`, `"claro"` ou `"escuro"` |
| `nomeUsuario` | — | Nome do perfil do usuário |
| `fotoUriUsuario` | — | URI da foto de perfil |
| `operadora` | — | Operadora do usuário (ISP/provedor) |
| `operadoraMovel` | — | Operadora de dados móveis do usuário (adicionada em AJ-B) |
| `planoInternet` | — | Plano contratado em Mbps — apenas dígitos, máx 4 chars, ex.: "300", "1000" (reformatado em AJ-D) |
| `estadoUf` | — | UF do estado do usuário — sigla de 2 letras, ex.: "SP" (substituiu `regiao` em AJ-C) |
| `cidadeNome` | — | Nome do município do usuário — fonte IBGE API (adicionada em AJ-C) |
| `ultimaVersaoVista` | — | Versão do changelog mais recente vista pelo usuário, ex.: "0.7.0" (adicionada em AJ-F) |

> `regiao` (String livre) foi substituída pelas chaves estruturadas `estadoUf` + `cidadeNome` no bloco AJ-C. Valores legados de `regiao` não são migrados automaticamente.

### Int

| Chave | Padrão | Descrição |
|---|---|---|
| `limiteAlertaMbps` | 0 | Limite de velocidade para alerta de velocidade |

### Long

| Chave | Descrição |
|---|---|
| `ultimaVerificacaoMonitoramento` | Timestamp da última verificação do worker de monitoramento |

---

## 6. Engines de Diagnóstico

Todos residem em `:featureDiagnostico`. São objetos stateless — recebem dados brutos e retornam `DiagnosticResult`.

**`DiagnosticResult` — modelo de saída de todos os engines:**

```
DiagnosticResult(
    id: String,
    titulo: String,
    status: DiagnosticStatus,   // ok | info | attention | critical | inconclusive
    mensagemUsuario: String,
    recomendacao: String?,
    categoria: String
)
```

### 6.1 WifiSignalQualityEngine

**Entrada:** RSSI (dBm), frequência (MHz), link speed, canal, padrão Wi-Fi
**Saída:** `WifiQualityResult`

**Thresholds de RSSI por banda (v0.8.1):** a classificação distingue 2.4GHz de 5GHz. Redes 5GHz têm atenuação maior por natureza, então os thresholds aceitam valores mais baixos em valor absoluto.

| Classificação | 5GHz (dBm) | 2.4GHz (dBm) | Status |
|---|---|---|---|
| Excelente | ≥ −55 | ≥ −50 | `ok` |
| Bom | ≥ −65 | ≥ −60 | `ok` |
| Regular | ≥ −75 | ≥ −70 | `attention` |
| Fraco | < −75 | < −70 | `critical` |

A banda é determinada pela frequência em MHz: < 3000 MHz → 2.4GHz, ≥ 3000 MHz → 5GHz (ou 6GHz, tratada como 5GHz nos thresholds).

Sinal enquadrado como Regular ou acima (≥ threshold Regular da banda) → `confiavelParaTeste = true`. Abaixo disso, resultados de internet ficam inconclusivos.

### 6.2 InternetDiagnosticEngine

**Entrada:** `InternetDiagnosticInput` + flag `wifiConfiavelParaTeste`
**Saída:** `DiagnosticResult`

Regras (por prioridade):
- `downloadMbps == null` → `critical` — Internet Indisponível
- `perda >= 3.0%` → `critical` — Perda Alta
- `perda >= 1.0%` → `attention` — Perda Moderada
- `jitter > 20ms` → `attention` — Jitter Elevado
- `latencia > 100ms` → `attention` — Latência Alta (limiar ANATEL RQUAL)
- `bufferbloat > 100ms` → `critical` — Bufferbloat Crítico
- `bufferbloat > 30ms` → `attention` — Bufferbloat Elevado
- `upload == 0.0 Mbps` → `critical` — Upload Zerado
- `upload < 5.0 Mbps` → `attention` — Upload Baixo
- `download < 25.0 Mbps` → `attention` — Download Baixo
- Wi-Fi não confiável + problemas → `inconclusive`
- Nenhum problema → `ok` — Conexão Saudável

### 6.3 WifiChannelDiagnosticEngine

**Entrada:** redes vizinhas + canal conectado
**Saída:** `DiagnosticResult`

Analisa congestionamento do canal Wi-Fi: conta redes vizinhas no mesmo canal e classifica interferência.

### 6.4 DnsDiagnosticEngine

**Entrada:** IP do DNS atual, latência, grade, melhor alternativa disponível
**Saída:** `DiagnosticResult`

Avalia qualidade do DNS em uso e se há alternativa mais rápida.

**Diagnósticos emitidos:**

| ID | Condição | Status | Descrição |
|---|---|---|---|
| DNS-01 | DNS com falha ou sem resposta | `critical` | DNS indisponível |
| DNS-02 | Latência > 150ms | `attention` | DNS lento — impacto perceptível no carregamento de páginas |
| DNS-03 | Latência entre 51ms e 150ms (inclusive) | `info` | DNS com latência moderada — funcional, mas abaixo do ideal. Mensagem explica a faixa e sugere alternativa mais rápida quando disponível |
| — | Latência ≤ 50ms | `ok` | DNS saudável |

> DNS-03 é novo em v0.8.1. Preenchia um gap entre "ok" e "attention" — latências entre 51-150ms eram classificadas como ok sem nenhuma informação ao usuário.

### 6.5 HistoricalDegradationEngine

**Entrada:** médias de 7d e 30d, contagem de testes, tendência
**Saída:** `DiagnosticResult`

Detecta degradação histórica comparando médias de curto e longo prazo.

### 6.6 FibraSignalQualityEngine

**Entrada:** `rxPowerDbm`, `txPowerDbm`, temperatura, `isUp`
**Saída:** `DiagnosticResult`

Avalia qualidade do sinal de fibra óptica GPON (potência Rx/Tx, temperatura da ONT).

### 6.7 MobileSignalDiagnosticEngine

**Entrada:** tecnologia, RSRP, RSRQ, SINR, banda
**Saída:** `DiagnosticResult`

Avalia qualidade do sinal de dados móveis 4G/5G.

### 6.8 DiagnosticDecisionEngine

**Responsabilidade:** motor de decisão final. Consolida resultados de todos os engines e retorna uma única decisão.

**Entrada:** resultados de `internetResultados`, `wifiQuality`, `fibraResultados`, `rttGatewayMs`, `latenciaInternetMs`
**Saída:** `DiagnosticResult` único (o diagnóstico principal)

Regras de decisão (por prioridade):

| ID | Condição | Status | Diagnóstico |
|---|---|---|---|
| DECISAO-DNS-01 | DNS crítico (sem outro crítico) | `critical` | Problema no DNS |
| DECISAO-HIST-01 | Degradação histórica detectada | `critical`/`attention` | Degradação Recente |
| DECISAO-WIFI-CANAL | Canal Wi-Fi congestionado | `attention` | Congestionamento Wi-Fi |
| DECISAO-00 | Fibra crítica + internet ruim | `critical` | Problema na Fibra |
| DECISAO-01 | Internet ruim + Wi-Fi bom | `critical` | Problema no ISP |
| DECISAO-02 | Wi-Fi ruim + internet ok | `attention` | Problema Local (Wi-Fi) |

### 6.9 TopologiaWifiEngine (featureWifi)

**Entrada:** `List<RedeVizinha>`, `connectedBssid`, `gatewayOui`
**Saída:** `List<RedeClassificada>`

Tipos de topologia:
- `ROTEADOR_MESH`: OUI mesh + nó principal
- `NO_MESH`: OUI mesh + nó secundário
- `ROTEADOR`: OUI ISP ou SSID único
- `REPETIDOR`: múltiplos BSSIDs + OUI diferente + sinal mais fraco

Confiança: `ALTA` (OUI confirmado) / `MEDIA` (múltiplos BSSIDs mesmo OUI) / `BAIXA` (rede única).

### 6.10 Outros Use Cases

| Use Case | Módulo | Função |
|---|---|---|
| `MontarResumoWifiUseCase` | featureWifi | Síntese de análise Wi-Fi |
| `UptimeChartUseCase` | featureHistory | Prepara dados para gráfico de uptime |
| `UptimeNarrativaEngine` | featureHistory | Gera narrativa textual de uptime |
| `DynamicQuestionEngine` | featureDiagnostico | Gera perguntas contextuais baseadas no estado da rede |

---

## 7. Componentes UI Reutilizáveis

Todos residem em `:app/ui/component/`.

| Componente | Função |
|---|---|
| `GaugeCircular` | Gauge circular animado (speedtest) |
| `MiniGrafico` | Gráfico sparkline ao vivo |
| `OrbitAiMessageBubble` | Bolha de resposta da IA em markdown |
| `OrbitUserMessageBubble` | Bolha de mensagem do usuário |
| `OrbitThinkingBubble` | Animação "pensando..." |
| `OrbitInlineQuestion` | Pergunta inline com chips de resposta |
| `OrbitInputArea` | Campo de texto + botão de envio |
| `OrbitActionsCard` | Card de ações do Orbit |
| `LinkaIaHeader` | Cabeçalho da sessão de IA |
| `LinkaPulseIcon` | Ícone do LinkaPulse |
| `LinkaPulseSymbol` | Símbolo animado do LinkaPulse |
| `AiModelFooter` | Footer com informação do modelo IA |
| `ConfirmacaoDialog` | Dialog de confirmação genérico |
| `ProfileAvatarButton` | Avatar do usuário no navigationIcon de todas as abas root. Exibe foto ou inicial com gradiente. Ao tocar, abre `PerfilEditSheet` |
| `WifiChannelGuide` | Visualização de congestionamento de canais Wi-Fi |
| `AppBorderGlowEffect` | Efeito de borda glow |
| `OperadoraContactCard` | Card de contato da operadora (SAC + WhatsApp + fallback Anatel). Exibido quando `categoria == "isp"` no resultado do diagnóstico |
| `ResultadoBitmapGenerator` | Suspend function que gera bitmap 1080×600px via Canvas Android com métricas do resultado e headline do diagnóstico |

---

## 8. Navegação — AppShell.kt

**NavigationBar com 5 abas:**

| Índice | Label | Composable | Ícone |
|---|---|---|---|
| 0 | Início | `HomeScreen` | — |
| 1 | Velocidade | `SpeedTestScreen` | — |
| 2 | Sinal | `SinalScreen` | — |
| 3 | Histórico | `HistoricoScreen` | — |
| 4 | Mais | — | `GridView` |

> `DispositivosScreen` foi removida da TabBar. Passa a ser acessível via aba "Mais".
> `AjustesScreen` foi removida do Drawer. Passa a ser acessível via aba "Mais".

**Fluxos secundários sobrepostos:**

| Tela | Trigger | Back button |
|---|---|---|
| `VelocidadeScreen` | Teste iniciado em SpeedTestScreen | `ArrowBack` — oculto durante `executando` |
| `ResultadoVelocidadeScreen` | Teste concluído | `ArrowBack` — sempre visível |
| `DiagnosticoScreen` | Via ExploreToolsRow ou ResultadoVelocidade | `ArrowBack` — sempre visível |
| `ChatScreen` | Botão "Conversar com IA" em ResultadoVelocidade | — |
| `FibraScreen` | Ajustes → seção Fibra | `ArrowBack` — sempre visível |

**ProfileAvatarButton:**
- Componente: `ui/component/ProfileAvatarButton.kt`
- Posição: `navigationIcon` (esquerda) do TopAppBar em todas as 5 abas root
- Exibe foto via `BitmapFactory` + `contentResolver`, ou inicial do nome com gradiente accent/accentBlue
- Ao tocar: abre `PerfilEditSheet`

**Scroll-aware NavBar:**
- `NestedScrollConnection` instalado no Scaffold do `AppShell`
- Ao scrollar down: `navBarOffsetY` decresce até `-navBarHeightPx` (NavBar some)
- Ao scrollar up: `navBarOffsetY` retorna a 0 (NavBar reaparece)
- Animação: `offset { IntOffset(0, navBarOffsetY.roundToInt()) }` + `graphicsLayer { alpha = 1f + (navBarOffsetY / navBarHeightPx) }`
- Durante execução do speedtest: NavBar some completamente (comportamento pré-existente, mantido)

**Confirmação de cancelamento de teste:**
- `BackHandler` ativo no `SpeedTestScreen` quando `estado == executando`
- Exibe `AlertDialog`: título "Cancelar o teste?", ações "Continuar testando" (dismiss) e "Cancelar teste" (confirma)

---

## 9. Build Config

| Parâmetro | Valor |
|---|---|
| compileSdk | 35 |
| minSdk | 28 |
| targetSdk | 35 |
| JVM target | 17 |
| AGP | 8.11.1 |
| Kotlin | 2.2.20 |
| Compose plugin | 2.2.20 |
| rootProject.name | linkaAndroidKotlin |
| Chaves de assinatura | `key.properties` (gitignored) |

### 9.1 Feature Flags

Controle granular de features via flags booleanas em compiletime — definidas em `app/build.gradle.kts` com blocos distintos para debug e release:

**15 flags MVP** (ativas em debug E release): `FEATURE_SPEEDTEST`, `FEATURE_DIAGNOSTICO_LOCAL`, `FEATURE_DIAGNOSTICO_IA`, `FEATURE_WIFI_ANALISE`, `FEATURE_REDE_MOVEL_ANALISE`, `FEATURE_HISTORICO`, `FEATURE_LAUDO_PDF`, `FEATURE_ONBOARDING`, `FEATURE_PERMISSOES_CONTEXTO`, `FEATURE_ESTADO_OFFLINE`, `FEATURE_SETTINGS_MVP`, `FEATURE_PRIVACIDADE_TELA`, `FEATURE_NOVIDADES_TELA`, `FEATURE_FIBRA_SCREEN`, `FEATURE_DNS_SCREEN`

> `FEATURE_FIBRA_SCREEN` e `FEATURE_DNS_SCREEN` promovidas de pós-MVP para MVP nesta entrega — FibraScreen e DNS Benchmark agora visíveis em release.

**18 flags pós-MVP** (ativas em debug, inativas em release): `FEATURE_DIAGNOSTICO_CHAT`, `FEATURE_LINKPULSE_ATIVO`, `FEATURE_NOTIFICACAO_INLINE`, `FEATURE_WIDGET`, `FEATURE_QUICK_SETTINGS_TILE`, `FEATURE_PROVA_REAL_COMPLETO`, `FEATURE_DIAGNOSTICO_ITERATIVO`, `FEATURE_TRACEROUTE`, `FEATURE_DEVICES_SCREEN_V2`, `FEATURE_TELEPHONY_AVANCADO`, `FEATURE_MAPA_CALOR_WIFI`, `FEATURE_AGENDAMENTO_TESTES`, `FEATURE_LINKPULSE_CHAT`, `FEATURE_LINKASYNC`, `FEATURE_BACKUP_LOCAL`, `FEATURE_CONTRIBUICAO_ANONIMA`, `FEATURE_RATE_US`, `FEATURE_ACESSIBILIDADE`

> `FEATURE_DIAGNOSTICO_CHAT` é uma flag nova criada nesta entrega. Controla especificamente o botão "Conversar com IA" em `ResultadoVelocidadeScreen` e o overlay `ChatScreen` no `AppShell`. Está **inativa em release** — o chat Orbit está oculto para o usuário final. `FEATURE_DIAGNOSTICO_IA` (card de diagnóstico + laudo IA) permanece como flag independente e ativa (MVP).

**Acesso:** use sempre `FeatureFlags.*` (objeto em `app/src/main/kotlin/io/linka/app/kotlin/FeatureFlags.kt`), nunca `BuildConfig.DEBUG` ou `BuildConfig.FEATURE_*` diretamente nas telas.

**Ativação em release:** alterar valor em bloco `release`, incrementar versão, rebuild e testar. Ver `RELEASE.md` para procedimento completo.

### 9.2 Arquitetura de Proteção de Overlays e Lambdas

Overlays controlados por feature flag seguem um padrão duplo de proteção:

1. **`AnimatedVisibility`** no `AppShell`: o parâmetro `visible` verifica `FeatureFlags.*` — quando a flag está inativa, o Composable não entra em composição.
2. **Gate na lambda de entrada**: callbacks como `onConectarFibra` e `onAbrirDnsBenchmark` verificam a flag antes de executar, evitando side-effects (navegação, IO) mesmo que a lambda seja chamada por outro caminho.

Este padrão garante que features com flags inativas não produzem efeitos colaterais mesmo em cenários de recomposição inesperada.

**Exemplos de aplicação desta entrega:**
- `ChatScreen` overlay: `visible = FeatureFlags.FEATURE_DIAGNOSTICO_CHAT` no `AppShell`
- Botão "Conversar com IA" em `ResultadoVelocidadeScreen`: renderizado apenas quando `FeatureFlags.FEATURE_DIAGNOSTICO_CHAT`
- `FibraScreen` overlay: `visible = FeatureFlags.FEATURE_FIBRA_SCREEN`
- Lambda `onConectarFibra`: guarda `if (!FeatureFlags.FEATURE_FIBRA_SCREEN) return`
- Lambda `onAbrirDnsBenchmark`: guarda `if (!FeatureFlags.FEATURE_DNS_SCREEN) return`


---

## 10. Assinaturas de Composables — Mudanças de Contrato

Esta seção documenta Composables cujas assinaturas sofreram mudanças relevantes de contrato. Não lista todos os parâmetros de todos os Composables — apenas os que impactam integrações com ViewModel ou outros Composables.

### 10.1 SinalScreen (v0.7.3)

**Módulo:** `:app`

**Mudança:** parâmetro `isOnWifi: Boolean` substituído por `estadoConexao: EstadoConexao` + `movelSnapshot: MovelSnapshot?`. A tela passou a ser adaptativa por tipo de conexão.

**Parâmetros relevantes:**

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `estadoConexao` | `EstadoConexao` | Enum: `WIFI`, `MOVEL`, `CABO`, `DESCONHECIDO` |
| `movelSnapshot` | `MovelSnapshot?` | Dados de sinal móvel (RSRP, RSRQ, SINR, operadora, tecnologia). Null quando não em modo móvel ou sem permissão |
| `localIp` | `String?` | IP local do dispositivo. Exibido nos modos Móvel e Cabo |
| `temPermissaoTelefonia` | `Boolean` | Controla exibição das métricas móveis ou do botão "Conceder permissão" |
| `onSolicitarPermissaoTelefonia` | `() -> Unit` | Callback para solicitar permissão de telefonia em runtime |

**Lógica interna:** `ConexaoTipo` é um enum interno da tela que mapeia `EstadoConexao` para a branch de UI correta. Não é exposto fora da tela.

### 10.2 ResultadoVelocidadeScreen (v0.7.3)

**Módulo:** `:app`

**Mudança:** adicionados `ispInfo: IspInfo?` (para `OperadoraContactCard`) e integração com `ResultadoBitmapGenerator` (botão Share no TopAppBar).

**Parâmetros novos:**

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `ispInfo` | `IspInfo?` | Dados do ISP (usado por `OperadoraContactCard` para resolver operadora via `BancoOperadoras`) |

**Comportamento do Share:**
- Geração do bitmap executada em `Dispatchers.IO` via suspend function
- `startActivity(Intent.createChooser(...))` chamado em `Dispatchers.Main` após geração
- FileProvider usado para URI do arquivo temporário
- Estado de loading (spinner no ícone) durante geração

---

## 11. Novos Arquivos — v0.7.3

| Arquivo | Módulo | Tipo | Responsabilidade |
|---|---|---|---|
| `BancoOperadoras.kt` | `:app` | Objeto singleton | Mapa de 16 ISPs brasileiros com SAC e número de WhatsApp. Expõe `resolver(ispNome: String): DadosOperadora?` via substring matching case-insensitive. Fallback para Anatel (1331) quando não identificado |
| `ResultadoBitmapGenerator.kt` | `:app` | Suspend function | Gera bitmap 1080×600px via Canvas Android com download, upload, latência, jitter, headline do diagnóstico e data/hora. Cor de fundo dinâmica por severidade (verde/amarelo/vermelho/neutro escuro) |
| `OperadoraContactCard.kt` | `:app` | Composable | Card com botões SAC (Intent de discagem) e WhatsApp (deep link `https://wa.me/`). Visível apenas quando `snapshotDiagnostico.categoria == "isp"` |

## 12. Novos Arquivos — Navigation MVP block

| Arquivo | Módulo | Tipo | Responsabilidade |
|---|---|---|---|
| `ProfileAvatarButton.kt` | `:app` | Composable | Avatar do usuário exibido no `navigationIcon` de todas as 5 abas root. Decodifica foto via `BitmapFactory` + `contentResolver`, ou renderiza inicial do nome com gradiente accent/accentBlue. Ao tocar, abre `PerfilEditSheet` |

**Mudanças estruturais no AppShell (sem arquivo novo):**
- `NestedScrollConnection` adicionado ao Scaffold para scroll-aware NavBar (NAV-B)
- `BackHandler` adicionado ao `SpeedTestScreen` para confirmação de cancelamento durante execução (NAV-E)
- Tabela de abas refatorada: 0=Início / 1=Velocidade / 2=Sinal / 3=Histórico / 4=Mais (NAV-A)
- `SpeedTestScreen` e `SinalScreen` promovidas de overlays `AnimatedVisibility` para tabs fixas (NAV-A)
- `DispositivosScreen` removida da TabBar — acessível via aba "Mais" (NAV-A)

## 13. Novos Arquivos — Bloco AJ (Ajustes MVP)

| Arquivo | Módulo | Tipo | Responsabilidade |
|---|---|---|---|
| `ui/screen/PrivacidadeScreen.kt` | `:app` | Composable (tela full-screen) | Exibe política de privacidade em 4 blocos: o que coleta, o que não coleta, retenção/controle, solicitação de exclusão. Overlay via `AnimatedVisibility` + `BackHandler` no AppShell. Controlado por `FEATURE_PRIVACIDADE_TELA` |
| `ui/screen/NovidadesScreen.kt` | `:app` | Composable (tela full-screen) | Exibe changelog de versões lido de `assets/changelog.json`. Persiste `ultimaVersaoVista` no DataStore via `MainViewModel.salvarUltimaVersaoVista()`. Overlay via `AnimatedVisibility` + `BackHandler` no AppShell. Controlado por `FEATURE_NOVIDADES_TELA` |
| `app/src/main/assets/changelog.json` | `:app` | Asset JSON | Changelog do app com versões v0.7.0, v0.6.0, v0.5.0. Lido em runtime por `NovidadesScreen` |

**Arquivos modificados no bloco AJ:**

| Arquivo | Módulo | Mudanças |
|---|---|---|
| `PreferenciasAppRepository.kt` | `:coreDatastore` | 5 novos keys/flows/setters: `ispConfirmado`, `operadoraMovel`, `estadoUf`, `cidadeNome`, `ultimaVersaoVista` |
| `MainViewModel.kt` | `:app` | 4 novas funções: `confirmarIspDetectado()`, `dispensarBannerIsp()`, `salvarEstadoCidade()`, `salvarUltimaVersaoVista()` |
| `MainActivity.kt` | `:app` | Coleta 3 novos flows do DataStore e passa para AppShell |
| `AppShell.kt` | `:app` | 6 novos parâmetros para suportar as telas de Privacidade e Novidades; overlays das duas novas telas |
| `AjustesScreen.kt` | `:app` | Refactor completo: TopAppBar renomeada, LazyColumn em 4 seções fixas + seção Avançado condicional, `ProvedorSheet` atualizado, banner ISP inline, seleção de região por Estado+Cidade com IBGE API, campo de plano numérico |
| `SpeedTestScreen.kt` | `:app` | Banner "plano vazio" quando `planoInternet.isBlank()`; mensagem de rescisão ANATEL em `CardRqualAnatel` quando resultado < 40% |

**IBGE API — cache de municípios:**
- Endpoint: `https://servicodados.ibge.gov.br/api/v1/localidades/estados/$uf/municipios`
- Cache: `HashMap<String, List<String>>` in-memory por UF, inicializado sob demanda na primeira seleção de cada estado
- Sem persistência no DataStore — cache é volátil (reinicia com o processo)

---

## 14. Mudanças de Comportamento — v0.8.1

Sem arquivos novos nesta versão. Todas as mudanças são em comportamento de componentes existentes.

### 14.1 WifiSignalQualityEngine — thresholds por banda

Thresholds de RSSI agora diferenciados por banda (2.4GHz vs 5GHz). Ver seção 6.1 para tabela completa.

### 14.2 ResultadoVelocidadeScreen — título e cores dinâmicas

- **Título:** "Resultado do teste" (corrigido).
- **MetricCard de latência:** cor determinada por threshold — verde < 20ms, amarelo < 60ms, vermelho ≥ 60ms.
- **MetricCard de jitter:** cor determinada por threshold — verde < 10ms, amarelo < 30ms, vermelho ≥ 30ms.

### 14.3 FibraScreen — loading dinâmico e hint RX

- **Texto de loading:** usa `deviceInfo.model` quando disponível; fallback "Conectando ao modem…".
- **Hint RX Power:** corrigido para "Ideal: −8 a −27 dBm" (ITU-T G.984). O valor anterior estava incorreto.

### 14.4 DiagnosticoScreen — tipo de conexão real

O campo `connectionType` enviado no contexto ao Worker Cloudflare passa a refletir o tipo real da conexão (`wifi`, `movel`, `ethernet`). Bug corrigido: versões anteriores enviavam `wifi` fixo.

### 14.5 DnsDiagnosticEngine — DNS-03

Novo diagnóstico para latência DNS entre 51ms e 150ms. Status `info` — não é erro, mas informa o usuário sobre a faixa e sugere alternativa quando disponível. Ver seção 6.4 para tabela completa de diagnósticos DNS.

### 14.6 Acessibilidade — semântica TalkBack

- `ModeSelector` (SpeedTestScreen): roles e labels adicionados para leitura por TalkBack.
- `PathConnector` (HomeScreen): marcado como elemento decorativo — não lido pelo TalkBack.

