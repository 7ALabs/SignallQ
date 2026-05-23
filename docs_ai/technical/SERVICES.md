# Services Overview

## Objective

This document details the services provided by project modules, mapping them to concrete file paths and interfaces.

## Service Categories and Mappings

Services are categorized and mapped to their likely locations:

### 1. Core Services

Foundational services provided by core modules:

-   **Network Service (`coreNetwork`)**:
    -   **Module Path**: `linka-android-kotlin/coreNetwork/`
    -   **Key Interface/Service**: `ApiService.kt` (e.g., `linka-android-kotlin/coreNetwork/src/main/kotlin/.../api/ApiService.kt` - *necessita validação humana*).
    -   **AI Service**: `AiDiagnosisService.kt` for Cloudflare worker communication (e.g., `linka-android-kotlin/coreNetwork/src/main/kotlin/.../ai/AiDiagnosisService.kt` - *inferential path, necessita validação humana*).
    -   **Consumer(s)**: Repositories.

-   **Database Service (`coreDatabase`)**:
    -   **Module Path**: `linka-android-kotlin/coreDatabase/`
    -   **Key Interfaces/Classes**: DAO interfaces (e.g., `linka-android-kotlin/.../dao/DeviceDao.kt` - *necessita validação humana*), Room Database class (e.g., `AppDatabase.kt` - *necessita validação humana*).
    -   **Consumer(s)**: Repositories.

-   **DataStore Service (`coreDatastore`)**:
    -   **Module Path**: `linka-android-kotlin/coreDatastore/`
    -   **Key Classes**: Data access classes (e.g., `linka-android-kotlin/.../UserPreferencesDataSource.kt` - *necessita validação humana*).
    -   **Consumer(s)**: ViewModels, Repositories.

-   **Permission Service (`corePermissions`)**:
    -   **Module Path**: `linka-android-kotlin/corePermissions/`
    -   **Key Classes**: Utility classes for permission management (*specific file paths necessita validação humana*).
    -   **Consumer(s)**: Feature modules.

-   **Telephony Service (`coreTelephony`)**:
    -   **Module Path**: `linka-android-kotlin/coreTelephony/`
    -   **Key Classes**: Utility classes for telephony APIs (*specific file paths necessita validação humana*).
    -   **Consumer(s)**: Features needing phone state access.

### 2. Feature Services

Services exposed by feature modules:

-   **Device Management (`featureDevices`)**:
    -   **Module Path**: `linka-android-kotlin/featureDevices/`
    -   **Key Components**: UI composables (e.g., `DeviceListScreen.kt`), ViewModel (`DeviceViewModel.kt`) (*inferential paths*).
    -   **Consumer(s)**: `featureHome`, other features.

-   **Diagnostic Service (`featureDiagnostico`)**:
    -   **Module Path**: `linka-android-kotlin/featureDiagnostico/`
    -   **Key Components**: UI (`DiagnosticScreen.kt`), ViewModel (`DiagnosticViewModel.kt`), AI interaction logic (*inferential paths*).
    -   **Consumer(s)**: UI, `coreNetwork`.

-   **Speedtest Service (`featureSpeedtest`)**:
    -   **Module Path**: `linka-android-kotlin/featureSpeedtest/`
    -   **Key Components**: UI (`SpeedtestScreen.kt`), ViewModel (`SpeedtestViewModel.kt`), network logic (*inferential paths*).
    -   **Consumer(s)**: UI, `coreNetwork`.

*(Services from other `feature` modules follow similar patterns within their respective directories.)*

### 3. Background Services / Workers

-   **Cloudflare AI Diagnosis Worker**:
    -   **Provider**: External service. Configuration: `cloudflare/ai-diagnosis-worker/wrangler.toml`.
    -   **Consumer**: Mobile app via `coreNetwork/ai/AiDiagnosisService.kt`.

## Known Risks

-   Specific file paths for DAOs, Room Database classes, DataStore accessors, and feature-specific services are inferential and require human validation.
-   The exact service interfaces and contract definitions for feature modules need human confirmation.
-   The communication protocols and endpoints for the Cloudflare worker require human validation.
