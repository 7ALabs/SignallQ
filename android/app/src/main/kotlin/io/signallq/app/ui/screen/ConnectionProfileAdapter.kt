package io.signallq.app.ui.screen

import io.signallq.app.core.datastore.ConnectionProfilePersistido
import io.signallq.app.feature.settings.ConnectionProfile
import io.signallq.app.feature.settings.ResolvedorNetworkId
import io.signallq.app.ui.ConnectionType

/**
 * GH#1249 (recorte de #1227) — ponte entre o contrato de persistência ([ConnectionProfilePersistido],
 * `core/datastore`) e o contrato de domínio ([ConnectionProfile], `feature/settings`). Vive em
 * `:app` porque `core/datastore` não pode depender de `feature/settings` (lei de dependências:
 * `:feature*` → `:core*` apenas) — mesmo padrão já usado por `HomeMedicaoAdapter.kt`.
 */
fun ConnectionProfilePersistido.paraConnectionProfile(): ConnectionProfile =
    ConnectionProfile(
        networkId = networkId,
        providerFixed = providerFixed,
        contractedDownloadMbps = contractedDownloadMbps,
        contractedUploadMbps = contractedUploadMbps,
        city = city,
        state = state,
        userConfirmed = userConfirmed,
    )

fun ConnectionProfile.paraPersistido(): ConnectionProfilePersistido =
    ConnectionProfilePersistido(
        networkId = networkId,
        providerFixed = providerFixed,
        contractedDownloadMbps = contractedDownloadMbps,
        contractedUploadMbps = contractedUploadMbps,
        city = city,
        state = state,
        userConfirmed = userConfirmed,
    )

/**
 * Resolve o identificador estável da rede atual — GH#1249 requisito B: Wi-Fi/Ethernet usa
 * BSSID/SSID ([ResolvedorNetworkId.paraWifi]); rede móvel usa a operadora do SIM ativo
 * ([ResolvedorNetworkId.paraRedeMovel]). Ethernet e conexão desconhecida não têm sinal estável
 * disponível hoje (sem BSSID/SSID) — retornam `null`, mesmo comportamento já documentado em
 * [ResolvedorNetworkId] (decisão de produto anterior: Ethernet não é tratada como jornada real
 * no app consumidor).
 */
fun resolverNetworkIdAtual(
    connectionType: ConnectionType,
    ssid: String?,
    bssid: String?,
    operadoraMovelAtiva: String?,
): String? =
    when (connectionType) {
        ConnectionType.WIFI -> ResolvedorNetworkId.paraWifi(ssid, bssid)
        ConnectionType.MOBILE -> ResolvedorNetworkId.paraRedeMovel(operadoraMovelAtiva)
        ConnectionType.ETHERNET, ConnectionType.UNKNOWN -> null
    }
