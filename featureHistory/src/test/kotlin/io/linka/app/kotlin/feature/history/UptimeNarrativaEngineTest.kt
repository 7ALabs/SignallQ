package io.linka.app.kotlin.feature.history

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class UptimeNarrativaEngineTest {

    private fun blocoOk(hora: Int = 12): BlocoUptime = BlocoUptime(
        dataHora = LocalDateTime.now().withHour(hora).withMinute(0),
        status = StatusUptime.OK,
        latencyMs = 100,
        latencyMediaMs = 120,
    )

    private fun blocoLento(hora: Int = 12): BlocoUptime = BlocoUptime(
        dataHora = LocalDateTime.now().withHour(hora).withMinute(0),
        status = StatusUptime.LENTO,
        latencyMs = 500,
        latencyMediaMs = 550,
    )

    private fun blocoOffline(hora: Int = 12): BlocoUptime = BlocoUptime(
        dataHora = LocalDateTime.now().withHour(hora).withMinute(0),
        status = StatusUptime.OFFLINE,
        latencyMs = null,
        latencyMediaMs = null,
    )

    private fun blocoSemDado(): BlocoUptime = BlocoUptime(
        dataHora = LocalDateTime.now(),
        status = StatusUptime.SEM_DADO,
        latencyMs = null,
        latencyMediaMs = null,
    )

    // -----------------------------------------------------------------------
    // Caso 1: Rede totalmente estavel
    // -----------------------------------------------------------------------

    @Test
    fun `tudo OK retorna mensagem de rede estavel`() {
        val blocos = List(336) { blocoOk() }
        val narrativa = UptimeNarrativaEngine.gerarNarrativa(blocos)
        assertTrue(
            "Deve mencionar estabilidade quando todos os blocos sao OK",
            narrativa.contains("estável", ignoreCase = true),
        )
    }

    @Test
    fun `tudo OK nao menciona offline nem lentidao`() {
        val blocos = List(336) { blocoOk() }
        val narrativa = UptimeNarrativaEngine.gerarNarrativa(blocos)
        assertTrue("Nao deve mencionar offline", !narrativa.contains("offline", ignoreCase = true))
        assertTrue("Nao deve mencionar lentidao", !narrativa.contains("lentidão", ignoreCase = true))
    }

    // -----------------------------------------------------------------------
    // Caso 2: Muito offline
    // -----------------------------------------------------------------------

    @Test
    fun `muitos blocos OFFLINE menciona indisponibilidade`() {
        val blocos = List(100) { blocoOk() } + List(20) { blocoOffline() } + List(216) { blocoOk() }
        val narrativa = UptimeNarrativaEngine.gerarNarrativa(blocos)
        assertTrue(
            "Deve mencionar offline ou indisponivel: $narrativa",
            narrativa.contains("offline", ignoreCase = true) ||
                narrativa.contains("indisponível", ignoreCase = true),
        )
    }

    @Test
    fun `sequencia longa de OFFLINE menciona interrupcao`() {
        // 8 blocos consecutivos = 4 horas de offline
        val blocos = List(164) { blocoOk() } + List(8) { blocoOffline() } + List(164) { blocoOk() }
        val narrativa = UptimeNarrativaEngine.gerarNarrativa(blocos)
        assertTrue(
            "Sequencia de 4h offline deve gerar narrativa especifica: $narrativa",
            narrativa.contains("offline", ignoreCase = true),
        )
    }

    // -----------------------------------------------------------------------
    // Caso 3: Mix (OK, LENTO, OFFLINE)
    // -----------------------------------------------------------------------

    @Test
    fun `blocos mistos retorna narrativa com conteudo`() {
        val blocos = List(200) { blocoOk() } +
            List(80) { blocoLento() } +
            List(10) { blocoOffline() } +
            List(46) { blocoOk() }
        val narrativa = UptimeNarrativaEngine.gerarNarrativa(blocos)
        assertTrue("Narrativa nao deve estar vazia para blocos mistos", narrativa.isNotBlank())
        assertTrue(
            "Deve mencionar lentidao ou offline: $narrativa",
            narrativa.contains("offline", ignoreCase = true) ||
                narrativa.contains("lentidão", ignoreCase = true) ||
                narrativa.contains("lento", ignoreCase = true) ||
                narrativa.contains("instabilidade", ignoreCase = true),
        )
    }

    // -----------------------------------------------------------------------
    // Caso 4: Sem dados
    // -----------------------------------------------------------------------

    @Test
    fun `lista vazia retorna mensagem de sem dados`() {
        val narrativa = UptimeNarrativaEngine.gerarNarrativa(emptyList())
        assertTrue("Lista vazia deve retornar mensagem de sem dados", narrativa.isNotBlank())
    }

    @Test
    fun `poucos blocos medidos retorna mensagem de monitoramento recente`() {
        val blocos = List(5) { blocoOk() } + List(331) { blocoSemDado() }
        val narrativa = UptimeNarrativaEngine.gerarNarrativa(blocos)
        assertTrue(
            "Poucos dados devem sugerir que monitoramento e recente: $narrativa",
            narrativa.contains("recentemente", ignoreCase = true) ||
                narrativa.contains("iniciado", ignoreCase = true),
        )
    }
}
