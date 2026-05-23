# Release Process

## Objective

This document outlines the process for releasing new versions of the Linka Android Kotlin application, covering the steps from build to deployment.

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
- [ ] ANDROID_TECNICO.md e ANDROID_FUNCIONAL.md atualizados (se necessário)

### Passos

1. **Alterar build.gradle.kts**
   - Arquivo: `app/build.gradle.kts`
   - Bloco: `release { ... }`
   - Linha: `buildConfigField("Boolean", "FEATURE_XXXX", "true")`
   - Mudar de `"false"` para `"true"`

2. **Incrementar versão em libs.versions.toml**
   - Arquivo: `gradle/libs.versions.toml`
   - Campo: `app.version.name` — seguir semver (ex: v0.7.0 → v0.7.1)
   - Campo: `app.version.code` — incrementar de 1 em 1

3. **Atualizar CHANGELOG**
   - Arquivo: `CHANGELOG.md`
   - Adicionar entrada em **Unreleased** ou na versão correspondente
   - Descrever o que a feature faz do ponto de vista do usuário

4. **Build Release APK**
   - Executar script `scripts/release-build.ps1` ou comando gradle equivalente
   - Assinar com chave de release (configurada em `key.properties`)
   - Gerar APK com `.\scripts\build-apk-release.ps1`
   - Conferir APK em `builds/apk/release/<versionName>/`

5. **Deploy ao Play Store**
   - Usar internal testing → beta → production conforme política
   - Ativar gradualmente (staged rollout) para monitorar crashes

6. **Atualizar documentação pública** (se necessário)
   - Release notes
   - Documentação de usuário
   - Página de novidades in-app

### Risco: Feature incompleta em release

Se uma flag foi ativada mas a feature não está 100% pronta, desativar imediatamente:
- Reverter `buildConfigField` para `"false"`
- Incrementar versão (hotfix)
- Rebuild e redeploy

