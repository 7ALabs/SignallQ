# Deployment

## Objective

This document outlines the deployment process for the Linka Android Kotlin application, detailing how new versions are released to end-users.

## Deployment Target

The primary deployment target for Android applications is typically the **Google Play Store**.

## Deployment Steps

The deployment process involves several key steps:

1.  **Prepare Release Build**:
    -   Ensure the application has gone through development, testing, and quality assurance.
    -   Generate a release-ready APK (or App Bundle) following the procedures in `operations/APK_BUILD.md`.
    -   Verify that versioning (`operations/VERSIONING.md`) is correctly applied for the release.

2.  **Sign the Application**:
    -   The release APK/App Bundle must be signed with a release keystore, as configured and managed according to `operations/APK_BUILD.md` and security best practices.

3.  **Upload to Google Play Console**:
    -   Log in to the Google Play Console.
    -   Navigate to the app's release management section.
    -   Upload the signed release APK or App Bundle.

4.  **Manage Release Tracks**:
    -   **Internal Testing**: For immediate distribution to a small internal team for basic validation.
    -   **Closed/Open Beta Testing**: For distributing to a larger group of testers before a full rollout.
    -   **Production**: The final release track for all users.
    -   The new build is assigned to one or more of these tracks.

5.  **Configure Release**:
    -   **Release Notes**: Draft and add release notes detailing new features, bug fixes, and improvements for the current version.
    -   **Staged Rollouts**: Configure a phased rollout (e.g., releasing to a percentage of users gradually) to monitor for issues before a full rollout.
    -   **Targeting**: Define target countries, devices, or user segments if applicable.

6.  **Rollout**:
    -   Initiate the release to the chosen track(s). The Google Play Store then manages the distribution to users based on the selected rollout strategy.

## Backend Service Deployment

-   Concurrent deployment of backend services is often necessary, especially for features relying on updated APIs or AI models.
-   The `cloudflare/ai-diagnosis-worker` would have its own deployment process, managed via Cloudflare tools (e.g., Wrangler CLI), coordinated with mobile app releases to ensure compatibility.

## Key Files/Configuration

-   `builds/apk/release/<versionName>/`: Contains final release APK files ready for upload.
-   `key.properties`: Holds sensitive signing credentials used during the build process.
-   Google Play Console: The primary platform for managing app releases.
-   Backend deployment configurations (e.g., `cloudflare/ai-diagnosis-worker/wrangler.toml` for worker deployment).

## Known Risks

-   The specific steps within the Google Play Console, including managing different release tracks, staged rollouts, and app store listing information, require human expertise.
-   Ensuring compatibility between the mobile app version and the deployed backend services (including the Cloudflare worker) is critical and needs human coordination.
-   Rollback procedures in case of critical issues post-deployment need to be clearly defined and validated by a human.
