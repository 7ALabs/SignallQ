# Settings Functionality

## Objective

This document details the settings functionality, mapping specific components and interactions within the Linka Android Kotlin application.

## Settings Management

User preferences and configurations are managed via the `featureSettings` module.

1.  **Accessing Settings**:
    -   **Entry Point**: UI elements in `linka-android-kotlin/featureSettings/ui/SettingsScreen.kt` (*inferential path*), likely accessible from `featureHome/ui/HomeScreen.kt` (*inferential path*).

2.  **Settings Categories and Mappings**:
    -   **General**: Theme (`coreui/Theme.kt` - *inferential path*), Language.
    -   **Account**: User profile management (if applicable, potentially within `app/` or a dedicated `coreAuth/` module - *needs validation*).
    -   **Notifications**: Preference management likely using `coreDatastore/` (e.g., `NotificationPreferencesDataSource.kt` - *inferential path*).
    -   **Privacy**: Settings related to data sharing, potentially interacting with `corePermissions/`.
    -   **Network**: DNS settings (`featureDns/ui/DnsScreen.kt`), Speedtest server preference (`featureSpeedtest/ui/SpeedtestScreen.kt`).
    -   **AI Features**: Toggles for AI diagnostics, data sharing for AI model improvement. Likely managed via `coreDatastore/` and affecting logic in `featureDiagnostico/` and `coreNetwork/ai/`.

3.  **Modifying Preferences**:
    -   UI controls in `SettingsScreen.kt` interact with ViewModels (`SettingsViewModel.kt` - *inferential path*).
    -   Changes are persisted using `coreDatastore/` accessors.

## Persistence

-   Settings are persisted using `coreDatastore/` (e.g., `linka-android-kotlin/coreDatastore/src/main/kotlin/com/linka/coredatastore/AppSettingsDataSource.kt` - *inferential path*).

## Key Files/Modules

-   **`linka-android-kotlin/featureSettings/`**: Contains `SettingsScreen.kt` and `SettingsViewModel.kt` (*inferential paths*).
-   **`linka-android-kotlin/coreDatastore/`**: Provides data storage for preferences.
-   **`linka-android-kotlin/featureDns/`, `featureSpeedtest/`, `featureDiagnostico/`**: Modules whose settings are configured here.

## Known Risks

-   Specific file paths for ViewModels and DataStore accessors are inferential.
-   The exact list of settings, their default values, and persistence mechanisms need human validation.
-   AI feature settings logic needs confirmation.
