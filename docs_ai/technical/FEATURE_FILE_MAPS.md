# Feature File Maps

## Objective

This document maps each feature module to its key files, including UI entry points, ViewModels, Repositories, and Data Sources, based on inferred project structure.

## Feature Module Mappings

-   **`app` Module**:
    -   **Entry Point**: `linka-android-kotlin/app/src/main/kotlin/com/linka/app/LinkaApp.kt`
    -   **Core UI/Navigation**: `linka-android-kotlin/app/ui/` (e.g., `AppNavigation.kt`, `MainScreen.kt`, `Theme.kt` - *inferential paths*)

-   **`featureDevices` Module**:
    -   **Module Path**: `linka-android-kotlin/featureDevices/`
    -   **UI Screens**: `ui/DeviceListScreen.kt`, `ui/DeviceDetailScreen.kt` (*inferential paths*)
    -   **ViewModels**: `ui/DeviceListViewModel.kt`, `ui/DeviceDetailViewModel.kt` (*inferential paths*)
    -   **Repository (if feature-specific)**: `data/DeviceRepository.kt` (*inferential path*)
    -   **Data Sources (if feature-specific)**: `data/DeviceNetworkDataSource.kt`, `data/DeviceLocalDataSource.kt` (*inferential paths*)

-   **`featureDiagnostico` Module**:
    -   **Module Path**: `linka-android-kotlin/featureDiagnostico/`
    -   **UI Screens**: `ui/DiagnosticScreen.kt`, `ui/DiagnosticResultScreen.kt` (*inferential paths*)
    -   **ViewModels**: `ui/DiagnosticViewModel.kt` (*inferential path*)
    -   **AI Interaction Logic**: `ui/` or `data/` package for `AiDiagnosisService` usage.

-   **`featureDns` Module**:
    -   **Module Path**: `linka-android-kotlin/featureDns/`
    -   **UI Screens**: `ui/DnsScreen.kt`, `ui/DnsTestScreen.kt` (*inferential paths*)
    -   **ViewModels**: `ui/DnsViewModel.kt` (*inferential path*)

-   **`featureFibra` Module**:
    -   **Module Path**: `linka-android-kotlin/featureFibra/`
    -   **Key Files**: Requires human validation for specific UI, ViewModel, Repository, and Data Source files.

-   **`featureHistory` Module**:
    -   **Module Path**: `linka-android-kotlin/featureHistory/`
    -   **UI Screens**: `ui/HistoryScreen.kt`, `ui/HistoryDetailScreen.kt` (*inferential paths*)
    -   **ViewModels**: `ui/HistoryViewModel.kt` (*inferential path*)
    -   **Repository/Data Source**: Likely interacts with `coreDatabase/dao/HistoryDao.kt`.

-   **`featureHome` Module**:
    -   **Module Path**: `linka-android-kotlin/featureHome/`
    -   **UI Screens**: `ui/HomeScreen.kt` (*inferential path*)
    -   **ViewModels**: `ui/HomeViewModel.kt` (*inferential path*)
    -   **Navigation Hub**: Likely orchestrates navigation to other features.

-   **`featureSettings` Module**:
    -   **Module Path**: `linka-android-kotlin/featureSettings/`
    -   **UI Screens**: `ui/SettingsScreen.kt`, `ui/SettingsCategoryScreen.kt` (*inferential paths*)
    -   **ViewModels**: `ui/SettingsViewModel.kt` (*inferential path*)
    -   **Preference Management**: Interacts with `coreDatastore/`.

-   **`featureSpeedtest` Module**:
    -   **Module Path**: `linka-android-kotlin/featureSpeedtest/`
    -   **UI Screens**: `ui/SpeedtestScreen.kt`, `ui/SpeedtestResultScreen.kt` (*inferential paths*)
    -   **ViewModels**: `ui/SpeedtestViewModel.kt` (*inferential path*)
    -   **Network Interaction**: Uses `coreNetwork/api/ApiService.kt`.

-   **`featureWifi` Module**:
    -   **Module Path**: `linka-android-kotlin/featureWifi/`
    -   **UI Screens**: `ui/WifiScreen.kt`, `ui/WifiDetailScreen.kt` (*inferential paths*)
    -   **ViewModels**: `ui/WifiViewModel.kt` (*inferential path*)

## Key Files/Configuration

-   Feature module directories (`feature*`).
-   `ui/` sub-directory within feature modules for screens and ViewModels.
-   `data/` sub-directory for feature-specific Repositories and Data Sources.
-   Core modules (`core*`) for shared components used by features.

## Known Risks

-   Specific file paths, naming conventions, and the exact internal structure (e.g., presence of a `data/` sub-package) are inferred and require human validation.
-   The existence and primary responsibility of Repositories/Data Sources specific to each feature need confirmation.
-   The exact implementation of inter-feature communication or shared logic needs detailed review.
