> **Arquivado em 2026-07-16.** Consolidacao de documentacao docs_ai/ (branch docs/consolidacao-documentacao-docs-ai). Registro de migracao ja concluida (2026-06-21). Preservado apenas como referencia historica.

---

# Migração: AiDiagnosisRepository como Singleton via Hilt

**Data:** 2026-06-21  
**Versão:** v0.16.0  
**Status:** Consolidada

---

## O Que Era Antes (Problema)

`AiDiagnosisRepository` existia como dependência espalhada e instanciada em múltiplos pontos da aplicação, sem gerenciamento centralizado de ciclo de vida.

### Instâncias Detectadas

1. **MainViewModel** — lazy delegate via `mainRepositoryComponent.aiRepository()`
2. **SignallQOrchestrator** — campo privado instanciado no construtor
3. **ChatDiagnosticoIaViewModel** — lazy delegate via `mainRepositoryComponent.aiRepository()`
4. **AppShell (Composable)** — `remember { mainRepositoryComponent.aiRepository() }`
5. **DiagnosticoScreen (Composable)** — `remember { mainRepositoryComponent.aiRepository() }`

### Consequências

- **Duplicação de instâncias:** código esperava singleton, mas criava múltiplas cópias em runtime
- **URL duplicada:** `AI_BASE_URL` definida em 4 arquivos diferentes (featureDiagnostico, orchestrator, ViewModels, Composables)
- **Vazamento de lógica:** regras de negócio (instanciação, configuração) espalhadas em UI e ViewModels
- **Dificuldade de teste:** sem DI centralizado, injetar mocks era manual e frágil

---

## O Que Foi Feito (Solução)

Centralização total via **Hilt Singleton Component** + **BuildConfig** para URL.

### Passos Implementados

#### 1. **featureDiagnostico/build.gradle.kts**

Adicionados plugins e buildConfig:

```kotlin
plugins {
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {
    buildFeatures {
        buildConfig = true
    }
    
    buildTypes {
        release {
            buildConfigField(
                "String",
                "AI_WORKER_URL",
                "\"https://linka-ai-diagnosis-worker.example.workers.dev\""
            )
        }
        debug {
            buildConfigField(
                "String",
                "AI_WORKER_URL",
                "\"https://linka-ai-diagnosis-worker-dev.example.workers.dev\""
            )
        }
    }
}

dependencies {
    api("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
}
```

**Resultado:** URL centralizada em `BuildConfig.AI_WORKER_URL`.

#### 2. **featureDiagnostico/.../di/DiagnosticoModule.kt** (novo arquivo)

Módulo Hilt com escopo Singleton:

```kotlin
package io.veloo.app.kotlin.feature.diagnostico.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.veloo.app.kotlin.feature.diagnostico.BuildConfig
import io.veloo.app.kotlin.feature.diagnostico.ai.AiDiagnosisRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiagnosticoModule {
    
    @Provides
    @Singleton
    fun provideAiDiagnosisRepository(): AiDiagnosisRepository {
        return AiDiagnosisRepository(
            baseUrl = BuildConfig.AI_WORKER_URL
        )
    }
}
```

**Resultado:** repositório é criado uma única vez e compartilhado por toda a app.

#### 3. **featureDiagnostico/.../ai/AiDiagnosisRepository.kt**

Bloco `init` para validação em device (protegido por `runCatching`):

```kotlin
init {
    // Validacao de instancia unica (debug)
    runCatching {
        Log.d(
            "AiDiagnosisRepository",
            "Instancia singleton criada: ${System.identityHashCode(this)}"
        )
    }
}
```

**Resultado:** log de debug para confirmar singleton em device.

#### 4. **app/.../pulse/SignallQOrchestrator.kt**

Removida instanciação interna; repositório recebido via construtor:

```kotlin
class SignallQOrchestrator(
    private val aiRepository: AiDiagnosisRepository  // Injeção Hilt
) {
    // ... resto do código
    
    // AI_BASE_URL removido — usa repository.baseUrl
}
```

**Resultado:** orquestrador não cria repositório, apenas consome.

#### 5. **app/.../MainViewModel.kt**

