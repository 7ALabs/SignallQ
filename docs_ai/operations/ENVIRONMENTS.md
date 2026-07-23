# Environments

- **Status:** ativo
- **Última validação:** 2026-07-23
- **Fonte de verdade:** `android/gradle/libs.versions.toml` (build), `integrations/cloudflare/*/wrangler.toml` (workers)
- **Escopo:** ambientes locais/CI do app Android e dos 5 workers Cloudflare

## Objective

This document outlines the different environments the SignallQ Android Kotlin application interacts with or targets, detailing their purpose and key configuration aspects.

## Environment Types

The application operates across several distinct environments:

### 1. Local Development Environment

-   **Description**: The environment set up on a developer's machine for coding, building, and testing.
-   **Components**:
    -   **IDE**: Android Studio.
    -   **Build Tools**: Gradle Wrapper (`gradlew`), Kotlin compiler.
    -   **SDK**: Android SDK (version specified in `local.properties`).
    -   **Emulators/Devices**: Android emulators or physical devices for running and debugging the app.
    -   **Dependencies**: Local caching of dependencies managed by Gradle.
-   **Configuration**: Managed via `local.properties`, `gradle.properties`, module `build.gradle.kts` files, and `key.properties` (for local debug signing).

### 2. Play Console Tracks (não há staging separado)

-   **Descrição real (corrigido em 2026-07-23):** o app **não** tem um ambiente de staging
    separado com backend próprio — todos os builds (`internal`, `alpha`, futuramente `beta`/
    `production`) apontam para os **mesmos** workers Cloudflare de produção. Não há flavor de
    build nem endpoint alternativo por trilha.
-   **Trilhas reais hoje:** `internal` (destino de todo `release.yml`) e `alpha` (promovida via
    `promote-release.yml`). `beta`/`production` bloqueadas por guardrail técnico até decisão do
    Luiz — ver `docs_ai/operations/DEPLOY.md` e `VERSIONING.md`.
-   **Validação pré-release:** feita via build local (`assembleRelease`/`bundleRelease`) e smoke
    test manual, não via ambiente de staging dedicado.

### 3. Production Environment

-   **Description**: The live environment where the final, released version of the application is available to end-users.
-   **Backend Interaction**: Connects to the live backend services (mesmos workers usados por `internal`/`alpha` — ver nota acima).
-   **Configuration**: Uses production-ready configurations, live API endpoints, and production signing credentials.

### 4. Cloudflare Workers (5, em `integrations/cloudflare/`)

| Worker (pasta) | Nome real (`wrangler.toml`) | Função |
|---|---|---|
| `ai-diagnosis-worker` | `linka-ai-diagnosis-worker` | Diagnóstico IA (Gemini 2.0 Flash primário / Qwen3 30B fallback) |
| `signallq-admin-worker` | `signallq-admin` | Backend do Console/Admin (D1) |
| `signallq-diagnostic-worker` | `signallq-diagnostic` | Diagnóstico/telemetria |
| `signallq-privacy-worker` | `signallq-privacy` | Política de privacidade e termos de uso (público) |
| `game-latency-probe-worker` | `signallq-game-latency-probe` | Probe de latência para o fluxo de Jogos |

-   **Configuração:** cada worker tem seu próprio `wrangler.toml` na respectiva pasta —
    não há um `wrangler.toml` único cobrindo todos.

## Configuration Management

-   **`local.properties`**: Specifies local paths like the Android SDK, crucial for the development environment build.
-   **`build.gradle.kts`**: Build types (debug, release) configuram signing e feature flags — não há product flavor por ambiente hoje.
-   **`gradle.properties`**: Can hold global properties applicable across environments.
-   **`key.properties`**: Stores sensitive credentials, which will differ between local development (debug keys) and production (release keystores).
-   **`integrations/cloudflare/<worker>/wrangler.toml`**: um por worker (5 no total), gerencia deployment settings, bindings e secrets de cada um.

## Riscos conhecidos

-   Não há staging real com backend isolado — qualquer mudança em worker afeta `internal`/
    `alpha`/produção igualmente; validar com cuidado antes de `npx wrangler deploy`.
-   Gerenciamento seguro de `key.properties`/keystore continua responsabilidade manual local —
    ver `docs_ai/operations/SIGNING.md`.
