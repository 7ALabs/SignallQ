# Release Process

## Objective

This document outlines the process for releasing new versions of the SignallQ Android Kotlin application, covering the steps from build to deployment.

> Versao atual: **0.26.0** (versionCode 61), release 2026-07-17.
> Namespace/applicationId atual: **`io.signallq.app`** (renomeado de `io.veloo.app`
> em 2026-06-28). O caminho fisico do codigo do modulo `:app` continua sendo
> `io/veloo/app/kotlin/` — nao alterar. Demais identificadores tecnicos de infra
> permanecem: repo `7ALabs/linka-android`, worker `linka-ai-diagnosis-worker`.
> Historico autoritativo de versoes Android: `android/CHANGELOG.md`.

## Processo Canônico do Projeto (atualizado 2026-07-17)

Dois canais, os dois via **GitHub Actions** — não mais comando local manual. Regra única
para os dois: **nunca subir um build (debug ou release) sem incrementar `versionCode`** em
`android/gradle/libs.versions.toml` antes, commitado e pushado (mesmo campo global, sem
contador separado por canal — evita dois uploads com o mesmo número).

### Canal 1 — Firebase App Distribution (debug/validação rápida)

Workflow `.github/workflows/firebase-distribution.yml`, `workflow_dispatch` manual (sob
demanda, não em todo push). Builda `assembleRelease` (ou `assembleDebug` via input), assina,
sobe via `appDistributionUploadRelease`/`...Debug`. Depende do secret `FIREBASE_TOKEN`
(gerado com `firebase login:ci` numa sessão interativa real — precisa de TTY, não roda em
CI headless nem via agente — configurado com `gh secret set FIREBASE_TOKEN --repo
7ALabs/linka-android`).

### Canal 2 — Play Console (release oficial), trilha em 2 etapas

1. Bump de versão (`libs.versions.toml`, `CHANGELOG.md`, `docs_ai/RELEASES.md`) — escopo
   real desde a última versão **realmente publicada** (ver `VERSIONING.md`).
2. `git tag vX.Y.Z && git push origin vX.Y.Z` — dispara `.github/workflows/release.yml`:
   build, assinatura, GitHub Release, e publica direto na trilha **`internal`** (teste
   interno, sem review do Google, só o Luiz valida).
3. Depois de validado, `.github/workflows/promote-release.yml` (`workflow_dispatch` manual)
   promove o MESMO AAB de `internal` pra `alpha` (`gradlew promoteReleaseArtifact`) — sem
   rebuild, sem reassinar (padrão recomendado pelo Google: testar um binário, promover o
   mesmo binário entre trilhas).
4. **Guardrail técnico**: `promote-release.yml` só aceita `internal`/`alpha` como destino.
   Beta e produção ainda não estão liberados — qualquer tentativa nessas trilhas falha o
   workflow e exige decisão explícita do Luiz.

**Worker Cloudflare:** quando houver mudanças em
`integrations/cloudflare/ai-diagnosis-worker/src/`, rodar `npx wrangler deploy`
**ANTES** do commit.

> Detalhe completo do fluxo Play Store (incluindo o guardrail de trilhas) em
> `operations/DEPLOY.md`.

## Release Stages

The release process generally follows these stages:

1.  **Development**: Features are developed and unit tested within feature modules.
2.  **Integration and Testing**: Code is integrated, and comprehensive testing (unit, integration, UI) is performed.
3.  **Build Generation (`operations/APK_BUILD.md`)**: A release-ready APK is generated using the defined build system and versioning strategy.
4.  **Staging/Pre-release Testing**: The generated APK may be deployed to a staging environment or distributed to a limited group of testers for final validation.
5.  **Release (`operations/DEPLOY.md`)**: The validated APK is deployed to the target distribution platform (e.g., Google Play Store).
6.  **Post-Release Monitoring**: Application performance and stability are monitored after release.

## Key Aspects of Release Management

-   **APK Generation**: Detailed information on how release APKs are built is covered in `operations/APK_BUILD.md`. This includes build configurations and signing.
-   **Versioning (`operations/VERSIONING.md`)**: The strategy for assigning version codes and version names is documented separately.
-   **Deployment (`operations/DEPLOY.md`)**: The steps and procedures for deploying the application to distribution channels are outlined.
-   **Release Notes**: Information regarding how release notes are drafted, reviewed, and included with releases needs to be validated. This might involve summarizing changes from version control or issue tracking.

## Artifacts and Locations

-   **Release APKs**: Final release builds are stored in `builds/apk/release/<versionName>/`.
-   **Versioning Script**: `scripts/version.ps1` likely plays a role in managing version information during the build process.

## Known Risks

-   The exact procedures for each stage, including quality gates, testing checklists, and rollback strategies, require human validation.
-   The process for generating release notes and managing user-facing documentation for each version needs to be confirmed.
-   Specific approval steps or sign-offs required before a release are not detailed and need human input.



## Feature Activation Process (pós-MVP)

Quando uma feature flag pós-MVP deve ser ativada no release:

### Checklist

- [ ] Implementação concluída e testada em debug
- [ ] Testes E2E passando
- [ ] Code review aprovado
- [ ] Arquivo `.changelog` adicionado descrevendo a feature
- [ ] TECNICO.md e FUNCIONAL.md atualizados (se necessário)

### Passos

1. **Alterar build.gradle.kts**
   - Arquivo: `app/build.gradle.kts`
   - Bloco: `release { ... }`
   - Linha: `buildConfigField("Boolean", "FEATURE_XXXX", "true")`
   - Mudar de `"false"` para `"true"`

2. **Incrementar versão em libs.versions.toml**
   - Arquivo: `gradle/libs.versions.toml`
   - Campo: `versionName` — seguir semver (ex: 0.23.0 → 0.23.1)
   - Campo: `versionCode` — incrementar de 1 em 1 (ex: 56 → 57)

3. **Atualizar CHANGELOG**
   - Arquivo: `CHANGELOG.md`
   - Adicionar entrada em **Unreleased** ou na versão correspondente
   - Descrever o que a feature faz do ponto de vista do usuário

4. **Build Release APK (clean, sem cache)**
   - `./gradlew clean assembleRelease --no-build-cache`
   - Assinar com chave de release (configurada em `key.properties`)
   - Para artefato arquivado/nomeado, use `.\scripts\build-apk-release.ps1`
   - Conferir APK em `builds/apk/release/<versionName>/`

5. **Distribuir (Firebase App Distribution)**
   - `./gradlew appDistributionUploadRelease`
   - Para publicação em loja, seguir o fluxo Play Store em `operations/DEPLOY.md`
     (internal testing → beta → production com staged rollout)

6. **Atualizar documentação pública** (se necessário)
   - Release notes
   - Documentação de usuário
   - Página de novidades in-app

### Risco: Feature incompleta em release

Se uma flag foi ativada mas a feature não está 100% pronta, desativar imediatamente:
- Reverter `buildConfigField` para `"false"`
- Incrementar versão (hotfix)
- Rebuild e redeploy

