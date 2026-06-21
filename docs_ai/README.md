# SignallQ Android AI Documentation

**Versao:** v0.16.0 (versionCode 46, release 2026-06-21) | Referencia para agentes de IA no projeto SignallQ Android Kotlin.
Recuperacao rapida, fatos operacionais, inferencia minima.

> Ponto de entrada obrigatorio antes de carregar qualquer doc especifico.
> Sempre prefira busca por simbolo (Grep) antes de ler arquivos completos.

> Nota de marca: a UI e a documentacao usam **SignallQ**. Identificadores tecnicos
> permanecem por compatibilidade de infra: package `io.veloo.app`, repo GitHub
> `gmmattey/linka-android`, worker Cloudflare `linka-ai-diagnosis-worker`.

---

## Documentos Consolidados (raiz)

- [Documentação Funcional Completa (`ANDROID_FUNCIONAL.md`)](./ANDROID_FUNCIONAL.md) — visao geral, navegacao, todas as telas, fluxos principais. Fonte primaria para entender o que o app faz.
- [Documentação Técnica Completa (`ANDROID_TECNICO.md`)](./ANDROID_TECNICO.md) — stack, modulos, MVVM, MainViewModel, Room, DataStore, engines, componentes, build config. Fonte primaria para entender como o app funciona internamente.
- [Guia Rápido de Agentes (`AGENTS_QUICK_REFERENCE.md`)](./AGENTS_QUICK_REFERENCE.md) — squad, responsabilidades e quando acionar cada agente.
- [Histórico de Releases (`RELEASES.md`)](./RELEASES.md) — linha do tempo de versoes a partir do git log real.

> Estes documentos sao consolidados e atualizados. Para detalhes especificos de feature ou fluxo, use os docs granulares abaixo.

---

## Functional

- [Telas Android (`functional/SCREENS_ANDROID.md`)](./functional/SCREENS_ANDROID.md) — telas, roteamento, navegacao
- [Core Features (`functional/FEATURES.md`)](./functional/FEATURES.md)
- [Diagnostic Flow (`functional/DIAGNOSTIC_FLOW.md`)](./functional/DIAGNOSTIC_FLOW.md)
- [DNS Flow (`functional/DNS_FLOW.md`)](./functional/DNS_FLOW.md)
- [Speedtest Flow (`functional/SPEEDTEST_FLOW.md`)](./functional/SPEEDTEST_FLOW.md)
- [Wi-Fi Features (`functional/WIFI_FEATURES.md`)](./functional/WIFI_FEATURES.md)
- [AI Assistant (`functional/AI_ASSISTANT.md`)](./functional/AI_ASSISTANT.md)
- [Central de Testes — Guia do Usuário (`functional/CENTRAL_DE_TESTES_USER_GUIDE.md`)](./functional/CENTRAL_DE_TESTES_USER_GUIDE.md)
- [Settings (`functional/SETTINGS.md`)](./functional/SETTINGS.md)

---

## Design System

- [Cores (`design-system/COLORS.md`)](./design-system/COLORS.md) — tokens brand, status, superficies, SignallQ
- [Tipografia (`design-system/TYPOGRAPHY.md`)](./design-system/TYPOGRAPHY.md) — escala MD3, animacao tipografica
- [Espacamento (`design-system/SPACING.md`)](./design-system/SPACING.md) — grid 8dp, valores canonicos
- [Design Tokens Cross-Platform (`design-system/DESIGN_TOKENS_CROSSPLATFORM.md`)](./design-system/DESIGN_TOKENS_CROSSPLATFORM.md) — Android vs. PWA
- [Componentes Android (`design-system/COMPONENTS_ANDROID.md`)](./design-system/COMPONENTS_ANDROID.md) — componentes SignallQ, SpeedTest, Layout
- [Material Design 3 (`design-system/MD3_GUIDELINES.md`)](./design-system/MD3_GUIDELINES.md)

---

## Technical

- [Architecture Overview (`technical/ARCHITECTURE.md`)](./technical/ARCHITECTURE.md)
- [Module Breakdown (`technical/MODULES.md`)](./technical/MODULES.md)
- [Data Flow (`technical/DATA_FLOW.md`)](./technical/DATA_FLOW.md)
- [API Map (`technical/API_MAP.md`)](./technical/API_MAP.md)
- [AI Flow (`technical/AI_FLOW.md`)](./technical/AI_FLOW.md)
- [Cloudflare Integration (`technical/CLOUDFLARE.md`)](./technical/CLOUDFLARE.md) — worker `linka-ai-diagnosis-worker`, modelo padrao Qwen3 30B, fallback local
- [Storage Details (`technical/STORAGE.md`)](./technical/STORAGE.md) — Room v10, DataStore
- [Services Overview (`technical/SERVICES.md`)](./technical/SERVICES.md)
- [Build System (`technical/BUILD_SYSTEM.md`)](./technical/BUILD_SYSTEM.md)
- [Code Patterns (`technical/CODE_PATTERNS.md`)](./technical/CODE_PATTERNS.md)
- [Screen Map (`technical/SCREEN_MAP.md`)](./technical/SCREEN_MAP.md) — localizacao de arquivos de tela
- [Feature File Maps (`technical/FEATURE_FILE_MAPS.md`)](./technical/FEATURE_FILE_MAPS.md) — mapa de arquivos por feature
- [Monitoramento Passivo (`technical/MONITORAMENTO_PASSIVO.md`)](./technical/MONITORAMENTO_PASSIVO.md) — WorkManager 30min, notificacoes de degradacao
- [Ping Executor Architecture (`technical/PING_EXECUTOR_ARCHITECTURE.md`)](./technical/PING_EXECUTOR_ARCHITECTURE.md)
- [AI Flow / Otimização — Deep Dive (`technical/OPTIMIZATION_DEEP_DIVE.md`)](./technical/OPTIMIZATION_DEEP_DIVE.md)
- [Execution Roadmap (`technical/EXECUTION_ROADMAP.md`)](./technical/EXECUTION_ROADMAP.md)

