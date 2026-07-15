package io.signallq.app.jogos

import org.junit.Assert.assertEquals
import org.junit.Test

class PerfilThresholdsTest {
    @Test
    fun `competitivo extremo classifica faixas exatas da tabela`() {
        val t = PerfilSensibilidade.COMPETITIVO_EXTREMO.thresholds()
        assertEquals(NivelMetrica.EXCELENTE, t.latenciaMs.classificar(30.0))
        assertEquals(NivelMetrica.BOA, t.latenciaMs.classificar(50.0))
        assertEquals(NivelMetrica.ATENCAO, t.latenciaMs.classificar(80.0))
        assertEquals(NivelMetrica.RUIM, t.latenciaMs.classificar(81.0))

        assertEquals(NivelMetrica.EXCELENTE, t.jitterMs.classificar(5.0))
        assertEquals(NivelMetrica.RUIM, t.jitterMs.classificar(20.1))

        assertEquals(NivelMetrica.EXCELENTE, t.perdaPercentual.classificar(0.0))
        assertEquals(NivelMetrica.BOA, t.perdaPercentual.classificar(0.5))
        assertEquals(NivelMetrica.ATENCAO, t.perdaPercentual.classificar(1.0))
        assertEquals(NivelMetrica.RUIM, t.perdaPercentual.classificar(1.5))
    }

    @Test
    fun `competitivo tem faixas mais tolerantes que competitivo extremo`() {
        val extremo = PerfilSensibilidade.COMPETITIVO_EXTREMO.thresholds()
        val competitivo = PerfilSensibilidade.COMPETITIVO.thresholds()
        assertEquals(NivelMetrica.ATENCAO, extremo.latenciaMs.classificar(55.0))
        assertEquals(NivelMetrica.BOA, competitivo.latenciaMs.classificar(55.0))
    }

    @Test
    fun `esporte competitivo penaliza jitter igual ao extremo mesmo com latencia mais tolerante`() {
        val t = PerfilSensibilidade.ESPORTE_COMPETITIVO.thresholds()
        assertEquals(NivelMetrica.EXCELENTE, t.latenciaMs.classificar(40.0))
        assertEquals(NivelMetrica.RUIM, t.jitterMs.classificar(21.0))
    }

    @Test
    fun `multiplayer moderado e o perfil mais tolerante`() {
        val t = PerfilSensibilidade.MULTIPLAYER_MODERADO.thresholds()
        assertEquals(NivelMetrica.EXCELENTE, t.latenciaMs.classificar(60.0))
        assertEquals(NivelMetrica.BOA, t.latenciaMs.classificar(100.0))
        assertEquals(NivelMetrica.ATENCAO, t.latenciaMs.classificar(150.0))
        assertEquals(NivelMetrica.RUIM, t.latenciaMs.classificar(151.0))
    }
}
