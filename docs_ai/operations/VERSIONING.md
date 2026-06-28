# Politica de Versionamento

## Fonte de verdade

```
android/gradle/libs.versions.toml
```

Campos relevantes: `versionName` e `versionCode`. Consumidos por `app/build.gradle.kts` no `defaultConfig`.

## SemVer

`versionName` segue **MAJOR.MINOR.PATCH**:

- **MAJOR** — mudanca incompativel ou marco de produto (ex: v1.0.0 no lancamento)
- **MINOR** — feature nova ou mudanca funcional relevante
- **PATCH** — correcao de bug ou ajuste menor

## Pre-release

Sufixos pre-release seguem SemVer:

- Beta fechado: `1.0.0-beta.1`, `1.0.0-beta.2`, ...
- Release candidate: `1.0.0-rc.1`, `1.0.0-rc.2`, ...
- Producao: `1.0.0` (sem sufixo)

## versionCode

- Inteiro crescente. **Nunca reutilizar** — requisito da Play Store.
- Incrementar em 1 para cada build enviado a Play Store.
- Builds internos (Firebase App Distribution) tambem incrementam, mas nao precisam ser consecutivos com a Play Store.

## Quando v1.0.0

O app sobe para **v1.0.0** no Beta Fechado (M2), que marca o primeiro upload a Play Store.

Antes disso (M0/M1), o app permanece em `0.x.y` — versao de desenvolvimento.

## Tabela de fases

| Fase | versionName | versionCode | Notas |
|------|-------------|-------------|-------|
| Desenvolvimento (M0/M1) | 0.x.y | 52+ | Firebase App Distribution |
| Beta Fechado (M2) | 1.0.0-beta.1+ | 53+ | Primeiro upload Play Store |
| Open Beta | 1.0.0-rc.1+ | 60+ | Track aberto na Play Store |
| Producao (M3) | 1.0.0 | 70+ | Release publico |

## Regras

- `versionName` muda apenas em releases significativos (feature, correcao, milestone).
- `versionCode` incrementa em todo build publicado (Play Store ou Firebase).
- APKs refletem a versao no nome conforme `docs_ai/operations/APK_OUTPUT_POLICY.md`.

## Comandos

```powershell
.\scripts\version.ps1 show
.\scripts\version.ps1 patch
.\scripts\version.ps1 minor
.\scripts\version.ps1 major
.\scripts\version.ps1 build
.\scripts\version.ps1 set 1.0.0-beta.1+53
```
