# Migração: SignallQOrchestrator para featureDiagnostico

**Status:** Concluído (mergeado em main)  
**Data:** 2026-06-21  
**Versão afetada:** v0.16.0+

---

## O que era antes (Problema)

`SignallQOrchestrator.kt` vivia no pacote `app` — especificamente em:
```
app/src/main/kotlin/io/veloo/app/kotlin/pulse/SignallQOrchestrator.kt
```

**Problemas:**
- Lógica de diagnóstico IA (orquestrador central) misturada com camada de app (MainViewModel, UI state mapping).
- Dependências de feature (SpeedTest, Network diagnostics, Wi-Fi analysis) puxadas transitivamente via `app`.
- Reutilização bloqueada: orquestrador é específico de diagnóstico, mas mora no módulo geral.
- Violação de princípio SOLID: `app` não deveria alocar lógica de negócio de feature.

---

## O que foi feito (Solução)

### 1. Transferência de código
`SignallQOrchestrator.kt` (976 linhas) foi movido para:
```
featureDiagnostico/src/main/kotlin/io/veloo/app/kotlin/feature/diagnostico/pulse/SignallQOrchestrator.kt
```

**Mudança de package:**
```kotlin
// Antes
package io.veloo.app.pulse

// Depois
package io.veloo.app.feature.diagnostico.pulse
```

### 2. Atualização de dependências em `featureDiagnostico`

`featureDiagnostico/build.gradle.kts` recebeu 3 novas dependências necessárias para compilação:

```kotlin
// Network diagnostics
implementation(projects.coreNetwork)

// Speedtest artifacts (ExecutorSpeedtest, ResultadoSpeedtest, ModoSpeedtest, etc.)
implementation(projects.featureSpeedtest)

// Logging
implementation(libs.timber)
```

### 3. Atualização de imports em `MainViewModel`

`app/src/main/kotlin/io/veloo/app/kotlin/MainViewModel.kt`:

```kotlin
// Antes
import io.veloo.app.pulse.SignallQOrchestrator

// Depois
import io.veloo.app.feature.diagnostico.pulse.SignallQOrchestrator
```

### 4. Remoção de arquivo legado

Arquivo original deletado:
```
app/src/main/kotlin/io/veloo/app/kotlin/pulse/SignallQOrchestrator.kt
```

### 5. Permanência de código de mapeamento UI

`SignallQUiStateMapper.kt` **permanece** em `app/pulse/` porque:
- É mapeamento de UI (state mapping) → responsabilidade da camada `app`.
- Transforma estado lógico do orquestrador em UI state para Compose.
- Depende de construtos de UI (Compose, Material Design tokens).

---

## Arquivos modificados

| Arquivo | Tipo | Mudança |
|---------|------|---------|
| `featureDiagnostico/build.gradle.kts` | Atualização | Adicionadas deps: coreNetwork, featureSpeedtest, timber |
| `featureDiagnostico/src/main/kotlin/io/veloo/app/kotlin/feature/diagnostico/pulse/SignallQOrchestrator.kt` | Novo | Movido de app/pulse/ com package atualizado |
| `app/src/main/kotlin/io/veloo/app/kotlin/MainViewModel.kt` | Atualização | Import corrigido: `io.veloo.app.feature.diagnostico.pulse.SignallQOrchestrator` |
| `app/src/main/kotlin/io/veloo/app/kotlin/pulse/SignallQOrchestrator.kt` | Deletado | Arquivo removido (conteúdo migrado) |

---

## Como validar que funciona

### 1. Compilação limpa
```bash
./gradlew clean build --no-build-cache
```
Deve passar sem erros de package/import.

### 2. Teste de imports
Verificar que não há ciclos de dependência:
```bash
./gradlew featureDiagnostico:dependencies
```
Confirmar que coreNetwork e featureSpeedtest aparecem como transitivas.

### 3. Teste funcional: Diagnóstico IA
- Abrir app SignallQ.
- Navegar para aba **Sinal** → botão **Diagnóstico** (overlay).
- Enviar pergunta via chat IA.
- Orquestrador deve invocar SpeedTest/Network diagnostics e retornar resposta.
- Não deve haver crashes de package/import.

### 4. Teste de build types
```bash
./gradlew assembleDebug
./gradlew assembleRelease --no-build-cache
```
Ambos devem compilar sem erros.

---

## Impacto arquitetural

### Melhoria
- **Separação de responsabilidades:** lógica de diagnóstico IA agora mora em seu módulo feature próprio.
- **Melhor organização:** `featureDiagnostico` é agora autocontido (orquestrador + UI + storage).
- **Reutilização:** orquestrador pode ser consumido por outros módulos/products sem puxar toda a camada `app`.

### Sem breaking changes
- `MainViewModel` continua funcionando igual — apenas import path muda.
- UI state mapping permanece em `app` (onde deve estar).
- API pública de `SignallQOrchestrator` não mudou.

---

## Commits relacionados

Dois commits atômicos foram criados:

1. **`feat(diagnostico): mover orquestrador de app → featureDiagnostico`**  
   - Adicionar deps em `build.gradle.kts`
   - Criar novo arquivo em featureDiagnostico com package atualizado

2. **`refactor(app): atualizar import de SignallQOrchestrator após migração`**  
   - Atualizar import em MainViewModel
   - Deletar arquivo legado de app/pulse/

---

## Referências

- [Documentação técnica — Módulos (`docs_ai/technical/MODULES.md`)](../MODULES.md)
- [Data Flow — Diagnóstico IA (`docs_ai/technical/DATA_FLOW.md`)](../DATA_FLOW.md)
- [Feature File Maps (`docs_ai/technical/FEATURE_FILE_MAPS.md`)](../FEATURE_FILE_MAPS.md)
