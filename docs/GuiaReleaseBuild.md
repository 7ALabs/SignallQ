# Guia De Release Build - Linka Android

Use este guia para gerar APK release assinado sem perder versao, nome ou local do artefato.

## Saida oficial

Todo APK release deve ficar em:

```text
C:\Projetos\Linka Android\builds\apk\release\<versionName>\
```

Nome obrigatorio:

```text
linka-android-v<versionName>+<versionCode>-release-<yyyyMMdd-HHmmss>.apk
```

Exemplo:

```text
C:\Projetos\Linka Android\builds\apk\release\0.9.1\linka-android-v0.9.1+26-release-20260523-112233.apk
```

## Pre-requisitos

- PowerShell 7+.
- Java/JDK disponivel via `JAVA_HOME` ou `PATH`.
- Android SDK configurado por `local.properties` ou `ANDROID_HOME`.
- `key.properties` na raiz do projeto.
- `segredos/linka.jks` presente localmente.

`key.properties` deve apontar para:

```properties
storeFile=segredos/linka.jks
```

## Versionamento

Versao fica em:

```text
gradle/libs.versions.toml
```

Antes de um release publico, incremente versao:

```powershell
.\scripts\version.ps1 patch
```

Use `minor`, `major` ou `build` quando fizer sentido. `versionCode` nunca deve diminuir.

## Build release oficial

```powershell
cd "C:\Projetos\Linka Android"
.\scripts\build-apk-release.ps1
```

Alternativa Gradle:

```powershell
.\gradlew.bat archiveReleaseApk
```

O arquivo `app/build/outputs/apk/release/app-release.apk` e apenas uma saida bruta interna do Gradle. Nao distribua esse arquivo diretamente.

## Validacao pos-build

```powershell
$apk = "C:\Projetos\Linka Android\builds\apk\release\<versionName>\<arquivo>.apk"
aapt dump badging $apk | findstr version
jarsigner -verify $apk
adb install -r $apk
```

## Checklist

- [ ] `versionCode` incrementado.
- [ ] `versionName` correto.
- [ ] `key.properties` presente.
- [ ] APK salvo em `builds/apk/release/<versionName>/`.
- [ ] Nome contem `versionName`, `versionCode`, `release` e timestamp.
- [ ] APK validado com `aapt`.
- [ ] APK assinado validado com `jarsigner`.
- [ ] Instala no dispositivo com `adb install -r`.
