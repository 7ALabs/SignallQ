package io.signallq.app.jogos

import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.WifiLinkSnapshot
import io.signallq.app.feature.speedtest.PingResultado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre os dois exemplos explicitos do spec (`JOGOS_TESTE_CONEXAO_SPEC.md`, secao
 * "Avaliacao final") e as regras de recomendacao/aviso condicionais.
 */
class JogoConexaoEngineTest {
    private val fortnite = CatalogoJogos.porId("fortnite")!! // perfil COMPETITIVO
    private val valorant = CatalogoJogos.porId("valorant")!! // perfil COMPETITIVO_EXTREMO, PROVIDER_NETWORK declarado

    @Test
    fun `25ms com 3 por cento de perda e classificado como ruim mesmo com latencia boa`() {
        val medicao = PingResultado(latenciaMs = 25.0, jitterMs = 3.0, perdaPercentual = 3.0, amostras = 24)
        val resultado =
            JogoConexaoEngine.avaliar(
                jogo = fortnite,
                plataforma = Plataforma.PC,
                medicao = medicao,
                tipoConexaoAtual = EstadoConexao.wifi,
                wifiLinkSnapshot = null,
            )
        assertEquals(NivelResultado.RUIM, resultado.nivel)
    }

    @Test
    fun `55ms com 0 por cento de perda e jitter baixo e classificado como boa`() {
        val medicao = PingResultado(latenciaMs = 55.0, jitterMs = 4.0, perdaPercentual = 0.0, amostras = 24)
        val resultado =
            JogoConexaoEngine.avaliar(
                jogo = fortnite,
                plataforma = Plataforma.PC,
                medicao = medicao,
                tipoConexaoAtual = EstadoConexao.wifi,
                wifiLinkSnapshot = null,
            )
        assertEquals(NivelResultado.BOA, resultado.nivel)
    }

    @Test
    fun `metricas perfeitas resultam em excelente`() {
        val medicao = PingResultado(latenciaMs = 20.0, jitterMs = 2.0, perdaPercentual = 0.0, amostras = 24)
        val resultado =
            JogoConexaoEngine.avaliar(
                jogo = fortnite,
                plataforma = Plataforma.PC,
                medicao = medicao,
                tipoConexaoAtual = EstadoConexao.wifi,
                wifiLinkSnapshot = null,
            )
        assertEquals(NivelResultado.EXCELENTE, resultado.nivel)
    }

    @Test
    fun `wifi em 2,4GHz gera recomendacao de usar 5GHz`() {
        val medicao = PingResultado(latenciaMs = 20.0, jitterMs = 2.0, perdaPercentual = 0.0, amostras = 24)
        val snapshot = WifiLinkSnapshot(ssid = "Casa", bssid = null, rssiDbm = -50, linkSpeedMbps = 100, frequenciaMhz = 2412, padraoWifi = null)
        val resultado =
            JogoConexaoEngine.avaliar(
                jogo = fortnite,
                plataforma = Plataforma.PC,
                medicao = medicao,
                tipoConexaoAtual = EstadoConexao.wifi,
                wifiLinkSnapshot = snapshot,
            )
        assertTrue(resultado.recomendacoes.contains("Use a rede de 5GHz."))
    }

    @Test
    fun `sinal fraco gera recomendacao de aproximar do roteador`() {
        val medicao = PingResultado(latenciaMs = 20.0, jitterMs = 2.0, perdaPercentual = 0.0, amostras = 24)
        val snapshot = WifiLinkSnapshot(ssid = "Casa", bssid = null, rssiDbm = -80, linkSpeedMbps = 100, frequenciaMhz = 5180, padraoWifi = null)
        val resultado =
            JogoConexaoEngine.avaliar(
                jogo = fortnite,
                plataforma = Plataforma.PC,
                medicao = medicao,
                tipoConexaoAtual = EstadoConexao.wifi,
                wifiLinkSnapshot = snapshot,
            )
        assertTrue(resultado.recomendacoes.contains("Aproxime-se do roteador."))
    }

    @Test
    fun `jitter alto gera recomendacao de variacao e perda gera recomendacao de dados perdidos`() {
        val medicao = PingResultado(latenciaMs = 40.0, jitterMs = 35.0, perdaPercentual = 2.0, amostras = 24)
        val resultado =
            JogoConexaoEngine.avaliar(
                jogo = fortnite,
                plataforma = Plataforma.PC,
                medicao = medicao,
                tipoConexaoAtual = EstadoConexao.movel,
                wifiLinkSnapshot = null,
            )
        assertTrue(resultado.recomendacoes.contains("Sua conexão está variando."))
        assertTrue(resultado.recomendacoes.contains("Parte dos dados não chegou ao destino."))
    }

    @Test
    fun `latencia alta com jitter estavel gera recomendacao de servidor distante`() {
        val medicao = PingResultado(latenciaMs = 110.0, jitterMs = 3.0, perdaPercentual = 0.0, amostras = 24)
        val resultado =
            JogoConexaoEngine.avaliar(
                jogo = fortnite,
                plataforma = Plataforma.PC,
                medicao = medicao,
                tipoConexaoAtual = EstadoConexao.movel,
                wifiLinkSnapshot = null,
            )
        assertTrue(resultado.recomendacoes.contains("O servidor está distante."))
    }

    @Test
    fun `PROVIDER_NETWORK cai em REGIONAL_ESTIMATE e nunca inventa dado`() {
        val medicao = PingResultado(latenciaMs = 20.0, jitterMs = 2.0, perdaPercentual = 0.0, amostras = 24)
        val resultado =
            JogoConexaoEngine.avaliar(
                jogo = valorant,
                plataforma = Plataforma.PC,
                medicao = medicao,
                tipoConexaoAtual = EstadoConexao.wifi,
                wifiLinkSnapshot = null,
            )
        assertEquals(EstrategiaTeste.REGIONAL_ESTIMATE, resultado.estrategiaUsada)
        assertTrue(resultado.avisos.any { it.contains("rede do fornecedor") })
    }

    @Test
    fun `veredito nunca chama estimativa regional de ping real`() {
        val medicao = PingResultado(latenciaMs = 20.0, jitterMs = 2.0, perdaPercentual = 0.0, amostras = 24)
        val resultado =
            JogoConexaoEngine.avaliar(
                jogo = fortnite,
                plataforma = Plataforma.PC,
                medicao = medicao,
                tipoConexaoAtual = EstadoConexao.wifi,
                wifiLinkSnapshot = null,
            )
        assertFalse(resultado.avisos.any { it.contains("ping real", ignoreCase = true) })
        assertTrue(resultado.avisos.any { it.contains("estimativa", ignoreCase = true) })
    }
}
