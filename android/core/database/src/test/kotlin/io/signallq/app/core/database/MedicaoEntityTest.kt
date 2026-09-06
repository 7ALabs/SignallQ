package io.signallq.app.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicaoEntityTest {
    // Entidade mínima válida — apenas campos obrigatórios
    private fun entidadeMinima(id: String = "abc-123") =
        MedicaoEntity(
            id = id,
            timestampEpochMs = 1_700_000_000_000L,
            connectionType = "wifi",
            connectionTypeStart = null,
            connectionTypeEnd = null,
            contaminado = false,
            speedtestMode = null,
            specVersion = null,
            downloadMbps = null,
            uploadMbps = null,
            latencyMs = null,
            jitterMs = null,
            perdaPercentual = null,
            bufferbloatMs = null,
            packetLossSource = null,
            vereditoStreaming = null,
            vereditoGamer = null,
            vereditoVideoChamada = null,
            gargaloPrimario = null,
        )

    @Test
    fun `entidade minima tem campos nulos para metricas opcionais`() {
        val e = entidadeMinima()

        assertNull(e.downloadMbps)
        assertNull(e.uploadMbps)
        assertNull(e.latencyMs)
        assertNull(e.jitterMs)
        assertNull(e.perdaPercentual)
        assertNull(e.bufferbloatMs)
        assertNull(e.gargaloPrimario)
    }

    @Test
    fun `contaminado false por padrao na entidade minima`() {
        val e = entidadeMinima()

        assertFalse(e.contaminado)
    }

    @Test
    fun `fonte e operadoraMovel tem default null`() {
        val e = entidadeMinima()

        assertNull(e.fonte)
        assertNull(e.operadoraMovel)
    }

    @Test
    fun `bandaWifi tem default null (GH#1027)`() {
        val e = entidadeMinima()

        assertNull(e.bandaWifi)
    }

    @Test
    fun `bandaWifi preserva valor ghz5 quando informado`() {
        val e = entidadeMinima().copy(bandaWifi = "ghz5")

        assertEquals("ghz5", e.bandaWifi)
    }

    @Test
    fun `entidade com todas as metricas preserva valores`() {
        val e =
            MedicaoEntity(
                id = "full-001",
                timestampEpochMs = 1_700_000_000_000L,
                connectionType = "movel",
                connectionTypeStart = "wifi",
                connectionTypeEnd = "movel",
                contaminado = true,
                speedtestMode = "full",
                specVersion = "1.0",
                downloadMbps = 150.5,
                uploadMbps = 20.3,
                latencyMs = 12.0,
                jitterMs = 2.5,
                perdaPercentual = 0.1,
                bufferbloatMs = 30.0,
                packetLossSource = "modem",
                vereditoStreaming = "bom",
                vereditoGamer = "ruim",
                vereditoVideoChamada = "regular",
                gargaloPrimario = "upload",
                fonte = "speedtest",
                operadoraMovel = "Vivo",
            )

        assertEquals(150.5, e.downloadMbps)
        assertEquals(20.3, e.uploadMbps)
        assertEquals(12.0, e.latencyMs)
        assertEquals("Vivo", e.operadoraMovel)
        assertTrue(e.contaminado)
    }

    @Test
    fun `entidades com ids diferentes nao sao iguais`() {
        val a = entidadeMinima("id-1")
        val b = entidadeMinima("id-2")

        assertNotEquals(a, b)
    }

    @Test
    fun `copy permite marcar entidade como contaminada`() {
        val original = entidadeMinima()
        val contaminada = original.copy(contaminado = true)

        assertFalse(original.contaminado)
        assertTrue(contaminada.contaminado)
        assertEquals(original.id, contaminada.id)
    }

    @Test
    fun `connectionType wifi e preservado`() {
        val e = entidadeMinima()
        assertEquals("wifi", e.connectionType)
    }

    // =========================================================================
    // GH#1228 (Fase 3) — executionId/rulesVersion
    // =========================================================================

    @Test
    fun `executionId tem default vazio e rulesVersion default legacy-unversioned na entidade minima`() {
        val e = entidadeMinima()

        assertEquals("", e.executionId)
        assertEquals("legacy-unversioned", e.rulesVersion)
    }

    @Test
    fun `executionId e rulesVersion explicitos sao preservados`() {
        val e =
            entidadeMinima().copy(
                executionId = "exec-real-001",
                rulesVersion = "diagnostic-rules-v1",
            )

        assertEquals("exec-real-001", e.executionId)
        assertEquals("diagnostic-rules-v1", e.rulesVersion)
    }

    @Test
    fun `duas entidades com o mesmo id mas executionId diferentes nao sao iguais`() {
        val a = entidadeMinima("mesmo-id").copy(executionId = "exec-A")
        val b = entidadeMinima("mesmo-id").copy(executionId = "exec-B")

        assertNotEquals(a, b)
    }

    // =========================================================================
    // GH#1737 (épico #1647) — remocao do modo `triplo` de ModoSpeedtest. `speedtestMode` e
    // uma String opaca persistida via `resultado.modo.name` (SpeedtestPersistenceCoordinator),
    // nunca reconstruida de volta pro enum (sem `ModoSpeedtest.valueOf`/`enumValueOf` em nenhum
    // consumidor de historico). Uma medicao antiga persistida com "triplo" continua uma entidade
    // valida e legivel — a remocao do valor do enum nao quebra leitura/exportacao de historico.
    // =========================================================================

    @Test
    fun `entidade historica com speedtestMode triplo continua valida apos a remocao do modo do enum`() {
        val e = entidadeMinima("historico-triplo").copy(speedtestMode = "triplo")

        assertEquals("triplo", e.speedtestMode)
    }
}
