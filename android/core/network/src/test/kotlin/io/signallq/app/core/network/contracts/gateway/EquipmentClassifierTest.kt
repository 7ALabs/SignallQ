package io.signallq.app.core.network.contracts.gateway

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitarios de [EquipmentClassifier] — cobre os criterios de aceite de
 * GH#545.
 */
class EquipmentClassifierTest {

    @Test
    fun `Nokia G-1425G-B e classificado como ONT_GPON`() {
        val evidencia = EquipmentFingerprintEvidence(
            gatewayIp = "192.168.1.1",
            vendorHint = "Nokia",
            modelHint = "G-1425G-B",
            httpBanner = "ALCL GPON ONT",
            knownRoutesDetected = setOf("device_status.cgi"),
        )

        val resultado = EquipmentClassifier.classificar(evidencia)

        assertEquals(DeviceType.ONT_GPON, resultado.deviceType)
        assertEquals(SupportLevel.LAB_VALIDATED, resultado.supportLevel)
        assertEquals("nokia-g1425g-b", resultado.driverId)
        assertTrue(resultado.fibraCapable)
    }

    @Test
    fun `Archer C20 e classificado como ROUTER`() {
        val evidencia = EquipmentFingerprintEvidence(
            vendorHint = "TP-Link",
            modelHint = "Archer C20",
            knownRoutesDetected = setOf("cgi-bin/luci"),
        )

        val resultado = EquipmentClassifier.classificar(evidencia)

        assertEquals(DeviceType.ROUTER, resultado.deviceType)
        assertEquals("tplink-archer-c20", resultado.driverId)
        assertEquals(SupportLevel.LAB_VALIDATED, resultado.supportLevel)
    }

    @Test
    fun `Archer C6 e classificado como ROUTER`() {
        val evidencia = EquipmentFingerprintEvidence(
            vendorHint = "TP-Link",
            modelHint = "Archer C6",
            knownRoutesDetected = setOf("cgi-bin/luci"),
        )

        val resultado = EquipmentClassifier.classificar(evidencia)

        assertEquals(DeviceType.ROUTER, resultado.deviceType)
        assertEquals("tplink-archer-c6", resultado.driverId)
        assertEquals(SupportLevel.LAB_VALIDATED, resultado.supportLevel)
    }

    @Test
    fun `Archer A6 v2 (sinonimo de fingerprint do C6) tambem resolve para o driver do C6`() {
        val evidencia = EquipmentFingerprintEvidence(vendorHint = "TP-Link", modelHint = "Archer A6 v2")

        val resultado = EquipmentClassifier.classificar(evidencia)

        assertEquals("tplink-archer-c6", resultado.driverId)
    }

    @Test
    fun `TP-Link nunca recebe capability de fibra mesmo com maior score possivel`() {
        val evidencia = EquipmentFingerprintEvidence(
            gatewayIp = "192.168.0.1",
            vendorHint = "TP-Link",
            modelHint = "Archer C20",
            httpBanner = "TP-Link Archer C20",
            knownRoutesDetected = setOf("cgi-bin/luci"),
        )

        val resultado = EquipmentClassifier.classificar(evidencia)

        assertFalse(resultado.fibraCapable)
    }

    @Test
    fun `TP-Link generico nao vira LAB_VALIDATED por parecer com Archer`() {
        // Roteador TP-Link nao catalogado especificamente (nao e C20 nem C6) cai na
        // familia generica stok-luci, PARSER_IMPORTED — nunca promovido a LAB_VALIDATED.
        val evidencia = EquipmentFingerprintEvidence(vendorHint = "TP-Link", modelHint = "Archer AX10")

        val resultado = EquipmentClassifier.classificar(evidencia)

        assertEquals(DeviceType.ROUTER, resultado.deviceType)
        assertEquals(SupportLevel.PARSER_IMPORTED, resultado.supportLevel)
        assertFalse(resultado.supportLevel == SupportLevel.LAB_VALIDATED)
    }

