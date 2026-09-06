---
description: Guardião da arquitetura Android SignallQ — orienta criação de módulos, screens, ViewModels, DAOs e serviços seguindo os padrões estabelecidos. Revisa código contra as regras arquiteturais.
argument-hint: [create <module|screen|viewmodel|dao|service> <Nome>|review <arquivo.kt>|map]
allowed-tools: Read(*), Bash(*)
---

## Contexto arquitetural atual (lido em tempo real)

**Módulos ativos (settings.gradle.kts):**
!`cat "${CLAUDE_PROJECT_DIR:-.}/android/settings.gradle.kts" 2>/dev/null`

**SDKs e dependências principais (libs.versions.toml):**
!`cat "${CLAUDE_PROJECT_DIR:-.}/android/gradle/libs.versions.toml" 2>/dev/null`

**Telas existentes (ui/screen/):**
!`ls "${CLAUDE_PROJECT_DIR:-.}/android/app/src/main/kotlin/io/signallq/app/ui/screen/" 2>/dev/null`

**Componentes reutilizáveis (ui/component/):**
!`ls "${CLAUDE_PROJECT_DIR:-.}/android/app/src/main/kotlin/io/signallq/app/ui/component/" 2>/dev/null`

> Path físico e package Kotlin agora estão alinhados em `io/signallq/app/` (migração resolvida em 2026-08-15, ver [regra de higiene §4.1](../rules/higiene-e-padronizacao-repositorio.md)).

---

## Arquitetura SignallQ — regras canônicas

### Visão geral dos módulos

Ver a fonte real: [`android/settings.gradle.kts`](../../android/settings.gradle.kts) + [`docs_ai/ARQUITETURA/README.md`](../../docs_ai/ARQUITETURA/README.md) + [`docs_ai/ARQUITETURA/MODULOS/`](../../docs_ai/ARQUITETURA/MODULOS/).

Grupos principais:

```
:app                  executável, composição de tudo
:core:*               infraestrutura compartilhada (network, database, datastore,
                      permissions, telephony, recommendation, featureflags)
:feature:*            features do app (home, wifi, devices, dns, speedtest,
                      diagnostico, fibra, history, settings)
```

### Lei das dependências (nunca violar)

```
:app       → :feature:* + :core:*
:feature:* → :core:* APENAS
:core:*    → sem dependências internas do projeto
:feature:* → :feature:* PROIBIDO
```

**Regra:** se uma feature precisa de dados de outra feature, extraia para um `:core:*` compartilhado. Composição entre domínios acontece em `:app` ou por contratos normalizados em um módulo `core` adequado (ver [regra de higiene §4.9](../rules/higiene-e-padronizacao-repositorio.md)).

### Stack tecnológico

Fonte da verdade: [`android/gradle/libs.versions.toml`](../../android/gradle/libs.versions.toml). Grupos aplicáveis: Compose (BOM), Room, Hilt, DataStore, OkHttp, Coroutines, min/target SDK.

### Padrão MVVM por feature

```
feature/<nome>/
└── src/main/kotlin/io/signallq/app/feature/<nome>/
    ├── data/
    │   ├── <Nome>Repository.kt        interface do repositório
    │   └── <Nome>RepositoryImpl.kt    implementação
    ├── domain/
    │   └── <modelo>.kt                data classes de domínio
    ├── presentation/
    │   ├── <Nome>ViewModel.kt         ViewModel + StateFlow + UiState
    │   └── <Nome>UiState.kt           sealed class de estados
    └── ui/
        └── <Nome>Screen.kt            @Composable screen (sem lógica de negócio)
```

### Padrão de Screen Composable

```kotlin
@Composable
fun NomeScreen(
    viewModel: NomeViewModel = viewModel(),
    onNavigate: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tokens = LocalLkTokens.current

    Scaffold(
        topBar = { /* TopAppBar com tokens */ },
        containerColor = tokens.bgPrimary,
    ) { padding ->
        // conteúdo — sem lógica de negócio aqui
    }
}
```

Regras: sem lógica de negócio; `collectAsStateWithLifecycle()`; cores via `LocalLkTokens.current`; arquivo em `PascalCase` + sufixo `Screen.kt`.

### Padrão de ViewModel

```kotlin
class NomeViewModel(
    private val repository: NomeRepository,
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
@Entity(tableName = "nome_tabela")
data class NomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campo: String,
    val timestamp: Long = System.currentTimeMillis(),
)

@Dao
interface NomeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: NomeEntity): Long

    @Query("SELECT * FROM nome_tabela ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<NomeEntity>>
}
```

### Convenções de nomenclatura

| Elemento | Padrão | Exemplo |
|---|---|---|
| Alias Gradle | camelCase | `featureWifi`, `coreNetwork` (migração para `:feature:wifi` é tarefa dedicada — ver higiene §5) |
| Arquivo Kotlin | PascalCase | `VelocidadeScreen.kt` |
| Composable | PascalCase | `GaugeCircular` |
| ViewModel | PascalCase + `ViewModel` | `SpeedtestViewModel` |
| Repository | PascalCase + `Repository` | `NetworkRepository` |
| Entity (Room) | PascalCase + `Entity` | `MedicaoEntity` |
| DAO | PascalCase + `Dao` | `MedicaoDao` |
| UiState | PascalCase + `UiState` | `DiagnosticoUiState` |
| Identificadores internos | camelCase pt-BR | `telaConsulta`, `servicoRede` |

**Proibido:** hifens em nomes de módulo/pasta; sufixos redundantes (`NomeScreenScreen`); nomes em inglês para código de domínio interno.

Idioma e nomes preservados: consultar [regra de higiene §2](../rules/higiene-e-padronizacao-repositorio.md).

### Princípios arquiteturais

1. **Backend antes de frontend** — serviço/repositório primeiro, UI depois.
2. **Dados antes de UI** — modelo de dados definido antes da tela.
3. **Contratos antes de implementação** — interface do repositório antes da impl.
4. **Simplicidade antes de sofisticação** — sem over-engineering.
5. **Sem duplicação de lógica** — repositórios e serviços não reimplementam o que já existe em outro módulo.

---

## Sua tarefa

**Argumento recebido:** $ARGUMENTS

### Modo `create <tipo> <Nome>`

Tipos suportados: `module`, `screen`, `viewmodel`, `dao`, `service`, `component`.

1. Identifique o módulo correto onde o elemento deve viver.
2. Verifique se já existe algo equivalente (consultar as listagens injetadas acima).
3. Valide o nome contra as convenções (PascalCase, pt-BR, sem hifens).
4. Gere o código completo seguindo os padrões acima.
5. Informe o caminho exato onde o arquivo deve ser criado.
6. Liste quais docs precisam de atualização (acione `/signallq-docs impact`).

### Modo `review <arquivo.kt>`

1. Leia o arquivo indicado.
2. Verifique contra as regras arquiteturais (módulo correto, lei das camadas, StateFlow, Screen sem lógica, Room, nomenclatura, sem `:feature:*` → `:feature:*`).
3. Gere relatório com linha, problema e correção sugerida.
4. Pergunte se quer aplicar as correções (Camilo executa; Caio faz revisão independente antes do merge).

### Modo `map`

Exiba o mapa dos módulos com responsabilidades e status lendo a estrutura atual do disco (`android/settings.gradle.kts`, `android/*/build.gradle.kts`, `docs_ai/ARQUITETURA/MODULOS/`).

### Sem argumento — modo consultor

Pergunte o que o usuário está tentando criar ou entender e oriente sobre onde e como deve ser implementado.
