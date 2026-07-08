# Arquitetura — Android SignallQ

**Última atualização:** 2026-07-05 (v0.23.0, versionCode 56)
**Fonte:** código real
**Padrão:** MVVM com Hilt DI

---

## 1. Visão Geral

```
UI (Composables)
    ↑ StateFlow.collectAsStateWithLifecycle()
MainViewModel (@HiltViewModel — único ViewModel raiz)
    ↑ dependências injetadas via Hilt (AppModule)
Serviços / Repositórios / Engines / Use Cases
    ↑ Room / DataStore / ConnectivityManager / TelephonyManager / OkHttp
```

**Fluxo unidirecional:** evento da UI → função no ViewModel → atualiza StateFlow → recomposição da UI.

---

## 2. Camadas

### Camada de UI

**Localização:** `:app/ui/`

Composta por:
- **Telas (screens):** Composables que recebem estado e emitem eventos. Sem lógica de negócio.
- **Componentes:** Composables reutilizáveis em `ui/component/`.
- **AppShell:** shell do app com `NavigationBar` de 5 abas + fluxos secundários sobrepostos.

**Como consome o estado:**
```kotlin
val snapshotRede by viewModel.snapshotRede.collectAsStateWithLifecycle()
```

Cada StateFlow do ViewModel é coletado individualmente pela tela que o consome. Não há estado global de UI — cada tela recebe apenas o que precisa como parâmetros.

### Camada de ViewModel

**Arquivo:** `MainViewModel.kt` — único ViewModel raiz do app.

**Tipo:** `AndroidViewModel` anotado com `@HiltViewModel` (tem acesso ao `Application` para contextos)

**Padrão de injeção:** dependências fornecidas pelo Hilt via `AppModule` (`di/AppModule.kt`). Os módulos (`*Modulo.kt`) ainda existem como fábricas estáticas usadas internamente pelo `AppModule` para construir as instâncias.

```kotlin
// AppModule.kt
@Provides @Singleton
fun provideBancoDados(@ApplicationContext ctx: Context): SignallQDatabase =
    CoreDatabaseModulo.criarBanco(ctx)

// MainViewModel.kt
@HiltViewModel
class MainViewModel @Inject constructor(
    private val bancoDados: SignallQDatabase,
    private val monitorRede: MonitorRede,
    // ...
) : AndroidViewModel(application)
```

### Camada de Domínio (Serviços, Engines, Use Cases)

**Engines de diagnóstico:** objetos stateless que recebem dados brutos e retornam `DiagnosticResult`. Residem em `:featureDiagnostico`.

**Use Cases:** ex. `MontarResumoWifiUseCase`, `UptimeChartUseCase`, `UptimeNarrativaEngine`. Cada um com responsabilidade única.

**Orchestrators:** coordenam múltiplos serviços e engines:
- `DiagnosticOrchestrator`: sequencia todos os engines de diagnóstico
- `SignallQOrchestrator`: coordena speedtest silencioso + diagnóstico + IA conversacional (fluxo Chat/Pulse)
- `MonitoramentoWorker`: coordena monitoramento passivo em background (WorkManager)

### Camada de Dados

| Mecanismo | Módulo | Uso |
|---|---|---|
| Room (SQLite) | `:coreDatabase` | Medições, apelidos de dispositivos |
| DataStore (Preferences) | `:coreDatastore` | Preferências do usuário |
| ConnectivityManager | `:coreNetwork` | Estado de rede em tempo real |
| TelephonyManager | `:coreTelephony` | Sinal móvel (RSRP, RSRQ, SINR) |
| OkHttp | `:featureSpeedtest`, `:featureDiagnostico` | HTTP para speedtest e IA |
| HTTP local | `:featureFibra` | Acesso ao modem GPON Nokia |

---

## 3. Fluxo de Dados — StateFlow

```
MonitorRedeAndroid (NetworkCallback)
    → snapshotRede: StateFlow<SnapshotRede>
        → HomeScreen, SpeedTestScreen, SinalScreen (collectAsStateWithLifecycle)

ExecutorSpeedtest
    → snapshotSpeedtest: StateFlow<SnapshotExecucaoSpeedtest>
        → VelocidadeScreen, ResultadoVelocidadeScreen

DiagnosticOrchestrator
    → snapshotDiagnostico: StateFlow<...>
        → DiagnosticoScreen

SignallQOrchestrator
    → orbitUiStateFlow: StateFlow<SignallQSnapshot>
        → ChatScreen / LLMChatScreen
```

