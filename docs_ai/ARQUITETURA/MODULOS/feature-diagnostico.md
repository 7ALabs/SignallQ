# Módulo :featureDiagnostico

- **Status:** ativo
- **Última validação:** 2026-07-16 (fonte: `android/feature/diagnostico/build.gradle.kts`, código real)
- **Caminho físico:** `android/feature/diagnostico/`
- **Namespace:** `io.signallq.app.feature.diagnostico`

## Responsabilidade

Motor de diagnóstico local (engines determinísticas), integração com IA (Cloudflare Worker +
Gemini/Qwen3), fluxo conversacional SignallQ/Pulse, e persistência de sessões de chat. É o módulo
feature com mais responsabilidades e mais dependências do monorepo.

## Principais packages/pastas

Base: `feature/diagnostico/src/main/kotlin/io/veloo/app/kotlin/feature/diagnostico/` (caminho
físico legado) — subpacotes `engines/`, `ai/`, `pulse/`, `chat/`.

## Classes/contratos públicos relevantes

| Arquivo/Pacote | Tipo | Responsabilidade |
|---|---|---|
| `DiagnosticOrchestrator.kt` | Orchestrator | Sequencia engines e retorna relatório completo. Chamada remota primeiro via `RemoteDiagnosticRepository` (timeout 42s, GH#962/GH#969), fallback para motor local em qualquer falha |
| `RemoteDiagnosticRepository.kt` | Repository | POST ao `signallq-diagnostic-worker` (GH#962), timeout **42s** (ampliado de 5s recomendado na spec por decisão de produto 2026-07-16 para reduzir fallback prematuro em rede lenta). Fallback automático a `DiagnosticRunner` (motor local 100% offline) em timeout/erro/sem rede |
| `DiagnosticRunner.kt` | Object stateless | Executa todos os engines e agrega resultados — motor local pure |
| `DiagnosticResult.kt` / `DiagnosticStatus.kt` | Data class/Enum | Saída dos engines — ok/info/attention/critical/inconclusive |
| `engines/WifiSignalQualityEngine.kt` | Engine | Qualidade do sinal Wi-Fi por banda |
| `engines/InternetDiagnosticEngine.kt` | Engine | Velocidade, latência, jitter, perda, bufferbloat |
| `engines/WifiChannelDiagnosticEngine.kt` | Engine | Congestionamento de canal Wi-Fi |
| `engines/DnsDiagnosticEngine.kt` | Engine | Qualidade do DNS em uso |
| `engines/HistoricalDegradationEngine.kt` | Engine | Degradação histórica 7d/30d |
| `engines/FibraSignalQualityEngine.kt` | Engine | Potência óptica GPON |
| `engines/MobileSignalDiagnosticEngine.kt` | Engine | Sinal 4G/5G (RSRP, RSRQ, SINR) |
| `engines/DiagnosticDecisionEngine.kt` | Engine | Decisão final consolidada |
| `ai/AiDiagnosisRepository.kt` | Repository | POST ao Cloudflare Worker `linka-ai-diagnosis-worker` |
| `ai/DiagnosisAiContextFactory.kt` | Factory | Monta payload schema `diagnostico_v3_raw` |
| `ai/AiFallbackFactory.kt` | Factory | Resultado local se IA falhar |
| `pulse/SignallQOrchestrator.kt` | Orchestrator | Coordena speedtest silencioso + diagnóstico + chat IA |
| `pulse/SignallQState.kt` / `pulse/SignallQSnapshot.kt` | Enum/Data class | Estado do fluxo Chat/Pulse |
| `pulse/DynamicQuestionEngine.kt` | Engine | Perguntas contextuais para o chat |
| `chat/ChatDiagnosticoIaRepository.kt` | Repository | Persistência de sessões de chat em Room |
| `chat/CotaIaRepository.kt` | Repository | Cota diária rolling 24h de uso de IA |

## Entradas/saídas

- **Entradas:** `SnapshotRede` (:coreNetwork), `ResultadoSpeedtest` (:featureSpeedtest),
  `MovelSnapshot`, histórico Room, resposta HTTP dos Workers Cloudflare.
- **Saídas:** `DiagnosticResult`, `SignallQSnapshot`, `RecommendationRequest` para
  `:coreRecommendation`, persistência de sessões de chat em `:coreDatabase`.

## Dependências declaradas (build.gradle.kts real)

`:featureSpeedtest` (⚠ feature→feature, ver "Riscos"), `:coreDatabase`, `:coreDatastore`,
`:coreNetwork`, `:coreRecommendation`. Hilt próprio + kapt. `BuildConfig` com `AI_WORKER_URL`,
`DIAGNOSTIC_WORKER_URL` (worker `signallq-diagnostic`, deployado em produção 2026-07-14, GH#967),
`APP_VERSION`, `VERSION_CODE`. Libs: `okhttp`, `androidx-datastore-preferences`,
`okhttp-mockwebserver` (teste).

## Consumidores

Via grep de `project(":featureDiagnostico")`: apenas `:app`.

## Testes existentes

`src/test`: **33 arquivos** — maior cobertura de teste entre os módulos feature. `src/androidTest`:
0.

## Riscos/dívidas conhecidas

- **Violação real de dependência feature→feature**: declara
  `implementation(project(":featureSpeedtest"))`. Contraria a regra 4.5 da regra de higiene
  ("Features não podem depender diretamente de outras features. A composição entre domínios
  acontece em `:app` ou por contratos normalizados em um módulo `core` adequado."). Não corrigido
  nesta tarefa (documentação read-only) — reportado ao final da entrega para decisão de squad.
- Módulo com o maior número de dependências de módulo (5) e de responsabilidades (engines de
  diagnóstico + IA + chat + Pulse) — candidato a revisão de coesão se crescer mais.
- Caminho físico `io/veloo` diverge do package — dívida 4.1.
```

