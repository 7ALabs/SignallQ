---
title: "Deployment"
description: "Processo de release do app Android SignallQ: build assinado, distribuição Firebase, publicação automatizada na Play Console e trilhas de release."
type: "runbook"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-26"
version: "1.1.0"
---

# Deployment

## Objective

This document outlines the deployment process for the SignallQ Android Kotlin application, detailing how new versions are released to end-users.

- **Status:** ativo
- **Última validação:** 2026-08-26 — corrigida defasagem: `release.yml` publica direto em
  `beta` (não `internal`) desde 2026-08-23 (commit `1555e92b`); documentado o
  `workflow_dispatch` de produção com ads (2026-08-26)
- **Fonte de verdade:** versão real em `android/gradle/libs.versions.toml` (não fixar número
  aqui, muda a cada release); trilhas/canais reais em `.github/workflows/release.yml` e
  `promote-release.yml`
- **Escopo:** deploy Android (Firebase App Distribution + Play Console) e worker de IA
- **Responsável:** Camilo (build/deploy), Rhodolfo (gate de release)

> Namespace/applicationId atual: **`io.signallq.app`** (renomeado de `io.veloo.app`
> em 2026-06-28; caminho fisico do codigo do `:app` continua `io/signallq/app/`).
> Demais identificadores tecnicos permanecem: repo `7ALabs/SignallQ`, worker
> `linka-ai-diagnosis-worker`. Publicacao na Play Console e distribuicao Firebase sao
> automatizadas via GitHub Actions — nao ha upload manual pela UI (ver abaixo).

## Deployment Target

Dois canais, os dois via **GitHub Actions**:
- **Firebase App Distribution** — validação rápida/debug, sob demanda.
- **Google Play Console** — release oficial. Push de tag comum publica direto na trilha
  `beta` com anúncios desligados. Produção (com anúncios reais) exige disparo manual
  parametrizado do mesmo workflow — ver seção "Manage Release Tracks" abaixo.

## Processo Canônico (atualizado 2026-07-17) — os dois canais via CI

**Regra única pros dois canais**: nunca subir um build sem incrementar `versionCode` em
`android/gradle/libs.versions.toml` antes, commitado e pushado.

### Firebase App Distribution

Workflow `.github/workflows/firebase-distribution.yml` (`workflow_dispatch` manual):
`clean` → `assembleRelease` (ou `assembleDebug`) → `appDistributionUploadRelease`/`...Debug`.
Depende do secret `FIREBASE_TOKEN` (gerado localmente via `firebase login:ci` — exige TTY
interativo, configurado uma vez com `gh secret set FIREBASE_TOKEN --repo
7ALabs/SignallQ`).

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
    -   `git tag vX.Y.Z && git push origin vX.Y.Z` dispara `.github/workflows/release.yml`
        (push de tag comum → sempre `-PplayTrack=beta -PadsEnabled=false`, sem input manual).
    -   O workflow builda, assina, cria o GitHub Release, e publica o AAB direto na trilha
        `beta` via `gradlew :app:publishReleaseBundle` (`gradle-play-publisher`, credencial
        `PLAY_SERVICE_ACCOUNT_JSON`).
    -   O release do consumer (SignallQ) é escopado exclusivamente ao módulo `:app` — o
        workflow roda `:app:assembleRelease`, `:app:bundleRelease`,
        `:app:uploadCrashlyticsMappingFileRelease` e `:app:publishReleaseBundle` (taskname
        prefixado, não o genérico), o que mantém o pipeline isolado de qualquer outro módulo
        do monorepo.
    -   `release.yml` também aceita `workflow_dispatch` manual com inputs `playTrack`
        (`beta`/`production`) e `adsEnabled` (booleano) — é o único jeito de publicar em
        `production` com anúncios reais; não muda o comportamento do push de tag.

4.  **Manage Release Tracks** — fluxo real do produto, não o genérico:
    -   **`beta`** (Teste aberto/fechado): destino de todo push de tag em `release.yml` — só
        o Luiz valida, disponível quase na hora, anúncios sempre desligados nesse caminho.
    -   **`internal`/`alpha`**: se for preciso mover o mesmo AAB já publicado em `beta` pra
        uma trilha de teste mais restrita, usar `.github/workflows/promote-release.yml`
        (`workflow_dispatch` manual, `gradlew promoteReleaseArtifact` — mesmo AAB, sem
        rebuild).
    -   **`production`**: exige decisão explícita do Luiz — não é autonomia do squad, e não
        é uma promoção. `promote-release.yml` bloqueia `production` como destino de
        propósito, e mesmo sem esse guardrail promover o binário de `beta` não ativaria
        anúncio nenhum: `USE_TEST_ADS`/`ADS_ENABLED` são compilados no AAB no momento do
        build (ver `app/build.gradle.kts`), não do publish. O caminho real é disparar
        `release.yml` manualmente com `playTrack=production` e `adsEnabled=true`, publicando
        um AAB **novo** direto na trilha — e só depois de criadas as chaves de Firebase
        Remote Config (`ads_native_enabled` + 5 por tela, ver
        `android/app/src/main/kotlin/io/signallq/app/ads/AdsRemoteConfigRepository.kt`); sem
        elas o app cai no fallback `AdsFlags.DESLIGADO` mesmo com o binário certo. Primeiro
        publish em `production` deve ser versionado **1.0.0** (ver `VERSIONING.md`).

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
