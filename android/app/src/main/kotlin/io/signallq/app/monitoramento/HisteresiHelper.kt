package io.signallq.app.monitoramento

/**
 * Lógica pura de histerese para os alertas de monitoramento.
 *
 * Extraída do MonitoramentoWorker para ser testável diretamente, sem depender
 * de Context, DataStore ou WorkManager.
 *
 * Thresholds:
 *  - Latência:    entra em alerta > 400ms,  sai < 300ms
 *  - DNS:         entra em alerta > 2500ms, sai < 1800ms
 *  - RSSI:        entra em alerta < -75dBm, sai > -68dBm
 *  - Sem internet: sem latência E sem DNS E motivo RSSI confirma ausência de Wi-Fi
 *
 * Regra geral: se a métrica for null (ex: Doze Mode, permissão negada),
 * mantém o estado anterior — sem transição.
 */
internal object HisteresiHelper {
    /**
     * Calcula o novo estado de alerta de latência HTTP.
     *
     * @param latencia Latência medida em ms, ou null se indisponível (Doze, falha).
     * @param estadoAnterior Estado de alerta atual.
     * @return Novo estado de alerta.
     */
    fun calcularAlertaLatencia(
        latencia: Long?,
        estadoAnterior: Boolean,
    ): Boolean =
        when {
            latencia == null -> estadoAnterior // Doze ou falha de rede: mantém
            latencia > 400L -> true
            latencia < 300L -> false
            else -> estadoAnterior // zona de histerese [300, 400]: mantém
        }

    /**
     * Calcula o novo estado de alerta de resolução DNS.
     *
     * @param dns Tempo de resolução DNS em ms, ou null se indisponível.
     * @param estadoAnterior Estado de alerta atual.
     * @return Novo estado de alerta.
     */
    fun calcularAlertaDns(
        dns: Long?,
        estadoAnterior: Boolean,
    ): Boolean =
        when {
            dns == null -> estadoAnterior
            dns > 2500L -> true
            dns < 1800L -> false
            else -> estadoAnterior // zona de histerese [1800, 2500]: mantém
        }

    /**
     * Calcula o novo estado de alerta de sinal Wi-Fi (RSSI).
     *
     * @param rssi Valor RSSI em dBm, ou null se sem Wi-Fi / Doze / sem permissão.
     * @param estadoAnterior Estado de alerta atual.
     * @return Novo estado de alerta.
     */
    fun calcularAlertaRssi(
        rssi: Int?,
        estadoAnterior: Boolean,
    ): Boolean =
        when {
            rssi == null -> estadoAnterior
            rssi < -75 -> true
            rssi > -68 -> false
            else -> estadoAnterior // zona de histerese [-75, -68]: mantém
        }

    /**
     * Calcula o novo estado de alerta "sem internet".
     *
     * Ativa quando: latência E DNS ambos null E [semWifi] confirma ausência de Wi-Fi.
     * Resolve quando: qualquer um de latência ou DNS retorna (conexão parcial é sinal de vida).
     * Ambíguo (ambos null, semWifi false): mantém estado anterior.
     *
     * @param latencia Latência HTTP em ms, ou null.
     * @param dns Resolução DNS em ms, ou null.
     * @param semWifi true quando o motivo do RSSI é explicitamente [RssiMotivo.SemWifi].
     * @param estadoAnterior Estado de alerta atual.
     * @return Novo estado de alerta.
     */
    fun calcularAlertaSemInternet(
        latencia: Long?,
        dns: Long?,
        semWifi: Boolean,
        estadoAnterior: Boolean,
    ): Boolean {
        val semConectividade = latencia == null && dns == null
        return when {
            semConectividade && semWifi -> true
            latencia != null || dns != null -> false
            else -> estadoAnterior // estado ambíguo: mantém
        }
    }
}
