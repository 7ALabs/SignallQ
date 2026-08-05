package io.signallq.app.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.wifi.EstadoScanWifi
import io.signallq.app.core.network.wifi.SnapshotScanWifi
import io.signallq.app.core.telephony.MovelSimSnapshot
import io.signallq.app.core.telephony.MovelSnapshot
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Teste de caracterizacao da aba Movel/SIM de [SinalScreen] — exigido pela regra local de
 * higiene (`.claude/rules/higiene-e-padronizacao-repositorio.md`, secao 4.8b) antes de
 * introduzir a chamada assincrona ao `OperadoraDirectoryResolver.resolveIdentity` nesta aba
 * (ver issue de investigacao original, 2026-08-05, e ressalva de revisao do Caio sobre cobrir
 * tambem `MobileSnapshotCard`, nao so `SimCard`).
 *
 * BASELINE (esta classe, neste commit, roda contra o `SinalScreen.kt` AINDA sem a correcao —
 * ver historico do git: este e o primeiro commit da branch, antes do commit funcional):
 *  - Operadora presente no catalogo local ([io.signallq.app.ui.BancoOperadoras]) exibe o logo
 *    bundled (`Image` com `contentDescription` = nome da operadora) via
 *    `OperadoraBadge(operadora: ContatoOperadora, ...)` — vale tanto no caminho `SimCard`
 *    (`simsAtivos` nao vazio) quanto no caminho `MobileSnapshotCard` (`simsAtivos` vazio, so
 *    `movelSnapshot`).
 *  - Operadora AUSENTE do catalogo local nunca tenta o diretorio remoto:
 *    - em `SimCard`, cai no placeholder generico e estatico (`PlaceholderOperadoraBadge`,
 *      texto fixo "logo");
 *    - em `MobileSnapshotCard`, o placeholder e INCONDICIONAL — nem tenta o catalogo local,
 *      sempre "logo", mesmo pra operadora conhecida (ex.: "Vivo").
 *
 * O commit seguinte desta branch aplica a correcao (cadeia local -> diretorio remoto ->
 * fallback, via `OperadoraDirectoryResolver`, mesmo padrao de `HomeScreen`/
 * `DiagnosticoGuiadoScreen`) nos dois caminhos, e as asercoes deste arquivo sao atualizadas
 * para refletir o novo comportamento — a intencao e documentar no historico do git que a
 * baseline foi capturada e confirmada (`BUILD SUCCESSFUL`) ANTES da mudanca funcional.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SinalMovelOperadoraBadgeCaracterizacaoTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val snapshotWifiVazio =
        SnapshotScanWifi(estado = EstadoScanWifi.concluido, redes = emptyList(), erroMensagem = null)

    // BASELINE: `SinalScreen` ainda nao recebe `resolveOperadoraIdentidadeLocal`/
    // `resolveOperadoraIdentidadeRemota` neste commit — esses seams so existem a partir do
    // commit funcional seguinte desta branch.
    private fun renderAbaMovel(
        movelSnapshot: MovelSnapshot?,
        simsAtivos: List<MovelSimSnapshot>,
    ) {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.movel,
                    movelSnapshot = movelSnapshot,
                    simsAtivos = simsAtivos,
                    temPermissaoTelefonia = true,
                    temPermissaoLocalizacao = true,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
    }

    private fun movelSnapshot(operadora: String) =
        MovelSnapshot(
            operadora = operadora,
            tecnologia = "4G",
            rsrpDbm = -90,
            rsrqDb = -10,
            sinrDb = 5,
            ecnoDb = null,
            bandaMovel = "B3 (1800 MHz)",
            cellId = null,
            mcc = "724",
            mnc = "06",
            tac = null,
            roaming = false,
            timestampMs = 0L,
        )

    // ── Caminho SimCard (simsAtivos nao vazio) ─────────────────────────────────

    @Test
    fun `simCard - operadora conhecida no catalogo local exibe o logo bundled com content description do nome`() {
        renderAbaMovel(
            movelSnapshot = movelSnapshot("Vivo"),
            simsAtivos =
                listOf(
                    MovelSimSnapshot(
                        subId = 1,
                        simIndex = 1,
                        operadora = "Vivo",
                        tecnologiaRede = "4G",
                        rsrpDbm = -90,
                        emRoaming = false,
                        isDefaultData = true,
                    ),
                ),
        )
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Vivo").assertExists()
        composeRule.onNodeWithText("logo").assertDoesNotExist()
    }

    @Test
    fun `simCard - operadora ausente do catalogo local sempre caia no placeholder estatico antes da correcao`() {
        renderAbaMovel(
            movelSnapshot = movelSnapshot("Operadora Regional Desconhecida XYZ"),
            simsAtivos =
                listOf(
                    MovelSimSnapshot(
                        subId = 2,
                        simIndex = 1,
                        operadora = "Operadora Regional Desconhecida XYZ",
                        tecnologiaRede = "4G",
                        rsrpDbm = -95,
                        emRoaming = false,
                        isDefaultData = true,
                    ),
                ),
        )
        composeRule.waitForIdle()

        // BASELINE (antes da correcao): sem match local, o SimCard cai direto no placeholder
        // estatico "logo" — nunca tenta o diretorio remoto.
        composeRule.onNodeWithText("logo").assertExists()
        composeRule.onNodeWithContentDescription("Operadora Regional Desconhecida XYZ").assertDoesNotExist()
    }

    // ── Caminho MobileSnapshotCard (simsAtivos vazio, so movelSnapshot) ────────

    @Test
    fun `mobileSnapshotCard - placeholder incondicional mesmo com operadora conhecida antes da correcao`() {
        renderAbaMovel(
            movelSnapshot = movelSnapshot("Vivo"),
            simsAtivos = emptyList(),
        )
        composeRule.waitForIdle()

        // BASELINE (antes da correcao): MobileSnapshotCard nunca consulta nem o catalogo
        // local nem o diretorio remoto — sempre "logo", mesmo pra operadora conhecida.
        composeRule.onNodeWithText("logo").assertExists()
        composeRule.onNodeWithContentDescription("Vivo").assertDoesNotExist()
    }

    @Test
    fun `mobileSnapshotCard - placeholder incondicional para operadora desconhecida antes da correcao`() {
        renderAbaMovel(
            movelSnapshot = movelSnapshot("Operadora Regional Desconhecida XYZ"),
            simsAtivos = emptyList(),
        )
        composeRule.waitForIdle()

        composeRule.onNodeWithText("logo").assertExists()
        composeRule.onNodeWithContentDescription("Operadora Regional Desconhecida XYZ").assertDoesNotExist()
    }
}
