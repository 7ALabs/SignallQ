package io.signallq.pro.feature.medicaodiagnostico

private const val DELTA_CANDIDATO_MINIMO_DBM = 8
private const val RSSI_LIMIAR_EXCELENTE_DBM = -50
private const val RSSI_LIMIAR_BOA_DBM = -60
private const val RSSI_LIMIAR_REGULAR_DBM = -70
private const val SEGUNDOS_POR_MINUTO = 60
private const val MS_POR_SEGUNDO = 1000

/**
 * Regras puras da Walk Test (issue #1176) -- extraídas de [WalkTestViewModel] para permitir
 * teste JVM sem mockar `Context`/`WifiManager`. Thresholds de RSSI seguem a convenção comum
 * de telecom (dBm mais próximo de 0 = melhor sinal).
 */
internal fun classificarRssi(rssiDbm: Int): QualidadeRssi =
    when {
        rssiDbm >= RSSI_LIMIAR_EXCELENTE_DBM -> QualidadeRssi.EXCELENTE
        rssiDbm >= RSSI_LIMIAR_BOA_DBM -> QualidadeRssi.BOA
        rssiDbm >= RSSI_LIMIAR_REGULAR_DBM -> QualidadeRssi.REGULAR
        else -> QualidadeRssi.FRACA
    }

/** Delta entre a leitura atual e o pior ponto da sessão -- não-nulo só quando atinge o
 *  mínimo pra ser considerado "ponto candidato" a reposicionamento de repetidor/mesh. */
internal fun calcularDeltaPontoCandidato(
    rssiAtual: Int,
    rssiMinSessao: Int,
): Int? = (rssiAtual - rssiMinSessao).takeIf { it >= DELTA_CANDIDATO_MINIMO_DBM }

internal fun formatarDuracaoSessao(duracaoMs: Long): String {
    val totalSegundos = duracaoMs / MS_POR_SEGUNDO
    val minutos = totalSegundos / SEGUNDOS_POR_MINUTO
    val segundos = totalSegundos % SEGUNDOS_POR_MINUTO
    return "%02d:%02d".format(minutos, segundos)
}
