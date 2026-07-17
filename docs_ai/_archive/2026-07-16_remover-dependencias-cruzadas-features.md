> **Arquivado em 2026-07-16.** Consolidacao de documentacao docs_ai/ (branch docs/consolidacao-documentacao-docs-ai). Registro de migracao ja concluida (2026-06-21). Preservado apenas como referencia historica.

---

# Migração: Remover Dependências Cruzadas de Features

**Status:** Mergeado para `main`  
**Branch:** `feat/remover-dependencias-cruzadas-features`  
**Data:** 2026-06-21  
**Commits:** 3 commits atômicos

---

## O que era antes (Problema)

A arquitetura possuía **dependências cruzadas entre features de domínio**:

- **featureDiagnostico** importava de **featureWifi** (`import io.veloo.app.feature.wifi.channel.*`)
- **featureDiagnostico** importava de **featureFibra** (`import io.veloo.app.kotlin.feature.fibra.*`)

**Consequência:** violação do princípio de modularização isolada. Features não deveriam importar de outras features — apenas de módulos core (coreNetwork, coreDatabase, etc.). Isso criava ciclos lógicos e dificultava testes, reuso de código e refatorações futuras.

### Grafo de dependências antes

```
featureDiagnostico
├── featureWifi ✗ (cross-feature import)
├── featureFibra ✗ (cross-feature import)
├── coreNetwork
├── coreDatabase
├── coreDatastore
└── featureSpeedtest
```

---

## O que foi feito (Solução)

Estratégia: **Extrair contratos compartilhados para `coreNetwork`**, transformando tipos e funções em abstrações reutilizáveis que qualquer feature pode consumir sem conhecer a implementação.

### Arquivos criados em `coreNetwork/src/main/kotlin/io/veloo/app/core/network/contracts/`

#### WiFi Contracts
- `wifi/RedeVizinha.kt` — modelo de rede vizinha detectada (SSID, BSSID, frequência, nível de sinal)
- `wifi/channel/ChannelEvalModels.kt` — tipos essenciais: `Band`, `ChannelWidth`, `ChannelScore`, `EvalConfig`
- `wifi/channel/ChannelEvaluator.kt` — função `evaluateChannels(...)` — algoritmo de seleção de melhor canal
- `wifi/channel/FrequencyUtils.kt` — função `freqToChannel(...)` — conversão frequência ↔ canal
- `wifi/channel/ChannelCandidates.kt` — constantes de canais e larguras suportadas

#### Fibra Contracts
- `fibra/GponSaudeStatus.kt` — modelo de status de saúde GPON (descida/subida/latência/perda)
- `fibra/ClassificadorSaudeGpon.kt` — função `classificarSaudeGpon(...)` — avaliação de qualidade GPON

### Refatoração por módulo

#### 1. **featureWifi** — Alinhamento via type aliases e delegates
- Convertidos tipos locais em **type aliases** apontando para coreNetwork:
  ```kotlin
  // featureWifi/src/main/kotlin/.../RedeVizinha.kt
  typealias RedeVizinha = io.veloo.app.core.network.contracts.wifi.RedeVizinha
  ```
- Convertidas funções locais em **delegates** (re-export):
  ```kotlin
  // featureWifi/src/main/kotlin/.../ChannelEvaluator.kt
  fun evaluateChannels(...) = 
    io.veloo.app.core.network.contracts.wifi.channel.evaluateChannels(...)
  ```
- **Impacto:** Nenhum import externo ao módulo alterado; consumidores veem a mesma API, transparentemente redirecionada para coreNetwork

#### 2. **featureFibra** — Nova dependência de coreNetwork
- `GponSaudeStatus.kt` → typealias de `coreNetwork.contracts.fibra.GponSaudeStatus`
- Adicionada dependência em `build.gradle.kts`:
  ```gradle
  implementation(project(":coreNetwork"))
  ```

#### 3. **featureDiagnostico** — Remoção de dependências cruzadas
- **Removidas linhas do `build.gradle.kts`:**
  ```gradle
  implementation(project(":featureWifi"))      // ✗ removido
  implementation(project(":featureFibra"))     // ✗ removido
  ```
- **Adicionada dependência centralizada:**
  ```gradle
  implementation(project(":coreNetwork"))     // ✓ adicionado
  ```
- **4 arquivos atualizados com imports novos:**
  - `FibraSignalQualityEngine.kt` — imports de `coreNetwork.contracts.fibra`
  - `WifiChannelDiagnosticEngine.kt` — imports de `coreNetwork.contracts.wifi`
  - `topology/TopologyDiagnostic.kt` — imports compartilhados de WiFi
  - `topology/lan/MeshDetector.kt` — imports compartilhados de WiFi

### Grafo de dependências depois

```
featureDiagnostico
├── coreNetwork ✓ (única source of truth para contratos)
├── coreDatabase
├── coreDatastore
└── featureSpeedtest

featureWifi
├── coreNetwork
└── ...

featureFibra
├── coreNetwork
└── ...

coreNetwork ✗ (zero feature dependencies — acíclico)
```

---

## Arquivos modificados

