# Versionamento

Fonte de verdade:

```text
gradle/libs.versions.toml
```

Campos:

```toml
versionName = "0.9.1"
versionCode = "26"
```

`app/build.gradle.kts` consome esses valores no `defaultConfig`.

## Comandos

```powershell
.\scripts\version.ps1 show
.\scripts\version.ps1 patch
.\scripts\version.ps1 minor
.\scripts\version.ps1 major
.\scripts\version.ps1 build
.\scripts\version.ps1 set 1.0.0+30
```

## Regras

- `versionCode` deve ser inteiro crescente.
- Release publico sempre incrementa `versionCode`.
- `versionName` segue SemVer: `MAJOR.MINOR.PATCH`.
- APKs devem refletir versao no nome pelo padrao de `docs/APK_OUTPUT_POLICY.md`.

## Exemplo

Atual:

```text
0.9.1+26
```

Patch:

```powershell
.\scripts\version.ps1 patch
```

Resultado:

```text
0.9.2+27
```
