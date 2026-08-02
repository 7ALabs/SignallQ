# Scripts Oficiais

- **Status:** ativo
- **Última validação:** 2026-07-23
- **Escopo:** scripts PowerShell/shell em `scripts/`

Scripts ficam em:

```text
<raiz-do-projeto>\scripts\
```

> Pasta local do repo: `C:\Projetos\SignallQ`.

## Build APK

### Debug

```powershell
.\scripts\build-apk-debug.ps1
```

Saida:

```text
builds/apk/debug/<versionName>/signallq-android-v<versionName>+<versionCode>-debug-<yyyyMMdd-HHmmss>.apk
```

### Release

```powershell
.\scripts\build-apk-release.ps1
```

Saida:

```text
builds/apk/release/<versionName>/signallq-android-v<versionName>+<versionCode>-release-<yyyyMMdd-HHmmss>.apk
```

Tambem existem tarefas Gradle equivalentes:

```powershell
.\gradlew.bat archiveDebugApk
.\gradlew.bat archiveReleaseApk
```

Nunca entregue `app-debug.apk` ou `app-release.apk` diretamente.

## Versionamento

```powershell
.\scripts\version.ps1 show
.\scripts\version.ps1 patch
.\scripts\version.ps1 minor
.\scripts\version.ps1 major
.\scripts\version.ps1 build
```

O script altera `gradle/libs.versions.toml`.

## Ambiente

```powershell
.\scripts\check-env.ps1
```

Valida Java, Android SDK, ADB e ferramentas auxiliares.

## Limpeza

```powershell
.\scripts\clean-build.ps1
```

Remove outputs de build locais, mas nao deve apagar `builds/apk/`, onde ficam os APKs arquivados.

## Legado

Scripts antigos de build release foram removidos. O projeto deve usar somente:

```powershell
.\scripts\build-apk-debug.ps1
.\scripts\build-apk-release.ps1
```

Scripts preservados por contexto historico ficam em `scripts/legacy/` e nao fazem parte do fluxo ativo.
