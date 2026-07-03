package io.signallq.app.feature.diagnostico

/**
 * Proveniencia de uma metrica usada pelo motor de diagnostico — generaliza para
 * TODAS as dimensoes do [ScoreEngine] o modelo que a Fase 1 introduziu apenas para
 * perda de pacotes ([InternetDiagnosticInput.packetLossSource]: "medida"/"estimated"/
 * "naoMedido"/"unknown", ja consumido pelo [RecommendationEngine.recomendarPerdaDePacotes]).
 *
 * Cada metrica bruta que entra no calculo de score carrega uma proveniencia junto do
 * valor:
 * - [medida] — leitura direta de hardware/API (RSSI, RSRP, potencia optica GPON, etc.)
 *   ou calculo estatistico direto sobre uma medicao real (latencia via ping, jitter).
 * - [estimada] — inferida indiretamente, sem medicao direta do fenomeno (ex.: perda de
 *   pacotes por timeout HTTP em vez de sequência real de pacotes — unico caso hoje).
 * - [indisponivel] — dado nao coletado nesta rodada (sem hardware, sem permissao, sem
 *   teste executado). NUNCA vira nota artificial — ver [ScoreEngine.Peso.reponderar].
 */
enum class Provenance {
    medida,
    estimada,
    indisponivel,
}

/**
 * Um valor de metrica (ja convertido para nota 0–100 pelo [ScoreEngine]) junto da sua
 * [Provenance]. [nota] so e significativa quando [provenance] != [Provenance.indisponivel]
 * — dimensoes indisponiveis nunca entram no denominador da media ponderada
 * (ver [ScoreEngine.Peso.reponderar]).
 */
data class EvidenceScore(
    val dimensao: String,
    val nota: Int?,
    val provenance: Provenance,
)
