# Performance & Energy Optimization Deep Dive
## LINKA Android v0.9.3 — Eficiência

**Última atualização**: 2026-05-23  
**Responsáveis**: Camilo (Arquitetura), Brás (Mobile UX)  
**Escopo**: 8 issues críticas (#17–24) focadas em dados móveis, bateria, throughput

---

## 1. Análise de Gargalos Identificados

### 1.1 Dados Móveis: Detecção de Rede Medida (Issue #17)

**Problema**: `ExecutorSpeedtestCloudflare.kt` não detecta rede medida (4G, cellular com limite) — sempre low-balling payload a ~5 MB, mesmo em Wi-Fi.

**Raiz**: Falta de chamada a `ConnectivityManager.isActiveNetworkMetered()` ou `Network.metered` property.

**Evidência**:
```kotlin
// ExecutorSpeedtestCloudflare.kt:1257, 1271, 1287
val payload = if (isHighPerformance) 10_MB else 5_MB  // sempre 5 MB em metered
```

**Impacto estimado**:
- Wi-Fi com limite de hotspot → resulta subdimensionado (perde 20–50% da banda real)
- Experiência mobile: speedtest parece lento mesmo em 5G desmesurado

**Solução**:
```kotlin
private suspend fun detectNetworkType(): NetworkType {
  val connectivityManager = context.getSystemService<ConnectivityManager>()
  val isMetered = connectivityManager?.isActiveNetworkMetered() ?: false
  val isWifi = connectivityManager?.activeNetwork?.let { network ->
    connectivityManager.getNetworkCapabilities(network)?.hasTransport(TRANSPORT_WIFI) ?: false
  } ?: false
  
  return when {
    isWifi && !isMetered -> NetworkType.WIFI_UNLIMITED
    isWifi && isMetered   -> NetworkType.WIFI_METERED
    isMetered             -> NetworkType.CELLULAR_METERED
    else                  -> NetworkType.CELLULAR_UNLIMITED
  }
}

val payloadSize = when (networkType) {
  NetworkType.WIFI_UNLIMITED       -> 50_MB
  NetworkType.WIFI_METERED         -> 10_MB
  NetworkType.CELLULAR_UNLIMITED   -> 10_MB
  NetworkType.CELLULAR_METERED     -> 3_MB
}
```

**Acceptance criteria** (Issue #17):
- [ ] `NetworkType` enum + `detectNetworkType()` implementado
- [ ] Payload ajustado por tipo (50/10/10/3 MB)
- [ ] UX exibe badge "Metered Network" (opcional para diagnóstico)
- [ ] Validação: speedtest em rede medida ≤ 10% erro vs. throughput real

---

### 1.2 Dados Móveis: Ping em Paralelo (Issue #18)

**Problema**: Ping executado a cada 300ms DURANTE o throughput do speedtest.

**Evidência**:
```kotlin
// ExecutorSpeedtestCloudflare.kt:587, 782, 1168
val pingJob = launch {
  while (isActive) {
    ping()
    delay(300.ms)  // 300ms loop DURANTE transferência
  }
}
```

**Impacto**:
- 3–4 requisições/segundo concorrendo com throughput principal
- Payload real reduzido ~10–15% (jitter de rede e CPU sharing)

**Solução**: Ping a cada 1–2s OU apenas antes/depois do throughput:

```kotlin
// Alternativa 1: Ping + throughput em série (mais preciso)
pingLatency = ping()
throughput = measureThroughput()  // sem concorrência

// Alternativa 2: Ping + throughput em paralelo, mas com intervalo relaxado
val pingJob = launch {
  while (isActive) {
    ping()
    delay(2000.ms)  // 2s entre pings — não impacta throughput
  }
}
```

**Acceptance criteria** (Issue #18):
- [ ] Ping concorrente reduzido a máximo 1 requisição/segundo
- [ ] Validação: throughput com novo intervalo ≤5% desvio vs. sem ping
- [ ] UX: exibir latência em real-time sem stutter

---

### 1.3 Energia: ConnectionPool Agressivo (Issue #19)

**Problema**: `OkHttpClient` global com `ConnectionPool(8, 60.seconds)` — mantém 8 conexões TCP abertas mesmo em modo Doze.

**Impacto**:
- Bateria: wake-locks causados por TCP keep-alives (~30s)
- Rede: tráfego desnecessário (SYN/ACK idle)

**Solução**: Reduzir para 2 conexões e 30s timeout; usar tipo de rede para ajustar:

```kotlin
private fun createHttpClient(context: Context): OkHttpClient {
  val connectivityManager = context.getSystemService<ConnectivityManager>()
  val isWifi = connectivityManager?.activeNetwork?.let { network ->
    connectivityManager.getNetworkCapabilities(network)?.hasTransport(TRANSPORT_WIFI) ?: false
  } ?: false
  
  val poolSize = if (isWifi) 4 else 2           // 4 em Wi-Fi, 2 em celular
  val poolIdleTime = if (isWifi) 60L else 30L   // 60s em Wi-Fi, 30s em celular
  
  return OkHttpClient.Builder()
    .connectionPool(ConnectionPool(poolSize, poolIdleTime, TimeUnit.SECONDS))
    .callTimeout(15000)  // global 15s timeout
    .build()
}

// Listener de rede: ajustar pool quando mudar de Wi-Fi para celular
connectivityManager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
  override fun onAvailable(network: Network) {
    // reconstruir cliente com novo poolSize/poolIdleTime
  }
})
```

**Acceptance criteria** (Issue #19):
- [ ] `ConnectionPool` ajustado por tipo de rede (Wi-Fi 4/60s, celular 2/30s)
- [ ] Validação: battery drain -5–10% em Idle Doze (medido via `dumpsys batterystats`)
- [ ] Funcionalidade: HTTP requests continuam funcionando sem latência extra

---

### 1.4 Energia: MonitoramentoWorker com .first() em Cascata (Issue #20)

**Problema**: Coleta de DataStore usando `.first()` em cascata (8 vezes) — bloqueia thread de trabalho por ~100ms cada uma.

**Evidência**:
```kotlin
// MonitoramentoWorker.kt:108-111
val sinal = sinalRepository.estado.first()      // ~50ms
val uptime = uptimeRepository.estado.first()    // ~50ms
val wifi = wifiRepository.estado.first()        // ~50ms
// ... mais 5 vezes

// Total: 8 × 50ms = 400ms de bloqueio por ciclo de trabalho
```

**Impacto**:
- Worker rodar a cada 30 min → 400ms × 48/dia = 19.2s bloqueado/dia
- Wake-lock extendido → bateria -8–12% em modo Doze

**Solução**: Usar `combine()` para coletar tudo paralelamente + cache local:

```kotlin
// Antes: cascata sequencial
val sinal = sinalRepository.estado.first()
val uptime = uptimeRepository.estado.first()
val wifi = wifiRepository.estado.first()

// Depois: paralelo com combine
data class MonitoringState(
  val sinal: Sinal,
  val uptime: Uptime,
  val wifi: Wifi,
)

val state = combine(
  sinalRepository.estado,
  uptimeRepository.estado,
  wifiRepository.estado,
) { sinal, uptime, wifi ->
  MonitoringState(sinal, uptime, wifi)
}.first()  // single .first() call, ~100ms total instead of 400ms
```

**Acceptance criteria** (Issue #20):
- [ ] `combine()` implementado para coletar múltiplas Flows
- [ ] Validação: tempo de coleta ≤100ms (vs. atual 400ms)
- [ ] Medição: battery drain -5–8% em Idle (vs. antes)

---

### 1.5 Energia: HTTP Timeout Global Faltante (Issue #21)

**Problema**: `MonitoramentoWorker` faz HTTP requests (`speedtest`, ping, health check) sem timeout global.

**Evidência**:
```kotlin
// MonitoramentoWorker.kt:164, 185-186
val request = Request.Builder().url(endpoint).build()
val response = okHttpClient.newCall(request).execute()  // no timeout!
```

**Impacto**:
- Rede lenta/congestionada → thread fica bloqueada indefinidamente
- Worker timeout do sistema (job cancellation) ocorre tarde → bateria drenada por até 10 min

**Solução**: Adicionar `callTimeout` global ao OkHttpClient + backoff exponencial:

```kotlin
// Cliente global com timeout
val okHttpClient = OkHttpClient.Builder()
  .callTimeout(15, TimeUnit.SECONDS)
  .connectTimeout(5, TimeUnit.SECONDS)
  .readTimeout(10, TimeUnit.SECONDS)
  .writeTimeout(10, TimeUnit.SECONDS)
  .build()

// Backoff exponencial no worker
suspend fun retryWithBackoff(
  operation: suspend () -> Response,
  maxRetries: Int = 3
): Response {
  repeat(maxRetries) { attempt ->
    try {
      return operation()
    } catch (e: SocketTimeoutException) {
      if (attempt == maxRetries - 1) throw
      delay(1000L * (attempt + 1))  // 1s, 2s, 3s
    }
  }
}
```

**Acceptance criteria** (Issue #21):
- [ ] `callTimeout(15s)` + `connectTimeout(5s)` + `readTimeout(10s)` configurados
- [ ] Backoff exponencial implementado (retry com 1s, 2s, 3s)
- [ ] Validação: worker sempre completa em <30s mesmo com rede instável

---

## 2. Otimizações de Compose/UI (Camilo + Claudete)

### 2.1 MainActivity: Recomposição em Cascata (Issue #22)

**Problema**: 40 `collectAsStateWithLifecycle` individuais sem `combine()` — qualquer atualização recompõe toda a `AppShell`.

**Evidência**:
```kotlin
// MainActivity.kt:67-136
val uiState1 by viewModel.state1.collectAsStateWithLifecycle()
val uiState2 by viewModel.state2.collectAsStateWithLifecycle()
// ... x40

// Composable
AppShell {
  NavigationHost()  // recomposto completamente a cada um deles
}
```

**Impacto**:
- Scroll em Home tela → 40 recomposições por scroll tick
- Frame drops (jank) em devices mid-tier

**Solução**:
```kotlin
// ViewModel
data class MainUiState(
  val navigation: NavigationState,
  val theme: ThemeState,
  val connectivity: ConnectivityState,
  val location: LocationState,
  // ... agrupe valores relacionados
)

// Combinar e distribuir
val uiState = combine(
  navigationState,
  themeState,
  connectivityState,
  locationState,
).distinctUntilChanged().stateIn(
  viewModelScope,
  SharingStarted.WhileSubscribed(5000),
  initialValue = MainUiState.Initial
)

// MainActivity
val appState by viewModel.uiState.collectAsStateWithLifecycle()
AppShell(appState)
```

**Acceptance criteria** (Issue #22):
- [ ] `MainUiState` data class criada com 5–8 grupos coesos
- [ ] `combine()` + `distinctUntilChanged()` implementado
- [ ] Validação: recomposições reduzidas ≥30% (Layout Inspector)
- [ ] Frame time em navegação ≤16ms (vs. atual 22–24ms)

---

### 2.2 ResultadoVelocidadeScreen: Canvas sem Remember (Issue #23)

**Problema**: `OrbitSymbolSmall()` renderiza `Canvas` com offsets/cores recalculados a cada composição — 40 Canvas elements no scroll.

**Impacto**:
- Frame drops em devices mid-tier (Pixel 4a, Moto G7) ao rolar resultado
- ~24–30ms frame time (vs. esperado 16ms)

**Solução**:
```kotlin
// Antes
@Composable
fun OrbitSymbolSmall() {
  Canvas(Modifier.size(100.dp)) {
    val offset = Offset(center.x + sin(angle) * radius, ...)  // recalculado sempre
    drawCircle(color, radius, offset)
  }
}

// Depois: memoizar valores caros
@Composable
fun OrbitSymbolSmall() {
  val cachedOffset = remember { mutableStateOf(Offset.Zero) }
  
  Canvas(Modifier.size(100.dp)) {
    cachedOffset.value = Offset(center.x + sin(angle) * radius, ...)
    drawCircle(color, 10.dp.toPx(), cachedOffset.value)
  }
}

// Ou: substituir Canvas por ImageVector pré-renderizada (mais eficiente)
@Composable
fun OrbitSymbolSmall() {
  Icon(
    imageVector = rememberOrbitIcon(),  // cached ImageVector
    contentDescription = "Orbit"
  )
}
```

**Acceptance criteria** (Issue #23):
- [ ] `OrbitSymbolSmall()` usa `remember` para offsets/cores
- [ ] Seções dinâmicas (>10 itens) migradas para `LazyColumn` com `key`
- [ ] Validação: frame time ≤16ms em Pixel 4a/Moto G7 (via `dumpsys gfxinfo`)
- [ ] UX visualmente idêntica

---

### 2.3 OkHttp DNS/Health Caching (Issue #24)

**Problema**: Chamadas auxiliares (DNS over HTTPS, health checks) vão sem cache — mesmo lookup DNS repetido 10x/min.

**Impacto**:
- Dados móveis: -20% em uso típico (excluindo speedtest)
- Latência percebida: +50–100ms por health check sem cache

**Solução**:
```kotlin
// Duas instâncias de cliente OkHttp
val okHttpSpeedtest = OkHttpClient.Builder()
  .addInterceptor { chain ->
    chain.proceed(
      chain.request().newBuilder()
        .header("Cache-Control", "no-store")
        .build()
    )
  }
  .build()

val okHttpAux = OkHttpClient.Builder()
  .cache(Cache(cacheDir, 5_MB))  // 5 MB cache em disco
  .addInterceptor { chain ->
    chain.proceed(
      chain.request().newBuilder()
        .header("Cache-Control", "max-age=300")  // 5 min cache
        .build()
    )
  }
  .build()

// Uso
// Speedtest → okHttpSpeedtest (sem cache)
// Health check → okHttpAux (com cache)
// DNS → okHttpAux (com cache)
```

**Acceptance criteria** (Issue #24):
- [ ] Duas instâncias de cliente (`okHttpSpeedtest`, `okHttpAux`)
- [ ] Cache de 5 MB configurado apenas para `okHttpAux`
- [ ] Endpoints de medição usam `okHttpSpeedtest`
- [ ] Validação: health check repetidas em <5min retornam 304 (from-cache)
- [ ] Impacto: -5–10% dados móveis em uso típico

---

## 3. Sequência de Implementação Recomendada

### Fase 1: Bloqueadores Críticos (Semana 1–2) — **Rodrigo + Gema**
- [ ] #1: Rotacionar keystore & purgar histórico Git
- [ ] #2: Network Security Config (remover cleartext global)

### Fase 2: Arquitetura & DI (Semana 2–3) — **Camilo**
- [ ] #3: Hilt (substituir DI manual)
- [ ] #4: Erradicar `!!` operators (refactor com Detekt)

### Fase 3: Qualidade & Tooling (Semana 3–4) — **Marina + Camilo**
- [ ] #5: Detekt + Ktlint + CI workflow
- [ ] #16: Elevar cobertura de testes em `core*`

### Fase 4: Otimizações de Dados & Energia (Semana 4–5) — **Brás + Camilo**
- [ ] #17: Detecção de rede medida (Speedtest adaptive payload)
- [ ] #18: Ping concorrente (relaxar intervalo 300ms → 2s)
- [ ] #19: ConnectionPool adaptativo por rede
- [ ] #20: Monitoramento com `combine()` (vs. .first() cascata)
- [ ] #21: HTTP timeouts global + backoff

### Fase 5: UI Performance (Semana 5–6) — **Camilo + Claudete**
- [ ] #22: MainActivity com `MainUiState` + `combine()`
- [ ] #23: ResultadoVelocidadeScreen com `remember` + LazyColumn
- [ ] #24: OkHttp DNS/health caching

### Fase 6: UX & Acessibilidade (Semana 6–7) — **Claudete + Gema**
- [ ] #10–12: Design System (strings i18n, acessibilidade, UiState padrão)
- [ ] #14–15: Documentação (ADRs, consolidação docs)

**Estimativa total**: 3–4 sprints (6–8 semanas) com 2 agentes em paralelo permanentemente.

---

## 4. Métricas de Sucesso

| Métrica | Baseline | Target | Método |
|---|---|---|---|
| **Frame time** (scroll Home/Resultado) | 22–24ms | ≤16ms | `dumpsys gfxinfo` |
| **Battery drain** (Doze 12h) | -25% | -15% | `dumpsys batterystats` |
| **Cold start** | ~800ms | ~600ms | Baseline Profile + profiling |
| **Dados móveis** (uso típico 1h) | ~150 MB | ~140 MB | Network Profiler |
| **Testes coverage** (`core*`) | ~30% | ≥70% | `./gradlew testDebugCoverage` |
| **Security scan** (OWASP MASVS) | L1 (básico) | L2 (intermediário) | `semgrep` + manual review |

---

## 5. Próximas Ações

1. ✅ **Agora**: Revisar este documento com todo o time (Camilo, Claudete, Gema, Rodrigo, Marina, Brás)
2. **Próxima semana**: Rodrigo inicia #1–#2 (bloqueadores críticos)
3. **Paralelo**: Camilo prepara PR com Hilt scaffolding para #3
4. **Tracking**: ADR por decision (ex.: "Por que 2 OkHttp clients?" → `docs_ai/technical/adr/005-okhttp-dual-client.md`)

