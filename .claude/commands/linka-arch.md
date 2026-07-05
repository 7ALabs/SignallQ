---
description: Guardião da arquitetura LINKA Android — orienta criação de módulos, screens, ViewModels, DAOs e serviços seguindo os padrões estabelecidos. Revisa código contra as regras arquiteturais.
argument-hint: [create <module|screen|viewmodel|dao|service> <Nome>|review <arquivo.kt>|map]
allowed-tools: Read(*), Bash(*), PowerShell(*)
---

## Contexto Arquitetural Atual (lido dos arquivos agora)

**Módulos ativos (settings.gradle.kts):**
!`cat "C:/Projetos/SignallQ Android/settings.gradle.kts" 2>/dev/null`

**SDKs e dependências principais (libs.versions.toml):**
!`cat "C:/Projetos/SignallQ Android/gradle/libs.versions.toml" 2>/dev/null`

**Telas existentes (ui/screen/):**
!`ls "C:/Projetos/SignallQ Android/app/src/main/kotlin/io/linka/app/kotlin/ui/screen/" 2>/dev/null`

**Componentes reutilizáveis (ui/component/):**
!`ls "C:/Projetos/SignallQ Android/app/src/main/kotlin/io/linka/app/kotlin/ui/component/" 2>/dev/null`

---

## Arquitetura LINKA — Regras Canônicas

### Visão Geral dos Módulos (16 módulos)

```
:app                  ← executável, composição de tudo
├── :coreNetwork      ← conectividade, gateway, DNS, callbacks
├── :corePermissions  ← permissões Android runtime
├── :coreDatabase     ← Room + migrations (schema v5)
├── :coreDatastore    ← preferências leves (DataStore)
├── :coreTelephony    ← Telephony API
├── :featureHome      ← tela principal
├── :featureWifi      ← sinal, canal, AP atual, link data
├── :featureDevices   ← scanner progressivo (ARP, mDNS, SSDP, NBNS)
├── :featureDns       ← benchmark DoH (multi-sample, mediana, success rate)
├── :featureSpeedtest ← speedtest canônico (Cloudflare), modos fast/completo
├── :featureDiagnostico ← interpretação local + recomendações (DiagnosticRunner)
├── :featureFibra     ← login modem + saúde fibra/WAN
├── :featureHistory   ← histórico + tendência (janela 24h)
└── :featureSettings  ← preferências e credenciais
```

### Lei das Dependências (NUNCA violar)

```
:app → :feature* + :core*
:feature* → :core* APENAS
:core* → sem dependências internas do projeto
:feature* → :feature* PROIBIDO
```

**Regra:** se uma feature precisa de dados de outra feature, extraia para um `:core*` compartilhado.

### Stack Tecnológico

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| UI | Jetpack Compose + Material 3 | BOM 2025.05.01 |
| Estado | MVVM + `StateFlow` / `Flow` | — |
| Persistência | Room | 2.8.4 |
| Preferências | DataStore Preferences | 1.1.1 |
| HTTP | OkHttp | 4.12.0 |
| Concorrência | Coroutines | 1.9.0 |
| Min SDK | Android 7.0 | API 24 |

### Padrão MVVM por Feature

```
feature<Nome>/
└── src/main/kotlin/io/linka/<nome>/
    ├── data/
    │   ├── <Nome>Repository.kt       ← interface do repositório
    │   └── <Nome>RepositoryImpl.kt   ← implementação
    ├── domain/
    │   └── <modelo>.kt               ← modelos de domínio (data class)
    ├── presentation/
    │   ├── <Nome>ViewModel.kt        ← ViewModel + StateFlow + UiState
    │   └── <Nome>UiState.kt          ← sealed class de estados
    └── ui/
        └── <Nome>Screen.kt           ← @Composable screen (sem lógica)
```

### Padrão de Screen Composable

```kotlin
// Nome do arquivo: {Feature}Screen.kt
// Localização: app/ui/screen/ (telas da app) ou feature*/ui/ (telas do módulo)

@Composable
fun NomeScreen(
    viewModel: NomeViewModel = viewModel(),
    onNavigate: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tokens = LocalLkTokens.current

    Scaffold(
        topBar = { /* TopAppBar com tokens */ },
        containerColor = tokens.bgPrimary
    ) { padding ->
        // conteúdo — sem lógica de negócio aqui
    }
}
```

