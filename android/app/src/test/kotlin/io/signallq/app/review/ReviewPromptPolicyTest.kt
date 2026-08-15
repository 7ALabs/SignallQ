package io.signallq.app.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ReviewPromptPolicyTest {
    private val agora = 1_700_000_000_000L

    @Test
    fun `veredito positivo com diagnosticos suficientes e sem solicitacao anterior libera o prompt`() {
        val resultado =
            ReviewPromptPolicy.deveExibirPrompt(
                veredito = "Excelente",
                diagnosticosPositivosAcumulados = ReviewPromptPolicy.DIAGNOSTICOS_POSITIVOS_MINIMOS,
                ultimaSolicitacaoEpochMs = null,
                agoraEpochMs = agora,
            )
        assertTrue(resultado)
    }

    @Test
    fun `veredito Regular nunca libera o prompt`() {
        val resultado =
            ReviewPromptPolicy.deveExibirPrompt(
                veredito = "Regular",
                diagnosticosPositivosAcumulados = 100,
                ultimaSolicitacaoEpochMs = null,
                agoraEpochMs = agora,
            )
        assertFalse(resultado)
    }

    @Test
    fun `menos diagnosticos positivos que o minimo nao libera o prompt`() {
        val resultado =
            ReviewPromptPolicy.deveExibirPrompt(
                veredito = "Bom",
                diagnosticosPositivosAcumulados = ReviewPromptPolicy.DIAGNOSTICOS_POSITIVOS_MINIMOS - 1,
                ultimaSolicitacaoEpochMs = null,
                agoraEpochMs = agora,
            )
        assertFalse(resultado)
    }

    @Test
    fun `dentro do periodo de cooldown nao libera o prompt`() {
        val umDiaEmMs = TimeUnit.DAYS.toMillis(1)
        val resultado =
            ReviewPromptPolicy.deveExibirPrompt(
                veredito = "Bom",
                diagnosticosPositivosAcumulados = ReviewPromptPolicy.DIAGNOSTICOS_POSITIVOS_MINIMOS,
                ultimaSolicitacaoEpochMs = agora - umDiaEmMs,
                agoraEpochMs = agora,
            )
        assertFalse(resultado)
    }

    @Test
    fun `apos o cooldown expirar libera o prompt novamente`() {
        val cooldownMaisUmDiaEmMs =
            TimeUnit.DAYS.toMillis(ReviewPromptPolicy.COOLDOWN_DIAS) + TimeUnit.DAYS.toMillis(1)
        val resultado =
            ReviewPromptPolicy.deveExibirPrompt(
                veredito = "Excelente",
                diagnosticosPositivosAcumulados = ReviewPromptPolicy.DIAGNOSTICOS_POSITIVOS_MINIMOS,
                ultimaSolicitacaoEpochMs = agora - cooldownMaisUmDiaEmMs,
                agoraEpochMs = agora,
            )
        assertTrue(resultado)
    }
}