Todos os StateFlows são criados no `MainViewModel` e coletados pelas telas via `collectAsStateWithLifecycle()` — garante coleta vinculada ao lifecycle da Activity.

---

## 4. Padrão de Módulo Feature

Cada feature segue o padrão:

```
:featureX/
├── FeatureXModulo.kt      ← objeto com funções fábrica estáticas
├── InterfaceX.kt          ← contrato público da feature
├── ImplementacaoX.kt      ← implementação Android
├── SnapshotX.kt           ← estado imutável da feature
├── EstadoX.kt             ← enum de estados
└── ModeloX.kt             ← data classes de domínio
```

A `:app` instancia as implementações via `FeatureXModulo.criar*(context)` e mantém referências como lazy singletons no `MainViewModel`.

---

## 5. Navegação

**Padrão:** não usa Navigation Component com rotas por URI para a navegação principal entre abas. A `AppShell` gerencia o índice da aba selecionada e renderiza o Composable correspondente.

**Fluxos secundários:** sobrepostos via `AnimatedVisibility` com `slideInVertically`. Não são rotas de navigation separadas — são controlados por estado booleano no ViewModel.

**Sem deep linking de tela** nas telas secundárias. Deep links (`signallq://screen/...`) só existem para as 5 abas principais.

---

## 6. Background Processing

**WorkManager (CoroutineWorker):** `MonitoramentoWorker` — executa periodicamente para monitoramento passivo.

Fluxo:
1. `MonitoramentoScheduler` agenda/cancela o worker via WorkManager.
2. Worker executa: mede latência HTTP, DNS resolve time, RSSI Wi-Fi.
3. Aplica histerese (`HisteresiHelper`) para decidir se dispara notificação.
4. Persiste medição em Room (`MedicaoEntity` com `connectionType = "monitor"`).
5. `SignallQNotificationHelper` cria e exibe notificações via `NotificationManager`.

**Estados de histerese** (armazenados no DataStore como Boolean):
- `alerta_latencia_ativo`
- `alerta_dns_ativo`
- `alerta_rssi_ativo`
- `alerta_sem_internet_ativo`

---

## 7. Integração com IA

```
App Android
    → DiagnosisAiContextFactory.fromRaw(...)    [monta payload schema v3]
    → AiDiagnosisRepository
    → POST https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev/api/ai/diagnostico-conexao
    → Cloudflare Worker (name: linka-ai-diagnosis-worker)
    → Gemini 2.0 Flash (primário, quando `GEMINI_API_KEY` configurada) / Qwen3 30B MoE FP8 (@cf/qwen/qwen3-30b-a3b-fp8, fallback cloud)
    → AiDiagnosisResult (JSON parseado pelo app)

Fallback: AiFallbackFactory.fromLocal() [se IA falhar ou timeout]
```

**Schema da versão atual:** `diagnostico_v3_raw` — payload com dados brutos; a IA faz toda a análise. O parser do worker aceita schemas v1/v2/v3 para retrocompatibilidade.

**Provider primário:** Gemini 2.0 Flash (Google), ativado quando a secret `GEMINI_API_KEY` está configurada. Sem a secret, Qwen3/Cloudflare é o único provider cloud.

**Modelos no worker (wrangler.toml `AI_MODEL`) — usado quando o fallback Qwen3/Cloudflare entra em ação:**
- Padrão: `@cf/qwen/qwen3-30b-a3b-fp8` (Qwen3 30B MoE FP8)
- Alternativas/legado: Gemma 7B-IT, Gemma 2 9B, Gemma 4 26B (descartado — timeout)

**Classes Android envolvidas:** `AiDiagnosisRepository`, `DiagnosisAiContextFactory`, `DiagnosticOrchestrator`, `DiagnosticRunner` + engines (`InternetDiagnosticEngine`, `WifiSignalQualityEngine`, `MobileSignalDiagnosticEngine`, `FibraSignalQualityEngine`, `DnsDiagnosticEngine`, `HistoricalDegradationEngine`).

