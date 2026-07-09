package io.signallq.app.feature.diagnostico

import io.signallq.app.core.network.contracts.localdevice.DeviceCapabilities
import io.signallq.app.core.network.contracts.localdevice.DeviceType
import io.signallq.app.core.network.contracts.localdevice.LocalDeviceSectionStatus
import io.signallq.app.core.network.contracts.localdevice.SafeLocalDeviceContext
import io.signallq.app.core.network.contracts.localdevice.SupportLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes de correlação do [FindingEngine] com o resumo seguro do equipamento de
 * rede local ([SafeLocalDeviceContext]) — GH#542, epic #547.
 *
 * Cobre os 5 cenários documentados na issue:
 *  1. Speedtest ruim + fibra Nokia OK + Wi-Fi fraco -> problema Wi-Fi/local.
 *  2. Speedtest ruim + fibra Nokia ruim -> possível problema óptico/provedor.
 *  3. Speedtest ruim + TP-Link WAN OK + muitos clientes -> saturação local.
 *  4. Speedtest ruim + TP-Link WAN down -> falha upstream/local, sem afirmar fibra.
 *  5. TP-Link sem dados de fibra -> diagnóstico declara limitação.
 *
 * Além de: motor continua funcionando sem snapshot (null), dados experimentais
 * pesam menos, e TP-Link nunca recomenda algo baseado em fibra.
 */
class FindingEngineLocalDeviceTest {

    private fun resultado(id: String, status: DiagnosticStatus, categoria: String) =
        DiagnosticResult(
            id = id,
            titulo = id,
            status = status,
            evidencia = "test",
            mensagemUsuario = "msg $id",
            recomendacao = null,
            categoria = categoria,
        )

    private fun ontOk(
        supportLevel: SupportLevel = SupportLevel.LAB_VALIDATED,
        statusFibra: LocalDeviceSectionStatus = LocalDeviceSectionStatus.OK,
    ) = SafeLocalDeviceContext(
        vendor = "Nokia",
        modelo = "G-1425G-B",
        firmwareVersion = "1.0",
        deviceType = DeviceType.ONT_GPON,
        supportLevel = supportLevel,
        capabilities = DeviceCapabilities(suportaFibra = true, suportaWan = true),
        connectionStatus = LocalDeviceSectionStatus.OK,
        statusFibra = statusFibra,
        statusWan = LocalDeviceSectionStatus.OK,
        statusWifi = LocalDeviceSectionStatus.NAO_SUPORTADO,
        statusLan = LocalDeviceSectionStatus.NAO_SUPORTADO,
        quantidadeClientes = 0,
        warnings = emptyList(),
        coletadoEmEpochMs = 1_000L,
    )

    private fun tpLinkRoteador(
        statusWan: LocalDeviceSectionStatus = LocalDeviceSectionStatus.OK,
        quantidadeClientes: Int = 0,
        supportLevel: SupportLevel = SupportLevel.LAB_VALIDATED,
    ) = SafeLocalDeviceContext(
        vendor = "TP-Link",
        modelo = "Archer C20",
        firmwareVersion = "1.0",
        deviceType = DeviceType.ROUTER,
        supportLevel = supportLevel,
        capabilities = DeviceCapabilities(suportaWan = true, suportaWifi = true, suportaClientes = true),
        connectionStatus = LocalDeviceSectionStatus.OK,
        statusFibra = LocalDeviceSectionStatus.NAO_SUPORTADO,
        statusWan = statusWan,
        statusWifi = LocalDeviceSectionStatus.OK,
        statusLan = LocalDeviceSectionStatus.OK,
        quantidadeClientes = quantidadeClientes,
        warnings = emptyList(),
        coletadoEmEpochMs = 1_000L,
    )

    // -------------------------------------------------------------------------
    // Motor continua funcionando sem snapshot
    // -------------------------------------------------------------------------

    @Test
    fun `sem localDevice, comportamento identico ao motor sem equipamento (DECISAO-01)`() {
        val r = FindingEngine.analisar(
            internetResultados = listOf(resultado("IN-CRITICO", DiagnosticStatus.critical, "internet")),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = false),
            localDevice = null,
        )

        assertEquals("DECISAO-01", r.principal.id)
        assertTrue(r.limitacoesEquipamentoLocal.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Cenário 1: speedtest ruim + fibra Nokia OK + Wi-Fi fraco -> problema Wi-Fi/local
    // -------------------------------------------------------------------------

    @Test
    fun `fibra ONT confirmada OK mais wifi ruim - LOCAL-EQUIP-WIFI-01 vence e DECISAO-01 vira secundario`() {
        val r = FindingEngine.analisar(
            internetResultados = listOf(resultado("IN-CRITICO", DiagnosticStatus.critical, "internet")),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = false),
            localDevice = ontOk(),
        )

        assertEquals("LOCAL-EQUIP-WIFI-01", r.principal.id)
        assertTrue(r.principal.mensagemUsuario.contains("Wi-Fi") || r.principal.mensagemUsuario.contains("local"))
        assertTrue(
            "DECISAO-01 deve continuar aparecendo como achado secundario, nao sumir",
            r.secundarios.any { it.id == "DECISAO-01" },
        )
    }

    @Test
    fun `suporte experimental reduz confianca - DECISAO-01 volta a vencer sobre LOCAL-EQUIP-WIFI-01`() {
        val r = FindingEngine.analisar(
            internetResultados = listOf(resultado("IN-CRITICO", DiagnosticStatus.critical, "internet")),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = false),
            localDevice = ontOk(supportLevel = SupportLevel.PARSER_IMPORTED),
        )

