package io.signallq.app

import io.signallq.app.feature.speedtest.ModoSpeedtest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testa isoladamente o gate de confirmacao de rede movel usado por
 * MainViewModel.reiniciarSuite (#838).
 *
 * Antes da correcao, esse gate lia networkCapabilitiesProvider.isMeteredNetwork() —
 * uma consulta avulsa ao ConnectivityManager feita no instante do toque em "Iniciar",
 * sem nenhum teste cobrindo o comportamento esperado. Agora a decisao usa o campo
 * `metered` de SnapshotRede (mantido atualizado por MonitorRedeAndroid via callback
 * continuo) e a logica booleana foi extraida para ser testavel sem Robolectric/Hilt.
 */
class DeveSolicitarConfirmacaoRedeMovelTest {
    @Test
    fun `pede confirmacao em rede movel medida com modo completo e sem confirmacao previa`() {
        assertTrue(
            deveSolicitarConfirmacaoRedeMovel(
                metered = true,
                modo = ModoSpeedtest.complete,
                jaConfirmadoRedeMovel = false,
            ),
        )
    }

    @Test
    fun `pede confirmacao em rede movel medida com modo triplo`() {
        assertTrue(
            deveSolicitarConfirmacaoRedeMovel(
                metered = true,
                modo = ModoSpeedtest.triplo,
                jaConfirmadoRedeMovel = false,
            ),
        )
    }

    @Test
    fun `nao pede confirmacao quando rede nao e medida`() {
        assertFalse(
            deveSolicitarConfirmacaoRedeMovel(
                metered = false,
                modo = ModoSpeedtest.complete,
                jaConfirmadoRedeMovel = false,
            ),
        )
    }

    @Test
    fun `nao pede confirmacao no modo rapido mesmo em rede medida`() {
        assertFalse(
            deveSolicitarConfirmacaoRedeMovel(
                metered = true,
                modo = ModoSpeedtest.fast,
                jaConfirmadoRedeMovel = false,
            ),
        )
    }

    @Test
    fun `nao pede confirmacao quando usuario ja confirmou via ForaDoWifiDialog da Home`() {
        assertFalse(
            deveSolicitarConfirmacaoRedeMovel(
                metered = true,
                modo = ModoSpeedtest.triplo,
                jaConfirmadoRedeMovel = true,
            ),
        )
    }
}
