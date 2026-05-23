# Build System

Gradle build with Kotlin DSL (.kts).

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

- **applicationId**: App ID (in `app/build.gradle.kts`)
- **versionCode**: Int version
- **versionName**: Semantic version string
- **Build types**: debug, release
- **Dependencies**: Declared per module

**Needs validation**: Exact signing config, flavors, build variants
