package io.signallq.app.ui.screen

import io.signallq.app.core.network.contracts.gateway.AcessoEquipamento
import io.signallq.app.core.network.contracts.localdevice.DataFreshness
import io.signallq.app.core.network.contracts.localdevice.DeviceCapabilities
import io.signallq.app.core.network.contracts.localdevice.DeviceType
import io.signallq.app.core.network.contracts.localdevice.FiberSnapshot
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.core.network.contracts.localdevice.SupportLevel
import io.signallq.app.feature.diagnostico.topology.model.NatStatus
import io.signallq.app.feature.fibra.EstadoFibra
import io.signallq.app.feature.fibra.GponStatus
import io.signallq.app.feature.fibra.SnapshotFibra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** GH#934, Fase 5 MD3 — cobre os 6 estados de [AcessoEquipamento] e a regra
 *  defensiva de Double NAT (nunca sinaliza com evidencia parcial/ausente). */
class EquipamentoInternetUiStateTest {
    private fun snapshotIdle() =
        SnapshotFibra(estado = EstadoFibra.idle, gpon = null, wan = null, ppp = null, deviceInfo = null, erroMensagem = null)

    private fun snapshotErro(chave: String) =
        SnapshotFibra(estado = EstadoFibra.erro, gpon = null, wan = null, ppp = null, deviceInfo = null, erroMensagem = chave)

    private fun snapshotConcluido() =
        SnapshotFibra(estado = EstadoFibra.concluido, gpon = null, wan = null, ppp = null, deviceInfo = null, erroMensagem = null)

    private fun localDevice(
        capabilities: DeviceCapabilities = DeviceCapabilities(suportaFibra = true),
        deviceType: DeviceType = DeviceType.ONT_GPON,
        supportLevel: SupportLevel = SupportLevel.LAB_VALIDATED,
        fiber: FiberSnapshot? =
            FiberSnapshot(
                linkAtivo = true,
                rxPowerDbm = -18.0,
                txPowerDbm = 2.0,
                temperaturaCelsius = 45.0,
                tensaoV = null,
                correnteLaserMa = null,
                serialOnt = null,
            ),
    ) = LocalNetworkDeviceSnapshot(
        deviceType = deviceType,
        supportLevel = supportLevel,
        capabilities = capabilities,
        vendor = "Nokia",
        modelo = "G-1425G-B",
        firmwareVersion = null,
        fiber = fiber,
        wan = null,
        wifi = null,
        lan = null,
        freshness = DataFreshness(capturadoEmEpochMs = 1_000L),
    )

    // ── Credenciais necessarias ────────────────────────────────────────────

    @Test
    fun `sem host usuario e senha configurados vira CREDENCIAIS_NECESSARIAS mesmo em idle`() {
        val acesso = mapAcessoEquipamento(snapshotIdle(), localDevice = null, modemHost = null, modemUsername = "", modemPassword = "")
        assertEquals(AcessoEquipamento.CREDENCIAIS_NECESSARIAS, acesso)
    }

    @Test
    fun `erro com err_t=1 vira CREDENCIAIS_NECESSARIAS`() {
        val acesso =
            mapAcessoEquipamento(
                snapshotErro("erroCredenciaisInvalidas"),
                localDevice = null,
                modemHost = "192.168.1.1",
                modemUsername = "admin",
                modemPassword = "errada",
            )
        assertEquals(AcessoEquipamento.CREDENCIAIS_NECESSARIAS, acesso)
    }

    // ── Sessao expirada ─────────────────────────────────────────────────────

    @Test
    fun `erro generico de comunicacao com credenciais configuradas vira SESSAO_EXPIRADA`() {
        val acesso =
            mapAcessoEquipamento(
                snapshotErro("erroTimeout"),
                localDevice = null,
                modemHost = "192.168.1.1",
                modemUsername = "admin",
                modemPassword = "certa",
            )
        assertEquals(AcessoEquipamento.SESSAO_EXPIRADA, acesso)
    }

    // ── Somente identificacao ───────────────────────────────────────────────

    @Test
    fun `falha de parsing da pagina de login vira SOMENTE_IDENTIFICACAO (provavel nao-Nokia)`() {
        val acesso =
            mapAcessoEquipamento(
                snapshotErro("erroRespostaModemInvalida"),
                localDevice = null,
                modemHost = "192.168.0.1",
                modemUsername = "admin",
                modemPassword = "admin",
            )
        assertEquals(AcessoEquipamento.SOMENTE_IDENTIFICACAO, acesso)
    }