### Migrations

- [Repositório IA como Singleton via Hilt (`migrations/repositorio-ia-hilt-singleton.md`)](./migrations/repositorio-ia-hilt-singleton.md) — unificação de AiDiagnosisRepository, centralização de URL, Hilt Singleton Component

### Decisions (ADRs)

- [ADR-001 — Timber Logging (`decisions/ADR-001-timber-logging.md`)](./decisions/ADR-001-timber-logging.md)
- [ADR-002 — Ktlint + Detekt (`decisions/ADR-002-ktlint-detekt-quality.md`)](./decisions/ADR-002-ktlint-detekt-quality.md)
- [ADR-003 — DispatcherProvider via DI (`decisions/ADR-003-dispatcher-provider-di.md`)](./decisions/ADR-003-dispatcher-provider-di.md)
- [ADR-004 — Module Structure (`decisions/ADR-004-module-structure-android.md`)](./decisions/ADR-004-module-structure-android.md)

---

## Operations

- [Release Process (`operations/RELEASE.md`)](./operations/RELEASE.md) — processo canonico: commit → push → clean build → Firebase
- [Deployment (`operations/DEPLOY.md`)](./operations/DEPLOY.md) — Firebase App Distribution + Play Store
- [Versioning Strategy (`operations/VERSIONING.md`)](./operations/VERSIONING.md) — versionName/versionCode, script version.ps1
- [APK Build Process (`operations/APK_BUILD.md`)](./operations/APK_BUILD.md)
- [App Signing (`operations/SIGNING.md`)](./operations/SIGNING.md) — keystore, key.properties, GitHub Secrets
- [Scripts (`operations/SCRIPTS.md`)](./operations/SCRIPTS.md)
- [Environments (`operations/ENVIRONMENTS.md`)](./operations/ENVIRONMENTS.md) — doc canonico de ambientes

---

## AI Agent Documentation

- [Agent Workflow (`ai/AGENT_WORKFLOW.md`)](./ai/AGENT_WORKFLOW.md) — fluxo completo do sistema multiagente
- [Context Policy (`ai/CONTEXT_POLICY.md`)](./ai/CONTEXT_POLICY.md) — fontes de contexto e estrategia de carregamento
- [Handoff Rules (`ai/HANDOFF_RULES.md`)](./ai/HANDOFF_RULES.md) — protocolo de handoff entre agentes
- [Task Breakdown (`ai/TASK_BREAKDOWN.md`)](./ai/TASK_BREAKDOWN.md) — decomposicao de tasks
- [Product Flow (`ai/PRODUCT_FLOW.md`)](./ai/PRODUCT_FLOW.md) — jornada do usuario e objetivos de produto
- [Engineering Flow (`ai/ENGINEERING_FLOW.md`)](./ai/ENGINEERING_FLOW.md) — workflow de engenharia
- [UX Flow (`ai/UX_FLOW.md`)](./ai/UX_FLOW.md) — fluxo UX, quando Lia e acionada
- [Review Flow (`ai/REVIEW_FLOW.md`)](./ai/REVIEW_FLOW.md) — processo de revisao (Gema + Lia)

---

## Arquivo (`_archive/`)

Material historico preservado, fora do fluxo ativo. Nao usar como fonte de verdade.

- `_archive/INDEX_v0.9.0.md`, `_archive/CHANGELOG_ENTRY_v0.9.0.md`, `_archive/QA_ACCEPTANCE_CHECKLIST_v0.9.0.md` — docs da release v0.9.0
- `_archive/FEATURES.md` — visao de features antiga (raiz); preferir `functional/FEATURES.md`
- `_archive/COMPONENTS.md` — componentes legados; preferir `design-system/COMPONENTS_ANDROID.md`
- `_archive/ENVIRONMENT.md` — ambiente legado; preferir `operations/ENVIRONMENTS.md`
- `_archive/FEATURE_CENTRAL_DE_TESTES_2026_05_20.md`, `_archive/FEATURE_SUMMARY_QUICK_REF.md`, `_archive/HANDOFF_RELEASE.md` — relatorios e handoffs historicos
