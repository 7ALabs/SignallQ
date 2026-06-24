package io.veloo.app.feature.wifi

enum class TipoTopologia {
    ROTEADOR,           // roteador único/principal
    ROTEADOR_MESH,      // nó principal de sistema mesh
    NO_MESH,            // nó secundário mesh (mesmo SSID, mesmo OUI base)
    REPETIDOR,          // repetidor/extensor (mesmo SSID, OUI diferente)
    PONTO_DE_ACESSO,    // AP corporativo ou desconhecido
    DESCONHECIDO
}

enum class ConfiancaTopologia { ALTA, MEDIA, BAIXA }

data class RedeClassificada(
    val rede: RedeVizinha,
    val tipo: TipoTopologia,
    val confianca: ConfiancaTopologia,
    val motivo: String
)

data class GrupoRedeWifi(
    val ssid: String,
    val redes: List<RedeClassificada>
)
