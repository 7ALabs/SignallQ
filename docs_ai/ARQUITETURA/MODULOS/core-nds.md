---
title: "Módulo :core:nds"
description: "Cliente HTTP e contrato tipado do Network Diagnostics Service (NDS) — atrás da flag consumer_diagnostico_nds_live_enabled (default ligada, com fallback local), primeiro consumidor real desde NDS-02k."
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-09-04"
---

# `:core:nds`

- **Caminho físico:** `android/core/nds/`
- **Namespace:** `io.signallq.app.core.nds`
- **Tipo:** biblioteca Android (`com.android.library`) — Kotlin puro além de `BuildConfig`

## Responsabilidade

Camada de rede e contrato tipado do NDS (Network Diagnostics Service,
`network-diagnostics-service.buildealabs.workers.dev`), fatia NDS-01 (issue #1744, ADR-017).
Cobre: cliente HTTP (`NdsClient.evaluate`), modelos de request (`NdsDiagnosticsRequest` e blocos
aninhados), modelos de resposta (`NdsDiagnosticsResponse`, `NdsNextBestAction`, `NdsModuleResult`, `NdsTrace`),
decoders tipados para os 3 módulos confirmados no ADR (`asScoring`, `asAi`, `asWifiDiagnostics`) e
tratamento de erro (`NdsDiagnosticsOutcome`).

**Não é dele**: decidir QUANDO religar um consumidor à chamada viva — isso é orquestração de quem
consome (`:featureDiagnostico`, ver seção Consumidores). NDS-01 (#1744) isolou o cliente; NDS-02a-j
(#1747–#1758) adicionaram mappers puros consumidos por seams locais sem tocar rede; NDS-02k
(#1759) plugou o primeiro consumidor de produção real (`NdsDiagnosticRepository`, atrás da flag
`consumer_diagnostico_nds_live_enabled`, default ligada — qualquer falha mantém o fallback local).

Módulo dedicado (não `:core:network` nem gaveta genérica) porque o NDS vai substituir
`:core:diagnostico`, `ai-diagnosis-worker` e `signallq-diagnostic-worker` (ADR-017) — precisa de um
contrato próprio, versionável e consumível por múltiplas features futuras (Home, Wifi, Devices,
Diagnostico) sem acoplamento feature-a-feature. `:core:network` é infraestrutura de conectividade
*on-device* (probes, gateway, scan Wi-Fi, topologia) — misturar um cliente HTTP de um serviço
externo específico ali confundiria a responsabilidade do módulo. Verificado via `/inventario` e
`/verificar-modulo` antes de criar — nenhum símbolo/módulo `Nds*` existia no repositório.

## Dependências

### Módulos do projeto

`:coreNetwork` (NDS-02a/#1747 — `ChannelScore` para `NdsWifiScanMapper`) e `:core:diagnostico`
(NDS-02a — `MetricStatus`/`DiagnosticStatus`, vocabulário canônico de severidade e os tipos
`DiagnosticInput`/`DiagnosticReport`/`DiagnosticResult` que os mappers de NDS-02k traduzem).
A dependência de `:core:diagnostico` é **intencionalmente temporária** — marcada para remoção
quando `core/diagnostico` sair do repositório (NDS-03), documentada em `core/nds/build.gradle.kts`.
Esta entrada estava desatualizada (dizia "módulo folha, sem dependências") desde a NDS-02a; corrigida
agora porque a NDS-02k depende diretamente dela para os dois mappers novos.

### Bibliotecas externas

| Biblioteca | Para quê |
|---|---|
| `androidx.core.ktx` | Extensões Kotlin do Android |
| `kotlinx.coroutines.android` | `suspend`/`withContext(Dispatchers.IO)` em `NdsClient.evaluate` |
| `okhttp` | Cliente HTTP — mesmo padrão de `AiDiagnosisRepository` (`:featureDiagnostico`) e `:core:diagnostico` |
| `timber` | Log defensivo de falha de rede/parse |
| `junit`, `kotlinx.coroutines.test`, `okhttp.mockwebserver`, `org.json:json:20260719` (test) | Suíte JVM — mock do NDS real, nunca bate na rede |
| `androidx.junit`, `androidx.espresso.core` (androidTest) | Declaradas, sem código correspondente ainda |

## Consumidores

- **`:featureDiagnostico` (NDS-02k, issue #1759)** — `NdsDiagnosticRepository` (novo,
  `feature/diagnostico/nds/`) chama `NdsClient.evaluate()` a partir de
  `DiagnosticOrchestrator.executarProtegido()`, atrás da flag
  `consumer_diagnostico_nds_live_enabled` (default `false`). Qualquer falha (`KnownError`/
  `UnknownError`/timeout) cai para `DiagnosticRunner` local — fallback total, sem exceção
  propagada. Com a flag desligada (todo mundo, hoje), este módulo continua sem tráfego real.
- Vários arquivos de UI do Consumer (`SignalBars.kt`, `SinalMovelClassificacao.kt`, etc., NDS-02b-f)
  consomem só os mappers puros (`parseNdsVeredicto`, `classificar*Local` em
  `ClassificacaoMetricaLocal.kt`) — sem chamada de rede, sem depender do `NdsClient` em si.

## Testes

Nenhum teste deste módulo ou de `NdsDiagnosticRepository` bate no NDS real — todos usam
`okhttp3.mockwebserver.MockWebServer` (dependência `test` já declarada acima), que sobe um
servidor HTTP real em loopback e simula o contrato do NDS (path, headers, corpo JSON, códigos de
erro). O `NdsClient`/`NdsDiagnosticRepository` fazem uma chamada HTTP de verdade, só que contra
esse servidor local — nada no caminho é substituído por um dublê em memória, mas nenhum tráfego sai
para rede externa nem depende de `NDS_API_TOKEN` real. Rodam 100% no CI (`android-ci.yml`, job de
testes unitários), sem flag ou variável de ambiente adicional.

| Teste | Cobertura |
|---|---|
| `core/nds/src/test/.../NdsClientTest.kt` | `NdsClient` isolado — path v1/v2, headers, parsing de sucesso/erro (401/429/504/5xx), envelope canônico vs. shape antigo |
| `feature/diagnostico/src/test/.../nds/NdsDiagnosticRepositoryTest.kt` | `NdsDiagnosticRepository` isolado — fallback local em erro, telemetria de outcome/cobertura de snapshot |
| `feature/diagnostico/src/test/.../nds/e2e/NdsE2ECenariosTest.kt` (NDS-Snapshot-11, issue #1843, épico #1832) | **Teste E2E** cobrindo a cadeia completa `DiagnosticInput -> snapshot (NdsDiagnosticsRequest) -> JSON -> NDS (mock) -> NdsDiagnosticsResponse -> DiagnosticReport`, nos dois cenários do Definition of Done de produto da issue-mãe (seção 21): Wi-Fi congestionado (valida que `wifiScan` chega ao NDS com as redes vizinhas e que a mensagem final explica o canal sem culpar o provedor) e móvel com sinal fraco (valida que `mobile` chega com RSRP/RSRQ/SINR e nunca com Cell ID/TAC/MCC/MNC) |

Rodar localmente (a partir de `android/`):

```bash
./gradlew :core:nds:testDebugUnitTest
./gradlew :featureDiagnostico:testDebugUnitTest --tests "io.signallq.app.feature.diagnostico.nds.e2e.NdsE2ECenariosTest"
```

Ou a suíte completa do critério de aceite (`ktlintCheck`/`detekt`/`test`, já cobre estes três
arquivos): `./gradlew ktlintCheck detekt test`.

**Fixtures**: o repositório irmão `network-diagnostics-service` não publica um diretório de
fixtures JSON versionadas para contract tests (auditado em 2026-09-04 — os testes em `tests/api/`
de lá cobrem regras de contrato via schemas Zod e asserts inline em TypeScript, não fixtures
exportáveis). As respostas simuladas em `NdsE2ECenariosTest` foram construídas localmente a partir
do vocabulário confirmado em `NdsSeverityParser` (veredicto), do shape de envelope v1 confirmado em
`NdsClientTest` e de `docs/api-contract.md` do repositório irmão. Se `network-diagnostics-service`
publicar fixtures dedicadas no futuro, migrar este teste para consumi-las.

## Componentes principais

| Arquivo / classe | Responsabilidade |
|---|---|
| `NdsClient.kt` | `suspend fun evaluate(NdsDiagnosticsRequest): NdsDiagnosticsOutcome` — `POST /v1/diagnostics/evaluate` com `Authorization: Bearer`. Nunca lança exceção ao chamador |
| `NdsClientFactory.kt` | Fábrica de conveniência que monta `NdsClient` a partir de `BuildConfig.NDS_BASE_URL`/`NDS_API_TOKEN` |
| `NdsDiagnosticsRequest.kt` | Request tipado (`NdsAppInfo`, `NdsConnectionInfo`, `NdsWifiInfo`, `NdsWifiScanInfo`, `NdsSpeedInfo`, `NdsQualityInfo`, `NdsDnsInfo`, `NdsGatewayInfo`, `NdsFiberInfo`) + `toJson()` interno — cada bloco opcional é omitido do payload quando `null` |
| `NdsDiagnosticsResponse.kt` | Resposta tipada (`NdsDiagnosticsResponse`, `NdsNextBestAction`, `NdsModuleResult`, `NdsTrace`) + `NdsResponseParser` tolerante. `recommendation` é objeto tipado e `steps` decompõe a ação única; `NdsModuleResult.result`/`cards` continuam como `Map`/`List` genéricos para manter extensibilidade |
| `NdsModuleResults.kt` | Decoders tipados para os 3 módulos confirmados no ADR-017: `asScoring()`, `asAi()`, `asWifiDiagnostics()` — cada um devolve `null` se o `module` não bater ou faltar campo obrigatório |
| `NdsDiagnosticsOutcome.kt` | `sealed class` do resultado: `Success`, `KnownError` (dois shapes — flat `{error,message}` confirmado no ADR-017, e o envelope canônico `{error:{code,message,retryable},request_id}` do PR#12/NDS, ainda em draft) e `UnknownError` (5xx/timeout/corpo não-JSON — nenhum dos dois shapes bateu, tratado defensivamente) |
| `NdsJson.kt` | Helpers internos de conversão `JSONObject`/`JSONArray` → `Map`/`List` Kotlin puros |
| `NdsProfileCapabilitiesMapper.kt` (NDS-02a/#1747) | `ndsCapabilities()`/`ndsProfile()` — regra `profile`/`capabilities` do payload |
| `NdsWifiScanMapper.kt` (NDS-02a/#1747) | `mapWifiScanToNds()` — traduz `ChannelScore` (`:coreNetwork`) para o bloco `wifiScan` |
| `NdsSeverityParser.kt` (NDS-02a/#1747, NDS-02k/#1759) | `parseNdsVeredicto()` (`veredicto` → `MetricStatus`) e `MetricStatus.toDiagnosticStatus()` (segundo salto, `MetricStatus` → `DiagnosticStatus` do `core/diagnostico`) |
| `NdsDiagnosticsRequestMapper.kt` (NDS-02k/#1759, expandido pelas fatias NDS-Snapshot-02..10 do épico #1832) | `DiagnosticInput.toNdsDiagnosticsRequest()` — ponte pura para o payload real de `evaluate()`; monta `wifiScan` (redes vizinhas + congestionamento via `ChannelEvaluator`), `mobile` (RSRP/RSRQ/SINR/operadora/tecnologia), `historical`, `localEquipment`, `plan`, `connection.natStatus` e `dns` expandido. Gap conhecido restante: `dns.hijacked` (sem coleta real ainda) |
| `NdsDiagnosticsResponseMapper.kt` (NDS-02k/#1759) | `NdsDiagnosticsResponse.toDiagnosticReport()` — ponte pura de volta para o `DiagnosticReport` que a UI já lê via `SnapshotDiagnostico` |

## Autenticação (ver ADR-017)

Bearer token estático via `BuildConfig.NDS_API_TOKEN`, lido de `local.properties`
(`NDS_API_TOKEN=...`, gitignorado) em dev ou da variável de ambiente `NDS_API_TOKEN` em CI/release
— nunca hardcoded em arquivo versionado. Placeholder vazio (`""`) não quebra o build; requisição
real sem token preenchido recebe 401 do NDS, tratado como `NdsDiagnosticsOutcome.KnownError` com o
shape confirmado. O preenchimento do valor real é manual, por quem tiver acesso ao token — fora do
escopo da fatia NDS-01.

## Riscos e dívidas

- **Envelope canônico de erro (PR#12/NDS) ainda em draft, não confirmado em produção.**
  `NdsClient.parseErrorOutcome` tenta o envelope novo primeiro (`{error:{code,message,retryable},
  request_id}`) e cai para o shape antigo flat (`{error,message}`, confirmado no ADR-017) se não
  bater — nenhum dos dois lança exceção, `UnknownError` continua o fallback final. Ligar tráfego
  real contra produção sem alguém confirmar qual formato o servidor de fato devolve continua sendo
  risco (ver ADR-017, seção de pendências) — não resolvido por este módulo, só tratado
  defensivamente nos dois sentidos.
- **Timeout do cliente reduzido de 20s para 12s (NDS-02k/#1759).** Escolhido para os dois gatilhos
  de produção que disparam em background sem "aguarde" explícito do usuário — ver kdoc do
  construtor de `NdsClient`. Continua acima do `EVALUATE_TIMEOUT_MS` default do servidor (10s), de
  propósito.
- **Cobertura de rede móvel** — RESOLVIDO pela issue #1837 (NDS-Snapshot bloco 10, épico #1832):
  `NdsDiagnosticsRequestMapper.toNdsMobileInfo` monta o bloco `mobile` (operadora/tecnologia/
  RSRP/RSRQ/SINR/banda) a partir de `DiagnosticInput.mobile`, omitido só sem conexão móvel, sem
  `READ_PHONE_STATE` (`capturaReduzida=true`) ou sem nenhuma evidência de sinal. Nunca carrega
  Cell ID/TAC/MCC/MNC.
- **`profile`/`capabilities` sem regra de mapeamento formal do app.** O ADR-017 lista como pendência
  em aberto a migração para o modelo `capabilities`/`requested_outputs` do PR#12 — este módulo ainda
  usa o modelo antigo (`ndsCapabilities()`/`ndsProfile()`, NDS-02a), aceito via alias legado.
