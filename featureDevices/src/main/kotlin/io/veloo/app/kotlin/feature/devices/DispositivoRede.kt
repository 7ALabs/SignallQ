package io.veloo.app.feature.devices

data class DispositivoRede(
    val id: String,
    val ip: String?,
    val mac: String?,
    val nomeExibicao: String,
    val fonteNome: String,
    val fabricante: String? = null,
    val tipoDispositivo: TipoDispositivo = TipoDispositivo.desconhecido,
    val esteDispositivo: Boolean = false,
    val tiposServicoMdns: Set<String> = emptySet(),
    val portasAbertas: Set<Int> = emptySet(),
)

