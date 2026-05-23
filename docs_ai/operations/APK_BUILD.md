# APK Build - Geracao, Nome E Localizacao

Esta e a regra oficial para gerar APKs do Linka Android.

## Pasta oficial

```text
C:\Projetos\Linka Android\builds\apk\<buildType>\<versionName>\
```

Exemplos:

```text
C:\Projetos\Linka Android\builds\apk\debug\0.9.1\
C:\Projetos\Linka Android\builds\apk\release\0.9.1\
```

## Nome oficial

```text
linka-android-v<versionName>+<versionCode>-<buildType>-<yyyyMMdd-HHmmss>.apk
```

Exemplo:

```text
linka-android-v0.9.1+26-release-20260523-112233.apk
```

## Comandos oficiais

Use os scripts:

```powershell
.\scripts\build-apk-debug.ps1
.\scripts\build-apk-release.ps1
```

Ou as tarefas Gradle de arquivamento:

```powershell
.\gradlew.bat archiveDebugApk
.\gradlew.bat archiveReleaseApk
```

## O que nao fazer

- Nao distribuir `app-debug.apk`.
- Nao distribuir `app-release.apk`.
- Nao salvar APK em `Downloads`, desktop, raiz do projeto, `apk/` ou pastas antigas.
- Nao renomear APK manualmente fora do padrao.

## Origem bruta do Gradle

O Gradle ainda gera arquivos brutos internos:

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

Esses arquivos sao apenas intermediarios. O artefato valido para teste ou distribuicao e sempre o copiado para `builds/apk/...`.

## Versionamento

`versionName` e `versionCode` ficam em:

```text
gradle/libs.versions.toml
```

Antes de release publico:

```powershell
.\scripts\version.ps1 patch
```

ou use `minor`, `major`, `build` conforme o caso. `versionCode` deve ser sempre crescente.

## Assinatura release

Release usa:

```text
key.properties
segredos/linka.jks
```

Esses arquivos sao locais e ficam fora do Git.
