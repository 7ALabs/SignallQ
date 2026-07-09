package io.signallq.app.feature.diagnostico

import io.signallq.app.core.recommendation.DeviceContext
import io.signallq.app.core.recommendation.DiagnosticMetrics
import io.signallq.app.core.recommendation.DiagnosticTag
import io.signallq.app.core.recommendation.NetworkContextType
import io.signallq.app.core.recommendation.RecommendationFlags
import io.signallq.app.core.recommendation.RecommendationHistoryEntry
import io.signallq.app.core.recommendation.RecommendationRequest

/**
 * Converte o resultado real do diagnostico ([DiagnosticReport] produzido pelo
 * [DiagnosticRunner], junto do [DiagnosticInput] que o originou) em um
 * [RecommendationRequest] valido para o `RecommendationEngine` (modulo
 * `coreRecommendation`, issue #790) -- issue #811.
 *
 * O motor de recomendacao e stateless e desacoplado do motor de diagnostico
 * (ver doc de [io.signallq.app.core.recommendation.RecommendationEngine]); esta
 * classe e a unica ponte entre os dois. Historico ([RecommendationRequest.history])
 * fica vazio por padrao aqui -- carregar e persistir o historico e responsabilidade
 * da camada Room introduzida na issue #812, que passa a lista real para [map].
 */
object RecommendationRequestMapper {

    fun map(
        report: DiagnosticReport,
        input: DiagnosticInput,
        isp: String? = null,
        history: List<RecommendationHistoryEntry> = emptyList(),
        flags: RecommendationFlags = RecommendationFlags(),
        diagnosticId: String? = null,
    ): RecommendationRequest = RecommendationRequest(
        tags = mapTags(report, input),
        network = mapNetworkContext(input.connectionType),
        metrics = mapMetrics(input),
        isp = isp,
        device = mapDeviceContext(input),
        history = history,
        flags = flags,
        diagnosticId = diagnosticId,
    )

    /** Reusa o mesmo [ConnectionType] ja detectado por [InternetDiagnosticEngine]/[DiagnosticRunner]
     *  -- nao duplica deteccao de tipo de conexao (criterio de aceite da #811). */
    fun mapNetworkContext(connectionType: ConnectionType): NetworkContextType = when (connectionType) {
        ConnectionType.wifi -> NetworkContextType.WIFI
        ConnectionType.mobile -> NetworkContextType.MOVEL
        ConnectionType.ethernet -> NetworkContextType.ETHERNET
        // Sem tipo de conexao definido (desconectado/desconhecido) nao ha um NetworkContextType
        // dedicado no engine; WIFI e o contexto mais comum e nao filtra recomendacoes universais
        // (candidate.applicableNetworkTypes vazio passa em qualquer contexto).
        ConnectionType.desconectado, ConnectionType.desconhecido -> NetworkContextType.WIFI
    }

    private fun mapMetrics(input: DiagnosticInput): DiagnosticMetrics {
        val internet = input.internet
        return DiagnosticMetrics(
            downloadMbps = internet?.downloadMbps,
            uploadMbps = internet?.uploadMbps,
            latencyMs = internet?.latencyMs,
            jitterMs = internet?.jitterMs,
            packetLossPercent = internet?.perdaPercentual,
            bufferbloatMs = internet?.bufferbloatMs,
        )
    }

    private fun mapDeviceContext(input: DiagnosticInput): DeviceContext? {
        val wifi = input.wifi ?: return null
        if (wifi.frequenciaMhz == null && wifi.rssiDbm == null) return null
        return DeviceContext(
            wifiFrequencyGhz = wifi.frequenciaMhz?.let { it / MHZ_PER_GHZ },
            signalStrengthDbm = wifi.rssiDbm,
        )
    }

    /**
     * Mapeia tags a partir dos IDs de [DiagnosticResult] ja emitidos pelos engines de
     * diagnostico existentes (nao reimplementa os thresholds -- so traduz o resultado).
     * Cobre os cenarios exigidos pela #811: perda de pacotes, Wi-Fi fraco, DNS lento,
     * muitos dispositivos e bufferbloat -- mais latencia alta e sinal movel baixo, ja
     * que os engines correspondentes ja existem.
     */
    private fun mapTags(report: DiagnosticReport, input: DiagnosticInput): Set<DiagnosticTag> {
        val ids = (
            report.wifiResultados +
                report.internetResultados +
                report.mobileResultados +
                report.dnsResultados +
                report.historicoResultados +
                report.wifiCanalResultados +
                report.redeResultados +
                listOf(report.decisao)
            ).map { it.id }.toSet()

        val tags = mutableSetOf<DiagnosticTag>()

        // IN-NORMAL-07/07b (perda de pacotes critica/moderada); sufixo "-inc" quando
        // o wifi nao confiavel torna o achado inconclusivo -- ainda assim a tag se aplica.
        if (ids.any { it.startsWith("IN-NORMAL-07") }) tags += DiagnosticTag.PERDA_PACOTES_ALTA

        // WIFI-03 (fraco) / WIFI-04 (muito fraco).
        if (ids.any { it.startsWith("WIFI-03") || it.startsWith("WIFI-04") }) tags += DiagnosticTag.WIFI_FRACO

        // DNS-01 (muito lento) / DNS-02 (lento). DNS-03 ("acima do ideal") nao entra --
        // e apenas informativo, navegacao normal nao e afetada.
        if (ids.any { it.startsWith("DNS-01") || it.startsWith("DNS-02") }) tags += DiagnosticTag.DNS_LENTO

        // WIFI-DEV-01 (varios) / WIFI-DEV-02 (muitos) dispositivos na rede.
        if (ids.any { it.startsWith("WIFI-DEV-01") || it.startsWith("WIFI-DEV-02") }) {
            tags += DiagnosticTag.MUITOS_DISPOSITIVOS
        }

        // IN-NORMAL-09/09b (bufferbloat critico/elevado).
        if (ids.any { it.startsWith("IN-NORMAL-09") }) tags += DiagnosticTag.BUFFERBLOAT_ALTO

        // IN-NORMAL-05 (latencia acima de 100ms, referencia Anatel RQUAL).
        if (ids.any { it.startsWith("IN-NORMAL-05") }) tags += DiagnosticTag.LATENCIA_ALTA

        // MOB-01/MOB-01b (sinal movel muito ruim/ruim).
        if (ids.any { it.startsWith("MOB-01") }) tags += DiagnosticTag.SINAL_BAIXO

        // Sem DiagnosticResult dedicado para "abaixo do contratado" -- calculado direto
        // das metricas brutas (mesma comparacao ja usada pelo RecommendationEngine legado
        // em recomendarRoteadorLimitado, so que aqui vira tag estruturada para o engine novo).
        val download = input.internet?.downloadMbps
        val contratado = input.velocidadeContratadaMbps
        if (download != null && contratado != null && contratado > 0 && download < contratado * CONTRATADO_TOLERANCIA) {
            tags += DiagnosticTag.VELOCIDADE_ABAIXO_DO_CONTRATADO
        }

        return tags
    }

    private const val MHZ_PER_GHZ = 1000.0

    // Tolerancia de 20% abaixo do contratado antes de marcar a tag -- evita marcar
    // por variacao normal de speedtest.
    private const val CONTRATADO_TOLERANCIA = 0.8
}
