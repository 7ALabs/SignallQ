# AI Assistant Functionality

## Objective

This document details the functionality and user experience of AI assistant features, mapping specific components and interactions within the Linka Android Kotlin application.

## AI Capabilities and Integration Points

AI is primarily integrated to enhance diagnostics and provide intelligent insights.

-   **AI-Driven Diagnostics (`featureDiagnostico`)**:
    -   **Functionality**: Interprets diagnostic results using AI analysis via `cloudflare/ai-diagnosis-worker`.
    -   **User Experience**: `featureDiagnostico/ui/DiagnosticScreen.kt` (*inferential path*) presents AI insights (summaries, causes, actions) derived from data sent via `coreNetwork/ai/AiDiagnosisService.kt` (*inferential path*).

-   **Contextual Insights (Potential)**:
    -   AI might provide insights in other modules like `featureHome/ui/HomeScreen.kt` (*inferential path*). Specific implementation requires human validation.

## User Interaction with AI

-   **Triggering**: Users initiate AI features by using primary functions (e.g., running diagnostics in `DiagnosticScreen.kt`). AI analysis occurs in the background.
-   **AI Output**: Insights are presented directly within the relevant feature's UI (e.g., `DiagnosticScreen.kt`), making them actionable.
-   **Interface Type**: The AI functions as an analytical engine integrated into workflows, not a conversational chatbot.

## Key Files/Modules

-   **`linka-android-kotlin/featureDiagnostico/`**: Primary module for AI-assisted diagnostics UI and ViewModel (`DiagnosticViewModel.kt` - *inferential path*).
-   **`linka-android-kotlin/coreNetwork/`**: Contains `AiDiagnosisService.kt` for backend AI communication.
-   **`cloudflare/ai-diagnosis-worker/wrangler.toml`**: External configuration for the AI backend.

## Known Risks

-   Specific AI models, training data, and algorithms are managed externally and require human validation.
-   Exact API contracts (`AiDiagnosisService.kt`) and AI response formats need confirmation.
-   The scope and accuracy of AI recommendations require human oversight.
