---
title: "Módulo :featureDiagnostico"
description: "Orquestração do diagnóstico de conexão, integração com o worker de IA e ingest de telemetria para o worker admin."
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-16"
---

# `:featureDiagnostico`

- **Caminho físico:** `android/feature/diagnostico/` (alias flat legado, remapeado por `projectDir` em `android/settings.gradle.kts`)
- **Namespace:** `io.signallq.app.feature.diagnostico`

## Responsabilidade

Orquestra o fluxo de diagnóstico de conexão do app consumer: coleta o contexto bruto (speedtest, rede, topologia LAN, histórico), executa o motor local de `:core:diagnostico`, roda a avaliação remota em shadow mode contra o worker `signallq-diagnostic`, pede o laudo em linguagem natural ao worker de IA e envia telemetria ao worker admin. Também abriga o motor de recomendações práticas (`RecomendacaoPraticaEngine`, regras REC-01..REC-14) e a sondagem de topologia (UPnP/IGD, STUN/NAT, OUI/mesh).

Não é dele: a UI do diagnóstico (as Screens e o `MainViewModel` vivem em `:app` — este módulo não tem nenhum `@Composable`), as regras de causa-raiz canônicas (`FindingEngine`/`ScoreEngine`/`DiagnosticRunner`, em `:core:diagnostico`), a persistência (`:coreDatabase`), a execução do speedtest em si (`:featureSpeedtest`) e a decisão de copy/fallback de UI — os repositories devolvem `null`/estado e quem decide é o chamador.

## Dependências

Extraídas de `android/feature/diagnostico/build.gradle.kts`.