**Fluxo Chat/Pulse:** `SignallQOrchestrator` → `SignallQState` → `SignallQSnapshot` → `DynamicQuestionEngine`.

---


## 7.1 Feature Flags via BuildConfig

**Sistema de controle de features em release:** cada feature é ativada/desativada via flag booleana em `app/build.gradle.kts` — debug sempre com MVP + feature flags pós-MVP enabled; release apenas MVP ativo.

**Arquivo de definição:** `app/build.gradle.kts` — blocos `debug` e `release` com 32 `buildConfigField` booleanos:

**Flags MVP (ativas em debug E release):**
`FEATURE_SPEEDTEST`, `FEATURE_DIAGNOSTICO_LOCAL`, `FEATURE_DIAGNOSTICO_IA`, `FEATURE_WIFI_ANALISE`, `FEATURE_REDE_MOVEL_ANALISE`, `FEATURE_HISTORICO`, `FEATURE_LAUDO_PDF`, `FEATURE_ONBOARDING`, `FEATURE_PERMISSOES_CONTEXTO`, `FEATURE_ESTADO_OFFLINE`, `FEATURE_SETTINGS_MVP`, `FEATURE_PRIVACIDADE_TELA`, `FEATURE_NOVIDADES_TELA`

**Flags pós-MVP (ativas em debug, inativas em release):**
`FEATURE_LINKPULSE_ATIVO`, `FEATURE_NOTIFICACAO_INLINE`, `FEATURE_WIDGET`, `FEATURE_QUICK_SETTINGS_TILE`, `FEATURE_PROVA_REAL_COMPLETO`, `FEATURE_DIAGNOSTICO_ITERATIVO`, `FEATURE_TRACEROUTE`, `FEATURE_FIBRA_SCREEN`, `FEATURE_DNS_SCREEN`, `FEATURE_DEVICES_SCREEN_V2`, `FEATURE_TELEPHONY_AVANCADO`, `FEATURE_MAPA_CALOR_WIFI`, `FEATURE_AGENDAMENTO_TESTES`, `FEATURE_LINKPULSE_CHAT`, `FEATURE_LINKASYNC`, `FEATURE_BACKUP_LOCAL`, `FEATURE_CONTRIBUICAO_ANONIMA`, `FEATURE_RATE_US`, `FEATURE_ACESSIBILIDADE`

**Acesso nas telas:** nunca usar `BuildConfig.DEBUG` ou `BuildConfig.FEATURE_*` diretamente. Usar sempre `FeatureFlags.*` — objeto Kotlin em `app/src/main/kotlin/io/veloo/app/kotlin/FeatureFlags.kt` (package `io.signallq.app`):

`kotlin
if (FeatureFlags.FEATURE_SPEEDTEST) {
    SpeedTestScreen(...)
}

if (FeatureFlags.FEATURE_DIAGNOSTICO_IA) {
    ChatScreen(...)
}
`

Cada propriedade em `FeatureFlags.kt` é um getter que mapeia para `BuildConfig.FEATURE_*`. O arquivo nunca deve ser editado manualmente — gerado/sincronizado via buildConfigField.

**Para ativar feature em release:**
1. Alterar o valor de `buildConfigField` no bloco `release` em `app/build.gradle.kts` — de `"false"` para `"true"`
2. Incrementar versão (`versionCode` e/ou `versionName`) em `libs.versions.toml`
3. Rebuild e testar
4. Distribuir release ao Play Store

---

## 8. Entry Points

| Arquivo | Papel |
|---|---|
| `SignallQApplication.kt` | `Application` — inicialização, Hilt, canais de notificação |
| `MainActivity.kt` | Activity única — `setContent { SignallQTheme { AppShell(...) } }` |
| `MainViewModel.kt` | ViewModel raiz `@HiltViewModel` — orquestra todos os serviços |
| `AppShell.kt` | Shell com NavigationBar + fluxos sobrepostos |
| `AppNavGraph.kt` | Definição de rotas das 5 abas |
| `SignallQTheme.kt` | Tema MD3 com ColorScheme e tokens customizados |