    @Test
    fun `deviceType desconhecido apos leitura concluida vira SOMENTE_IDENTIFICACAO`() {
        val device = localDevice(deviceType = DeviceType.UNKNOWN_SUPPORTED, supportLevel = SupportLevel.UNKNOWN)
        val acesso =
            mapAcessoEquipamento(snapshotConcluido(), localDevice = device, modemHost = "10.0.0.1", modemUsername = "a", modemPassword = "b")
        assertEquals(AcessoEquipamento.SOMENTE_IDENTIFICACAO, acesso)
    }

    // ── Leitura parcial / completa / gerenciamento ──────────────────────────

    @Test
    fun `secao suportada sem dado nesta captura vira LEITURA_PARCIAL`() {
        val device = localDevice(capabilities = DeviceCapabilities(suportaFibra = true, suportaWan = true), fiber = null)
        val acesso =
            mapAcessoEquipamento(snapshotConcluido(), localDevice = device, modemHost = "192.168.1.1", modemUsername = "a", modemPassword = "b")
        assertEquals(AcessoEquipamento.LEITURA_PARCIAL, acesso)
    }

    @Test
    fun `leitura completa sem capability de gerenciamento vira LEITURA_COMPLETA`() {
        val device = localDevice(capabilities = DeviceCapabilities(suportaFibra = true, suportaGerenciamento = false))
        val acesso =
            mapAcessoEquipamento(snapshotConcluido(), localDevice = device, modemHost = "192.168.1.1", modemUsername = "a", modemPassword = "b")
        assertEquals(AcessoEquipamento.LEITURA_COMPLETA, acesso)
    }

    @Test
    fun `leitura completa com capability de gerenciamento vira GERENCIAMENTO_DISPONIVEL`() {
        val device = localDevice(capabilities = DeviceCapabilities(suportaFibra = true, suportaGerenciamento = true))
        val acesso =
            mapAcessoEquipamento(snapshotConcluido(), localDevice = device, modemHost = "192.168.1.1", modemUsername = "a", modemPassword = "b")
        assertEquals(AcessoEquipamento.GERENCIAMENTO_DISPONIVEL, acesso)
    }

    // ── Double NAT ────────────────────────────────────────────────────────

    @Test
    fun `double nat so sinaliza com nat status DOUBLE_NAT_OR_CGNAT e ont fora de bridge`() {
        assertTrue(suspeitaDoubleNat(NatStatus.DOUBLE_NAT_OR_CGNAT, "IP_Routed"))
    }

    @Test
    fun `nao sinaliza quando nat status e so CGNAT`() {
        assertFalse(suspeitaDoubleNat(NatStatus.CGNAT, "IP_Routed"))
    }

    @Test
    fun `nao sinaliza quando ont esta em modo bridge`() {
        assertFalse(suspeitaDoubleNat(NatStatus.DOUBLE_NAT_OR_CGNAT, "Bridge"))
    }

    @Test
    fun `nao sinaliza quando modo da ont e desconhecido (falso negativo mais seguro)`() {
        assertFalse(suspeitaDoubleNat(NatStatus.DOUBLE_NAT_OR_CGNAT, null))
        assertFalse(suspeitaDoubleNat(NatStatus.DOUBLE_NAT_OR_CGNAT, "—"))
    }

    @Test
    fun `nao sinaliza quando nat status e nulo ou direto`() {
        assertFalse(suspeitaDoubleNat(null, "IP_Routed"))
        assertFalse(suspeitaDoubleNat(NatStatus.DIRECT_PUBLIC, "IP_Routed"))
    }

    // Referencia GponStatus so para deixar claro qual campo alimenta o "modo" acima
    // (documentacao viva do contrato, sem acoplar o teste ao parser real).
    @Test
    fun `campo mode do GponStatus e a fonte do parametro gponMode`() {
        val gpon =
            GponStatus(
                status = "up",
                mode = "IP_Routed",
                rxPowerDbm = -18.0,
                txPowerDbm = 2.0,
                temperatureCelsius = 45.0,
                serial = "X",
                voltageV = 0.0,
                laserCurrentMa = 0.0,
            )
        assertEquals("IP_Routed", gpon.mode)
    }
}
