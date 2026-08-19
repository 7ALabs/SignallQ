---
title: "Feature Flags do Consumer — Firebase Remote Config"
description: "Mecanismo técnico do módulo :core:featureflags: catálogo tipado, FeatureFlagProvider e integração com Firebase Remote Config."
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-19"
version: "1.1.0"
---

# Fundação de Feature Flags do Consumer — Firebase Remote Config (`:core:featureflags`)

- **Status:** ativo
- **Última validação:** 2026-08-19 (NDS-02k, issue #1759 — 12ª entrada do catálogo)
- **Fonte de verdade:** este arquivo para o mecanismo técnico do módulo `:core:featureflags`; o
  catálogo em si (schema completo, valores reais) tem fonte de verdade única no arquivo JSON —
  `android/core/featureflags/src/main/resources/featureflags/consumer-catalog.json` — não copiado
  aqui além de exemplos ilustrativos.
- **Escopo:** módulo Android `:core:featureflags` (issue #1477, Épico #1347) — catálogo tipado,
  `FeatureFlagProvider`, integração com Firebase Remote Config — **mais F4/#1480** (instrumentação
  das 9 flags principais de módulo em `AppShell.kt`, ver seção 10) **e #1497** (migração do kill
  switch do shadow mode de diagnóstico, ver seção 9). **Não cobre** a UI do SignallQ
  Admin (F3/#1479, protótipo apenas) — nem o sistema de feature flags de compile-time
  (`FeatureFlags.kt`), documentado em `docs_ai/TECNICO.md` §5.2 e
  `docs_ai/functional/FEATURE_FLAGS.md`. O sistema remoto legado SIG-13
  (`FeatureFlagManager`/`FeatureFlagRepository`) continua existindo no repositório (não removido
  nesta Feature nem em #1497 — ver seção 9), mas **sem nenhum consumidor real** desde #1497. O
  backend do Admin (F2/#1478,
  `integrations/cloudflare/signallq-admin-worker/src/remoteConfigAdmin.ts` +
  `src/featureFlagCatalog.ts`) fechou em 2026-07-26 e já lê este mesmo catálogo diretamente (import
  JSON cross-diretório, embarcado no bundle a cada `wrangler deploy`) — contrato completo em
  `docs_ai/CONTRATOS/openapi/signallq-admin-api.yaml`.
- **Responsável:** Camilo (implementação Android e backend do Admin). Consumidor pendente: Marina
  (F3, UI Admin real — hoje só protótipo estático).

---

## 1. Objetivo técnico

Criar a camada Android compartilhada que **todo o resto do Épico #1347 depende**: um catálogo
tipado versionado no repositório e um único ponto de acesso (`FeatureFlagProvider`) sobre o
Firebase Remote Config, para que o SignallQ Admin (fases futuras) possa governar remotamente
funcionalidades do Consumer sem exigir nova publicação de app.

Esta Feature (#1477) entregou só a fundação: módulo, contratos, catálogo com 2 flags de
smoke-test. F4 (#1480, ver seção 10) instrumentou as 9 flags principais de módulo de verdade em
`AppShell.kt` — o catálogo tem 10 entradas desde então (as 2 de #1477 + as 8 novas; a 9ª chave
principal, `consumer.speedtest.enabled`, já existia como smoke-test e passou a `androidImplemented:
true`). #1497 (ver seção 9) acrescentou a 11ª entrada, `consumer.diagnostico.shadow_mode_enabled`,
migrando o último consumidor real do sistema legado SIG-13. NDS-02k (issue #1759) acrescentou a
12ª entrada, `consumer.diagnostico.nds_live_enabled` — kill switch da chamada viva ao NDS
(`NdsClient.evaluate`) dentro de `DiagnosticOrchestrator`, `defaultValue: false`, mutuamente
exclusiva com a flag do shadow mode acima (ligada, desliga o shadow mode para o mesmo install).
Backend/UI do Admin (F2/#1478, F3/#1479) continuam fora do escopo deste documento.

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
depende dele — isso é F4).

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
- **Sistema legado SIG-13 sem consumidor real (atualizado #1497):**
  `io.signallq.app.core.network.FeatureFlagProvider` (`FeatureFlagManager`/`FeatureFlagRepository`,
  HTTP `signallq-admin-worker`) continua existindo no repositório, sem mudança de comportamento —
  `SignallQApplication` ainda injeta `FeatureFlagManager` e chama `inicializar()` no startup
  (sincroniza `GET /flags`/`GET /feature-flags` e persiste em DataStore). A binding Hilt
  `provideLegacyHttpFeatureFlagProvider`/alias `LegacyHttpFeatureFlagProvider` em `AppModule.kt` foi
  **removida** em #1497 por ter ficado morta (nenhum consumidor pedia mais o tipo
  `io.signallq.app.core.network.FeatureFlagProvider` via injeção). `DiagnosticDivergenceReporter`
  era o único consumidor real do sistema legado e migrou para
  `FeatureFlagKeys.CONSUMER_DIAGNOSTICO_SHADOW_MODE_ENABLED` (`:core:featureflags`).
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
  `core.featureflags`) continua sendo uma fonte real de confusão para quem for tocar código nos dois
  módulos — mitigado com KDoc explícito nos dois pontos. **Atualizado por #1497:** F4/#1480
  instrumentou as 9 flags de módulo sobre o sistema novo; #1497 migrou o único consumidor real do
  sistema legado (`DiagnosticDivergenceReporter`, kill switch do shadow mode de diagnóstico,
  GH#1444/#1445) para `FeatureFlagKeys.CONSUMER_DIAGNOSTICO_SHADOW_MODE_ENABLED`
  (`consumer.diagnostico.shadow_mode_enabled` no catálogo, `disabledBehavior: SILENT_NO_OP`,
  `defaultValue: true` — preserva o default do sistema legado). O binding Hilt que só existia para
  esse consumidor (`provideLegacyHttpFeatureFlagProvider`/`LegacyHttpFeatureFlagProvider`) foi
  removido de `AppModule.kt` por ficar morto. **Não resolvido por #1497 (fora do escopo daquela
  issue, decisão explícita):** o restante do sistema legado SIG-13 continua no repositório e em
  produção — `FeatureFlagManager`/`FeatureFlagRepository`, os dois endpoints HTTP
  (`GET /flags`/`GET /feature-flags`) e a inicialização em `SignallQApplication.onCreate`. Como não
  sobra nenhum consumidor real do valor lido por esse sistema, a remoção completa (código Android +
  endpoints do Worker + tabelas D1 `feature_flags`/`feature_flag_audit`) é candidata a uma
  **próxima issue dedicada** — avaliação de arquitetura, não decidida nem executada em #1497 (issue
  de acompanhamento a abrir quando priorizado).
- **Atualização em tempo real (`addOnConfigUpdateListener`)** listada no Épico como responsabilidade
  de `:core:featureflags` **não foi implementada** nesta Feature — os critérios de aceite de #1477
  não exigem, e adicionar um listener sem um caso de uso real pra testar geraria código não
  exercitado. Extensível sem quebra de contrato público (`FeatureFlagProvider` não muda) quando
  virar necessário. F4/#1480 cobre "mudanças em runtime" só até onde o `refresh()` existente
  alcança (chamado uma vez no startup); sem o listener, uma publicação no Admin só reflete no app
  depois do próximo `refresh()`, não instantaneamente.

---

## 10. F4/#1480 — instrumentação dos 9 módulos feature (Consumer)

Cobriu as 9 flags principais de módulo listadas no Épico (`consumer.{modulo}.enabled`), todas
`androidImplemented: true`, `disabledBehavior: HIDE_ENTRY_AND_BLOCK_ROUTE`, default `true`. Só a
chave `enabled` de cada módulo — sub-flags mais finas (ex.: `consumer.diagnostico.ai_enabled`,
`consumer.settings.privacidade_enabled`) não fazem parte desta Feature.

**Onde vive a lógica:**
- `android/app/src/main/kotlin/io/signallq/app/ui/screen/AppShellFeatureGating.kt` — funções
  puras (`tabHabilitada`, `tabModuleId`, `primeiraTabHabilitada`, `permitirOuBloquear`) + o
  placeholder `FeatureDisabledContent`. Extraído de `AppShell.kt` (já dívida crítica em linhas) em
  vez de crescer o arquivo ainda mais.
- `android/app/src/main/kotlin/io/signallq/app/featureflags/ConsumerFeatureGateCoordinator.kt`
  — `@Singleton` que combina os 9 `FeatureFlagProvider.observe(...)` num único
  `StateFlow<AppShellFeatureFlagsState>`, e centraliza o disparo de `feature_blocked_remote`.
  `MainViewModel` só expõe `featureFlagsState` delegando pro coordinator (regra de higiene —
  MainViewModel não ganha lógica nova).
- `AppShellFeatureFlagsState` (em `AppShellState.kt`) — 9 booleanos + `onFeatureBlocked`, mesmo
  padrão dos outros grupos de estado do AppShell (`AppShellSpeedtestState` etc.).

**Mapeamento módulo → superfície de navegação:**

Módulo | Superfície gateada em AppShell.kt
---|---
`:featureHome` | Tab 0 (Início)
`:featureSpeedtest` | Tab 1 (Velocidade)
`:featureWifi` | Tab 2 (Sinal) + `Overlay.SinalWifi`
`:featureDevices` | `Overlay.Dispositivos`
`:featureDns` | `Overlay.Dns`
`:featureFibra` | `Overlay.Fibra` / `Overlay.EquipamentoInternet`
`:featureDiagnostico` | `Overlay.Laudo`
`:featureHistory` | Tab 3 (Histórico)
`:featureSettings` | `Overlay.Perfil` (Ajustes) + `MonitoramentoSheet` + `MonitoramentoWorker.doWork()`

Fora do escopo de gate por decisão explícita: `Overlay.Privacidade`/`Overlay.Termos` (obrigação
legal/LGPD, nunca escondidas por flag — regra do próprio Épico), o hub `Overlay.Ferramentas`/tab 4
(não pertence a um único módulo do catálogo), `Overlay.ModoGamer`/`Overlay.Ping`/
`Overlay.DiagnosticoGuiado`/`Overlay.DetalhesTecnicos` (não nomeados nos 9 módulos da issue
#1480 — `DiagnosticoGuiado`/`DetalhesTecnicos` ficam de fato sob a guarda indireta de
`Overlay.ResultadoVelocidade`, que só abre após um speedtest bem-sucedido, já gateado
indiretamente pela flag de `:featureSpeedtest`).

**Comportamento ao desativar (`HIDE_ENTRY_AND_BLOCK_ROUTE`):**
1. Tab: `NavigationBarItem(enabled = false)` — item fica visualmente apagado e não recebe clique
   (Compose bloqueia o callback nesse estado, então não há "tentativa" de tap pra registrar).
2. Overlay: a função `onAbrir*Overlay` correspondente checa a flag antes de empilhar — se
   desligada, não empilha, registra `feature_blocked_remote` e mostra um `Snackbar` neutro
   ("Recurso temporariamente indisponível.").
3. Redirecionamento em runtime: se a tab atual perde a flag (Admin publicou remotamente enquanto o
   usuário já estava nela), um `LaunchedEffect` redireciona para a primeira tab habilitada (ordem
   de prioridade 1→0→2→3, `Ferramentas` como último recurso) e registra o bloqueio — único caminho
   onde isso acontece sem tap explícito.
4. `MonitoramentoWorker.doWork()` consulta `FeatureFlagProvider.isEnabled` diretamente (não passa
   pelo coordinator, que é `:app`-only reativo) — se `consumer.settings.enabled` estiver desligado,
   pula a execução (não mede, não persiste, não notifica) sem cancelar o agendamento do
   WorkManager em si, e sem apagar histórico já salvo.

**Deep links:** o app não tem nenhum deep link real hoje (`AndroidManifest.xml` só declara o
intent-filter do launcher) — o critério "bloquear deep link" fica coberto por construção, porque
todo `onAbrir*Overlay` já é o único ponto de entrada de navegação (inclusive de um deep link
futuro que reusasse os mesmos callbacks).

**Analytics:** `AnalyticsTracker.registrarFeatureBloqueadaRemota(featureId)` (novo método
tipado, mesmo padrão de `registrarFeatureUsada`) — implementado em `FirebaseAnalyticsTracker`
(GA4, evento `feature_blocked_remote` com `feature_id`/`session_id`/`app_version`, sem dado
pessoal). `CompositeAnalyticsTracker` encaminha só ao Firebase, decisão documentada no próprio
método — o ingest do `signallq-admin-worker` (`AnalyticsEventIngestPayload`) não ganhou campo
equivalente nesta Feature (extensão de schema fica pra quando o Admin realmente for consumir esse
evento).

**Não coberto por F4/#1480 (fora do critério de aceite da issue, registrado aqui pra não se
perder):**
- ~~Migração do consumidor legado SIG-13 (`DiagnosticDivergenceReporter`) para o sistema novo~~ —
  feito em #1497 (2026-08-01), ver seção 9.
- Gate de capacidades de `:core:*` (`coreNetwork`/discovery, `coreTelephony`, `coreRecommendation`)
  — o Épico lista isso como parte da visão maior, mas fora do escopo textual de #1480 (só os 9
  módulos `:feature:*`).
- Sub-flags mais finas por módulo (ex.: `consumer.diagnostico.ai_enabled` pra bloquear só a
  chamada de IA sem desligar o diagnóstico local inteiro).
- F5 (#1480 bloqueia, não inclui): CI gate de consistência catálogo↔código↔Remote Config.
