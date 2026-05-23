# Cloudflare Integration

## Objective

This document details the integration of Cloudflare services, mapping specific components and interaction points within the Linka Android Kotlin project.

## Cloudflare Components and Interaction Mappings

The project interacts with a Cloudflare worker for AI-driven diagnostics.

-   **`ai-diagnosis-worker`**:
    -   **Configuration File**: `cloudflare/ai-diagnosis-worker/wrangler.toml`.
    -   **Purpose**: Executes AI models for diagnostic data analysis on Cloudflare's infrastructure.

-   **Mobile App Client Interaction**:
    -   **Communication Service**: `linka-android-kotlin/coreNetwork/src/main/kotlin/com/linka/corenetwork/ai/AiDiagnosisService.kt` (*inferential path*) handles sending data to and receiving results from the worker.
    -   **Initiation**: The `featureDiagnostico/` module (e.g., `DiagnosticViewModel.kt` - *inferential path*) triggers the diagnostic process.
    -   **Data Flow**: Data collected in `featureDiagnostico/` is transmitted via `AiDiagnosisService.kt` to the worker. Results are returned and processed by the ViewModel for UI display in `featureDiagnostico/ui/DiagnosticScreen.kt` (*inferential paths*).

## Key Files/Configuration

-   **`cloudflare/ai-diagnosis-worker/wrangler.toml`**: Cloudflare worker configuration.
-   **`linka-android-kotlin/coreNetwork/src/main/kotlin/com/linka/corenetwork/ai/AiDiagnosisService.kt`** (*inferential path*): Client-side interface for worker communication.
-   **`linka-android-kotlin/featureDiagnostico/`**: Module initiating diagnostics and displaying results.

## Known Risks

-   Specific file paths for service classes and the exact API endpoints require human validation.
-   Details regarding AI models, worker logic, and deployment status are managed externally and need human oversight.
