# Scripts

## Oficiais

- `build-apk-debug.ps1`: gera APK debug versionado em `builds/apk/debug/<versionName>/`.
- `build-apk-release.ps1`: gera APK release versionado em `builds/apk/release/<versionName>/`.
- `version.ps1`: altera `gradle/libs.versions.toml`.
- `check-env.ps1`: valida ambiente local.
- `clean-build.ps1`: remove outputs/cache locais sem apagar `builds/apk/`.

## Agentes

- `agent-status.ps1`
- `agent-delegate.ps1`
- `agent-wake.ps1`

## Investigacao Android

- `audit-gpon/`: auditoria GPON.
- `modem/`: scripts de analise/sondagem de modem que nao alteram o app Flutter.
- `speedtest/`: calibracao e paridade historica de speedtest.

## Legacy

`legacy/` contem scripts preservados apenas como referencia historica. Eles nao fazem parte do fluxo ativo do Android Kotlin.

Nao use scripts em `legacy/` para tarefas novas.
