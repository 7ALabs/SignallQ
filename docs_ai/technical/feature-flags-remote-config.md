# Fundação de Feature Flags do Consumer — Firebase Remote Config (`:core:featureflags`)

- **Status:** ativo
- **Última validação:** 2026-07-26
- **Fonte de verdade:** este arquivo para o mecanismo técnico do módulo `:core:featureflags`; o
  catálogo em si (schema completo, valores reais) tem fonte de verdade única no arquivo JSON —
  `android/core/featureflags/src/main/resources/featureflags/consumer-catalog.json` — não copiado
  aqui além de exemplos ilustrativos.
- **Escopo:** módulo Android `:core:featureflags` (issue #1477, Épico #1347) — catálogo tipado,
  `FeatureFlagProvider`, integração com Firebase Remote Config. **Não cobre** o backend do Admin
  (F2/#1478) nem a UI do SignallQ Admin (F3/#1479), ainda não implementados — nem os dois sistemas
  de feature flags mais antigos do repositório (compile-time `FeatureFlags.kt` e o remoto legado
  SIG-13 `FeatureFlagManager`/`FeatureFlagRepository`), documentados em
  `docs_ai/TECNICO.md` §5.2 e `docs_ai/functional/FEATURE_FLAGS.md`.
- **Responsável:** Camilo (implementação Android). Consumidores previstos: Bruno (F2, backend
  Worker), Marina (F3, UI Admin), Camilo/outro (F4, instrumentação dos 9 módulos feature).

---

## 1. Objetivo técnico

Criar a camada Android compartilhada que **todo o resto do Épico #1347 depende**: um catálogo
tipado versionado no repositório e um único ponto de acesso (`FeatureFlagProvider`) sobre o
Firebase Remote Config, para que o SignallQ Admin (fases futuras) possa governar remotamente
funcionalidades do Consumer sem exigir nova publicação de app.

Esta Feature (#1477) entrega só a fundação: módulo, contratos, catálogo com 2 flags de
smoke-test. **Não** instrumenta os 9 módulos `feature/*` reais (isso é F4/#1480) nem o backend/UI
do Admin (F2/#1478, F3/#1479).

---

## 2. Visão geral da solução

```
android/core/featureflags/                       (:core:featureflags, novo módulo Gradle)
├── src/main/resources/featureflags/
│   └── consumer-catalog.json                     (catálogo canônico — fonte da verdade)
└── src/main/kotlin/io/signallq/app/core/featureflags/
    ├── FeatureFlagKey.kt                         (value class — identificador tipado)
    ├── FeatureFlagKeys.kt                         (constantes — único jeito de referenciar uma flag)
    ├── FeatureFlagType.kt / Criticality.kt / DisabledBehavior.kt / Source.kt   (enums do schema)
    ├── FeatureFlagRawValue.kt / FeatureFlagValue.kt   (valor tipado, com/sem contexto de origem)
    ├── FeatureFlagDefinition.kt                   (entrada do catálogo, 1:1 com o schema JSON)
    ├── FeatureFlagCatalogParser.kt                 (JSON → List<FeatureFlagDefinition>, puro)
    ├── FeatureFlagCatalogLoader.kt                 (lê o classpath, delega ao parser)
    ├── FeatureFlagCatalog.kt                       (lookup em memória + defaults)
    ├── FeatureFlagProvider.kt                       (interface pública — observe/isEnabled/refresh)
    ├── RemoteConfigFeatureFlagProvider.kt            (implementação real sobre FirebaseRemoteConfig)
    └── FeatureFlagsModulo.kt                         (fábrica — wiring Hilt fica em :app/AppModule)
```

`:core:featureflags` é consumido **apenas** por `:app` nesta fase (nenhum módulo `feature/*` ainda
depende dele — isso é F4). Nunca é consumido por `:pro:*`.

### Por que `src/main/resources/`, não `src/main/assets/`

O catálogo precisa ser lido em teste JVM puro (sem Robolectric) e em runtime real, com o mesmo
código. `src/main/assets/` exige `Context`/`AssetManager` (só disponível com Robolectric ou em
device real); `src/main/resources/` é resolvido via `Class.getResourceAsStream()`, que funciona
idêntico nos dois ambientes — decisão tomada para manter `FeatureFlagCatalogParser`/`Loader`
testáveis em JUnit puro (critério de aceite da #1477: "testes unitários do provider, parser e
comportamento offline").

### Por que não usar `dagger.Lazy` dentro do módulo

`RemoteConfigFeatureFlagProvider` recebe `remoteConfigProvider: () -> FirebaseRemoteConfig` (lambda
simples), não `dagger.Lazy<FirebaseRemoteConfig>` — `:core:*` não deve depender de Hilt/Dagger
(nenhum outro módulo `core/*` do Consumer depende). Quem monta a lambda é `AppModule` (`:app`, que
já tem Hilt no classpath), encapsulando o `dagger.Lazy<FirebaseRemoteConfig>.get()` real.

---

## 3. Modelo de dados

### 3.1 Modelo de chaves

Obrigatório, definido no Épico #1347: `consumer.{modulo}.{funcionalidade}.{controle}` (ou
`shared.*`/`app.*` para núcleos compartilhados). Exemplos reais no catálogo desta Feature:

- `consumer.speedtest.enabled`
- `consumer.speedtest.cloudflare_engine_enabled`

### 3.2 Schema do catálogo (por entrada)

Campo | Tipo | Obrigatório | Observação
---|---|---|---
`key` | string | sim | Identificador único, modelo de chaves acima
`module` | string | sim | Alias Gradle do módulo dono (ex.: `:featureSpeedtest`)
`type` | `BOOLEAN`\|`STRING`\|`LONG`\|`DOUBLE` | sim | Espelha os getters nativos do Remote Config
`defaultValue` | conforme `type` | sim | Aplicado localmente antes de qualquer fetch
`criticality` | `LOW`\|`MEDIUM`\|`HIGH`\|`CRITICAL` | sim | Metadado — sem efeito no provider Android; F3 usa para exigir confirmação reforçada
`owner` | string | sim | Time/produto responsável
`description` | string | sim | Descrição funcional
`disabledBehavior` | `HIDE_ENTRY_AND_BLOCK_ROUTE`\|`SHOW_DISABLED_MESSAGE`\|`SILENT_NO_OP`\|`FALLBACK_MODE` | sim | Comportamento quando desativada — enum não-exaustivo por design, F4 pode adicionar valor
`disabledMessage` | string? | não | Mensagem exibida ao usuário quando aplicável
`dependencies` | `FeatureFlagKey[]` | sim (pode ser `[]`) | Outras flags das quais esta depende
`androidImplemented` | boolean | sim | `false` nas 2 flags desta Feature — nenhum código real lê ainda (F4 muda isso por flag)
`adminManaged` | boolean | sim | Se o Admin (F3) pode gerenciar
`analyticsEvent` | string? | não | Evento disparado ao bloquear por flag (`feature_blocked_remote`, convenção do Épico)

Top-level do arquivo: `{ "schemaVersion": "1.0", "flags": [ ...entradas... ] }` — `schemaVersion` é
do documento inteiro, não repetido por entrada (o Épico mostra um exemplo de entrada única com
`schemaVersion` embutido; aqui foi promovido a campo do documento, ver decisão registrada no PR da
#1477).

### 3.3 Contrato Kotlin (`FeatureFlagProvider`)

```kotlin
interface FeatureFlagProvider {
    fun observe(key: FeatureFlagKey): Flow<FeatureFlagValue>
    fun isEnabled(key: FeatureFlagKey): Boolean
    suspend fun refresh(force: Boolean = false): FeatureFlagRefreshResult
}
```

- `observe`/`isEnabled` nunca bloqueiam — primeira emissão é sempre o default local do catálogo.
- `refresh(force = false)`: `fetchAndActivate()` (respeita throttling do SDK). `force = true`:
  `fetch(0)` + `activate()` (ignora throttling — uso típico: tela de debug, não fluxo normal).
- `fetchAndActivate()`/`activate()` retornando `false` (nada novo para ativar) **nunca** é erro —
  vira `FeatureFlagRefreshResult.Success(activated = false, ...)`. Só exceção/timeout reais viram
  `Failure`. Em qualquer um dos dois casos, os valores em memória só são substituídos se a leitura
  pós-fetch do SDK funcionar — uma falha de rede nunca apaga a última config válida (mesma lógica já
  validada em `AdsRemoteConfigRepository`, GH#1224).

---

## 4. APIs/Endpoints

Nenhum endpoint HTTP nesta Feature — só integração com o SDK cliente do Firebase Remote Config
(`com.google.firebase:firebase-config`, via `firebase-bom`). O endpoint/API REST do Firebase Remote
Config (usado pelo Worker/Admin em F2/#1478 para publicar templates) é responsabilidade da próxima
Feature, não desta.

---

## 5. Integrações e dependências

- **Firebase Remote Config compartilhado:** a instância `FirebaseRemoteConfig` é a **mesma** já
  usada pelo toggle de anúncios (issue #555, `AdsRemoteConfigRepository`) — um único template
  remoto, namespaces de chave diferentes (`consumer.*`/`shared.*`/`app.*` aqui,
  `ads_native_*` lá). `AppModule.provideFirebaseRemoteConfig()` foi ajustado para mesclar os dois
  mapas de defaults num único `setDefaultsAsync` (chamar duas vezes substituiria o mapa inteiro, não
  soma).
- **Sistema legado SIG-13 não tocado:** `io.signallq.app.core.network.FeatureFlagProvider`
  (`FeatureFlagManager`/`FeatureFlagRepository`, HTTP `signallq-admin-worker`) continua existindo,
  sem mudança de comportamento — colisão de nome resolvida com alias de import
  (`LegacyHttpFeatureFlagProvider`) em `AppModule.kt`. Migração dos consumidores legados
  (`DiagnosticDivergenceReporter`) para o novo mecanismo é F4/#1480, não esta Feature.
- **`:app`:** injeta `FeatureFlagCatalog` e `FeatureFlagProvider` via Hilt (`AppModule.kt`), chama
  `refresh()` uma vez no startup (`SignallQApplication.onCreate`, não bloqueante).

---

## 6. Segurança e privacidade

- Nenhuma credencial nova — reusa a configuração Firebase já existente do app (`google-services.json`).
- `refresh()` nunca lança exceção para o chamador — todo erro é log (`Timber.w`) + `Failure`
  tipado, sem crash.
- Nenhum dado pessoal trafega neste módulo — só chaves/valores de flag.

---

## 7. Performance e escalabilidade

- `FeatureFlagCatalog` é carregado uma vez (singleton via Hilt) — parse do JSON acontece só na
  primeira injeção, não a cada leitura de flag.
- `isEnabled`/`observe` são leituras em memória (`MutableStateFlow`), sem I/O.
- `refresh()` roda em `Dispatchers.IO`, timeout configurável (default 8s, mesmo valor do
  `AdsRemoteConfigRepository`).

---

## 8. Rollout e observabilidade

- Sem rollout de produto nesta Feature — as 2 flags do catálogo são smoke-test
  (`androidImplemented = false`), não gateiam nenhuma tela real.
- Observabilidade: `FeatureFlagValue.source` (`DEFAULT`/`REMOTE`/`STATIC`) permite diagnosticar de
  onde veio um valor; `FeatureFlagRefreshResult` expõe `activated`/`fetchTimeMillis` em sucesso e
  `reason`/`cause` tipados em falha. Nenhum evento de analytics é disparado por esta Feature —
  `analyticsEvent` do catálogo só passa a ser emitido quando F4 instrumentar o consumo real.

---

## 9. Riscos técnicos

- **Divergência catálogo ↔️ `FeatureFlagKeys`:** mitigado por `FeatureFlagKeysParityTest` (roda a
  cada build, falha se uma constante não tiver entrada no catálogo ou vice-versa). Não cobre ainda
  validação automática em CI de PR — herdado como escopo do backlog de CI do Épico (§ "Automação e
  CI obrigatórias" do #1347), não desta Feature.
- **Dois `FeatureFlagProvider` com o mesmo nome simples** (legado `core.network` vs. novo
  `core.featureflags`) é uma fonte real de confusão para quem for tocar `AppModule.kt` no futuro —
  mitigado com KDoc explícito nos dois pontos e o alias `LegacyHttpFeatureFlagProvider`, mas
  **não** resolve a raiz (dois sistemas paralelos coexistindo). F4/#1480 deve avaliar
  deprecar/remover o sistema SIG-13 depois que os consumidores migrarem — não decidido nesta
  Feature.
- **Atualização em tempo real (`addOnConfigUpdateListener`)** listada no Épico como responsabilidade
  de `:core:featureflags` **não foi implementada** nesta Feature — os critérios de aceite de #1477
  não exigem, e adicionar um listener sem um caso de uso real pra testar geraria código não
  exercitado. Extensível sem quebra de contrato público (`FeatureFlagProvider` não muda) quando
  virar necessário.
