# AI Flow

## Objective

This document details the flow and integration of AI functionalities, mapping specific components, modules, and file paths involved in AI processing.

## AI Components and Integration Points

The primary AI integration involves the `cloudflare/ai-diagnosis-worker` and the mobile client's interaction via `coreNetwork`.

-   **AI Worker**:
    -   **Configuration**: `cloudflare/ai-diagnosis-worker/wrangler.toml`.
    -   **Purpose**: Executes AI models for diagnostic data analysis.

-   **Mobile App Client Interaction**:
    -   **Initiation**: `linka-android-kotlin/featureDiagnostico/` module (e.g., `DiagnosticScreen.kt`, `DiagnosticViewModel.kt` - *inferential paths*) likely initiates the workflow.
    -   **Data Collection**: Handled within `featureDiagnostico/`.
    -   **Transmission**: `linka-android-kotlin/coreNetwork/src/main/kotlin/com/linka/corenetwork/ai/AiDiagnosisService.kt` (*inferential path*) sends data to the worker's endpoint.
    -   **Response Handling**: `AiDiagnosisService.kt` receives results.
    -   **UI Presentation**: Results passed to `DiagnosticViewModel.kt` and displayed in `DiagnosticScreen.kt` composables.

## Data Flow for AI

1.  **User Initiates Diagnostic**: Triggered in `featureDiagnostico/ui/DiagnosticScreen.kt`.
2.  **Data Collection**: `featureDiagnostico/` gathers data.
3.  **Transmission**: `AiDiagnosisService.kt` in `coreNetwork/` sends data to `cloudflare/ai-diagnosis-worker`.
4.  **AI Processing**: Performed by the `cloudflare/ai-diagnosis-worker`.
5.  **AI Insight Reception**: Results returned to `AiDiagnosisService.kt`.
6.  **ViewModel Update**: Data passed to `featureDiagnostico/*ViewModel.kt`.
7.  **UI Display**: Composables in `featureDiagnostico/ui/` render AI insights.

## Key Files/Modules

-   **`linka-android-kotlin/featureDiagnostico/`**: Core module for AI diagnostics UI and ViewModel.
-   **`linka-android-kotlin/coreNetwork/`**: Contains `ApiService.kt` and inferred `AiDiagnosisService.kt` for AI worker communication.
-   **`cloudflare/ai-diagnosis-worker/wrangler.toml`**: Cloudflare worker configuration.
-   **`coreDatabase/`**: May store historical AI diagnostic results via inferred DAOs (e.g., `DiagnosticDao.kt`).

## Known Risks

-   Specific file paths for `AiDiagnosisService`, ViewModels, DAOs, and Repository implementations are inferential and require human validation.
-   Exact API endpoints, request/response formats, and authentication for the AI worker need human confirmation.
-   The internal workings and models of the `cloudflare/ai-diagnosis-worker` are managed externally and require human oversight.
