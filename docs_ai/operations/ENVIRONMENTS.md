# Environments

## Objective

This document outlines the different environments the Linka Android Kotlin application interacts with or targets, detailing their purpose and key configuration aspects.

## Environment Types

The application operates across several distinct environments:

### 1. Local Development Environment

-   **Description**: The environment set up on a developer's machine for coding, building, and testing.
-   **Components**:
    -   **IDE**: Android Studio.
    -   **Build Tools**: Gradle Wrapper (`gradlew`), Kotlin compiler.
    -   **SDK**: Android SDK (version specified in `local.properties`).
    -   **Emulators/Devices**: Android emulators or physical devices for running and debugging the app.
    -   **Dependencies**: Local caching of dependencies managed by Gradle.
-   **Configuration**: Managed via `local.properties`, `gradle.properties`, module `build.gradle.kts` files, and `key.properties` (for local debug signing).

### 2. Testing/Staging Environment

-   **Description**: An environment used for pre-release testing, mimicking the production environment as closely as possible but using separate backend services or configurations.
-   **Purpose**: To validate new features, bug fixes, and the overall release candidate before it goes live.
-   **Backend Interaction**: The app might be configured to point to separate staging backend APIs or services.
-   **Configuration**: Often managed via Gradle build types or product flavors, and potentially separate configuration files or environment variables for backend services.

### 3. Production Environment

-   **Description**: The live environment where the final, released version of the application is available to end-users.
-   **Backend Interaction**: Connects to the live backend services.
-   **Configuration**: Uses production-ready configurations, live API endpoints, and production signing credentials.

### 4. Cloudflare Worker Environment (`cloudflare/ai-diagnosis-worker`)

-   **Description**: The environment where the AI diagnostic worker runs, hosted on Cloudflare's infrastructure.
-   **Purpose**: To execute AI models for data analysis.
-   **Configuration**: Managed via `cloudflare/ai-diagnosis-worker/wrangler.toml`, which specifies deployment settings, environment variables, and access to necessary AI models or services. This environment is distinct from the mobile app's local environments.

## Configuration Management

-   **`local.properties`**: Specifies local paths like the Android SDK, crucial for the development environment build.
-   **`build.gradle.kts`**: Build types (debug, release) and product flavors can be used to configure environment-specific settings, such as API endpoints or feature flags.
-   **`gradle.properties`**: Can hold global properties applicable across environments.
-   **`key.properties`**: Stores sensitive credentials, which will differ between local development (debug keys) and production (release keystores).
-   **`cloudflare/ai-diagnosis-worker/wrangler.toml`**: Manages the configuration for the Cloudflare worker environment.

## Known Risks

-   The precise configuration details for each environment, including specific API endpoints, backend service URLs, and feature flags, require human validation.
-   Ensuring secure management of secrets (`key.properties`) across different environments is critical.
-   The setup and maintenance of the Cloudflare worker environment need to be understood by a human expert.
