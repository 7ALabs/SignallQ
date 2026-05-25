package io.linka.app.kotlin.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicaoEntityTest {

    // Entidade mínima válida — apenas campos obrigatórios
    private fun entidadeMinima(id: String = "abc-123") = MedicaoEntity(
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
    fun `entidade com todas as metricas preserva valores`() {
        val e = MedicaoEntity(
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
}
