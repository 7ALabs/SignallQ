package io.signallq.app.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import io.mockk.coEvery
import io.mockk.mockk
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.wifi.EstadoScanWifi
import io.signallq.app.core.network.wifi.SnapshotScanWifi
import io.signallq.app.core.telephony.MovelSimSnapshot
import io.signallq.app.core.telephony.MovelSnapshot
import io.signallq.app.feature.diagnostico.remote.ProviderDirectoryRepository
import io.signallq.app.feature.diagnostico.remote.RemoteProviderInfo
import io.signallq.app.ui.OperadoraDirectoryResolver
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
 * Historico do git desta classe (rastreabilidade exigida na revisao do Caio):
 *  - Commit "test(android): caracteriza comportamento atual..." — esta mesma classe, SEM as
 *    asercoes de diretorio remoto abaixo, rodada e confirmada (`BUILD SUCCESSFUL`, 4/4) contra
 *    o `SinalScreen.kt` AINDA sem a correcao. Baseline documentada la: operadora conhecida
 *    exibia logo bundled so no `SimCard`; operadora desconhecida sempre caia no placeholder
 *    estatico "logo" nos dois caminhos (`SimCard` e `MobileSnapshotCard`); `MobileSnapshotCard`
 *    nem tentava o catalogo local, nem pra operadora conhecida.
 *  - Este commit (funcional, em cima do anterior): `SinalScreen.kt` passa a usar
 *    `OperadoraDirectoryResolver.resolveIdentity` (local -> diretorio remoto -> fallback,
 *    GH#965/#970, mesmo padrao de `HomeScreen`/`DiagnosticoGuiadoScreen`) nos dois caminhos
 *    (`SimCard` e `MobileSnapshotCard`). As asercoes abaixo refletem o novo comportamento e
 *    validam o criterio de aceite (operadora ausente do catalogo local mas presente no
 *    diretorio remoto nao trava mais no placeholder estatico).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SinalMovelOperadoraBadgeCaracterizacaoTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val snapshotWifiVazio =
        SnapshotScanWifi(estado = EstadoScanWifi.concluido, redes = emptyList(), erroMensagem = null)

    /** Mesma cadeia real usada em producao (via Hilt) — [ProviderDirectoryRepository] mockado
     *  porque o nivel 1 (catalogo local) nao depende dele; so os testes que exercitam o nivel 2
     *  (diretorio remoto) configuram [coEvery] nele. */
    private fun renderAbaMovel(
        movelSnapshot: MovelSnapshot?,
        simsAtivos: List<MovelSimSnapshot>,
        repo: ProviderDirectoryRepository = mockk(relaxed = true),
    ) {
        val resolver = OperadoraDirectoryResolver(repo)
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
                    resolveOperadoraIdentidadeLocal = resolver::resolveLocalIdentity,
                    resolveOperadoraIdentidadeRemota = resolver::resolveIdentity,
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

    private fun remoteInfoCom(
        logoUrl: String,
        displayName: String,
    ) =
        RemoteProviderInfo(
            providerId = "regional_remota",
            displayName = displayName,
            logoUrl = logoUrl,
            sacPhone = null,
            technicalSupportPhone = null,
            whatsappUrl = null,
            websiteUrl = null,
            customerAreaUrl = null,
            ombudsmanPhone = null,
            status = "VERIFIED",
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
    fun `simCard - operadora ausente do catalogo local e sem match remoto cai no fallback com monograma`() {
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
            repo =
                mockk<ProviderDirectoryRepository>().also {
                    coEvery { it.searchByName(any()) } returns null
                },
        )
        composeRule.waitForIdle()

        // Comportamento APOS a correcao: sem match local nem remoto, a cadeia completa cai
        // no fallback generico com monograma ("O") — nunca mais no placeholder estatico
        // "logo" permanente da baseline (commit anterior).
        composeRule.onNodeWithText("logo").assertDoesNotExist()
        composeRule.onNodeWithText("O").assertExists()
    }

    @Test
    fun `simCard - operadora ausente do catalogo local mas presente no diretorio remoto nao fica presa no placeholder`() {
        renderAbaMovel(
            movelSnapshot = movelSnapshot("Operadora Regional Remota"),
            simsAtivos =
                listOf(
                    MovelSimSnapshot(
                        subId = 3,
                        simIndex = 1,
                        operadora = "Operadora Regional Remota",
                        tecnologiaRede = "4G",
                        rsrpDbm = -95,
                        emRoaming = false,
                        isDefaultData = true,
                    ),
                ),
            repo =
                mockk<ProviderDirectoryRepository>().also {
                    coEvery { it.searchByName(any()) } returns
                        remoteInfoCom(
                            logoUrl = "https://assets.signallq.com/providers/regional_remota/logo-square-v1.webp",
                            displayName = "Operadora Regional Remota",
                        )
                },
        )
        composeRule.waitForIdle()

        // Criterio de aceite: operadora ausente do catalogo local mas presente no diretorio
        // remoto nao trava mais no placeholder estatico "logo".
        composeRule.onNodeWithText("logo").assertDoesNotExist()
    }

    // ── Caminho MobileSnapshotCard (simsAtivos vazio, so movelSnapshot) ────────

    @Test
    fun `mobileSnapshotCard - operadora conhecida no catalogo local exibe o logo bundled`() {
        renderAbaMovel(
            movelSnapshot = movelSnapshot("Vivo"),
            simsAtivos = emptyList(),
        )
        composeRule.waitForIdle()

        // Gap identificado na revisao do Caio: antes desta correcao, MobileSnapshotCard nunca
        // consultava o catalogo local, mesmo pra operadora conhecida. Agora resolve como
        // SimCard.
        composeRule.onNodeWithContentDescription("Vivo").assertExists()
        composeRule.onNodeWithText("logo").assertDoesNotExist()
    }

    @Test
    fun `mobileSnapshotCard - operadora ausente do catalogo local e sem match remoto cai no fallback com monograma`() {
        renderAbaMovel(
            movelSnapshot = movelSnapshot("Operadora Regional Desconhecida XYZ"),
            simsAtivos = emptyList(),
            repo =
                mockk<ProviderDirectoryRepository>().also {
                    coEvery { it.searchByName(any()) } returns null
                },
        )
        composeRule.waitForIdle()

        composeRule.onNodeWithText("logo").assertDoesNotExist()
        composeRule.onNodeWithText("O").assertExists()
    }

    @Test
    fun `mobileSnapshotCard - operadora ausente do catalogo local mas presente no diretorio remoto nao fica presa no placeholder`() {
        renderAbaMovel(
            movelSnapshot = movelSnapshot("Operadora Regional Remota"),
            simsAtivos = emptyList(),
            repo =
                mockk<ProviderDirectoryRepository>().also {
                    coEvery { it.searchByName(any()) } returns
                        remoteInfoCom(
                            logoUrl = "https://assets.signallq.com/providers/regional_remota/logo-square-v1.webp",
                            displayName = "Operadora Regional Remota",
                        )
                },
        )
        composeRule.waitForIdle()

        composeRule.onNodeWithText("logo").assertDoesNotExist()
    }
}