Repositório injetado via construtor Hilt + repassado ao orquestrador:

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    val diagAiRepository: AiDiagnosisRepository,  // Singleton injetado
    private val othersServices: OtherServices
) : ViewModel() {
    
    private val orchestrator = SignallQOrchestrator(
        aiRepository = diagAiRepository
    )
    
    // ... resto do código
}
```

**Resultado:** MainViewModel recebe singleton e passa para orquestrador.

#### 6. **app/.../ui/viewmodel/ChatDiagnosticoIaViewModel.kt**

Repositório injetado via construtor:

```kotlin
@HiltViewModel
class ChatDiagnosticoIaViewModel @Inject constructor(
    val aiRepository: AiDiagnosisRepository  // Singleton injetado
) : ViewModel() {
    // ... resto do código
}
```

**Resultado:** ViewModel recebe singleton sem lazy delegate.

#### 7. **app/.../ui/screen/DiagnosticoScreen.kt** (Composable)

Repositório recebido como parâmetro, removida instanciação local:

```kotlin
@Composable
fun DiagnosticoScreen(
    aiRepository: AiDiagnosisRepository,  // Parametro da composable
    // ... outros parametros
) {
    // ... resto da UI
}
```

**Resultado:** regra de negócio removida da UI.

#### 8. **app/.../ui/screen/AppShell.kt** (Composable)

Repositório recebido como parâmetro, removida instanciação via `remember`:

```kotlin
@Composable
fun AppShell(
    diagAiRepository: AiDiagnosisRepository,  // Parametro da composable
    mainViewModel: MainViewModel,
    // ... outros parametros
) {
    // ... resto da navegacao
}
```

**Resultado:** AppShell é stateless em relação ao repositório.

#### 9. **app/.../MainActivity.kt**

Passa repositório do ViewModel para AppShell:

```kotlin
@Composable
fun MainScreen() {
    val mainViewModel: MainViewModel = hiltViewModel()
    
    AppShell(
        diagAiRepository = mainViewModel.diagAiRepository,
        mainViewModel = mainViewModel,
        // ... outros parametros
    )
}
```

**Resultado:** fluxo de injeção: Hilt → MainViewModel → AppShell → DiagnosticoScreen.

---

## Arquivos Modificados

| Arquivo | Alteração |
|---------|-----------|
| `featureDiagnostico/build.gradle.kts` | Plugins kapt + hilt, buildConfig AI_WORKER_URL |
| `featureDiagnostico/src/main/kotlin/.../di/DiagnosticoModule.kt` | **NOVO** — módulo Hilt com @Provides @Singleton |
| `featureDiagnostico/src/main/kotlin/.../ai/AiDiagnosisRepository.kt` | Bloco `init` com Log.d protegido por `runCatching` |
| `app/src/main/kotlin/.../pulse/SignallQOrchestrator.kt` | Repositório recebido via construtor, AI_BASE_URL removida |
| `app/src/main/kotlin/.../MainViewModel.kt` | @Inject construtor recebendo singleton |
| `app/src/main/kotlin/.../ui/viewmodel/ChatDiagnosticoIaViewModel.kt` | @Inject construtor recebendo singleton |
| `app/src/main/kotlin/.../ui/screen/DiagnosticoScreen.kt` | Repositório como parâmetro Composable |
| `app/src/main/kotlin/.../ui/screen/AppShell.kt` | Repositório como parâmetro Composable |
| `app/src/main/kotlin/.../MainActivity.kt` | Repassa `mainViewModel.diagAiRepository` para AppShell |

---

## Como Validar Que Funciona

### 1. **Build sem erros**

```bash
./gradlew clean build
```

Verificar que `DiagnosticoModule` é reconhecido e compilado.

### 2. **Debug Log em Device**

Rodando a app em emulador ou device, verificar logcat:

```
D/AiDiagnosisRepository: Instancia singleton criada: 12345678
```

O hash (`System.identityHashCode()`) deve ser **o mesmo em todos os logs** durante a sesão de app — confirmando singleton.

### 3. **Teste Unitário (opcional)**

```kotlin
@Test
fun testAiRepositorySingleton() {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val hiltTestApplication = HiltAndroidRule(this).apply { init() }
    
    val repo1: AiDiagnosisRepository = // via Hilt
    val repo2: AiDiagnosisRepository = // via Hilt
    
    assertTrue(repo1 === repo2)  // Mesma instância
}
```

### 4. **Fluxo de Chat/Diagnóstico**

Abrir diagnóstico → iniciar chat → verificar que requisições ao worker funcionam. Se falhar, logs de erro vão para `ChatDiagnosticoIaViewModel.onErrorResponse()`.

### 5. **Verificar URL Centralizada**

Em `featureDiagnostico/build/generated/source/buildConfig/`:

```kotlin
public final class BuildConfig {
    public static final String AI_WORKER_URL = "https://linka-ai-diagnosis-worker...workers.dev";
}
```

Deve conter exatamente a URL definida no build.gradle.kts.

---

## Benefícios

✓ **Singleton garantido** — Hilt garante uma única instância  
✓ **URL centralizada** — BuildConfig é fonte única de verdade  
✓ **Injeção declarativa** — `@Inject` clarifica dependências  
✓ **Testabilidade** — MockK ou testDouble pode substituir repositório em testes  
✓ **Sem vazamento de lógica** — UI é stateless em relação ao repositório  
✓ **Ciclo de vida gerenciado** — destruído quando app fecha  

---

## Retrocompatibilidade

- Semântica de API não muda — `AiDiagnosisRepository` continua público com mesmas funções
- Consumidores existentes (orchesrator, ViewModels, Composables) recebem singleton transparentemente
- BuildConfig é gerado automaticamente pelo Gradle — não requer mudanças manuais

---

## Referências

- [Hilt Dependency Injection — Android Developers](https://developer.android.com/training/dependency-injection/hilt-android)
- [SingletonComponent — Dagger Documentation](https://dagger.dev/api/latest/dagger/hilt/components/SingletonComponent.html)
- [Documento Técnico: AI_FLOW.md](../AI_FLOW.md)
