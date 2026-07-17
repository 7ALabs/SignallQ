> **Arquivado em 2026-07-16.** Consolidacao de documentacao docs_ai/ (branch docs/consolidacao-documentacao-docs-ai). Conteudo absorvido por: TECNICO.md secao 3 (Config Android). Preservado apenas como referencia historica -- nao reflete o estado atual.

---

# Build System — Android SignallQ

**Última atualização:** 2026-06-21 (v0.16.0)

Gradle build com Kotlin DSL (.kts).

## Versão Atual

| Campo | Valor |
|---|---|
| versionName | 0.16.0 |
| versionCode | 46 |
| applicationId | io.signallq.app |
| compileSdk | 36 |
| minSdk | 24 |
| targetSdk | 36 |
| JVM target | 17 |

Versões declaradas em `gradle/libs.versions.toml`.

## Root Files

- **`build.gradle.kts`**: Global config, repositories, plugins
- **`settings.gradle.kts`**: Module inclusion (app, core*, etc.)
- **`gradle.properties`**: Version codes, build flags, JVM args
- **`local.properties`**: Android SDK path (local only)
- **`key.properties.template`**: Signing keys template

## Module Build Scripts

- **`app/build.gradle.kts`**: App config, versionCode, versionName, build types, deps
- **`coreDatabase/build.gradle.kts`**: Library config, deps
- Each module: `module/build.gradle.kts`

## Commands

```bash
./gradlew build           # Full build
./gradlew assembleDebug   # Debug APK
./gradlew assembleRelease # Release APK
./gradlew lint           # Static analysis
./gradlew test           # Tests
```

## Key Configs

- **applicationId**: `io.signallq.app` (declarado em `app/build.gradle.kts`)
- **versionCode / versionName**: declarados em `gradle/libs.versions.toml` (atualmente 46 / 0.16.0)
- **Build types**: `debug` (todas as feature flags ativas) e `release` (apenas flags MVP ativas)
- **Feature flags**: 32 `buildConfigField` booleanos em `app/build.gradle.kts`; acessados via `FeatureFlags.*` (não `BuildConfig.*` diretamente)
- **Signing**: chaves em `key.properties` (gitignored). Template em `key.properties.template`
- **DI**: Hilt — `@HiltAndroidApp` em `SignallQApplication`, kapt plugin em `:app` e `:coreDatabase`
- **Desugaring**: habilitado — suporte a APIs Java 8+ em minSdk 24