| Dependência | Configuração | Observação |
|---|---|---|
| `:coreDatabase` | `implementation` | Room: `MedicaoDao`, `ProviderDirectoryCacheDao`, `RecommendationHistoryDao` |
| `:coreDatastore` | `implementation` | `PreferenciasAppRepository` (consentimento LGPD, `anon_device_id`) |
| `:coreNetwork` | `implementation` | `AnalyticsHelper`, `MonitorRede`, `GatewayLatencyMeasurer`, contratos de topologia |
| `:coreRecommendation` | `implementation` | `RecommendationEngine` (monetização, issue #790) |
| `:core:featureflags` | `implementation` | Kill switch do shadow mode (issue #1497) |
| `:core:diagnostico` | `implementation` | `DiagnosticRunner`, `FindingEngine`, `DiagnosticInput/Report/Result` |
| `libs.hilt.android` + `kapt(libs.hilt.compiler)` | `implementation`/`kapt` | Único dos quatro feature modules aqui documentados com Hilt |
| `libs.androidx.core.ktx` | `implementation` | |
| `libs.androidx.lifecycle.runtime.ktx` | `implementation` | |
| `libs.kotlinx.coroutines.android` | `implementation` | |
| `libs.timber` | `implementation` | |
| `libs.okhttp` | `implementation` | Cliente HTTP de todos os repositories remotos |
| `libs.androidx.datastore.preferences` | `implementation` | |
| `libs.junit`, `org.json:json:20260719`, `libs.kotlinx.coroutines.test`, `libs.okhttp.mockwebserver` | `testImplementation` | `org.json` é necessário porque `JSONObject` do SDK não existe no unit test JVM |
| `libs.androidx.junit`, `libs.androidx.espresso.core`, `libs.kotlinx.coroutines.test`, `libs.androidx.room.testing` | `androidTestImplementation` | |

`buildConfigField` declarados no módulo: `AI_WORKER_URL` (`https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev`), `DIAGNOSTIC_WORKER_URL` (`https://signallq-diagnostic.giammattey-luiz.workers.dev`), `APP_VERSION`, `VERSION_CODE`.

## Consumidores

`grep -rn 'project(":featureDiagnostico")' --include=*.kts .`

| Consumidor | Local |
|---|---|
| `:app` | `android/app/build.gradle.kts:316` |

Nenhum outro módulo depende deste.

## Componentes principais

### Integração de rede — worker de IA (`AiDiagnosisRepository`)

`android/feature/diagnostico/src/main/kotlin/io/signallq/app/kotlin/feature/diagnostico/ai/AiDiagnosisRepository.kt` (686 linhas).

Cliente do worker `ai-diagnosis-worker`, instanciado como `@Singleton` em `DiagnosticoModule.provideAiDiagnosisRepository()` com `baseUrl = BuildConfig.AI_WORKER_URL` e `isAuthorized = { true }`.

| Método | Endpoint | Comportamento |
|---|---|---|
| `checkAvailability()` | `HEAD {AI_WORKER_URL}/api/ai/diagnostico-conexao` | `OkHttpClient` próprio com connect/read de **5 s**. Considera vivo em 2xx **ou HTTP 405**. Qualquer exceção → `false`. |
| `explainDiagnosis(...)` | `POST {AI_WORKER_URL}/api/ai/diagnostico-conexao` | Payload schema v3 (só dados brutos: métricas, contexto de rede, móvel, dispositivos, histórico, evidências sem interpretação, achados locais, equipamento local). Parser tolerante aceita schema v1 e v2 na resposta. |
| `explainDiagnosisStream(...)` | `POST {AI_WORKER_URL}/api/ai/diagnostico-conexao?stream=true` | SSE (`data: ` linha a linha até `[DONE]`). Fallback silencioso se o `Content-Type` não for `text/event-stream`. `call.cancel()` no `finally`. Captura o bloco `usage` em `lastStreamUsage`. |

**Timeouts do `OkHttpClient` default:** `connectTimeout` 15 s, `readTimeout` 90 s (dimensionado para os 40–60 s típicos de inferência do Gemma 4 26B no free tier), `writeTimeout` 30 s. Além disso, `explainDiagnosis` envolve toda a chamada em `withTimeoutOrNull(40_000L)` — ou seja, o teto efetivo de espera é **40 s**, menor que o `readTimeout` de 90 s; estourado o teto, o retorno é `AiDiagnosisState.timeout`.

**Cache:** `ConcurrentHashMap<String, Pair<AiDiagnosisResult, Long>>` em memória, TTL de **5 minutos** (`CACHE_TTL_MS = 5 * 60 * 1000L`). A chave é `SHA-256(AI_PROMPT_VERSION + context.toString())` — a versão do prompt entra no hash para que resposta gerada com prompt antigo não seja servida a um cliente que espera o schema novo. Hit dentro do TTL devolve `AiDiagnosisState.success` com `source = "cache"`; expirado, a entrada é removida.

**Degradação:** sem autorização, HTTP não-2xx, corpo vazio, JSON não parseável ou exceção → `AiDiagnosisState.fallback(localFallback())`. `normalizeStatus` cruza o status da IA com o status local e sobrescreve `"inconclusivo"` para `"regular"` quando há dado de speedtest presente.

### Integração de rede — worker admin (`AdminIngestRepository`)

`android/feature/diagnostico/src/main/kotlin/io/signallq/app/kotlin/feature/diagnostico/ingest/AdminIngestRepository.kt` (283 linhas).

Envia telemetria ao `signallq-admin-worker`. `baseUrl` e `ingestKey` vêm de fora via `@Named("adminIngestUrl")`/`@Named("adminIngestKey")` (BuildConfig de `:app`), e o `OkHttpClient` dedicado (`@Named("adminIngestClient")`) usa connect/read/write de **10 s** — telemetria é best-effort, não bloqueia o usuário.

| Método | Endpoint | Payload |
|---|---|---|
| `sendDiagnostic` | `POST {ADMIN_INGEST_URL}/ingest/diagnostic` | `DiagnosticIngestPayload` (id, created_at, network_type, status, score, métricas, issues, operator, device_model, os_version, app_version, ai_summary_report, environment, dist_channel, build_type, version_code, device_id) |
| `sendAiUsage` | `POST {ADMIN_INGEST_URL}/ingest/ai-usage` | `AiUsageIngestPayload` (id, model, session_id, tokens, cost_usd, metadados de build) |
| `sendAnalyticsEvent` | `POST {ADMIN_INGEST_URL}/ingest/analytics` | `AnalyticsEventIngestPayload` embrulhado em `{ "events": [ ... ] }` — o worker aceita batch, o app sempre envia um evento por chamada |

Autenticação: header `Authorization: Bearer {INGEST_KEY}` — chave com escopo limitado a `/ingest/`, distinta do `ADMIN_SECRET` do painel.

Privacidade: `consentimentoProvider` (default `false`) é consultado em **todos** os três métodos; sem consentimento LGPD nada sai do aparelho. Wire real em `DiagnosticoModule`: `{ prefs.buscarConsentimentoLgpd() == true }`.

Confirmação e ordenação (GH#1332): o retorno `Boolean` só é `true` quando o worker confirma o **mesmo identificador** (`{ok: true, id}` ou `acceptedIds` contendo o id) — isso permite que a fila persistente avance o checkpoint com segurança. Como `ai_usage.session_id` tem FK para `diagnostic_sessions(id)` no D1, `sendAiUsage` aguarda (via `CompletableDeferred`, timeout de 10 s) a confirmação do `sendDiagnostic` da mesma sessão antes de enviar; pendências não consumidas são varridas após 30 s.

### Demais componentes

| Arquivo | Responsabilidade |
|---|---|
| `.../ai/AiModels.kt` (840 linhas) | Modelos do contrato de IA (`DiagnosisAiContext`, `AiDiagnosisResult`, `ModeloIa`, `PerguntaContextual`…), `DiagnosisAiContextFactory` e `AiFallbackFactory` |
| `.../RecomendacaoPraticaEngine.kt` (638 linhas) | Motor de recomendações práticas locais (REC-01..REC-14). Renomeado de `RecommendationEngine` na auditoria #1228 para não colidir com o motor de monetização de `:coreRecommendation` |
| `.../DiagnosticOrchestrator.kt` (125 linhas) | Fachada `StateFlow` do diagnóstico; delega para `RemoteDiagnosticRepository.evaluateShadow` |
| `.../remote/RemoteDiagnosticRepository.kt` (295 linhas) | Cliente do worker `signallq-diagnostic` (`POST /api/diagnostic/evaluate`). Timeouts OkHttp 3 s/4 s/3 s com teto de 42 s. Fallback de 3 níveis: `REMOTE` → `CACHED_LOCAL` → `BUNDLED_LOCAL`. Produção usa `evaluateShadow` (local autoritativo), não `evaluate` |
| `.../remote/DiagnosticDivergenceReporter.kt` (165 linhas) | Shadow mode: envia só o resumo já comparado para `POST /ingest/diagnostic-divergence`. Kill switch via `:core:featureflags` + rollout percentual, fail closed |
| `.../remote/DiagnosticRolloutStatusRepository.kt` (105 linhas) | `GET /diagnostic/rollout-status` com cache em memória e TTL curto |
| `.../remote/ProviderDirectoryRepository.kt` (204 linhas) | Diretório remoto de provedores (`GET /providers/...`) com cache Room (`RoomProviderDirectoryCache`) |
| `.../remote/RulesetCacheStore.kt` (141 linhas) | `FileRulesetCacheStore` persiste o último ruleset remoto válido em `filesDir/diagnostic_ruleset` (nunca `cacheDir`) |
| `.../remote/DiagnosticSnapshotMapper.kt` (192 linhas) / `RemoteDiagnosticReportMapper.kt` (145 linhas) | Serialização do `DiagnosticInput` e desserialização do relatório remoto |
| `.../topology/TopologyDiagnostic.kt` (79 linhas) + `topology/lan/*` | Sondagem de topologia: `GatewayResolver`, `UpnpIgdDiscovery`, `UpnpSoapClient`, `UpnpParser`, `MeshDetector`, `OuiVendorLookup`, `StunNatProbe` (145 linhas), `StunMessageCodec` (140 linhas) |
| `.../recommendation/RecommendationDecisionCoordinator.kt` (54 linhas) e `RecommendationHistoryRepository.kt` (64 linhas) | Ponte entre diagnóstico, histórico Room e o `RecommendationEngine` de `:coreRecommendation`. Únicos arquivos `main` já em `io/signallq/` |
| `.../di/DiagnosticoModule.kt` (250 linhas) | Módulo Hilt `@InstallIn(SingletonComponent)` com todos os providers acima |

## Riscos e dívidas

- **Motor SignallQ Pulse removido (GH#1682).** O pacote `.../pulse/` (`SignallQOrchestrator.kt` e mais 11 arquivos: `DynamicQuestionEngine`, `IntelligentDiagnosticSession`, `ContextAccumulator`, `QuestionAnswer`, `QuestionNode`, `OpcaoResposta`, `RotatingMessageProvider`, `SignallQInsightGenerator`, `SignallQSnapshot`, `SignallQState`, `AiAnalysisEntry`) era um segundo motor de diagnóstico conversacional (chat) sem nenhum consumidor de UI — a decisão de produto que descontinuou o chat (#564, SIG-282) removeu a experiência mas não o motor por trás. Removido junto: as duas telas `ContextualQuestionCard.kt`/`PulseResultCard.kt` (`:app`, também sem call site), a dependência `implementation(project(":featureSpeedtest"))` (único consumidor era o próprio `SignallQOrchestrator`, resolvendo a violação "feature não depende de feature" abaixo) e o `by lazy { SignallQOrchestrator(...) }` em `MainViewModel.kt`. `MainViewModel.verificarDisponibilidadeGemma()` passou a chamar `diagAiRepository.checkAvailability()` diretamente — mesmo comportamento, sem o motor intermediário. Ver `git show <SHA-antes-da-remoção>:android/feature/diagnostico/src/main/kotlin/io/signallq/app/feature/diagnostico/pulse/SignallQOrchestrator.kt` para recuperar o código se necessário.
- **Dependência entre features (RESOLVIDA em GH#1682).** `android/feature/diagnostico/build.gradle.kts` declarava `implementation(project(":featureSpeedtest"))` só para o `SignallQOrchestrator.kt` importar `io.signallq.app.feature.speedtest.ExecutorSpeedtest`/`ModoSpeedtest`/`EstadoExecucaoSpeedtest`/`DiagnosticoFasesSpeedtest` — violava `.claude/rules/higiene-e-padronizacao-repositorio.md` §4.9. Com a remoção do motor, a dependência foi removida do `build.gradle.kts`; nenhum outro arquivo do módulo referenciava `:featureSpeedtest`.
- **Arquivos acima de 800 linhas** (contagem real via `wc -l`):
  - `.../ai/AiModels.kt` — **840 linhas**, acima do limiar de extração obrigatória (800). Agrega ~25 data classes do contrato de IA mais `DiagnosisAiContextFactory` e `AiFallbackFactory`; os dois `object` são candidatos naturais a arquivos próprios.
  - No source set de teste, `AiDiagnosisRepositoryTest.kt` tem 771 linhas e `RecomendacaoPraticaEngineTest.kt` 757 — abaixo do limiar, mas próximos.
- **Path físico alinhado ao package `io.signallq.app.*`** — migração de `io/signallq/app/kotlin/` concluída em 2026-08-15 (#1645).
- **Regra de negócio em Composable:** não aplicável — o módulo não contém nenhum `@Composable` (verificado por `grep -rn "@Composable"`, 0 ocorrências).
- **Divergência entre `readTimeout` (90 s) e o teto de `withTimeoutOrNull` (40 s)** em `AiDiagnosisRepository.explainDiagnosis`: o comentário no construtor justifica os 90 s para dar margem à inferência, mas o teto de 40 s cancela antes. Só o caminho de streaming (`explainDiagnosisStream`, sem `withTimeoutOrNull`) usa de fato os 90 s. Comportamento não documentado no código; se for intencional, o comentário está desatualizado.
- **Falta de teste:** os componentes de rede e mapeamento têm cobertura (há 28 arquivos de teste no módulo, incluindo `AiDiagnosisRepositoryTest`, `AdminIngestRepositoryTest`, `RemoteDiagnosticRepositoryTest`). Sem teste próprio: `TopologyDiagnostic`, `GatewayResolver`, `MeshDetector`, `UpnpSoapClient` e o módulo Hilt.
- **Analytics e input órfãos desde `740f558b` (2026-07-13, GH#937), não desde GH#1682 (ver issue de acompanhamento):** `AnalyticsHelper.registrarIaLaudoSolicitado`/`registrarIaLaudoRecebido` e `AnalyticsTracker.registrarFeatureUsada("diagnostico", sessionIdOverride=...)` só eram chamados de dentro do `SignallQOrchestrator`, cujos call sites no `MainViewModel` caíram de oito para um em `740f558b` — desde então nenhum caminho de produção os dispara. GH#1682 apagou código já inalcançável; reverter não restauraria o funil. `DiagnosticInput.deviceGamingSelecionado` (consumido por `RecomendacaoPraticaEngine`) também ficou sem escritor em produção — só a árvore de perguntas do Pulse ("qual_jogo_device") preenchia esse campo. Além disso, `core/database/.../chat/` (`ChatSessionEntity`/`ChatMessageEntity`/`ChatSessionDao`, tabelas `chat_sessions`/`chat_messages`) já estava órfão antes desta remoção — nenhum caminho de produção grava linhas ali (`AdminSyncWorker` só lê, nunca encontra nada) — mudança de schema/migration fica fora do escopo desta remoção (não é decisão a tomar silenciosamente, ver `.claude/rules/higiene-e-padronizacao-repositorio.md` §9).
- **Shadow mode ainda não validado:** o kdoc de `RemoteDiagnosticRepository` registra que a paridade entre motor local e remoto (GH#1442) tem várias regras `PARCIAL`/`PENDENTE`. `evaluate()` (remoto-primeiro) permanece no código, testado, mas fora do caminho de produção — código vivo não exercitado em produção é risco de regressão silenciosa.
