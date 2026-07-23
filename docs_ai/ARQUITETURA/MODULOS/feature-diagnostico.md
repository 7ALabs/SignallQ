# Módulo :featureDiagnostico

- **Status:** ativo
- **Última validação:** 2026-07-23 (fonte: `android/feature/diagnostico/build.gradle.kts`, código real)
- **Fonte de verdade:** código real do módulo — em caso de divergência, vale o código
- **Escopo:** módulo Gradle `:featureDiagnostico` (alias legado; pasta física `android/feature/diagnostico/`)
- **Responsável:** Camilo (dono da implementação Android), squad SignallQ mantém

## Visão geral

Orquestração do diagnóstico (local + remoto), integração com IA (Cloudflare Worker + Gemini/Qwen3),
fluxo conversacional SignallQ/Pulse, topologia de rede local (mesh/NAT/UPnP) e ponte para o
Recommendation Engine. Namespace declarado: `io.signallq.app.feature.diagnostico`.

**Correção factual relevante desde a última auditoria (2026-07-16):** os engines de diagnóstico por
domínio (`WifiSignalQualityEngine`, `InternetDiagnosticEngine`, `WifiChannelDiagnosticEngine`,
`DnsDiagnosticEngine`, `HistoricalDegradationEngine`, `FibraSignalQualityEngine`,
`MobileSignalDiagnosticEngine`, `DiagnosticDecisionEngine`, além de `DiagnosticRunner`,
`DiagnosticResult`, `DiagnosticStatus`) **foram extraídos para o módulo compartilhado
`:core:diagnostico`** (issue #1157, Fase 1a — comentário no `build.gradle.kts`: "Domínio de
causa-raiz extraído... FindingEngine, ScoreEngine, DiagnosticInput/Report/Result, engines por
domínio, topology/model+correlation+internet"). O pacote `chat/` (`ChatDiagnosticoIaRepository`,
`CotaIaRepository`) citado em versões anteriores deste doc **foi removido como código morto** (PR
#912, "remove código morto do chat de diagnóstico IA") — não existe mais no código.

## Diagrama de componentes

```
:featureDiagnostico (orquestração, IA, Pulse, topologia local, ponte de recomendação)
├── DiagnosticOrchestrator — sequencia engines (hoje em :core:diagnostico) e retorna relatório
├── remote/
│     RemoteDiagnosticRepository — POST ao signallq-diagnostic-worker, timeout 42s, fallback local
│     ProviderDirectoryRepository, DiagnosticSnapshotMapper, RemoteDiagnosticReportMapper
├── ai/
│     AiDiagnosisRepository — POST ao linka-ai-diagnosis-worker
│     AiModels
├── pulse/
│     SignallQOrchestrator — coordena speedtest silencioso + diagnóstico + chat IA
│     DynamicQuestionEngine, IntelligentDiagnosticSession, SignallQInsightGenerator,
│     ContextAccumulator, RotatingMessageProvider
├── topology/
│     TopologyDiagnostic + topology/lan/ (GatewayResolver, MeshDetector, StunNatProbe,
│     UpnpIgdDiscovery, UpnpParser, UpnpSoapClient, OuiVendorLookup)
├── ingest/
│     AdminIngestRepository, AdminIngestPayloads — POST ao signallq-admin-worker
├── recommendation/ (io/signallq/ — já no caminho correto)
│     RecommendationDecisionCoordinator, RecommendationHistoryRepository — ponte para :coreRecommendation
├── RecommendationEngine.kt — dicas práticas do diagnóstico local (NÃO é o :coreRecommendation)
└── di/DiagnosticoModule.kt — Hilt

     ┌────────────────────────┐
     │   :core:diagnostico     │  (compartilhado com SignallQ Pro, GH#1157)
     │ FindingEngine, ScoreEngine, DiagnosticRunner, DiagnosticResult/Report/Status,
     │ engines por domínio (Internet/Dns/Fibra/MobileSignal/HistoricalDegradation),
     │ topology/correlation (NatClassifier, TopologyTracer)
     └────────────────────────┘
```

## Componentes em detalhe

| Componente | Tipo | Responsabilidade |
|---|---|---|
| `DiagnosticOrchestrator.kt` | Orchestrator | Sequencia o diagnóstico e retorna relatório completo. Chamada remota primeiro via `RemoteDiagnosticRepository` (timeout 42s, GH#962/GH#969), fallback para o motor local (`:core:diagnostico`) em qualquer falha |
| `remote/RemoteDiagnosticRepository.kt` | Repository | POST ao `signallq-diagnostic-worker` (GH#962), timeout **42s** (ampliado de 5s por decisão de produto 2026-07-16 para reduzir fallback prematuro em rede lenta). Fallback automático ao motor local 100% offline em timeout/erro/sem rede |
| `remote/ProviderDirectoryRepository.kt` | Repository | Diretório de provedores/logo de operadora via worker remoto |
| `ai/AiDiagnosisRepository.kt` | Repository | POST ao Cloudflare Worker `linka-ai-diagnosis-worker` |
| `ai/AiModels.kt` | Data class | Modelos de request/response da IA |
| `pulse/SignallQOrchestrator.kt` | Orchestrator | Coordena speedtest silencioso + diagnóstico + chat IA |
| `pulse/SignallQState.kt` / `pulse/SignallQSnapshot.kt` | Enum/Data class | Estado do fluxo Chat/Pulse |
| `pulse/DynamicQuestionEngine.kt` | Engine | Perguntas contextuais para o chat |
| `pulse/IntelligentDiagnosticSession.kt` / `SignallQInsightGenerator.kt` / `ContextAccumulator.kt` | Sessão/Engine | Sessão de diagnóstico conversacional e geração de insight |
| `topology/TopologyDiagnostic.kt` + `topology/lan/*` | Engine | Topologia de rede local — resolução de gateway, detecção de mesh, NAT (STUN), descoberta UPnP/IGD |
| `ingest/AdminIngestRepository.kt` | Repository | POST de métricas ao `signallq-admin-worker` |
| `recommendation/RecommendationDecisionCoordinator.kt` | Coordinator | Monta `DiagnosticInput`/`DiagnosticReport` (de `:core:diagnostico`) e chama `RecommendationEngine` (de `:coreRecommendation`) |
| `recommendation/RecommendationHistoryRepository.kt` | Repository | Persiste histórico de recomendação via `:coreDatabase` |
| `RecommendationEngine.kt` (raiz do módulo) | Engine local | Dicas práticas do diagnóstico local — **não confundir** com `:coreRecommendation` |
| `di/DiagnosticoModule.kt` | Hilt Module | Wiring de DI do módulo |

## Fluxo de dados principal

- **Entradas:** `SnapshotRede`/scan de topologia (`:coreNetwork`), `ResultadoSpeedtest`
  (`:featureSpeedtest`), `MovelSnapshot`, histórico Room, resposta HTTP dos Workers Cloudflare.
- **Processamento:** `DiagnosticOrchestrator` tenta motor remoto primeiro (42s de timeout), cai para
  `DiagnosticRunner` de `:core:diagnostico` em falha; `RecommendationDecisionCoordinator` traduz o
  relatório de diagnóstico em `RecommendationRequest` para `:coreRecommendation`.
- **Saídas:** `DiagnosticResult`/`DiagnosticReport` (tipos de `:core:diagnostico`), `SignallQSnapshot`,
  `RecommendationDecision`, ingest de métricas para o Console via `AdminIngestRepository`.

## Decisões arquiteturais (ADR)

- **Dependências de módulo:** `:featureSpeedtest` (⚠ feature→feature, ver "Riscos"), `:coreDatabase`,
  `:coreDatastore`, `:coreNetwork`, `:coreRecommendation`, e desde a issue #1157 também
  `:core:diagnostico` (módulo Pro-shared — ver `docs_ai/ARQUITETURA/README.md` seção 4.1). Hilt
  próprio + kapt. `BuildConfig` com `AI_WORKER_URL`, `DIAGNOSTIC_WORKER_URL` (worker
  `signallq-diagnostic`, deployado em produção 2026-07-14, GH#967), `APP_VERSION`, `VERSION_CODE`.
  Libs: `okhttp`, `androidx-datastore-preferences`, `okhttp-mockwebserver` (teste).
- **Extração de engines para `:core:diagnostico` (GH#1157, Fase 1a)** — decisão explícita para
  reaproveitar o motor de diagnóstico com a linha SignallQ Pro sem duplicar lógica. `:featureDiagnostico`
  passa a ser a camada de orquestração/IA/Pulse/ingest específica do consumer, não mais dona dos
  engines de domínio.
- **Remoção do fluxo de chat de diagnóstico IA (PR #912)** — código morto eliminado; o diagnóstico
  assistido por IA hoje é inline em `ResultadoVelocidadeScreen` (turno único), não um chat contínuo
  persistido — ver `docs_ai/ARQUITETURA/README.md` seção 4.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| **Violação real de dependência feature→feature**: `implementation(project(":featureSpeedtest"))` | Contraria a regra 4.5 da regra de higiene | Extrair contrato normalizado para um módulo `core`, ou mover a composição para `:app` |
| Módulo com o maior número de dependências de módulo (5 consumer + 1 cross-linha) e múltiplas responsabilidades (orquestração, IA, Pulse, topologia local, ingest, ponte de recomendação) | Candidato a revisão de coesão se crescer mais | Ao tocar, avaliar se nova responsabilidade pertence de fato aqui ou a `:core:diagnostico`/`:app` |
| Caminho misto `io/veloo` (maioria) + `io/signallq` (`recommendation/`) dentro do mesmo módulo | Duas convenções coexistindo | Não migrar oportunisticamente — parte da dívida 4.1 |
| Caminho físico `io/veloo` diverge do package declarado | Dívida 4.1 da regra de higiene | Não migrar oportunisticamente |

`src/test`: **22 arquivos** (queda frente aos 33 da última auditoria — parte da cobertura dos engines
migrou junto com o código para `:core:diagnostico`, fora do escopo deste doc). `src/androidTest`: 0.
