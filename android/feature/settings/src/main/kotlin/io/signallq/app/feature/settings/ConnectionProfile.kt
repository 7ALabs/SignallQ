package io.signallq.app.feature.settings

/**
 * GH#1227 item 3/RF-A — antes o provedor fixo e o plano contratado eram salvos em chaves
 * DataStore globais (`velocidadeContratadaDownMbps`/`velocidadeContratadaUpMbps`, sem nenhuma
 * associação com a rede), então o app podia aplicar o plano residencial numa rede de trabalho,
 * hotel, hotspot ou casa de terceiros. [networkId] é o que evita isso — cada perfil é vinculado
 * a uma rede específica, nunca global.
 *
 * @param networkId identificador estável da rede a que este perfil pertence (ver
 * [ResolvedorNetworkId]). Nunca reaproveitado entre redes diferentes.
 * @param userConfirmed true quando o usuário confirmou explicitamente este provedor pra esta
 * rede (item 2 — divergência entre valor salvo e detectado só deve sobrescrever silenciosamente
 * quando isto for false).
 */
data class ConnectionProfile(
    val networkId: String,
    val providerFixed: String?,
    val contractedDownloadMbps: Int?,
    val contractedUploadMbps: Int?,
    val city: String?,
    val state: String?,
    val userConfirmed: Boolean,
)
