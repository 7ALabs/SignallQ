package io.signallq.app.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.signallq.app.core.network.contracts.localdevice.DataFreshness
import io.signallq.app.core.network.contracts.localdevice.DeviceCapabilities
import io.signallq.app.core.network.contracts.localdevice.DeviceType
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.core.network.contracts.localdevice.SupportLevel
import io.signallq.app.feature.fibra.EstadoFibra
import io.signallq.app.feature.fibra.SnapshotFibra
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Teste de caracterização — task 2.0.16 (#1664, épico #1647). Cobre, no nível
 * da tela (não só da função pura já testada em [EquipamentoInternetUiStateTest]),
 * os estados listados na issue que ainda não tinham teste renderizado:
 * indisponível/sem suporte (modelo não reconhecido), offline, autenticação
 * requerida e dados parciais. `loading` (idle) e `online` (conectado
 * completo) já são cobertos por [EquipamentoInternetScreenLoadingTest] e
 * [EquipamentoInternetScreenLayoutTest] respectivamente — não duplicados aqui.
 *
 * Escrito ANTES da mudança de decisão de produto (Luiz, 2026-08-19, item 1 —
 * orientação genérica útil para modelo não suportado, em vez de só avisar
 * "não suportado"): a primeira asserção do teste `modelo nao suportado`
 * documenta o texto ANTERIOR à mudança (baseline), e é atualizada junto da
 * implementação para o texto novo — ver comentário no teste.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class EquipamentoInternetScreenEstadosTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun snapshotErro(chave: String) =
        SnapshotFibra(estado = EstadoFibra.erro, gpon = null, wan = null, ppp = null, deviceInfo = null, erroMensagem = chave)

    private fun snapshotConcluido() =
        SnapshotFibra(estado = EstadoFibra.concluido, gpon = null, wan = null, ppp = null, deviceInfo = null, erroMensagem = null)

    @Test
    fun `credenciais necessarias mostra orientacao para configurar acesso`() {
        composeRule.setContent {
            SignallQTheme {
                EquipamentoInternetScreen(
                    snapshotFibra = snapshotConcluido(),
                    localDevice = null,
                    natStatus = null,
                    modemHost = null,
                    modemUsername = "",
                    modemPassword = "",
                    onVoltar = {},
                    onRetentar = {},
                    onAbrirAjustes = {},
                    onReiniciarEquipamento = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Configure o acesso ao equipamento").assertExists()
        composeRule.onNodeWithText("Revisar configurações").assertExists()
    }

    @Test
    fun `modelo nao suportado oferece orientacao generica util, nao so aviso`() {
        // SOMENTE_IDENTIFICACAO via falha de parsing da pagina de login (equipamento
        // provavelmente nao-Nokia) -- mesmo cenario coberto na funcao pura por
        // EquipamentoInternetUiStateTest.`falha de parsing da pagina de login vira
        // SOMENTE_IDENTIFICACAO`.
        composeRule.setContent {
            SignallQTheme {
                EquipamentoInternetScreen(
                    snapshotFibra = snapshotErro("erroRespostaModemInvalida"),
                    localDevice = null,
                    natStatus = null,
                    modemHost = "192.168.0.1",
                    modemUsername = "admin",
                    modemPassword = "admin",
                    onVoltar = {},
                    onRetentar = {},
                    onAbrirAjustes = {},
                    onReiniciarEquipamento = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Equipamento não suportado").assertExists()
        // Decisão de produto (Luiz, 2026-08-19, item 1): orientação genérica útil
        // (como acessar configurações pelo navegador, ou contato de suporte) em vez
        // de só avisar "não suportado" -- as duas ações abaixo materializam isso.
        composeRule.onNodeWithText("Abrir configurações no navegador").assertExists()
        composeRule.onNodeWithText("Falar com o suporte").assertExists()
    }

    @Test
    fun `modelo nao suportado sem host configurado nao mostra abrir no navegador`() {
        // credenciaisConfiguradas pode ser true so por usuario/senha (host em
        // branco) -- ExternalActionLauncher.abrirView ja e defensivo contra URI
        // vazia, mas a tela nao deve nem oferecer o botao quando nao ha host.
        composeRule.setContent {
            SignallQTheme {
                EquipamentoInternetScreen(
                    snapshotFibra = snapshotErro("erroHostInvalido"),
                    localDevice = null,
                    natStatus = null,
                    modemHost = null,
                    modemUsername = "admin",
                    modemPassword = "admin",
                    onVoltar = {},
                    onRetentar = {},
                    onAbrirAjustes = {},
                    onReiniciarEquipamento = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Equipamento não suportado").assertExists()
        composeRule.onNodeWithText("Abrir configurações no navegador").assertDoesNotExist()
        composeRule.onNodeWithText("Falar com o suporte").assertExists()
    }

    @Test
    fun `offline sem janela pos-reboot orienta revisar IP usuario e senha`() {
        composeRule.setContent {
            SignallQTheme {
                EquipamentoInternetScreen(
                    snapshotFibra = snapshotErro("erroTimeout"),
                    localDevice = null,
                    natStatus = null,
                    modemHost = "192.168.1.1",
                    modemUsername = "admin",
                    modemPassword = "admin",
                    onVoltar = {},
                    onRetentar = {},
                    onAbrirAjustes = {},
                    onReiniciarEquipamento = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Não consegui acessar o equipamento agora").assertExists()
        composeRule.onNodeWithText("O equipamento está reiniciando").assertDoesNotExist()
    }

    @Test
    fun `autenticacao com credenciais invalidas orienta configurar acesso novamente`() {
        composeRule.setContent {
            SignallQTheme {
                EquipamentoInternetScreen(
                    snapshotFibra = snapshotErro("erroCredenciaisInvalidas"),
                    localDevice = null,
                    natStatus = null,
                    modemHost = "192.168.1.1",
                    modemUsername = "admin",
                    modemPassword = "errada",
                    onVoltar = {},
                    onRetentar = {},
                    onAbrirAjustes = {},
                    onReiniciarEquipamento = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Configure o acesso ao equipamento").assertExists()
    }

    @Test
    fun `dados parciais mostra aviso de leitura parcial no conteudo conectado`() {
        val device =
            LocalNetworkDeviceSnapshot(
                deviceType = DeviceType.ONT_GPON,
                supportLevel = SupportLevel.LAB_VALIDATED,
                capabilities = DeviceCapabilities(suportaFibra = true, suportaWan = true),
                vendor = "Nokia",
                modelo = "G-1425G-B",
                firmwareVersion = null,
                // suportaFibra = true mas fiber = null -> dadosParciais() == true (LocalDeviceSection.kt)
                fiber = null,
                wan = null,
                wifi = null,
                lan = null,
                freshness = DataFreshness(capturadoEmEpochMs = 1_000L),
            )
        composeRule.setContent {
            SignallQTheme {
                EquipamentoInternetScreen(
                    snapshotFibra = snapshotConcluido(),
                    localDevice = device,
                    natStatus = null,
                    modemHost = "192.168.1.1",
                    modemUsername = "admin",
                    modemPassword = "admin",
                    onVoltar = {},
                    onRetentar = {},
                    onAbrirAjustes = {},
                    onReiniciarEquipamento = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Texto completo do card de aviso (EquipamentoInternetScreen.kt) -- "Leitura
        // parcial" sozinho tambem aparece no rotulo de status/acesso do card
        // principal, entao o teste casa a frase inteira pra nao colidir com eles.
        composeRule
            .onNodeWithText(
                "Leitura parcial — algumas seções deste equipamento não vieram preenchidas nesta captura.",
            ).assertExists()
    }
}
