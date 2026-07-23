# APK Build - Geracao, Nome E Localizacao

Esta e a regra oficial para gerar APKs do SignallQ Android.

## Pasta oficial

```text
<raiz-do-projeto>\builds\apk\<buildType>\<versionName>\
```

> A pasta local do repo é `C:\Projetos\Linka Android` (nome de diretório histórico;
> o produto é **SignallQ**).

Exemplos:

```text
C:\Projetos\Linka Android\builds\apk\debug\0.16.0\
C:\Projetos\Linka Android\builds\apk\release\0.16.0\
```

## Nome oficial

```text
signallq-android-v<versionName>+<versionCode>-<buildType>-<yyyyMMdd-HHmmss>.apk
```

Exemplo:

```text
signallq-android-v0.16.0+46-release-20260621-112233.apk
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
segredos/signallq.jks
```

Esses arquivos sao locais e ficam fora do Git.
