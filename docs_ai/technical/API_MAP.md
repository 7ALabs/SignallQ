# APIs

## Internal Module APIs

- **`coreDatabase`**: DAOs for database access
  - `MedicaoDao.kt` - measurements CRUD
  - `ApelidoDispositivoDao.kt` - device nicknames CRUD
  - `LinkaDatabase.kt` - DB schema

- **`coreNetwork`**: Network monitoring
  - `MonitorRede.kt` - interface for monitoring
  - `MonitorRedeAndroid.kt` - Android implementation
  - Models: `EstadoConexao.kt`, `SnapshotRede.kt`

- **`coreDatastore`**: Key-value storage (needs validation)

## External Services

- **Cloudflare AI Worker**: `cloudflare/ai-diagnosis-worker/`
  - Diagnostic data processing and AI analysis
  
- **Speed Test Services**: Via `featureSpeedtest` (needs endpoint validation)

- **Backend APIs**: Needs full endpoint documentation (missing)
    -   **Validation**: Need to check `build.gradle.kts` and `AndroidManifest.xml` for any such integrations.

## Key Files/Classes

-   `coreNetwork/src/main/kotlin/.../ApiService.kt` (and other API interface definitions)
-   `coreDatabase/src/main/kotlin/.../Dao.kt` (and other DAO definitions)
-   Repository implementations that consume these APIs.
-   `AndroidManifest.xml`: May list permissions required for network access.

## Documentation Standards

-   This map provides a high-level overview. Specific endpoint URLs, request/response structures, and authentication mechanisms are not detailed here.
-   All details regarding external API integrations require human validation.

## Known Risks

-   The exact list of external services, their endpoints, authentication methods, and data formats are not definitively known from the directory structure alone and require human validation.
-   The specific technologies used for backend services (e.g., REST, GraphQL) are inferred.
