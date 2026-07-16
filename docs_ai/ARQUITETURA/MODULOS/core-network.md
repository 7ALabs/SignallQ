# Módulo :coreNetwork

- **Status:** ativo
- **Última validação:** 2026-07-16 (fonte: `android/core/network/build.gradle.kts`, código real)
- **Caminho físico:** `android/core/network/`
- **Namespace:** `io.signallq.app.core.network`

## Responsabilidade

Monitoramento de conectividade de rede em tempo real e medição de latência de gateway. Infra
compartilhada — sem dependência de nenhum outro módulo do monorepo.

## Principais packages/pastas

Base: `coreNetwork/src/main/kotlin/io/veloo/app/kotlin/core/network/` (caminho físico legado).

## Classes/contratos públicos relevantes

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `MonitorRede.kt` | Interface | Contrato de monitoramento de conectividade |
| `MonitorRedeAndroid.kt` | Implementação | `ConnectivityManager.NetworkCallback` |
| `SnapshotRede.kt` | Data class | Estado atual da rede |
| `WifiLinkSnapshot.kt` | Data class | Snapshot do link Wi-Fi (RSSI, canal, freq, link speed) |
| `EstadoConexao.kt` | Enum | WIFI, MOVEL, CABO, DESCONHECIDO |
| `GatewayLatencyMeasurer.kt` | Utilitário | RTT TCP do gateway local |
| `NetworkCapabilitiesProvider.kt` | Interface | Acesso a `NetworkCapabilities` |
| `DispatcherProvider.kt` / `DefaultDispatcherProvider.kt` | Interface/Impl | Abstração de Dispatchers para testabilidade |

## Entradas/saídas

- **Entradas:** callbacks do `ConnectivityManager` do SO.
- **Saídas:** `StateFlow<SnapshotRede>` consumido por `:app` e por features que precisam de contexto
  de rede.

## Dependências declaradas (build.gradle.kts real)

Nenhum módulo do monorepo. Libs: `androidx-core-ktx`, `lifecycle-runtime-ktx`,
`kotlinx-coroutines-android`.

## Consumidores

Via grep de `project(":coreNetwork")` nos `build.gradle.kts`: `:app`, `:featureWifi`,
`:featureFibra`, `:featureSpeedtest`, `:featureDiagnostico`.

## Testes existentes

`src/test`: **11 arquivos**. `src/androidTest`: 0.

## Riscos/dívidas conhecidas

Caminho físico `io/veloo/app/kotlin/` diverge do package declarado `io.signallq.app.core.network`
— dívida 4.1 da regra de higiene.
```

