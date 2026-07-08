# Deployment

## Objective

This document outlines the deployment process for the SignallQ Android Kotlin application, detailing how new versions are released to end-users.

> Versao atual: **0.23.0** (versionCode 56), release 2026-07-05.
> Namespace/applicationId atual: **`io.signallq.app`** (renomeado de `io.veloo.app`
> em 2026-06-28; caminho fisico do codigo do `:app` continua `io/veloo/app/kotlin/`).
> Demais identificadores tecnicos permanecem: repo `gmmattey/linka-android`, worker
> `linka-ai-diagnosis-worker`. A partir de 0.22.1 ha publicacao automatizada na Play
> Console (trilha de teste fechado) via gradle-play-publisher no release por tag.

## Deployment Target

O canal **primário** de distribuição é o **Firebase App Distribution** (builds
internos). O **Google Play Store** é o alvo para publicação pública em loja.

## Processo Canônico (Firebase) — ordem obrigatória

NUNCA pular etapas. NUNCA rodar `assembleRelease` sem `clean` + `--no-build-cache`.

1. **Commit** — stage e commit de todos os arquivos modificados.
2. **Push** — `git push origin main`.
3. **Clean build** — `./gradlew clean assembleRelease --no-build-cache`.
4. **Upload** — `./gradlew appDistributionUploadRelease`.

**Worker Cloudflare:** se houver mudanças em
`integrations/cloudflare/ai-diagnosis-worker/src/`, rodar `npx wrangler deploy`
**ANTES** do commit.

## Deployment Steps

The deployment process involves several key steps:

1.  **Prepare Release Build**:
    -   Ensure the application has gone through development, testing, and quality assurance.
    -   Generate a release-ready APK (or App Bundle) following the procedures in `operations/APK_BUILD.md`.
    -   Verify that versioning (`operations/VERSIONING.md`) is correctly applied for the release.

2.  **Sign the Application**:
    -   The release APK/App Bundle must be signed with a release keystore, as configured and managed according to `operations/APK_BUILD.md` and security best practices.

3.  **Upload to Google Play Console**:
    -   Log in to the Google Play Console.
    -   Navigate to the app's release management section.
    -   Upload the signed release APK or App Bundle.

4.  **Manage Release Tracks**:
    -   **Internal Testing**: For immediate distribution to a small internal team for basic validation.
    -   **Closed/Open Beta Testing**: For distributing to a larger group of testers before a full rollout.
    -   **Production**: The final release track for all users.
    -   The new build is assigned to one or more of these tracks.

5.  **Configure Release**:
    -   **Release Notes**: Draft and add release notes detailing new features, bug fixes, and improvements for the current version.
    -   **Staged Rollouts**: Configure a phased rollout (e.g., releasing to a percentage of users gradually) to monitor for issues before a full rollout.
    -   **Targeting**: Define target countries, devices, or user segments if applicable.

6.  **Rollout**:
    -   Initiate the release to the chosen track(s). The Google Play Store then manages the distribution to users based on the selected rollout strategy.

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