### coreNetwork — Novos contratos
- `coreNetwork/src/main/kotlin/io/veloo/app/core/network/contracts/wifi/RedeVizinha.kt`
- `coreNetwork/src/main/kotlin/io/veloo/app/core/network/contracts/wifi/channel/ChannelEvalModels.kt`
- `coreNetwork/src/main/kotlin/io/veloo/app/core/network/contracts/wifi/channel/ChannelEvaluator.kt`
- `coreNetwork/src/main/kotlin/io/veloo/app/core/network/contracts/wifi/channel/FrequencyUtils.kt`
- `coreNetwork/src/main/kotlin/io/veloo/app/core/network/contracts/wifi/channel/ChannelCandidates.kt`
- `coreNetwork/src/main/kotlin/io/veloo/app/core/network/contracts/fibra/GponSaudeStatus.kt`
- `coreNetwork/src/main/kotlin/io/veloo/app/core/network/contracts/fibra/ClassificadorSaudeGpon.kt`

### featureWifi — Alinhamento (type aliases, delegates)
- `featureWifi/src/main/kotlin/io/veloo/app/kotlin/feature/wifi/RedeVizinha.kt` (typealias)
- `featureWifi/src/main/kotlin/io/veloo/app/feature/wifi/channel/ChannelEvalModels.kt` (typealias)
- `featureWifi/src/main/kotlin/io/veloo/app/feature/wifi/channel/ChannelEvaluator.kt` (delegate)
- `featureWifi/src/main/kotlin/io/veloo/app/feature/wifi/channel/FrequencyUtils.kt` (delegate)
- `featureWifi/src/main/kotlin/io/veloo/app/feature/wifi/channel/ChannelCandidates.kt` (typealias/delegate)

### featureFibra — Nova dependência
- `featureFibra/build.gradle.kts` — adicionado `implementation(project(":coreNetwork"))`
- `featureFibra/src/main/kotlin/io/veloo/app/kotlin/feature/fibra/GponSaudeStatus.kt` (typealias)

### featureDiagnostico — Remoção de cross-feature imports
- `featureDiagnostico/build.gradle.kts` — removido `:featureWifi`, `:featureFibra`; adicionado `:coreNetwork`
- `featureDiagnostico/src/main/kotlin/io/veloo/app/kotlin/feature/diagnostico/FibraSignalQualityEngine.kt` — imports atualizados
- `featureDiagnostico/src/main/kotlin/io/veloo/app/kotlin/feature/diagnostico/WifiChannelDiagnosticEngine.kt` — imports atualizados
- `featureDiagnostico/src/main/kotlin/io/veloo/app/kotlin/feature/diagnostico/topology/TopologyDiagnostic.kt` — imports atualizados
- `featureDiagnostico/src/main/kotlin/io/veloo/app/kotlin/feature/diagnostico/topology/lan/MeshDetector.kt` — imports atualizados

---

## Como validar que funciona

### 1. Compilação sem erros
```bash
./gradlew clean build
```
✓ Nenhum erro de import ou resolução de dependência.

### 2. Testes unitários do módulo diagnostic
```bash
./gradlew :featureDiagnostico:test
```
✓ Todos os testes passam (nenhum mock ou import quebrado).

### 3. Testes de integração featureWifi (backwards compatibility)
```bash
./gradlew :featureWifi:test
```
✓ Type aliases e delegates são transparentes; testes passam sem alteração.

### 4. Inspecionar grafo de dependências
```bash
./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -E "(featureWifi|featureFibra|coreNetwork)"
```
✓ Confirmação: `app` → `featureDiagnostico` → `coreNetwork` (sem featureWifi/featureFibra diretos).

### 5. Lint e análise estática
```bash
./gradlew detekt ktlintCheck
```
✓ Sem problemas de qualidade introduzidos.

### 6. Teste funcional manual — Diagnóstico de WiFi
- Abrir tela de Diagnóstico
- Completar diagnóstico de WiFi
- Validar que canais recomendados são exibidos corretamente
- ✓ Sem erros de runtime, interface responde normalmente

### 7. Teste funcional manual — Diagnóstico de Fibra
- Abrir tela de Diagnóstico
- Completar diagnóstico de Fibra
- Validar que status de saúde GPON é exibido
- ✓ Sem erros de runtime

---

## Benefícios da refatoração

1. **Acíclico** — coreNetwork não depende de features; features não se importam
2. **Testável** — tipos contratuais isolados, facilita mocks e testes unitários
3. **Reutilizável** — qualquer novo módulo pode consumir `coreNetwork.contracts` sem conhecer featureWifi/featureFibra
4. **Manutenível** — mudanças em lógica de avaliação (WiFi/Fibra) ficam centralizadas em coreNetwork
5. **Transparente** — consumidores externos veem a mesma API; redirecionamento via type aliases/delegates
6. **Preparado para refatoração futura** — se algum contrato crescer (ex: suporte a 6GHz), apenas coreNetwork muda

---

## Notas de implementação

- **Type aliases vs. Delegates:** Usamos ambas conforme necessário. Type aliases (para tipos data) são mais leves; delegates (para funções) permitem lógica envolvente se necessária futuramente.
- **Compatibilidade retroativa:** Nenhuma quebra de API em featureWifi ou featureFibra. Código consumidor permanece inalterado.
- **Sem mudanças funcionais:** O comportamento de avaliação de canais, classificação GPON e detecção de topologia é idêntico ao antes.
