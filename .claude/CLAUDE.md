# Project Memory

Instructions here apply to this project and are shared with team members.

## Design System

Toda UI deste projeto segue o **SignallQ Design System** (`.claude/skills/linka-design/`).
Antes de criar ou editar telas/componentes, consulte a skill `linka-design` e use os tokens de `colors_and_type.css` / `SignallQTheme.kt` como fonte de verdade.

Não-negociáveis:
- Material 3 claro, acento violeta `#6C2BFF`, semântica de status verde/âmbar/vermelho
- Ícones Material Symbols (Outlined), tipo Roboto, grid 8dp, card radius 16dp, flat (sem sombras pesadas)
- Superfícies SignallQ (IA) sempre escuras (`#0D0D1A` / `#1A0B2E` / `#1E1130`)
- Copy em PT-BR com "você", sentence case em títulos, UPPERCASE em overlines, SEM emoji
- Métrica crua sempre acompanhada de veredito humano (Excelente/Bom/Regular/Fraco/Forte)
- Separador inline: ponto médio `·`

Referência rápida de tokens: ver `.claude/skills/linka-design/HANDOFF_README.md` (tabela completa de cores, espaçamento, raios e tipografia).

## Release Process

Quando o usuário pedir para subir/deploy/publicar no Firebase, seguir OBRIGATORIAMENTE nesta ordem:

1. **Commit** — stage todos os arquivos modificados, commit com mensagem descritiva
2. **Push** — `git push origin main` para sincronizar GitHub
3. **Clean build** — `./gradlew clean assembleRelease --no-build-cache` (NUNCA usar cache em release)
4. **Upload** — `./gradlew appDistributionUploadRelease`

Nunca pular etapas. Nunca fazer assembleRelease sem clean + --no-build-cache antes. O cache do Gradle já causou builds desatualizados no Firebase.

Worker Cloudflare: quando houver mudanças em `integrations/cloudflare/ai-diagnosis-worker/src/`, fazer `npx wrangler deploy` ANTES do commit.

## Context

> Resumo factual do projeto (fonte de verdade = código). Atualizado em 2026-06-21 (v0.16.0).

**Identidade**
- App: **SignallQ** (`app_name`). Marca anterior: Linka → Veloo → **SignallQ** (rebrand em 0.16.0).
- Package/applicationId/namespace: **`io.veloo.app`** — identificador técnico, **NÃO renomear** (mexer quebra Firebase/assinatura). O "veloo" aqui é técnico, não marca.
- Versão atual: **0.16.0** (versionCode 46), em `gradle/libs.versions.toml`. minSdk 24, target/compileSdk 36, JVM 17.

**Arquitetura**
- **15 módulos Gradle**: `app` + core(5): `coreNetwork`, `coreDatabase`, `coreDatastore`, `coreTelephony`, `corePermissions` + feature(9): `featureHome`, `featureSpeedtest`, `featureWifi`, `featureDevices`, `featureDns`, `featureFibra`, `featureDiagnostico`, `featureHistory`, `featureSettings`.
- DI: **Hilt** (`app/.../di/AppModule.kt` + `featureDiagnostico/.../di/DiagnosticoModule.kt`). MVVM + Jetpack Compose; ViewModels por escopo: `AppViewModel`, `DevicesViewModel`, `SpeedtestViewModel`, `DiagnosticoViewModel` (substituem o antigo `MainViewModel` god-object); estado via `StateFlow`.
- Features são **independentes entre si** — sem dependência cruzada `:feature*` → `:feature*`. `featureDiagnostico` não depende de `featureWifi` nem `featureWifi`.
- Persistência: **Room** `SignallQDatabase` (v10; entidades Medicao, ApelidoDispositivo, ChatSession, ChatMessage) + **DataStore** `linkaPreferencias` (nome técnico — manter).
- Navegação: `AppShell.kt` — bottom bar de **5 abas** (índice 0–4): **Início, Velocidade, Sinal, Histórico, Ajustes**. Parâmetros agrupados em `@Stable` data classes. Diagnóstico/IA, Dispositivos, Fibra, Laudo etc. são **overlays** (`overlayStack`), não abas. `navigation/AppNavGraph.kt` tem constantes legadas que NÃO refletem a nav atual.
- Background: WorkManager `MonitoramentoWorker` (30 min, histerese) + `SignallQNotificationHelper`.
- HTTP UPnP: `OkHttpClient` `@Named("upnpClient")` provido como `@Singleton` via `AppModule`.

**IA de diagnóstico**
- App → **worker Cloudflare** (`integrations/cloudflare/ai-diagnosis-worker/`), URL via `BuildConfig.AI_WORKER_URL` (não mais hardcoded). Nome do worker = infra, manter.
- Modelo padrão: **Qwen3 30B MoE FP8** (`@cf/qwen/qwen3-30b-a3b-fp8`). Fallback local sem IA (`AiFallbackFactory`). Persona = "SignallQ". Chat/Pulse via `SignallQOrchestrator` (vive em `featureDiagnostico/pulse/`).
- `AiDiagnosisRepository`: `@Singleton` provido por `DiagnosticoModule` (não mais instanciado manualmente em múltiplos pontos).

**Testes**
- ~37 classes de teste unitário (JUnit4 + Robolectric + coroutines-test + room-testing) em `*/src/test/`; 3 `androidTest` de Room/DAO/migração. Sem androidTest de UI. Rodar: `./gradlew test`. Plano em `tests/`.

**Documentação**
- Doc viva para agentes em `docs_ai/` (subpastas `ai/`, `design-system/`, `functional/`, `operations/`, `technical/`). Material histórico/obsoleto em `docs/_archive/` e `docs_ai/_archive/` — não usar como verdade atual. Índice: `docs_ai/README.md`.

**Identificadores técnicos a preservar** (parecem marca, mas são técnicos): package `io.veloo.app`, repo GitHub `gmmattey/linka-android`, worker `linka-ai-diagnosis-worker`, skill/comando `linka-design` / `/linka*`, banco `linkaKotlin.db`, canais `linka_*`, DataStore `linkaPreferencias`.

