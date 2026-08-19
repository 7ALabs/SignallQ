package io.signallq.app.feature.speedtest

import io.signallq.app.core.network.EstadoConexao
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * GH#1737 (Task 2.0.10a, épico #1647) — decisão de produto: SignallQ 2.0 não oferece mais
 * escolha manual de modo (a antiga MedicaoTipoSheet e o ModeSelector, com "Rápido"/"Completo"/
 * "Triplo", foram removidos). [modoAutomaticoPara] é a única fonte de decisão de modo agora,
 * usada no disparo em AppShell.kt tanto pela Home quanto pela tab Velocidade.
 */
class ModoSpeedtestTest {
    @Test
    fun `rede movel usa modo fast`() {
        assertEquals(ModoSpeedtest.fast, modoAutomaticoPara(EstadoConexao.movel))
    }

    @Test
    fun `wifi usa modo complete`() {
        assertEquals(ModoSpeedtest.complete, modoAutomaticoPara(EstadoConexao.wifi))
    }

    @Test
    fun `ethernet usa modo complete`() {
        assertEquals(ModoSpeedtest.complete, modoAutomaticoPara(EstadoConexao.ethernet))
    }

    @Test
    fun `desconhecido cai no padrao complete`() {
        assertEquals(ModoSpeedtest.complete, modoAutomaticoPara(EstadoConexao.desconhecido))
    }

    @Test
    fun `desconectado cai no padrao complete (botao de iniciar fica desabilitado antes disso)`() {
        assertEquals(ModoSpeedtest.complete, modoAutomaticoPara(EstadoConexao.desconectado))
    }

    @Test
    fun `GH1737 modo triplo nao existe mais no enum`() {
        assertEquals(setOf(ModoSpeedtest.fast, ModoSpeedtest.complete), ModoSpeedtest.entries.toSet())
    }
}
