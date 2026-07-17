# Deployment

## Objective

This document outlines the deployment process for the SignallQ Android Kotlin application, detailing how new versions are released to end-users.

> Versao atual: **0.26.0** (versionCode 61), release 2026-07-17.
> Namespace/applicationId atual: **`io.signallq.app`** (renomeado de `io.veloo.app`
> em 2026-06-28; caminho fisico do codigo do `:app` continua `io/veloo/app/kotlin/`).
> Demais identificadores tecnicos permanecem: repo `gmmattey/linka-android`, worker
> `linka-ai-diagnosis-worker`. Publicacao na Play Console e distribuicao Firebase sao
> automatizadas via GitHub Actions — nao ha upload manual pela UI (ver abaixo).

## Deployment Target

Dois canais, os dois via **GitHub Actions**:
- **Firebase App Distribution** — validação rápida/debug, sob demanda.
- **Google Play Console** — release oficial, trilha `internal` → `alpha`. Beta e produção
  ainda não liberados.

## Processo Canônico (atualizado 2026-07-17) — os dois canais via CI

**Regra única pros dois canais**: nunca subir um build sem incrementar `versionCode` em
`android/gradle/libs.versions.toml` antes, commitado e pushado.

### Firebase App Distribution

Workflow `.github/workflows/firebase-distribution.yml` (`workflow_dispatch` manual):
`clean` → `assembleRelease` (ou `assembleDebug`) → `appDistributionUploadRelease`/`...Debug`.
Depende do secret `FIREBASE_TOKEN` (gerado localmente via `firebase login:ci` — exige TTY
interativo, configurado uma vez com `gh secret set FIREBASE_TOKEN --repo
gmmattey/linka-android`).

**Worker Cloudflare:** se houver mudanças em
`integrations/cloudflare/ai-diagnosis-worker/src/`, rodar `npx wrangler deploy`
**ANTES** do commit.

## Deployment Steps (Play Console)

1.  **Prepare Release Build**:
    -   Ensure the application has gone through development, testing, and quality assurance.
    -   Verify that versioning (`operations/VERSIONING.md`) is correctly applied for the release.

2.  **Sign the Application**:
    -   Feito automaticamente pelo workflow, via os secrets `KEYSTORE_BASE64`/
        `STORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD` já configurados no repo.

3.  **Upload to Google Play Console** — automatizado, não é login manual:
    -   `git tag vX.Y.Z && git push origin vX.Y.Z` dispara `.github/workflows/release.yml`.
    -   O workflow builda, assina, cria o GitHub Release, e publica o AAB direto na trilha
        `internal` via `gradlew publishReleaseBundle` (`gradle-play-publisher`, credencial
        `PLAY_SERVICE_ACCOUNT_JSON`).

4.  **Manage Release Tracks** — fluxo real do produto, não o genérico:
    -   **`internal`** (Teste interno): destino de todo `release.yml` — só o Luiz valida,
        sem review do Google, disponível quase na hora.
    -   **`alpha`** (Teste fechado): promovido a partir de `internal` via
        `.github/workflows/promote-release.yml` (`workflow_dispatch` manual,
        `gradlew promoteReleaseArtifact` — mesmo AAB, sem rebuild) depois que o Luiz validar
        o teste interno.
    -   **`beta`/`production`**: **ainda não liberados**. `promote-release.yml` tem um
        guardrail técnico que falha o workflow se alguém tentar essas trilhas — exige
        decisão explícita do Luiz, não é autonomia do squad. Primeiro publish em
        `production` deve ser versionado **1.0.0** (ver `VERSIONING.md`).

5.  **Configure Release**:
    -   **Release Notes**: lidas de `android/app/src/main/play/release-notes/pt-BR/
        default.txt` pelo próprio `release.yml`.
    -   **Staged Rollouts / Targeting**: ainda não configurado — não relevante enquanto o
        app estiver só em `internal`/`alpha`.

6.  **Rollout**:
    -   `internal` e `alpha` são 100% rollout por padrão (trilhas de teste). Rollout
        gradual (`userFraction`) só entra em jogo quando `production` for liberado.

## Backend Service Deployment

-   Concurrent deployment of backend services is often necessary, especially for features relying on updated APIs or AI models.
-   The worker em `integrations/cloudflare/ai-diagnosis-worker/` (Cloudflare `linka-ai-diagnosis-worker`, **Gemini 2.0 Flash primário** / Qwen3 30B fallback cloud / fallback local) tem seu próprio deploy via `npx wrangler deploy`, executado ANTES do commit quando há mudanças em `src/`.

## Key Files/Configuration

-   `builds/apk/release/<versionName>/`: Contains final release APK files ready for upload.
-   `key.properties`: Holds sensitive signing credentials used during the build process.
-   Google Play Console: The primary platform for managing app releases.
-   Backend deployment configurations (e.g., `cloudflare/ai-diagnosis-worker/wrangler.toml` for worker deployment).

## Known Risks

-   The specific steps within the Google Play Console, including managing different release tracks, staged rollouts, and app store listing information, require human expertise.
-   Ensuring compatibility between the mobile app version and the deployed backend services (including the Cloudflare worker) is critical and needs human coordination.
-   Rollback procedures in case of critical issues post-deployment need to be clearly defined and validated by a human.
