package io.signallq.app.feature.devices

data class DispositivoRede(
    val id: String,
    val ip: String?,
    val mac: String?,
    val nomeExibicao: String,
    val fonteNome: String,
    val fabricante: String? = null,
    /** Modelo do dispositivo (ex: "UE55NU7100", "LaserJet Pro M404"). Vem do XML UPnP ou TXT mDNS. */
    val modeloDispositivo: String? = null,
    val tipoDispositivo: TipoDispositivo = TipoDispositivo.desconhecido,
    val esteDispositivo: Boolean = false,
    val tiposServicoMdns: Set<String> = emptySet(),
    val portasAbertas: Set<Int> = emptySet(),
    /** GH#1217 item 1 — nível de confiança da identidade deste registro entre scans (ver
     *  [NivelConfiancaIdentidade]). Computado no enriquecimento final do scan. */
    val confiancaIdentidade: NivelConfiancaIdentidade = NivelConfiancaIdentidade.DESCONHECIDA,
)