**Regras para screens:**
- Sem lógica de negócio — apenas estado → UI
- Recebe `UiState` via `collectAsStateWithLifecycle()`
- Usa sempre `LocalLkTokens.current` para cores
- Nome do arquivo: `PascalCase` + sufixo `Screen.kt`

### Padrão de ViewModel

```kotlin
class NomeViewModel(
    private val repository: NomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NomeUiState())
    val uiState: StateFlow<NomeUiState> = _uiState.asStateFlow()

    fun onAction(action: NomeAction) {
        viewModelScope.launch {
            // lógica de negócio
        }
    }
}

data class NomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    // dados específicos
)
```

### Padrão Room (Entity + DAO)

```kotlin
// Entity — em coreDatabase/
@Entity(tableName = "nome_tabela")
data class NomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campo: String,
    val timestamp: Long = System.currentTimeMillis()
)

// DAO — em coreDatabase/
@Dao
interface NomeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: NomeEntity): Long

    @Query("SELECT * FROM nome_tabela ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<NomeEntity>>
}
```

### Convenções de Nomenclatura

| Elemento | Padrão | Exemplo |
|----------|--------|---------|
| Módulo | camelCase | `featureWifi`, `coreNetwork` |
| Arquivo Kotlin | PascalCase | `VelocidadeScreen.kt` |
| Composable | PascalCase | `GaugeCircular` |
| ViewModel | PascalCase + `ViewModel` | `SpeedtestViewModel` |
| Repository | PascalCase + `Repository` | `NetworkRepository` |
| Entity (Room) | PascalCase + `Entity` | `MedicaoEntity` |
| DAO | PascalCase + `Dao` | `MedicaoDao` |
| UiState | PascalCase + `UiState` | `DiagnosticoUiState` |
| Identificadores internos | camelCase pt-BR | `telaConsulta`, `servicoRede` |

**PROIBIDO:** hifens em nomes de módulo/pasta (`feature-wifi`), sufixos redundantes (`NomeScreenScreen`), nomes em inglês para código de domínio interno.

### Princípios Arquiteturais (do arquiteturaAndroidKotlin.md)

1. **Backend antes de frontend** — serviço/repositório primeiro, UI depois
2. **Dados antes de UI** — modelo de dados definido antes da tela
3. **Contratos antes de implementação** — interface do repositório antes da impl
4. **Simplicidade antes de sofisticação** — sem over-engineering
5. **Sem duplicação de lógica** — repositórios e serviços não reimplementam o que já existe em outro módulo

---

## Sua Tarefa

**Argumento recebido:** $ARGUMENTS

### Modo `create <tipo> <Nome>`

**Tipos suportados:** `module`, `screen`, `viewmodel`, `dao`, `service`, `component`

1. Identifique o módulo correto onde o elemento deve viver
2. Verifique se já existe algo equivalente (consulte a lista de telas/componentes injetada acima)
3. Valide o nome contra as convenções (PascalCase, pt-BR, sem hifens)
4. Gere o código completo seguindo os padrões acima:
   - Screen → com Scaffold, `LocalLkTokens`, `collectAsStateWithLifecycle`
   - ViewModel → com `StateFlow`, `UiState`, `viewModelScope`
   - DAO + Entity → com Room annotations, `Flow`, `suspend`
   - Componente → `@Composable` stateless, recebe estado como parâmetro
5. Informe o caminho exato onde o arquivo deve ser criado
6. Liste quais docs precisam de atualização (acione `/linka-docs impact` para o mapeamento completo)

### Modo `review <arquivo.kt>`

1. Leia o arquivo indicado
2. Verifique contra as regras arquiteturais:
   - Módulo correto? (feature vs core vs app)
   - Dependências respeitam a lei das camadas?
   - ViewModel usando StateFlow corretamente?
   - Screen sem lógica de negócio?
   - Room Entity/DAO no padrão?
   - Nomenclatura correta (PascalCase, pt-BR, sufixos)?
   - Sem feature→feature dependency?
3. Gere relatório com linha, problema e correção sugerida
4. Pergunte se quer aplicar as correções

### Modo `map`

Exiba o mapa completo dos módulos com suas responsabilidades, arquivos principais e status (implementado/pendente), lendo a estrutura de diretórios atual.

### Sem argumento — modo consultor

Pergunte o que o usuário está tentando criar ou entender, e oriente sobre onde e como deve ser implementado segundo a arquitetura estabelecida.
