# SignallQ

App Android de **diagnóstico de conectividade** com IA — analisa Wi-Fi, fibra, rede móvel e dispositivos da rede local e gera laudos com explicações em linguagem natural.

- **Site do produto:** https://signallq.pages.dev/
- **Inscrição no teste fechado:** https://groups.google.com/g/testadores-signallq
- **Google Play:** https://play.google.com/store/apps/details?id=io.signallq.app
- **Desenvolvido pela Buildea:** https://github.com/buildea-labs (site institucional temporariamente fora do ar — pendente rename do repo `7ALabs.github.io`, ver decisão de portfólio sobre o rebrand)

> Package/namespace atual é `io.signallq.app` (renomeado de `io.veloo.app` em 2026-06-28). Outros identificadores técnicos preservam nomes anteriores — repo `SignallQ`, banco `linkaKotlin.db`, worker `linka-ai-diagnosis-worker` — são técnicos, não a marca. A marca é **SignallQ**.

## Stack

- **Android** Kotlin + Jetpack Compose (Material 3), MVVM + `StateFlow`
- **DI** Hilt · **Persistência** Room (`SignallQDatabase`) + DataStore · **Background** WorkManager (`MonitoramentoWorker`)
- **IA** Cloudflare Worker (`integrations/cloudflare/ai-diagnosis-worker`), URL via `BuildConfig.AI_WORKER_URL` — provider primário Gemini 2.0 Flash (quando `GEMINI_API_KEY` configurada), fallback Qwen3 30B MoE FP8 (Cloudflare Workers AI)
- **Analytics** Firebase Analytics + Crashlytics
- minSdk 24 · target/compileSdk 36 · JVM 17 (alvo de build) · CI roda em Java 21

## Arquitetura (16 módulos Gradle)

- **app** — shell, navegação (`AppShell.kt`, 5 abas: Início, Velocidade, Sinal, Histórico, Ferramentas), DI
- **core** (6): `coreNetwork`, `coreDatabase`, `coreDatastore`, `coreTelephony`, `corePermissions`, `coreRecommendation`
- **feature** (9): `featureHome`, `featureSpeedtest`, `featureWifi`, `featureDevices`, `featureDns`, `featureFibra`, `featureDiagnostico`, `featureHistory`, `featureSettings`

Features são independentes entre si (sem dependência cruzada `:feature*` → `:feature*`).

## Como rodar localmente

```bash
# Build de debug
cd android && ./gradlew assembleDebug

# Testes unitários
cd android && ./gradlew test

# Lint
cd android && ./gradlew ktlintCheck detekt
```

Requer JDK 17+ e o `app/google-services.json` (já versionado).

## Release (resumo)

> Checklist completo e obrigatório na skill `checar-release` (`.claude/skills/checar-release/`).
> Nunca rodar `assembleRelease` sem `clean` + `--no-build-cache` (cache já causou build
> desatualizado no Firebase). Publicação exige aprovação explícita do Luiz (ver `AGENTS.md`).

```bash
git push origin main
cd android && ./gradlew clean assembleRelease --no-build-cache
cd android && ./gradlew appDistributionUploadRelease
```

Worker Cloudflare: havendo mudança em `integrations/cloudflare/*/src/`, `npx wrangler deploy` antes do commit.

## CI

`.github/workflows/quality.yml` roda em todo PR/push para `main`: **detekt**, **ktlint**, **testes unitários** e **build debug**.

## Subprojetos no repositório

- `integrations/cloudflare/ai-diagnosis-worker/` — worker de diagnóstico IA
- `integrations/cloudflare/game-latency-probe-worker/` — worker de sonda de latência para jogos
- `integrations/cloudflare/signallq-admin-worker/` — worker do painel admin (o painel em si vive em `buildea-admin`)
- `integrations/cloudflare/signallq-diagnostic-worker/` — worker de diagnóstico de rede
- `integrations/cloudflare/signallq-privacy-worker/` — worker da política de privacidade

O painel Admin (React/Vite/TS) e o site/PWA pertencem aos repositórios `buildea-admin` e
`signallq-web`, respectivamente (ver ADR-016) — não vivem neste repositório.

## Roadmap de lançamento (Play Store — alvo 07/08/2026)

Planejamento de Escopo → Desenvolvimento & Documentação → Firebase Beta Testing → Play Store (Teste Interno) → Preparação para Lançamento → Lançamento. Acompanhamento no Linear (time SignallQ).

## Documentação

Documentação viva para agentes em [`docs_ai/`](docs_ai/README.md). Personas de agentes legadas
arquivadas em `docs/archive/ai-governance/legacy-agents/` — não participam do roteamento ativo.
