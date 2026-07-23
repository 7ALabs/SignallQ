# Politica De Saida De APK

- **Status:** ativo
- **Última validação:** 2026-07-23
- **Fonte de verdade:** este documento (regra de nome/local de artefato); versão real em `android/gradle/libs.versions.toml`
- **Escopo:** build local e scripts de empacotamento do app Android
- **Documentos substituídos:** `docs_ai/operations/APK_BUILD.md` (arquivado, duplicava este conteúdo)

Todo APK gerado pelo projeto deve ser arquivado na pasta oficial:

```text
builds/apk/<buildType>/<versionName>/
```

## Nome obrigatorio

```text
signallq-android-v<versionName>+<versionCode>-<buildType>-<yyyyMMdd-HHmmss>.apk
```

Exemplo:

```text
builds/apk/release/0.23.0/signallq-android-v0.23.0+56-release-20260705-112233.apk
```

## Comandos oficiais

Use estes comandos para gerar APKs arquivados:

```powershell
.\scripts\build-apk-debug.ps1
.\scripts\build-apk-release.ps1
```

Ou diretamente via Gradle:

```powershell
.\gradlew.bat archiveDebugApk
.\gradlew.bat archiveReleaseApk
```

## Regras

- Nao distribuir `app-debug.apk` ou `app-release.apk` diretamente.
- Nao salvar APK em `Downloads`, desktop, raiz do projeto ou pastas antigas.
- Nao criar pasta `apk/` paralela.
- `versionName` e `versionCode` vem de `android/gradle/libs.versions.toml`.
- Para release publico, incrementar `versionCode` antes do build.
- APKs gerados continuam fora do Git.
