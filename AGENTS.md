# Linka Android - Guia Para Agentes

Este repositorio agora representa apenas o projeto Android nativo Kotlin.

## Entrada rapida

1. Leia `docs_ai/README.md` antes de carregar documentos especificos.
2. Use `settings.gradle.kts` para entender os modulos ativos.
3. Prefira busca por simbolo antes de abrir arquivos grandes.
4. Nao reintroduza PWA, Flutter legado, APKs gerados, caches, segredos ou dumps temporarios.

## Escopo dos agentes

- Produto/UX: use `docs_ai/functional/` e `docs_ai/design-system/`.
- Engenharia: use `docs_ai/technical/`, `settings.gradle.kts` e os modulos `core*`/`feature*`.
- QA/Release: use `docs_ai/operations/`, `docs/GuiaReleaseBuild.md` e `scripts/`.
- Integracoes: use `integrations/`, mantendo dependencias baixadas fora do Git.

## Regras de trabalho

- Codigo Android fica nos modulos Gradle da raiz.
- Documentacao viva fica em `docs_ai/`.
- Documentacao operacional complementar fica em `docs/`.
- Scripts versionados ficam em `scripts/`.
- Segredos devem ser recriados localmente a partir de templates, nunca migrados.
- Artefatos de build devem ser gerados de novo, nunca versionados.

## Validacao minima

Antes de concluir uma mudanca:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

Quando precisar gerar APK, use somente:

```powershell
.\scripts\build-apk-debug.ps1
.\scripts\build-apk-release.ps1
```

ou:

```powershell
.\gradlew.bat archiveDebugApk
.\gradlew.bat archiveReleaseApk
```

Nunca entregue `app-debug.apk` ou `app-release.apk` diretamente. A regra de saida esta em `docs/APK_OUTPUT_POLICY.md`.
