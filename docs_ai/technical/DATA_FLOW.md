# Data Flow

User Action → Composable (UI) → ViewModel → Data Layer → State Update → UI Recomposition

## Layer Details

**Presentation**:
- Screens: `app/src/main/kotlin/io/linka/app/kotlin/ui/screen/`
- Components: `app/src/main/kotlin/io/linka/app/kotlin/ui/component/`
- ViewModel: `MainViewModel.kt`

**Data**:
- Database: `coreDatabase/` (DAOs, Entities)
  - `MedicaoDao.kt`, `ApelidoDispositivoDao.kt`
  - `LinkaDatabase.kt`
- Network: `coreNetwork/` (Monitoring)
  - `MonitorRede.kt`, `MonitorRedeAndroid.kt`
  - Models: `EstadoConexao.kt`, `SnapshotRede.kt`
- DataStore: `coreDatastore/` (needs validation)

## Key Points

- Kotlin Coroutines for async operations
- Unidirectional flow: UI → ViewModel → Data
- Database queries via DAOs
- Network monitoring via MonitorRede

**Needs validation**: Repository pattern, exact state management (StateFlow/LiveData)
