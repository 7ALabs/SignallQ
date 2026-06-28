package io.signallq.app.feature.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class UptimeNarrativaEngineTest {

    // ---------------------------------------------------------------------------
    // Helpers de construcao de blocos
    // ---------------------------------------------------------------------------

    private fun blocoOk(
        hora: Int = 12,
        minuto: Int = 0,
        dia: Int = 1,
    ): BlocoUptime = BlocoUptime(
        dataHora = LocalDateTime.of(2025, 5, dia, hora, minuto),
        status = StatusUptime.OK,
        latencyMs = 100,
        latencyMediaMs = 120,
    )

    private fun blocoLento(
        hora: Int = 12,
        minuto: Int = 0,
        dia: Int = 1,
    ): BlocoUptime = BlocoUptime(
        dataHora = LocalDateTime.of(2025, 5, dia, hora, minuto),
        status = StatusUptime.LENTO,
        latencyMs = 500,
        latencyMediaMs = 550,
    )

    private fun blocoOffline(
        hora: Int = 12,
        minuto: Int = 0,
        dia: Int = 1,
    ): BlocoUptime = BlocoUptime(
        dataHora = LocalDateTime.of(2025, 5, dia, hora, minuto),
        status = StatusUptime.OFFLINE,
        latencyMs = null,
        latencyMediaMs = null,
    )

    private fun blocoSemDado(dia: Int = 1): BlocoUptime = BlocoUptime(
        dataHora = LocalDateTime.of(2025, 5, dia, 0, 0),
        status = StatusUptime.SEM_DADO,
        latencyMs = null,
        latencyMediaMs = null,
    )

    // ---------------------------------------------------------------------------
    // Caso 1: Rede totalmente estavel
    // ---------------------------------------------------------------------------

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
    fun `tudo OK nao reporta problemas de offline nem lentidao`() {
        val blocos = List(336) { blocoOk() }
        val narrativa = UptimeNarrativaEngine.gerarNarrativa(blocos)
        // A mensagem estável contém "Nenhuma lentidão" em contexto negativo — correto.
        // O teste verifica que não há RELATO de problema (sem "houve lentidão", "lentidão por X").
        assertTrue("Nao deve mencionar offline", !narrativa.contains("offline", ignoreCase = true))
        assertTrue("Nao deve relatar lentidao como problema", !narrativa.contains("houve lentidão", ignoreCase = true) && !narrativa.contains("lentidão por", ignoreCase = true))
    }

    // ---------------------------------------------------------------------------
    // Caso 2: Muito offline
    // ---------------------------------------------------------------------------

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

    // ---------------------------------------------------------------------------
    // Caso 3: Mix (OK, LENTO, OFFLINE)
    // ---------------------------------------------------------------------------

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

    // ---------------------------------------------------------------------------
    // Caso 4: Sem dados
    // ---------------------------------------------------------------------------

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

    // ---------------------------------------------------------------------------
    // v2.0 — Padroes horarios recorrentes
    // ---------------------------------------------------------------------------

    @Test
    fun `detectarPadraoHorario retorna null quando nao ha recorrencia`() {
        // Quedas espalhadas em horas diferentes, cada uma so em 1 dia
        val blocos = listOf(
            blocoOffline(hora = 8, dia = 1),
            blocoOffline(hora = 14, dia = 2),
            blocoOffline(hora = 20, dia = 3),
        ) + List(50) { blocoOk() }

        val padrao = UptimeNarrativaEngine.detectarPadraoHorario(blocos)
        assertNull("Sem recorrencia nao deve retornar padrao: $padrao", padrao)
    }

    @Test
    fun `detectarPadraoHorario detecta quedas recorrentes no mesmo horario`() {
        // Quedas as 8h em 3 dias distintos — deve detectar padrao
        val blocos = listOf(
            blocoOffline(hora = 8, minuto = 0, dia = 1),
            blocoOffline(hora = 8, minuto = 30, dia = 1),
            blocoOffline(hora = 8, minuto = 0, dia = 2),
            blocoOffline(hora = 8, minuto = 0, dia = 3),
        ) + List(50) { blocoOk() }

        val padrao = UptimeNarrativaEngine.detectarPadraoHorario(blocos)
        assertNotNull("Deve detectar padrao as 8h em 3 dias: $padrao", padrao)
        assertTrue(
            "Descricao deve mencionar o horario 08h: $padrao",
            padrao!!.contains("08h"),
        )
    }

    @Test
    fun `detectarPadraoHorario descreve periodo do dia corretamente`() {
        // Quedas as 15h (tarde) em 2 dias distintos
        val blocos = listOf(
            blocoOffline(hora = 15, dia = 1),
            blocoOffline(hora = 15, dia = 2),
        ) + List(50) { blocoOk() }

        val padrao = UptimeNarrativaEngine.detectarPadraoHorario(blocos)
        assertNotNull("Deve detectar padrao as 15h: $padrao", padrao)
        assertTrue(
            "Descricao deve mencionar 'tarde': $padrao",
            padrao!!.contains("tarde", ignoreCase = true),
        )
    }

    // ---------------------------------------------------------------------------
    // v2.0 — Sequencias longas de OFFLINE
    // ---------------------------------------------------------------------------

    @Test
    fun `detectarInterrupcoesLongas retorna lista vazia quando nao ha sequencia longa`() {
        // So 1 bloco OFFLINE isolado = 30 min = nao e longa
        val blocos = List(10) { blocoOk() } +
            listOf(blocoOffline()) +
            List(10) { blocoOk() }

        val interrupcoes = UptimeNarrativaEngine.detectarInterrupcoesLongas(blocos)
        assertTrue(
            "1 bloco offline isolado nao deve gerar interrupcao longa",
            interrupcoes.isEmpty(),
        )
    }

    @Test
    fun `detectarInterrupcoesLongas detecta sequencia de 2 blocos (60 min)`() {
        // 2 blocos consecutivos = 60 min — deve aparecer
        val blocos = List(10) { blocoOk() } +
            listOf(blocoOffline(hora = 9, minuto = 0), blocoOffline(hora = 9, minuto = 30)) +
            List(10) { blocoOk() }

        val interrupcoes = UptimeNarrativaEngine.detectarInterrupcoesLongas(blocos)
        assertEquals("Deve retornar exatamente 1 interrupcao", 1, interrupcoes.size)
        assertEquals("Duracao deve ser 60 minutos", 60, interrupcoes[0].duracaoMinutos)
    }

    @Test
    fun `detectarInterrupcoesLongas detecta multiplas interrupcoes e ordena por duracao`() {
        // Interrupcao 1: 3 blocos = 90 min; Interrupcao 2: 4 blocos = 120 min
        val blocos = List(10) { blocoOk() } +
            List(3) { blocoOffline(hora = 8) } +
            List(5) { blocoOk() } +
            List(4) { blocoOffline(hora = 14) } +
            List(10) { blocoOk() }

        val interrupcoes = UptimeNarrativaEngine.detectarInterrupcoesLongas(blocos)
        assertEquals("Deve retornar 2 interrupcoes", 2, interrupcoes.size)
        assertTrue(
            "Interrupcoes devem estar ordenadas por duracao decrescente",
            interrupcoes[0].duracaoMinutos >= interrupcoes[1].duracaoMinutos,
        )
        assertEquals("Maior interrupcao deve ter 120 min", 120, interrupcoes[0].duracaoMinutos)
        assertEquals("Menor interrupcao deve ter 90 min", 90, interrupcoes[1].duracaoMinutos)
    }

    @Test
    fun `detectarInterrupcoesLongas detecta sequencia que termina no fim da lista`() {
        // Sequencia offline ao final da lista
        val blocos = List(10) { blocoOk() } + List(3) { blocoOffline(hora = 23) }

        val interrupcoes = UptimeNarrativaEngine.detectarInterrupcoesLongas(blocos)
        assertEquals("Deve detectar interrupcao no final da lista", 1, interrupcoes.size)
        assertEquals("Duracao deve ser 90 minutos", 90, interrupcoes[0].duracaoMinutos)
    }

    // ---------------------------------------------------------------------------
    // v2.0 — Tendencia de qualidade
    // ---------------------------------------------------------------------------

    @Test
    fun `calcularTendencia retorna ESTAVEL quando dados insuficientes`() {
        val blocos = List(47) { blocoOk() } // menos de 2 * BLOCOS_POR_DIA = 96
        val tendencia = UptimeNarrativaEngine.calcularTendencia(blocos)
        assertEquals("Menos de 96 blocos deve retornar ESTAVEL", Tendencia.ESTAVEL, tendencia)
    }

    @Test
    fun `calcularTendencia retorna PIORANDO quando ultima 24h tem mais offline`() {
        // Anteriores 24h: tudo OK (48 blocos)
        // Ultimas 24h: 40% offline = 20 blocos offline, 28 OK
        val anteriores = List(48) { blocoOk(dia = 1) }
        val ultimas = List(28) { blocoOk(dia = 2) } + List(20) { blocoOffline(dia = 2) }
        val blocos = anteriores + ultimas

        val tendencia = UptimeNarrativaEngine.calcularTendencia(blocos)
        assertEquals("Queda brusca de uptime deve ser PIORANDO", Tendencia.PIORANDO, tendencia)
    }

    @Test
    fun `calcularTendencia retorna MELHORANDO quando ultima 24h tem mais uptime`() {
        // Anteriores 24h: 40% offline
        // Ultimas 24h: tudo OK
        val anteriores = List(28) { blocoOk(dia = 1) } + List(20) { blocoOffline(dia = 1) }
        val ultimas = List(48) { blocoOk(dia = 2) }
        val blocos = anteriores + ultimas

        val tendencia = UptimeNarrativaEngine.calcularTendencia(blocos)
        assertEquals("Melhora brusca de uptime deve ser MELHORANDO", Tendencia.MELHORANDO, tendencia)
    }

    @Test
    fun `calcularTendencia retorna ESTAVEL quando delta e pequeno`() {
        // Anteriores: 95% OK (46/48), Ultimas: 93% OK (45/48) — delta ~2pp
        val anteriores = List(46) { blocoOk(dia = 1) } + List(2) { blocoOffline(dia = 1) }
        val ultimas = List(45) { blocoOk(dia = 2) } + List(3) { blocoOffline(dia = 2) }
        val blocos = anteriores + ultimas

        val tendencia = UptimeNarrativaEngine.calcularTendencia(blocos)
        assertEquals("Delta pequeno deve ser ESTAVEL", Tendencia.ESTAVEL, tendencia)
    }

    // ---------------------------------------------------------------------------
    // v2.0 — Narrativa integrada com novos recursos
    // ---------------------------------------------------------------------------

    @Test
    fun `narrativa menciona tendencia piorando quando detectada`() {
        val anteriores = List(48) { blocoOk(dia = 1) }
        val ultimas = List(20) { blocoOk(dia = 2) } + List(28) { blocoOffline(dia = 2) }
        // Preenche ate 336 blocos com OK antes das janelas de tendencia
        val padding = List(240) { blocoOk(dia = 1) }
        val blocos = padding + anteriores + ultimas

        val narrativa = UptimeNarrativaEngine.gerarNarrativa(blocos)
        assertTrue(
            "Narrativa deve mencionar piora quando tendencia e detectada: $narrativa",
            narrativa.contains("piorando", ignoreCase = true),
        )
    }

    // ---------------------------------------------------------------------------
    // v2.0 — Edge cases obrigatorios (criticos #42)
    // ---------------------------------------------------------------------------

    @Test
    fun `calcularTendencia retorna ESTAVEL com exatamente 96 blocos distribuidos 50-50`() {
        // 96 blocos: anteriores24h = indices 0..47, ultimas24h = indices 48..95
        // Cada janela tem 24 OK + 24 OFFLINE = 50% uptime em cada lado => delta = 0 => ESTAVEL
        val anteriores = List(24) { blocoOk(dia = 1) } + List(24) { blocoOffline(dia = 1) }
        val ultimas = List(24) { blocoOk(dia = 2) } + List(24) { blocoOffline(dia = 2) }
        val blocos = anteriores + ultimas

        assertEquals("96 blocos com 50/50 em cada janela deve ser ESTAVEL", 96, blocos.size)
        val tendencia = UptimeNarrativaEngine.calcularTendencia(blocos)
        assertEquals("Delta zero deve retornar ESTAVEL", Tendencia.ESTAVEL, tendencia)
    }

    @Test
    fun `detectarInterrupcoesLongas nao lanca excecao quando sequencia OFFLINE comeca no bloco 0`() {
        // Sequencia OFFLINE comecando no primeiro bloco (indice 0) — NPE potencial sem fix
        val blocos = List(3) { blocoOffline(hora = 0) } + List(10) { blocoOk() }

        val interrupcoes = UptimeNarrativaEngine.detectarInterrupcoesLongas(blocos)
        assertEquals("Deve detectar 1 interrupcao iniciada no bloco 0", 1, interrupcoes.size)
        assertEquals("Duracao deve ser 90 minutos (3 blocos)", 90, interrupcoes[0].duracaoMinutos)
    }

    @Test
    fun `calcularTendencia retorna PIORANDO quando anteriores 80pct OK e ultimas 50pct OK`() {
        // anteriores24h: 80% OK = 39 OK + 9 OFFLINE de 48 blocos
        val anteriores = List(39) { blocoOk(dia = 1) } + List(9) { blocoOffline(dia = 1) }
        // ultimas24h: 50% OK = 24 OK + 24 OFFLINE de 48 blocos
        val ultimas = List(24) { blocoOk(dia = 2) } + List(24) { blocoOffline(dia = 2) }
        val blocos = anteriores + ultimas

        val tendencia = UptimeNarrativaEngine.calcularTendencia(blocos)
        assertEquals(
            "Queda de ~80pp para 50pp (delta -30pp) deve retornar PIORANDO",
            Tendencia.PIORANDO,
            tendencia,
        )
    }

    @Test
    fun `narrativa menciona padrao horario recorrente quando detectado`() {
        // 50 blocos OK como base + quedas as 8h em 3 dias diferentes
        val base = List(50) { blocoOk(dia = 1) }
        val quedas = listOf(
            blocoOffline(hora = 8, dia = 1),
            blocoOffline(hora = 8, dia = 2),
            blocoOffline(hora = 8, dia = 3),
        )
        val enchimento = List(233) { blocoOk(dia = 1) }
        val blocos = base + quedas + enchimento

        val narrativa = UptimeNarrativaEngine.gerarNarrativa(blocos)
        assertTrue(
            "Narrativa deve mencionar padrao recorrente: $narrativa",
            narrativa.contains("padrão", ignoreCase = true) ||
                narrativa.contains("recorrente", ignoreCase = true) ||
                narrativa.contains("08h", ignoreCase = true),
        )
    }
}