    @Test
    fun `nenhuma entrada do catalogo pode ser promovida alem do supportLevel declarado, mesmo com score maximo`() {
        DeviceDriverCatalog.entries
            .filter { it.supportLevel != SupportLevel.LAB_VALIDATED }
            .forEach { perfil ->
                val evidenciaComScoreMaximo = EquipmentFingerprintEvidence(
                    gatewayIp = perfil.canonicalGatewayIps.firstOrNull() ?: "10.0.0.1",
                    vendorHint = perfil.modelPatterns.firstOrNull(),
                    modelHint = perfil.modelPatterns.firstOrNull(),
                    httpBanner = perfil.bannerPatterns.firstOrNull(),
                    knownRoutesDetected = perfil.routeSignatures.toSet(),
                )

                val resultado = EquipmentClassifier.classificar(evidenciaComScoreMaximo, catalog = listOf(perfil))

                assertFalse(
                    "driver '${perfil.driverId}' nao pode virar LAB_VALIDATED via score",
                    resultado.supportLevel == SupportLevel.LAB_VALIDATED,
                )
            }
    }

    @Test
    fun `equipamento desconhecido com superficie administrativa vira UNKNOWN_SUPPORTED sem quebrar`() {
        val evidencia = EquipmentFingerprintEvidence(
            gatewayIp = "192.168.15.1",
            httpBanner = "lighttpd/1.4.55",
            knownRoutesDetected = setOf("cgi-bin/status.cgi"),
        )

        val resultado = EquipmentClassifier.classificar(evidencia)

        assertEquals(DeviceType.UNKNOWN_SUPPORTED, resultado.deviceType)
        assertEquals(SupportLevel.UNKNOWN, resultado.supportLevel)
        assertNull(resultado.driverId)
        assertFalse(resultado.fibraCapable)
    }

    @Test
    fun `equipamento sem qualquer evidencia vira UNKNOWN_UNSUPPORTED sem quebrar`() {
        val evidencia = EquipmentFingerprintEvidence()

        val resultado = EquipmentClassifier.classificar(evidencia)

        assertEquals(DeviceType.UNKNOWN_UNSUPPORTED, resultado.deviceType)
        assertEquals(SupportLevel.UNKNOWN, resultado.supportLevel)
        assertEquals(0.0, resultado.confidenceScore, 0.0)
    }

    @Test
    fun `mesh conhecido por nome e classificado como MESH_OR_EXTENDER com familia inferida`() {
        val evidencia = EquipmentFingerprintEvidence(modelHint = "Deco M5")

        val resultado = EquipmentClassifier.classificar(evidencia)

        assertEquals(DeviceType.MESH_OR_EXTENDER, resultado.deviceType)
        assertEquals(SupportLevel.INFERRED_FAMILY, resultado.supportLevel)
        assertFalse(resultado.fibraCapable)
    }

    @Test
    fun `score nunca fica abaixo do piso minimo quando ha pelo menos um sinal`() {
        val evidencia = EquipmentFingerprintEvidence(modelHint = "Archer C20")

        val resultado = EquipmentClassifier.classificar(evidencia)

        assertTrue(resultado.confidenceScore >= 0.10)
    }

    @Test
    fun `casamento por IP canonico sozinho ja classifica corretamente a Nokia`() {
        val evidencia = EquipmentFingerprintEvidence(gatewayIp = "192.168.1.1")
        val catalogoComIpUnico = listOf(
            DeviceDriverProfile(
                driverId = "nokia-teste-ip",
                vendor = "Nokia",
                canonicalGatewayIps = listOf("192.168.1.1"),
                deviceType = DeviceType.ONT_GPON,
                supportLevel = SupportLevel.LAB_VALIDATED,
                fibraCapable = true,
            ),
        )

        val resultado = EquipmentClassifier.classificar(evidencia, catalog = catalogoComIpUnico)

        assertEquals(DeviceType.ONT_GPON, resultado.deviceType)
        assertTrue(resultado.fibraCapable)
    }
}
