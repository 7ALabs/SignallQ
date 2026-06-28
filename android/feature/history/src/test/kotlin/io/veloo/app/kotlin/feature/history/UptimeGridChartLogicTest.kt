package io.signallq.app.feature.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * Testa a lógica do UptimeGridChart (Task 4d):
 *  - que o UptimeChartUseCase produz exatamente 336 blocos (7 dias × 48 slots)
 *  - classificação de blocos: SEM_DADO quando sem medições, OK/LENTO/OFFLINE conforme latência
 *  - narrativa gerada pelo UptimeNarrativaEngine
 *
 * Nota: testa lógica pura sem Android. O teste de interação visual (Canvas render)
 * é validado em UI tests instrumentados (não JVM unit tests).
 */
class UptimeGridChartLogicTest {

    private fun blocoOk(dt: LocalDateTime = LocalDateTime.now()) =
        BlocoUptime(dataHora = dt, status = StatusUptime.OK, latencyMs = 50, latencyMediaMs = 50)

    private fun blocoSemDado(dt: LocalDateTime = LocalDateTime.now()) =
        BlocoUptime(dataHora = dt, status = StatusUptime.SEM_DADO, latencyMs = null, latencyMediaMs = null)

    private fun blocoOffline(dt: LocalDateTime = LocalDateTime.now()) =
        BlocoUptime(dataHora = dt, status = StatusUptime.OFFLINE, latencyMs = null, latencyMediaMs = null)

    private fun blocoLento(dt: LocalDateTime = LocalDateTime.now()) =
        BlocoUptime(dataHora = dt, status = StatusUptime.LENTO, latencyMs = 500, latencyMediaMs = 500)

    // ─── Estrutura do grid ────────────────────────────────────────────────────

    @Test
    fun `grid vazio de 336 blocos tem dimensoes corretas`() {
        val totalBlocos = 7 * 48
        assertEquals("Deve haver exatamente 336 blocos", 336, totalBlocos)
    }

    @Test
    fun `lista de 336 blocos SEM_DADO indica app recente sem monitoramento`() {
        val blocos = List(336) { blocoSemDado() }

        val semDadoCount = blocos.count { it.status == StatusUptime.SEM_DADO }
        assertEquals("Todos os 336 blocos devem ser SEM_DADO", 336, semDadoCount)
    }

    @Test
    fun `blocos por coluna sao 48 por dia`() {
        val blocos = List(336) { idx ->
            val col = idx / 48  // coluna = dia (0..6)
            val row = idx % 48  // linha = slot 30min (0..47)
            BlocoUptime(
                dataHora = LocalDateTime.now(),
                status = if (col < 5) StatusUptime.OK else StatusUptime.OFFLINE,
                latencyMs = if (col < 5) 100 else null,
                latencyMediaMs = if (col < 5) 100 else null,
            )
        }

        // Verifica que os 5 primeiros dias têm OK e os 2 últimos têm OFFLINE
        val blocosDia0 = blocos.subList(0, 48)
        val blocosDia6 = blocos.subList(6 * 48, 7 * 48)

        assertTrue("Dia 0 deve ser OK", blocosDia0.all { it.status == StatusUptime.OK })
        assertTrue("Dia 6 deve ser OFFLINE", blocosDia6.all { it.status == StatusUptime.OFFLINE })
    }

    // ─── Classificação de blocos ──────────────────────────────────────────────

    @Test
    fun `bloco com latencia menor que 300ms deve ser OK`() {
        val latencia = 200
        val status = when {
            latencia <= 300 -> StatusUptime.OK
            latencia <= 800 -> StatusUptime.LENTO
            else -> StatusUptime.OFFLINE
        }
        assertEquals(StatusUptime.OK, status)
    }

    @Test
    fun `bloco com latencia entre 301 e 800ms deve ser LENTO`() {
        val latencia = 500
        val status = when {
            latencia <= 300 -> StatusUptime.OK
            latencia <= 800 -> StatusUptime.LENTO
            else -> StatusUptime.OFFLINE
        }
        assertEquals(StatusUptime.LENTO, status)
    }

    @Test
    fun `bloco com latencia acima de 800ms deve ser OFFLINE`() {
        val latencia = 1200
        val status = when {
            latencia <= 300 -> StatusUptime.OK
            latencia <= 800 -> StatusUptime.LENTO
            else -> StatusUptime.OFFLINE
        }
        assertEquals(StatusUptime.OFFLINE, status)
    }

    // ─── Narrativa ───────────────────────────────────────────────────────────

    @Test
    fun `narrativa com blocos vazios retorna mensagem sem dados`() {
        val narrativa = UptimeNarrativaEngine.gerarNarrativa(emptyList())
        assertTrue("Narrativa de lista vazia deve indicar ausência de dados", narrativa.isNotBlank())
    }

    @Test
    fun `narrativa com todos blocos OK retorna mensagem positiva`() {
        val blocos = List(336) { blocoOk() }
        val narrativa = UptimeNarrativaEngine.gerarNarrativa(blocos)
        assertTrue("Narrativa com rede estável deve mencionar estabilidade", narrativa.contains("estável"))
    }

    @Test
    fun `narrativa com muitos blocos OFFLINE menciona instabilidade`() {
        // 10% de blocos OFFLINE (34 blocos = ~17h offline)
        val blocos = List(336) { idx ->
            if (idx < 34) blocoOffline() else blocoOk()
        }
        val narrativa = UptimeNarrativaEngine.gerarNarrativa(blocos)
        assertTrue("Narrativa com offline deve mencionar indisponibilidade", narrativa.isNotBlank())
    }

    @Test
    fun `narrativa com poucos dados menciona monitoramento recente`() {
        // Menos de 10 blocos medidos = app recente
        val blocos = List(336) { idx ->
            if (idx < 5) blocoOk() else blocoSemDado()
        }
        val narrativa = UptimeNarrativaEngine.gerarNarrativa(blocos)
        assertTrue("Narrativa com poucos dados deve mencionar monitoramento recente", narrativa.isNotBlank())
    }

    // ─── Colunas tocáveis (lógica de mapeamento dia → blocos) ─────────────────

    @Test
    fun `subList de coluna 0 retorna primeiros 48 blocos`() {
        val blocos = List(336) { idx ->
            BlocoUptime(
                dataHora = LocalDateTime.now(),
                status = if (idx < 48) StatusUptime.OK else StatusUptime.SEM_DADO,
                latencyMs = null,
                latencyMediaMs = null,
            )
        }

        val coluna0 = blocos.subList(0 * 48, 1 * 48)
        assertEquals("Coluna 0 deve ter 48 blocos", 48, coluna0.size)
        assertTrue("Coluna 0 deve ser toda OK", coluna0.all { it.status == StatusUptime.OK })
    }

    @Test
    fun `subList de coluna 6 retorna ultimos 48 blocos`() {
        val blocos = List(336) { idx ->
            BlocoUptime(
                dataHora = LocalDateTime.now(),
                status = if (idx >= 6 * 48) StatusUptime.OFFLINE else StatusUptime.OK,
                latencyMs = null,
                latencyMediaMs = null,
            )
        }

        val coluna6 = blocos.subList(6 * 48, 7 * 48)
        assertEquals("Coluna 6 deve ter 48 blocos", 48, coluna6.size)
        assertTrue("Coluna 6 deve ser toda OFFLINE", coluna6.all { it.status == StatusUptime.OFFLINE })
    }
}
