package io.signallq.app.ui.screen

import io.signallq.app.core.datastore.ConnectionProfilePersistido
import io.signallq.app.feature.settings.DetectorDivergenciaPerfilConexao
import io.signallq.app.feature.settings.ResultadoDivergenciaPerfilConexao

/**
 * GH#1249 (recorte de #1227) — estado de UI da seção "Minha conexão" em Ajustes, derivado do
 * perfil persistido pra rede atual mais o provedor detectado nesta rede (ex.: ISP resolvido por
 * IP em Wi-Fi). Função pura, testável sem Robolectric/Compose — mesmo padrão de
 * `mapAcessoEquipamento`/`resolverOperadoraUiState`.
 */
data class MinhaConexaoUiState(
    val providerFixed: String?,
    val contractedDownloadMbps: Int?,
    val contractedUploadMbps: Int?,
    val city: String?,
    val state: String?,
    val userConfirmed: Boolean,
    val divergencia: ResultadoDivergenciaPerfilConexao,
)

fun mapMinhaConexaoUiState(
    perfil: ConnectionProfilePersistido?,
    providerDetectado: String?,
): MinhaConexaoUiState =
    MinhaConexaoUiState(
        providerFixed = perfil?.providerFixed,
        contractedDownloadMbps = perfil?.contractedDownloadMbps,
        contractedUploadMbps = perfil?.contractedUploadMbps,
        city = perfil?.city,
        state = perfil?.state,
        userConfirmed = perfil?.userConfirmed ?: false,
        divergencia = DetectorDivergenciaPerfilConexao.avaliar(perfil?.paraConnectionProfile(), providerDetectado),
    )
