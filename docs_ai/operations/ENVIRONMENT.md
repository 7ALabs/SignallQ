# Environment - Ferramentas E Configuracao

Valide o ambiente com:

```powershell
.\scripts\check-env.ps1
```

## Requisitos

- PowerShell 7+.
- Java/JDK compativel com o Gradle Android.
- Android SDK com API configurada para `compileSdk`.
- `adb` em `PATH` ou acessivel pelo Android SDK.
- Gradle wrapper da raiz: `.\gradlew.bat`.

## Android SDK

Use uma destas opcoes:

```powershell
$env:ANDROID_HOME = "C:\Users\<usuario>\AppData\Local\Android\Sdk"
```

ou crie `local.properties` na raiz:

```properties
sdk.dir=C\:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk
```

`local.properties` e local e nao deve ir para Git.

## Java

Se o `java` nao estiver no `PATH`, use o JBR do Android Studio ou uma JDK instalada:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## Build

Comandos oficiais:

```powershell
.\scripts\build-apk-debug.ps1
.\scripts\build-apk-release.ps1
```

ou:

```powershell
.\gradlew.bat archiveDebugApk
.\gradlew.bat archiveReleaseApk
```

APKs validos ficam em `builds/apk/<buildType>/<versionName>/`.

## Signing — Credenciais e Keystore

Para builds de **release** assinados, veja `docs_ai/operations/SIGNING.md` para:

- Setup local de `key.properties` e keystore
- Configuração de GitHub Secrets para CI/CD
- Geração de novo keystore se necessário
- Checklist de segurança
