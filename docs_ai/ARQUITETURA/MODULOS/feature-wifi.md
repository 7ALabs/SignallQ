# Módulo :featureWifi

- **Status:** ativo
- **Última validação:** 2026-07-16 (fonte: `android/feature/wifi/build.gradle.kts`, código real)
- **Caminho físico:** `android/feature/wifi/`
- **Namespace:** `io.signallq.app.feature.wifi`

## Responsabilidade

Scan de redes Wi-Fi vizinhas e classificação de topologia (mesh, repetidor, roteador).

## Principais packages/pastas

Base: `feature/wifi/src/main/kotlin/io/veloo/app/kotlin/feature/wifi/` (caminho físico legado).

## Classes/contratos públicos relevantes

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `ScannerRedesWifi.kt` | Interface | Contrato de scan Wi-Fi |
| `SnapshotScanWifi.kt` | Data class | Estado do scan |
| `RedeVizinha.kt` | Data class | Dados de uma rede vizinha |
| `GrupoRedeWifi.kt` | Data class | Grupo de BSSIDs do mesmo SSID |
| `TopologiaWifiEngine.kt` | Engine | Classifica topologia (ROTEADOR_MESH, NO_MESH, ROTEADOR, REPETIDOR) |
| `MontarResumoWifiUseCase.kt` | Use case | Síntese da análise Wi-Fi |
| `MeshOuiDatabase.kt` | Object | Base de OUIs de fabricantes mesh |

## Entradas/saídas

- **Entradas:** `WifiManager.scanResults` (via `ScannerRedesWifi`).
- **Saídas:** `SnapshotScanWifi` consumido por `:app` (tela Sinal).

## Dependências declaradas (build.gradle.kts real)

`:coreNetwork`. Libs: `androidx-core-ktx`, `kotlinx-coroutines-android`, `timber`.

## Consumidores

Via grep de `project(":featureWifi")`: apenas `:app`.

## Testes existentes

`src/test`: **5 arquivos**. `src/androidTest`: 0.

## Riscos/dívidas conhecidas

Caminho físico `io/veloo` diverge do package — dívida 4.1.
```

