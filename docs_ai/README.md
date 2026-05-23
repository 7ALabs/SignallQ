# Linka Android AI Documentation

**Versao:** v0.7.1 (versionCode 16, release 2026-05-16) | Referencia para agentes de IA no projeto Linka Android Kotlin.
Recuperacao rapida, fatos operacionais, inferencia minima.

> Ponto de entrada obrigatorio antes de carregar qualquer doc especifico.
> Sempre prefira busca por simbolo (Grep) antes de ler arquivos completos.

---

## Documentos Consolidados (raiz)

- [Documentação Funcional Completa (`ANDROID_FUNCIONAL.md`)](./ANDROID_FUNCIONAL.md) — visao geral, navegacao, todas as telas, fluxos principais. Fonte primaria para entender o que o app faz.
- [Documentação Técnica Completa (`ANDROID_TECNICO.md`)](./ANDROID_TECNICO.md) — stack, modulos, MVVM, MainViewModel, Room, DataStore, engines, componentes, build config. Fonte primaria para entender como o app funciona internamente.

> Estes dois documentos sao consolidados e atualizados. Para detalhes especificos de feature ou fluxo, use os docs granulares abaixo.

---

## Functional

- [Telas Android (`functional/SCREENS_ANDROID.md`)](./functional/SCREENS_ANDROID.md) — telas, roteamento, navegacao
- [Core Features (`functional/FEATURES.md`)](./functional/FEATURES.md)
- [Diagnostic Flow (`functional/DIAGNOSTIC_FLOW.md`)](./functional/DIAGNOSTIC_FLOW.md)
- [DNS Flow (`functional/DNS_FLOW.md`)](./functional/DNS_FLOW.md)
- [Speedtest Flow (`functional/SPEEDTEST_FLOW.md`)](./functional/SPEEDTEST_FLOW.md)
- [AI Assistant (`functional/AI_ASSISTANT.md`)](./functional/AI_ASSISTANT.md)
- [Settings (`functional/SETTINGS.md`)](./functional/SETTINGS.md)

---

## Design System

- [Cores (`design-system/COLORS.md`)](./design-system/COLORS.md) — tokens brand, status, superficies, Orbit
- [Tipografia (`design-system/TYPOGRAPHY.md`)](./design-system/TYPOGRAPHY.md) — escala MD3, animacao tipografica
- [Espacamento (`design-system/SPACING.md`)](./design-system/SPACING.md) — grid 8dp, valores canonicos
- [Design Tokens Cross-Platform (`design-system/DESIGN_TOKENS_CROSSPLATFORM.md`)](./design-system/DESIGN_TOKENS_CROSSPLATFORM.md) — Android vs. PWA
- [Componentes Android (`design-system/COMPONENTS_ANDROID.md`)](./design-system/COMPONENTS_ANDROID.md) — componentes Orbit, SpeedTest, Layout
- [Componentes (legacy) (`design-system/COMPONENTS.md`)](./design-system/COMPONENTS.md) — versao anterior; prefira COMPONENTS_ANDROID.md
- [Material Design 3 (`design-system/MD3_GUIDELINES.md`)](./design-system/MD3_GUIDELINES.md)
- [Chat Patterns (`design-system/CHAT_PATTERNS.md`)](./design-system/CHAT_PATTERNS.md)
- [Navigation Patterns (`design-system/NAVIGATION.md`)](./design-system/NAVIGATION.md)
- [Motion (`design-system/MOTION.md`)](./design-system/MOTION.md)

---

## Technical

- [Monitoramento Passivo (`technical/MONITORAMENTO_PASSIVO.md`)](./technical/MONITORAMENTO_PASSIVO.md) — WorkManager 30min, notificacoes de degradacao
- [Architecture Overview (`technical/ARCHITECTURE.md`)](./technical/ARCHITECTURE.md)
- [Module Breakdown (`technical/MODULES.md`)](./technical/MODULES.md)
- [Data Flow (`technical/DATA_FLOW.md`)](./technical/DATA_FLOW.md)
- [API Map (`technical/API_MAP.md`)](./technical/API_MAP.md)
- [AI Flow (`technical/AI_FLOW.md`)](./technical/AI_FLOW.md)
- [Cloudflare Integration (`technical/CLOUDFLARE.md`)](./technical/CLOUDFLARE.md)
- [Storage Details (`technical/STORAGE.md`)](./technical/STORAGE.md)
- [Services Overview (`technical/SERVICES.md`)](./technical/SERVICES.md)
- [Build System (`technical/BUILD_SYSTEM.md`)](./technical/BUILD_SYSTEM.md)
- [Screen Map (`technical/SCREEN_MAP.md`)](./technical/SCREEN_MAP.md) — localizacao de arquivos de tela
- [Feature File Maps (`technical/FEATURE_FILE_MAPS.md`)](./technical/FEATURE_FILE_MAPS.md) — mapa de arquivos por feature

---

## Operations

- [Release Process (`operations/RELEASE.md`)](./operations/RELEASE.md)
- [Versioning Strategy (`operations/VERSIONING.md`)](./operations/VERSIONING.md) — versionName/versionCode, script version.ps1
- [APK Build Process (`operations/APK_BUILD.md`)](./operations/APK_BUILD.md)
- [Scripts (`operations/SCRIPTS.md`)](./operations/SCRIPTS.md)
- [Environments (`operations/ENVIRONMENTS.md`)](./operations/ENVIRONMENTS.md)
- [Environment (alternativo) (`operations/ENVIRONMENT.md`)](./operations/ENVIRONMENT.md)
- [Deployment (`operations/DEPLOY.md`)](./operations/DEPLOY.md)
- [Paperclip Integration (`operations/PAPERCLIP_INTEGRATION.md`)](./operations/PAPERCLIP_INTEGRATION.md)
- ORB tasks: ver arquivos `operations/ORB-*.md` — documentacao de tarefas especificas (ORB-159, ORB-161, ORB-163, ORB-165)

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

## Outros

- [Audit Summary (`AUDIT_SUMMARY.md`)](./AUDIT_SUMMARY.md) — registro de auditoria de docs
