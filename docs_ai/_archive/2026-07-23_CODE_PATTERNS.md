# Padrões de Código Recomendados
## SIGNALLQ Android — Guidelines para Implementação

**Audiência**: Camilo, Claudete, Gema, Rodrigo, Marina, Brás  
**Status**: Normativo (obrigatório para PRs das issues #1–24)

---

## 1. Padrão de ViewModel com State Aggregation (Issue #22)

**Problema que resolve**: Recomposições desnecessárias em MainActivity.

### ✅ Padrão Recomendado

```kotlin
// 1. Defina um sealed state + agregador
data class MainUiState(
  val navigationState: NavigationState = NavigationState.Home,
  val themeState: ThemeState = ThemeState.System,
  val connectivityState: ConnectivityState = ConnectivityState.Unknown,
  val locationState: LocationState = LocationState.Unknown,
) {
  companion object {
    val Initial = MainUiState()
  }
}

// 2. ViewModel combina todos os Flows
class MainViewModel(
  private val navigationRepo: NavigationRepository,
  private val themeRepo: ThemeRepository,
  private val connectivityRepo: ConnectivityRepository,
  private val locationRepo: LocationRepository,
) : ViewModel() {
  
  val uiState: StateFlow<MainUiState> = combine(
    navigationRepo.state.distinctUntilChanged(),
    themeRepo.state.distinctUntilChanged(),
    connectivityRepo.state.distinctUntilChanged(),
    locationRepo.state.distinctUntilChanged(),
  ) { nav, theme, conn, loc ->
    MainUiState(
      navigationState = nav,
      themeState = theme,
      connectivityState = conn,
      locationState = loc,
    )
  }.distinctUntilChanged()  // evita recomposição se nenhum campo mudou
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      MainUiState.Initial
    )
}

// 3. Use em Composable
@Composable
fun AppShell(viewModel: MainViewModel = hiltViewModel()) {
  val appState by viewModel.uiState.collectAsStateWithLifecycle()
  
  NavigationHost(appState.navigationState)  // recomposto só se navigationState mudou
  ThemedSurface(appState.themeState)
  ConnectivityBanner(appState.connectivityState)
}
```

### ❌ Padrão Anti (que estamos eliminando)

```kotlin
// ❌ NÃO FAÇA ISSO: 40 collectAsStateWithLifecycle
@Composable
fun AppShell(viewModel: MainViewModel) {
  val nav1 by viewModel.state1.collectAsStateWithLifecycle()
  val nav2 by viewModel.state2.collectAsStateWithLifecycle()
  // ... x40
  
  // Toda Composable filha recompõe
}
```

---

## 2. Padrão de Logger Abstrato (Issue #6)

**Problema que resolve**: Acoplamento direto a `Log.*`, impossível de mockar em testes.

### ✅ Padrão Recomendado

```kotlin
// Interface abstrata
interface Logger {
  fun d(tag: String, message: String, throwable: Throwable? = null)
  fun w(tag: String, message: String, throwable: Throwable? = null)
  fun e(tag: String, message: String, throwable: Throwable? = null)
}

// Implementação com Timber
class TimberLogger : Logger {
  override fun d(tag: String, message: String, throwable: Throwable?) {
    Timber.tag(tag).d(throwable, message)
  }
  
  override fun w(tag: String, message: String, throwable: Throwable?) {
    Timber.tag(tag).w(throwable, message)
  }
  
  override fun e(tag: String, message: String, throwable: Throwable?) {
    Timber.tag(tag).e(throwable, message)
  }
}

// Injetar em repositórios/workers
class MonitoramentoWorker(
  context: Context,
  params: WorkerParameters,
  private val logger: Logger,  // injetado
) : CoroutineWorker(context, params) {
  
  override suspend fun doWork(): Result {
    logger.d("MonitoramentoWorker", "Iniciando monitoramento...")
    // ...
    return Result.success()
  }
}

// Teste: mock Logger facilmente
class MonitoramentoWorkerTest {
  @get:Rule
  val workerTestRule = WorkerTestRule()
  
  @Test
  fun testMonitoramento() {
    val mockLogger = mock<Logger>()
    val worker = MonitoramentoWorker(
      context = workerTestRule.context,
      params = WorkerParameters.Builder().build(),
      logger = mockLogger
    )
    
    workerTestRule.startWork(worker)
    
    verify(mockLogger).d("MonitoramentoWorker", "Iniciando monitoramento...")
  }
}
```

---

## 3. Padrão de Dispatchers Explícitos (Issue #7)

**Problema que resolve**: Coroutines em `Main` thread bloqueando UI.

### ✅ Padrão Recomendado

```kotlin
// Repositório injecta Dispatcher
class SinalRepository(
  private val database: SinalDatabase,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  
  suspend fun fetchSignalFromNetwork(): Signal = withContext(ioDispatcher) {
    // Executa em thread IO (não bloqueia Main)
    val response = apiService.getSignal()
    database.sinalDao.insert(response.toEntity())
    response.toModel()
  }
}

// ViewModel usa viewModelScope (Main por padrão)
class SinalViewModel(
  private val sinalRepo: SinalRepository,
) : ViewModel() {
  
  fun loadSignal() {
    viewModelScope.launch {  // Main context, é ok para atualizar UI
      val signal = sinalRepo.fetchSignalFromNetwork()  // withContext(Dispatchers.IO) interno
      _uiState.value = signal  // atualiza state na Main
    }
  }
}

// Teste: injetar Dispatcher de teste
class SinalRepositoryTest {
  private val testDispatcher = StandardTestDispatcher()
  
  @Test
  fun testFetchSignal() = runTest {
    val repo = SinalRepository(
      database = mockDatabase,
      ioDispatcher = testDispatcher
    )
    
    val signal = repo.fetchSignalFromNetwork()
    
    assertEquals(expectedSignal, signal)
  }
}
```

### ❌ Padrão Anti

```kotlin
// ❌ NÃO FAÇA ISSO: Dispatchers implícito
class SinalRepository(private val database: SinalDatabase) {
  suspend fun fetchSignalFromNetwork(): Signal {
    val response = apiService.getSignal()  // poderia estar em Main!
    database.sinalDao.insert(response.toEntity())
    return response.toModel()
  }
}
```

---

## 4. Padrão de UiState Selado (Issue #12)

**Problema que resolve**: Estados inconsistentes entre telas (Loading, Error, Empty, Success).

### ✅ Padrão Recomendado

```kotlin
sealed class UiState<T> {
  class Loading<T> : UiState<T>()
  data class Success<T>(val data: T) : UiState<T>()
  data class Empty<T>(val message: String = "No data") : UiState<T>()
  data class Error<T>(val exception: Throwable) : UiState<T>()
}

// ViewModel expõe StateFlow<UiState<T>>
class ResultadoVelocidadeViewModel(
  private val speedtestRepo: SpeedtestRepository,
) : ViewModel() {
  
  private val _uiState = MutableStateFlow<UiState<SpeedtestResult>>(UiState.Loading())
  val uiState = _uiState.asStateFlow()
  
  fun loadResult(testId: String) {
    viewModelScope.launch {
      _uiState.value = UiState.Loading()
      try {
        val result = speedtestRepo.getResult(testId)
        _uiState.value = if (result.isEmpty()) {
          UiState.Empty("No measurements found")
        } else {
          UiState.Success(result)
        }
      } catch (e: Exception) {
        _uiState.value = UiState.Error(e)
      }
    }
  }
}

// Composable genérico para renderizar UiState
@Composable
fun <T> StatefulScreen(
  uiState: UiState<T>,
  onSuccess: @Composable (T) -> Unit,
  onError: @Composable (Throwable) -> Unit = { DefaultErrorView(it) },
  onEmpty: @Composable () -> Unit = { DefaultEmptyView() },
  onLoading: @Composable () -> Unit = { DefaultLoadingView() },
) {
  when (uiState) {
    is UiState.Loading -> onLoading()
    is UiState.Success -> onSuccess(uiState.data)
    is UiState.Empty -> onEmpty()
    is UiState.Error -> onError(uiState.exception)
  }
}

// Uso em Screen
@Composable
fun ResultadoVelocidadeScreen(viewModel: ResultadoVelocidadeViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  
  StatefulScreen(
    uiState = uiState,
    onSuccess = { result ->
      Column {
        ResultadoHeader(result)
        ResultadoChart(result)
        ResultadoDetails(result)
      }
    },
    onError = { error ->
      ErrorDialog("Failed to load result", error.message)
    },
    onEmpty = {
      EmptyStateView("No speedtest results available")
    }
  )
}
```

---

## 5. Padrão de Network Security Config (Issue #2)

**Problema que resolve**: `usesCleartextTraffic="true"` globalmente expõe app a MITM.

### ✅ Padrão Recomendado

```xml
<!-- app/src/main/res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
  <!-- Por padrão: HTTPS obrigatório -->
  <domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">*.signallq.io</domain>
    <domain includeSubdomains="true">api.speedtest.net</domain>
  </domain-config>

  <!-- Exceção: apenas gateways locais permitem cleartext para diagnóstico -->
  <domain-config cleartextTrafficPermitted="true">
    <!-- RFC 1918: IP ranges privadas -->
    <domain includeSubdomains="true">10.0.0.0</domain>
    <domain includeSubdomains="true">172.16.0.0</domain>
    <domain includeSubdomains="true">192.168.0.0</domain>
    <domain includeSubdomains="true">localhost</domain>
    <domain includeSubdomains="true">127.0.0.1</domain>
  </domain-config>

  <!-- Pins para certificados críticos (opcional) -->
  <pin-set expiration="2026-12-31">
    <pin digest="SHA-256">+MIIDfTxtG7zsIWgV4+4qo0K+Pm+DwC6o0JG8l93zXo=</pin>
  </pin-set>
</network-security-config>
```

```xml
<!-- AndroidManifest.xml -->
<application
  android:networkSecurityConfig="@xml/network_security_config"
  ...>
</application>
```

### Validação

```bash
# Conferir que build de release não tem cleartext global
apkanalyzer manifest print app/build/outputs/apk/release/app-release.apk \
  | grep -i cleartext  # não deve retornar nada

# Teste manual: tentar HTTP em domínio público deve falhar
adb shell curl http://example.com  # deve falhar com SSLException
adb shell curl http://192.168.1.1  # deve funcionar (gateway local)
```

---

## 6. Padrão de Cache com OkHttp (Issue #24)

**Problema que resolve**: DNS/health checks sem cache desperdiçam dados móveis.

### ✅ Padrão Recomendado

```kotlin
// Factory para criar dois clientes distintos
class OkHttpClientFactory(
  private val context: Context,
  private val cacheDir: File,
) {
  
  // Cliente para speedtest: sem cache (payload medido é real)
  fun createSpeedtestClient(): OkHttpClient =
    OkHttpClient.Builder()
      .addInterceptor { chain ->
        chain.proceed(
          chain.request().newBuilder()
            .header("Cache-Control", "no-store")
            .build()
        )
      }
      .callTimeout(15, TimeUnit.SECONDS)
      .connectTimeout(5, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .writeTimeout(10, TimeUnit.SECONDS)
      .build()
  
  // Cliente para auxiliar (DNS, health, metadata): com cache
  fun createAuxiliaryClient(): OkHttpClient =
    OkHttpClient.Builder()
      .cache(Cache(File(cacheDir, "aux_cache"), 5 * 1024 * 1024))  // 5 MB
      .addInterceptor { chain ->
        chain.proceed(
          chain.request().newBuilder()
            .header("Cache-Control", "max-age=300")  // 5 min
            .build()
        )
      }
      .callTimeout(10, TimeUnit.SECONDS)
      .connectTimeout(3, TimeUnit.SECONDS)
      .readTimeout(5, TimeUnit.SECONDS)
      .writeTimeout(5, TimeUnit.SECONDS)
      .build()
}

// Injetar em Hilt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
  
  @Singleton
  @Qualifier
  @Retention(AnnotationRetention.BINARY)
  annotation class SpeedtestClient
  
  @Singleton
  @Qualifier
  @Retention(AnnotationRetention.BINARY)
  annotation class AuxiliaryClient
  
  @Singleton
  @SpeedtestClient
  @Provides
  fun providesSpeedtestClient(factory: OkHttpClientFactory): OkHttpClient =
    factory.createSpeedtestClient()
  
  @Singleton
  @AuxiliaryClient
  @Provides
  fun providesAuxiliaryClient(factory: OkHttpClientFactory): OkHttpClient =
    factory.createAuxiliaryClient()
}

// Usar em repositórios
class MonitoramentoRepository(
  @OkHttpClientFactory.SpeedtestClient private val speedtestClient: OkHttpClient,
  @OkHttpClientFactory.AuxiliaryClient private val auxClient: OkHttpClient,
) {
  
  suspend fun measureThroughput(): Long = withContext(Dispatchers.IO) {
    val request = Request.Builder().url("https://speedtest.cloudflare.com/__down").build()
    // usa speedtestClient (sem cache)
  }
  
  suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
    val request = Request.Builder().url("https://gateway.local/health").build()
    // usa auxClient (com cache)
  }
}
```

---

## 7. Padrão de Teste Unitário (Issue #5, #16)

**Problema que resolve**: Cobertura baixa, sem testes em `core*` módulos.

### ✅ Padrão Recomendado

```kotlin
// Test de Repositório com MockWebServer
@RunWith(RobolectricTestRunner::class)
class SinalRepositoryTest {
  
  private lateinit var mockWebServer: MockWebServer
  private lateinit var database: TestDatabase
  private lateinit var repository: SinalRepository
  
  @Before
  fun setup() {
    mockWebServer = MockWebServer()
    mockWebServer.start()
    
    database = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      SinalDatabase::class.java
    ).build()
    
    repository = SinalRepository(
      apiService = Retrofit.Builder()
        .baseUrl(mockWebServer.url("/"))
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SinalApiService::class.java),
      database = database,
      ioDispatcher = StandardTestDispatcher()
    )
  }
  
  @Test
  fun testFetchSignalSuccess() = runTest {
    // Mock server retorna sinal válido
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody("""{"latency": 20, "jitter": 5, "loss": 0.1}""")
    )
    
    val result = repository.fetchSignal()
    
    assertEquals(20, result.latency)
    assertEquals(5, result.jitter)
  }
  
  @Test
  fun testFetchSignalError() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(500))
    
    val exception = assertThrows<IOException> {
      repository.fetchSignal()
    }
    
    assertTrue(exception.message?.contains("500") ?: false)
  }
  
  @After
  fun teardown() {
    mockWebServer.shutdown()
    database.close()
  }
}

// Test de ViewModel com StateFlow
@RunWith(RobolectricTestRunner::class)
class SinalViewModelTest {
  
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()
  
  @Test
  fun testLoadSignalSuccess() = runTest {
    val mockRepo = mock<SinalRepository> {
      onBlocking { fetchSignal() } doReturn sampleSignal
    }
    
    val viewModel = SinalViewModel(sinalRepository = mockRepo)
    
    viewModel.loadSignal()
    advanceUntilIdle()
    
    val state = (viewModel.uiState.value as UiState.Success).data
    assertEquals(sampleSignal, state)
  }
}
```

---

## 8. Checklist para PR (Todas as Issues)

Antes de submeter um PR, valide:

- [ ] **Código**
  - [ ] Sem `!!` operators (ou com justificativa em comentário)
  - [ ] Sem `Log.d/e/w` direto (use Logger abstrato)
  - [ ] Dispatchers.IO em operações I/O (DB, rede)
  - [ ] `distinctUntilChanged()` em flows que emitem frequentemente
  - [ ] `remember()` para valores caros em Composables

- [ ] **Testes**
  - [ ] Novo teste unitário para função > 20 linhas
  - [ ] Coverage ≥ 70% em arquivos modificados
  - [ ] MockWebServer ou mock para externa calls

- [ ] **Segurança**
  - [ ] Sem credenciais em código (usar env vars)
  - [ ] Sem HTTP hardcoded em produção (usar config)
  - [ ] Sem `usesCleartextTraffic="true"` global

- [ ] **Performance**
  - [ ] Sem blocking calls em Main thread
  - [ ] Sem loops tights em Composable bodies
  - [ ] Sem allocations desnecessárias em hot paths

- [ ] **Documentação**
  - [ ] Método público tem KDoc
  - [ ] Tricky logic tem comentário explicativo
  - [ ] ADR criado se for decisão arquitetural

- [ ] **Commit & PR**
  - [ ] Mensagem de commit imperativa ("Fix X", "Add Y", não "Fixed X")
  - [ ] Apenas 1 concern por commit (não misturar refactor + feature)
  - [ ] PR title < 70 chars, body com "Fixes #N"

---

