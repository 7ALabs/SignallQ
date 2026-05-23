# Politica De Saida De APK

Todo APK gerado pelo projeto deve ser arquivado na pasta oficial:

```text
builds/apk/<buildType>/<versionName>/
```

## Nome obrigatorio

```text
linka-android-v<versionName>+<versionCode>-<buildType>-<yyyyMMdd-HHmmss>.apk
```

Exemplo:

```text
builds/apk/release/0.9.1/linka-android-v0.9.1+26-release-20260523-112233.apk
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
- `versionName` e `versionCode` vem de `gradle/libs.versions.toml`.
- Para release publico, incrementar `versionCode` antes do build.
- APKs gerados continuam fora do Git.
