package io.signallq.app.review

import java.util.concurrent.TimeUnit

/**
 * Regras de elegibilidade do prompt nativo de avaliacao do Google Play (SIG-173/#664).
 *
 * Fluxo "sem atrito": nunca interrompe o diagnostico em andamento — a elegibilidade so
 * e avaliada no momento em que o usuario fecha um Laudo com veredito positivo
 * (ver [io.signallq.app.MainViewModel.onLaudoFechado]). Exige um numero minimo de
 * diagnosticos positivos acumulados desde a ultima solicitacao, alem de um cooldown
 * minimo em dias — evita pedir avaliacao cedo demais ou repetidamente.
 *
 * A API do Play (ReviewManager) ja aplica sua propria cota interna e nao garante que o
 * dialogo sera exibido a cada solicitacao — esta politica so controla quando o app
 * *pede* o fluxo, nunca se ele aparece.
 */
object ReviewPromptPolicy {
    val VEREDITOS_POSITIVOS = setOf("Excelente", "Bom")
    const val DIAGNOSTICOS_POSITIVOS_MINIMOS = 3
    const val COOLDOWN_DIAS = 30L

    fun deveExibirPrompt(
        veredito: String,
        diagnosticosPositivosAcumulados: Int,
        ultimaSolicitacaoEpochMs: Long?,
        agoraEpochMs: Long,
    ): Boolean {
        if (veredito !in VEREDITOS_POSITIVOS) return false
        if (diagnosticosPositivosAcumulados < DIAGNOSTICOS_POSITIVOS_MINIMOS) return false
        if (ultimaSolicitacaoEpochMs == null) return true
        val cooldownMs = TimeUnit.DAYS.toMillis(COOLDOWN_DIAS)
        return agoraEpochMs - ultimaSolicitacaoEpochMs >= cooldownMs
    }
}
