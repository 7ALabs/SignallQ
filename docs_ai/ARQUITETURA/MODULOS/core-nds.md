---
title: "Módulo :core:nds"
description: "Cliente HTTP e contrato tipado do Network Diagnostics Service (NDS) — fatia NDS-01, isolado, sem consumidor real ainda."
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-19"
---

# `:core:nds`

- **Caminho físico:** `android/core/nds/`
- **Namespace:** `io.signallq.app.core.nds`
- **Tipo:** biblioteca Android (`com.android.library`) — Kotlin puro além de `BuildConfig`

## Responsabilidade

Camada de rede e contrato tipado do NDS (Network Diagnostics Service,
`network-diagnostics-service.buildealabs.workers.dev`), fatia NDS-01 (issue #1744, ADR-017).
Cobre: cliente HTTP (`NdsClient.evaluate`), modelos de request (`NdsDiagnosticsRequest` e blocos
aninhados), modelos de resposta (`NdsDiagnosticsResponse`, `NdsModuleResult`, `NdsTrace`),
decoders tipados para os 3 módulos confirmados no ADR (`asScoring`, `asAi`, `asWifiDiagnostics`) e
tratamento de erro (`NdsDiagnosticsOutcome`).

**Não é dele** (ainda): religar nenhuma tela ou ViewModel real ao NDS — isso é fatia NDS-02+.
Este módulo é infraestrutura isolada; nenhum consumidor de produção existe nesta fatia.

Módulo dedicado (não `:core:network` nem gaveta genérica) porque o NDS vai substituir
`:core:diagnostico`, `ai-diagnosis-worker` e `signallq-diagnostic-worker` (ADR-017) — precisa de um
contrato próprio, versionável e consumível por múltiplas features futuras (Home, Wifi, Devices,
Diagnostico) sem acoplamento feature-a-feature. `:core:network` é infraestrutura de conectividade
*on-device* (probes, gateway, scan Wi-Fi, topologia) — misturar um cliente HTTP de um serviço
externo específico ali confundiria a responsabilidade do módulo. Verificado via `/inventario` e
`/verificar-modulo` antes de criar — nenhum símbolo/módulo `Nds*` existia no repositório.

## Dependências

### Módulos do projeto

Nenhuma. Módulo folha — não depende de `:core:network`, `:core:diagnostico` nem de nenhum outro
módulo do monorepo. Deliberado: o contrato do NDS é autocontido (schema JSON documentado no
ADR-017), não reaproveita tipos de domínio do motor local para não criar acoplamento entre o motor
que está sendo substituído e o que vai substituí-lo.

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

Nenhum. Fatia NDS-01 é infraestrutura isolada por decisão explícita da issue #1744 — religar Home,
Wifi, Devices ou Diagnostico ao NDS é fatia NDS-02+.

## Componentes principais

| Arquivo / classe | Responsabilidade |
|---|---|
| `NdsClient.kt` | `suspend fun evaluate(NdsDiagnosticsRequest): NdsDiagnosticsOutcome` — `POST /v1/diagnostics/evaluate` com `Authorization: Bearer`. Nunca lança exceção ao chamador |
| `NdsClientFactory.kt` | Fábrica de conveniência que monta `NdsClient` a partir de `BuildConfig.NDS_BASE_URL`/`NDS_API_TOKEN` |
| `NdsDiagnosticsRequest.kt` | Request tipado (`NdsAppInfo`, `NdsConnectionInfo`, `NdsWifiInfo`, `NdsWifiScanInfo`, `NdsSpeedInfo`, `NdsDnsInfo`, `NdsFiberInfo`) + `toJson()` interno — cada bloco opcional é omitido do payload quando `null` |
| `NdsDiagnosticsResponse.kt` | Resposta tipada (`NdsDiagnosticsResponse`, `NdsModuleResult`, `NdsTrace`) + `NdsResponseParser` tolerante. `NdsModuleResult.result`/`cards` ficam como `Map`/`List` genéricos (não `org.json.JSONObject`) para manter `equals` estrutural e permanecer extensível a módulos futuros sem mudança de contrato |
| `NdsModuleResults.kt` | Decoders tipados para os 3 módulos confirmados no ADR-017: `asScoring()`, `asAi()`, `asWifiDiagnostics()` — cada um devolve `null` se o `module` não bater ou faltar campo obrigatório |
| `NdsDiagnosticsOutcome.kt` | `sealed class` do resultado: `Success`, `KnownError` (401/429, shape `{error,message}` confirmado) e `UnknownError` (5xx/timeout/corpo não-JSON — shape não confirmado, tratado defensivamente) |
| `NdsJson.kt` | Helpers internos de conversão `JSONObject`/`JSONArray` → `Map`/`List` Kotlin puros |

## Autenticação (ver ADR-017)

Bearer token estático via `BuildConfig.NDS_API_TOKEN`, lido de `local.properties`
(`NDS_API_TOKEN=...`, gitignorado) em dev ou da variável de ambiente `NDS_API_TOKEN` em CI/release
— nunca hardcoded em arquivo versionado. Placeholder vazio (`""`) não quebra o build; requisição
real sem token preenchido recebe 401 do NDS, tratado como `NdsDiagnosticsOutcome.KnownError` com o
shape confirmado. O preenchimento do valor real é manual, por quem tiver acesso ao token — fora do
escopo da fatia NDS-01.

## Riscos e dívidas

- **Formato de erro genérico (5xx/timeout) não confirmado.** O ADR-017 documenta apenas o shape de
  401/429. `NdsDiagnosticsOutcome.UnknownError` cobre esse caso defensivamente (sem assumir JSON
  parseável), mas não há teste contra um 5xx real do NDS — lacuna herdada do ADR, não fechada nesta
  fatia (decisão explícita da issue #1744, "fora de escopo").
- **Nenhum consumidor real ainda.** Todo o módulo é código morto do ponto de vista do app até
  NDS-02+ religar uma tela. Esperado nesta fatia — não é dívida, é sequenciamento deliberado.
- **`profile`/`capabilities` sem regra de mapeamento do app.** O ADR-017 lista como pendência em
  aberto "regra de mapeamento `profile`/`capabilities` do app para os valores aceitos pelo NDS" —
  este módulo aceita qualquer `String`/`List<String>` livre, a validação de vocabulário fica para
  quem construir o payload real em NDS-02+.