        // 0.85 * 0.7 = 0.595 -> score 2*0.595=1.19 < DECISAO-01 score 2*0.65=1.3
        assertEquals("DECISAO-01", r.principal.id)
        assertTrue(r.secundarios.any { it.id == "LOCAL-EQUIP-WIFI-01" })
    }

    // -------------------------------------------------------------------------
    // Cenário 2: speedtest ruim + fibra Nokia ruim -> possível problema óptico/provedor
    // -------------------------------------------------------------------------

    @Test
    fun `fibra ONT com atencao mais internet ruim - LOCAL-EQUIP-FIBRA-01 vence quando nao ha leitura direta`() {
        val r = FindingEngine.analisar(
            internetResultados = listOf(resultado("IN-CRITICO", DiagnosticStatus.critical, "internet")),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            localDevice = ontOk(statusFibra = LocalDeviceSectionStatus.ATENCAO),
        )

        assertEquals("LOCAL-EQUIP-FIBRA-01", r.principal.id)
    }

    @Test
    fun `fibra ONT com atencao suprimida quando leitura direta da fibra ja e critica`() {
        val r = FindingEngine.analisar(
            internetResultados = listOf(resultado("IN-CRITICO", DiagnosticStatus.critical, "internet")),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            fibraResultados = listOf(resultado("FIB-CRITICO", DiagnosticStatus.critical, "fibra")),
            localDevice = ontOk(statusFibra = LocalDeviceSectionStatus.ATENCAO),
        )

        assertEquals("DECISAO-00", r.principal.id)
        assertTrue(
            "LOCAL-EQUIP-FIBRA-01 deve virar hipotese descartada, nao desaparecer",
            r.hipotesesDescartadas.any { it.id == "LOCAL-EQUIP-FIBRA-01" },
        )
    }

    // -------------------------------------------------------------------------
    // Cenário 3: speedtest ruim + TP-Link WAN OK + muitos clientes -> saturação local
    // -------------------------------------------------------------------------

    @Test
    fun `TP-Link com WAN ok e muitos clientes mais internet ruim - LOCAL-EQUIP-SATURACAO-01 vence`() {
        val r = FindingEngine.analisar(
            internetResultados = listOf(resultado("IN-CRITICO", DiagnosticStatus.critical, "internet")),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            localDevice = tpLinkRoteador(statusWan = LocalDeviceSectionStatus.OK, quantidadeClientes = 15),
        )

        assertEquals("LOCAL-EQUIP-SATURACAO-01", r.principal.id)
        assertFalse(
            "TP-Link nunca pode recomendar algo baseado em fibra",
            r.principal.recomendacao!!.contains("fibra", ignoreCase = true),
        )
        assertFalse(
            "TP-Link nunca pode recomendar algo baseado em fibra",
            r.principal.mensagemUsuario.contains("fibra", ignoreCase = true),
        )
    }

    @Test
    fun `TP-Link com poucos clientes nao aciona saturacao`() {
        val r = FindingEngine.analisar(
            internetResultados = listOf(resultado("IN-CRITICO", DiagnosticStatus.critical, "internet")),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            localDevice = tpLinkRoteador(statusWan = LocalDeviceSectionStatus.OK, quantidadeClientes = 3),
        )

        assertTrue(r.principal.id != "LOCAL-EQUIP-SATURACAO-01")
    }

    // -------------------------------------------------------------------------
    // Cenário 4: speedtest ruim + TP-Link WAN down -> falha upstream/local, sem afirmar fibra
    // -------------------------------------------------------------------------

    @Test
    fun `TP-Link com WAN indisponivel mais internet ruim - LOCAL-EQUIP-WAN-01 vence e nao afirma fibra`() {
        val r = FindingEngine.analisar(
            internetResultados = listOf(resultado("IN-CRITICO", DiagnosticStatus.critical, "internet")),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            localDevice = tpLinkRoteador(statusWan = LocalDeviceSectionStatus.INDISPONIVEL),
        )

        assertEquals("LOCAL-EQUIP-WAN-01", r.principal.id)
        assertEquals(DiagnosticStatus.critical, r.principal.status)
        assertFalse(
            "roteador sem dados de fibra nao pode afirmar problema de fibra",
            r.principal.recomendacao!!.contains("fibra", ignoreCase = true),
        )
    }

    // -------------------------------------------------------------------------
    // Cenário 5: TP-Link sem dados de fibra -> diagnóstico declara limitação
    // -------------------------------------------------------------------------

    @Test
    fun `TP-Link sem suporte a fibra declara limitacao sem virar achado principal quando tudo ok`() {
        val r = FindingEngine.analisar(
            internetResultados = emptyList(),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            localDevice = tpLinkRoteador(),
        )

        // Limitacao nao pode mascarar "Conexao Sem Problemas" quando tudo esta ok.
        assertEquals("DECISAO-04", r.principal.id)
        assertTrue(
            "limitacao de fibra do TP-Link deve ser declarada explicitamente",
            r.limitacoesEquipamentoLocal.any { it.contains("fibra", ignoreCase = true) },
        )
    }

    @Test
    fun `suporte experimental declara limitacao explicita`() {
        val r = FindingEngine.analisar(
            internetResultados = emptyList(),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            localDevice = tpLinkRoteador(supportLevel = SupportLevel.INFERRED_FAMILY),
        )

        assertTrue(
            r.limitacoesEquipamentoLocal.any { it.contains("experimental", ignoreCase = true) },
        )
    }

    @Test
    fun `equipamento LAB_VALIDATED sem outras limitacoes nao gera aviso de experimental`() {
        val r = FindingEngine.analisar(
            internetResultados = emptyList(),
            wifiQuality = WifiQualityResult(emptyList(), confiavelParaTeste = true),
            localDevice = ontOk(),
        )

        assertFalse(r.limitacoesEquipamentoLocal.any { it.contains("experimental", ignoreCase = true) })
    }
}
