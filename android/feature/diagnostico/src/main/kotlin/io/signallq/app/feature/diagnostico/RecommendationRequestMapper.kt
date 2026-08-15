package io.signallq.app.feature.diagnostico

import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticReport
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
        tags = mapTags(report),
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
     * Mapeia tags para o motor de recomendacao comercial (`coreRecommendation`).
     *
     * Issue #1528 (P1-2 da auditoria #1228): tags que tem uma regra REC-01..14 equivalente
     * direta em [RecomendacaoPraticaEngine] passam a ler `report.recomendacoes` (saida real
     * do motor local) em vez de re-derivar por regex sobre IDs brutos de [DiagnosticResult].
     * Isso elimina a divergencia onde o motor legado suprime uma recomendacao por causa raiz
     * externa (ex.: REC-01 nao dispara quando ha causa DNS/operadora/fibra), mas o mapper
     * antigo gerava a tag de qualquer forma so por olhar o achado bruto WIFI-03/04.
     *
     * Tags SEM regra REC dedicada continuam via regex sobre IDs brutos (comportamento
     * preservado, fora do escopo da #1528): LATENCIA_ALTA (IN-NORMAL-05, sem REC equivalente)
     * e MUITOS_DISPOSITIVOS (WIFI-DEV-01/02 -- e so um dos 3 motivos possiveis dentro de
     * REC-04, nao tem regra dedicada so pra "muitos dispositivos").
     */
    private fun mapTags(report: DiagnosticReport): Set<DiagnosticTag> {
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

        val recIds = report.recomendacoes.map { it.id }.toSet()

        val tags = mutableSetOf<DiagnosticTag>()

        // REC-11 (perda de pacotes).
        if (recIds.contains("REC-11")) tags += DiagnosticTag.PERDA_PACOTES_ALTA

        // REC-01 (Troque para Wi-Fi 5GHz) / REC-02 (Aproxime-se do roteador) -- ambas ja
        // suprimem quando a causa raiz principal e externa (DNS/operadora/fibra).
        if (recIds.contains("REC-01") || recIds.contains("REC-02")) tags += DiagnosticTag.WIFI_FRACO

        // REC-06 (Troque o DNS) -- so dispara com alternativa de DNS com margem segura.
        if (recIds.contains("REC-06")) tags += DiagnosticTag.DNS_LENTO

        // WIFI-DEV-01 (varios) / WIFI-DEV-02 (muitos) dispositivos na rede -- sem REC dedicado.
        if (ids.any { it.startsWith("WIFI-DEV-01") || it.startsWith("WIFI-DEV-02") }) {
            tags += DiagnosticTag.MUITOS_DISPOSITIVOS
        }

        // REC-05 (bufferbloat).
        if (recIds.contains("REC-05")) tags += DiagnosticTag.BUFFERBLOAT_ALTO

        // IN-NORMAL-05 (latencia acima de 100ms -- limiar historico) -- sem REC dedicado.
        if (ids.any { it.startsWith("IN-NORMAL-05") }) tags += DiagnosticTag.LATENCIA_ALTA

        // REC-10 (sinal de rede movel fraco).
        if (recIds.contains("REC-10")) tags += DiagnosticTag.SINAL_BAIXO

        // REC-04 (o roteador pode estar limitando sua velocidade) -- compara contra linkSpeed,
        // nao contra download; substitui o calculo manual com tolerancia de 20% contra download
        // que divergia de REC-04 (P1-2).
        if (recIds.contains("REC-04")) tags += DiagnosticTag.VELOCIDADE_ABAIXO_DO_CONTRATADO

        return tags
    }

    private const val MHZ_PER_GHZ = 1000.0
}
