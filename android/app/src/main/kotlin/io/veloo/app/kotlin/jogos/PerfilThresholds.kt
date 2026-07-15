package io.signallq.app.jogos

/** Nivel de resultado por metrica — mesma escala usada no veredito final (worst-wins). */
enum class NivelMetrica { EXCELENTE, BOA, ATENCAO, RUIM }

/**
 * Faixa de uma metrica (limite superior inclusive de Excelente/Boa/Atencao — acima do
 * limite de Atencao e sempre Ruim). Valores exatos da tabela de thresholds do spec
 * (`JOGOS_TESTE_CONEXAO_SPEC.md`, secao "Perfis de sensibilidade").
 */
data class FaixaMetrica(
    val excelenteAte: Double,
    val boaAte: Double,
    val atencaoAte: Double,
) {
    fun classificar(valor: Double): NivelMetrica =
        when {
            valor <= excelenteAte -> NivelMetrica.EXCELENTE
            valor <= boaAte -> NivelMetrica.BOA
            valor <= atencaoAte -> NivelMetrica.ATENCAO
            else -> NivelMetrica.RUIM
        }
}

data class ThresholdsPerfil(
    val latenciaMs: FaixaMetrica,
    val jitterMs: FaixaMetrica,
    val perdaPercentual: FaixaMetrica,
)

fun PerfilSensibilidade.thresholds(): ThresholdsPerfil =
    when (this) {
        PerfilSensibilidade.COMPETITIVO_EXTREMO ->
            ThresholdsPerfil(
                latenciaMs = FaixaMetrica(excelenteAte = 30.0, boaAte = 50.0, atencaoAte = 80.0),
                jitterMs = FaixaMetrica(excelenteAte = 5.0, boaAte = 10.0, atencaoAte = 20.0),
                perdaPercentual = FaixaMetrica(excelenteAte = 0.0, boaAte = 0.5, atencaoAte = 1.0),
            )

        PerfilSensibilidade.COMPETITIVO ->
            ThresholdsPerfil(
                latenciaMs = FaixaMetrica(excelenteAte = 50.0, boaAte = 80.0, atencaoAte = 120.0),
                jitterMs = FaixaMetrica(excelenteAte = 10.0, boaAte = 20.0, atencaoAte = 30.0),
                perdaPercentual = FaixaMetrica(excelenteAte = 0.0, boaAte = 0.5, atencaoAte = 1.0),
            )

        PerfilSensibilidade.ESPORTE_COMPETITIVO ->
            ThresholdsPerfil(
                latenciaMs = FaixaMetrica(excelenteAte = 40.0, boaAte = 70.0, atencaoAte = 100.0),
                jitterMs = FaixaMetrica(excelenteAte = 5.0, boaAte = 10.0, atencaoAte = 20.0),
                perdaPercentual = FaixaMetrica(excelenteAte = 0.0, boaAte = 0.5, atencaoAte = 1.0),
            )

        PerfilSensibilidade.MULTIPLAYER_MODERADO ->
            ThresholdsPerfil(
                latenciaMs = FaixaMetrica(excelenteAte = 60.0, boaAte = 100.0, atencaoAte = 150.0),
                jitterMs = FaixaMetrica(excelenteAte = 10.0, boaAte = 20.0, atencaoAte = 30.0),
                perdaPercentual = FaixaMetrica(excelenteAte = 0.0, boaAte = 0.5, atencaoAte = 1.0),
            )
    }
