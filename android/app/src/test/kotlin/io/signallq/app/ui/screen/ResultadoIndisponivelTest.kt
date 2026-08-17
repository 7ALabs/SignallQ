package io.signallq.app.ui.screen

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.signallq.app.feature.speedtest.DiagnosticoFasesSpeedtest
import io.signallq.app.feature.speedtest.DiagnosticoQualidadeSpeedtest
import io.signallq.app.feature.speedtest.GargaloPrimario
import io.signallq.app.feature.speedtest.MeasurementStatus
import io.signallq.app.feature.speedtest.ModoSpeedtest
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import io.signallq.app.feature.speedtest.SeveridadeBufferbloat
import io.signallq.app.feature.speedtest.VereditoUso
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Estado vazio de resultado indisponível — issue #1714.
 *
 * O bug: `AppShellNavigator.Saver` faz a pilha de overlays sobreviver ao process death, mas o
 * `ResultadoSpeedtest` não sobrevive (vem de um `@Singleton` em memória). Na volta, três overlays
 * ficavam simultaneamente **na pilha** e **sem resultado**, e o `AnimatedVisibility` não compunha
 * nada — o usuário via a tela de trás e o back consumia um `pop()` invisível.
 *
 * O que estes testes travam é a inversão: a ausência de resultado deixou de **esconder** o overlay
 * e passou a **decidir o que mostrar**. O mutante que interessa é voltar a guarda para o `visible`
 * do `AnimatedVisibility` — que é literalmente o código anterior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ResultadoIndisponivelTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val titulo = "Este resultado não está mais disponível"

    // ─── A tela em si ──────────────────────────────────────────────────────────

    @Test
    fun `explica sem jargao e sem culpar o usuario`() {
        // A copy é o produto aqui: sem jargão, sem culpar o usuário. E enuncia a CONSEQUÊNCIA,
        // não a causa — afirmar "o aplicativo foi fechado em segundo plano" só seria verdade se
        // process death fosse o único caminho para chegar aqui, e ninguém provou isso.
        composeRule.setContent {
            SignallQTheme { ResultadoIndisponivelScreen(titulo = "Resultado", onVoltar = {}) }
        }
        composeRule.onNodeWithText(titulo).assertExists()
        composeRule.onNodeWithText("não ficam guardados", substring = true).assertExists()
    }

    @Test
    fun `o titulo e parametrizado por overlay`() {
        // Uma tela, três consumidores. Se o título fosse fixo, o usuário veria "Resultado" ao
        // voltar de Detalhes técnicos.
        composeRule.setContent {
            SignallQTheme { ResultadoIndisponivelScreen(titulo = "Detalhes da conexão", onVoltar = {}) }
        }
        composeRule.onNodeWithText("Detalhes da conexão").assertExists()
    }

    @Test
    fun `voltar aciona o callback`() {
        var voltou = false
        composeRule.setContent {
            SignallQTheme { ResultadoIndisponivelScreen(titulo = "Resultado", onVoltar = { voltou = true }) }
        }
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.runOnIdle { assertTrue(voltou) }
    }

    @Test
    fun `botao medir agora so aparece quando ha para onde ir`() {
        // `onMedirNovamente` é opcional de propósito: o Detalhes técnicos não tem destino de nova
        // medição, e oferecer um botão que não leva a lugar nenhum seria pior que não oferecer.
        composeRule.setContent {
            SignallQTheme { ResultadoIndisponivelScreen(titulo = "Detalhes da conexão", onVoltar = {}) }
        }
        composeRule.onNodeWithText("Medir agora").assertDoesNotExist()
    }

    @Test
    fun `botao medir agora aciona o callback quando oferecido`() {
        var mediu = false
        composeRule.setContent {
            SignallQTheme {
                ResultadoIndisponivelScreen(
                    titulo = "Resultado",
                    onVoltar = {},
                    onMedirNovamente = { mediu = true },
                )
            }
        }
        composeRule.onNodeWithText("Medir agora").performClick()
        composeRule.runOnIdle { assertTrue(mediu) }
    }

    // ─── A ligação no overlay: é aqui que o bug morava ─────────────────────────

    @Test
    fun `detalhes tecnicos na pilha sem resultado mostra o estado vazio, nao um container mudo`() {
        // ESTE é o teste do bug. Mutante: devolver `&& resultadoSpeedtest != null` ao `visible` do
        // AnimatedVisibility — o container volta a não compor e o `testTag` some.
        val stack = mutableStateListOf(AppShellOverlay.DetalhesTecnicos)
        composeRule.setContent {
            SignallQTheme {
                AppShellDetalhesTecnicosOverlay(
                    overlayStack = stack,
                    resultadoSpeedtest = null,
                    localizacaoServidor = null,
                    localDevice = null,
                )
            }
        }
        composeRule.onNodeWithTag("appshell_overlay_detalhes_tecnicos").assertExists()
        composeRule.onNodeWithText(titulo).assertExists()
    }

    @Test
    fun `voltar do estado vazio remove o overlay da pilha`() {
        // Fecha o outro lado do defeito: antes, o back consumia um `pop()` que não correspondia a
        // nada visível. Agora o usuário vê o que está fechando.
        val stack = mutableStateListOf(AppShellOverlay.DetalhesTecnicos)
        composeRule.setContent {
            SignallQTheme {
                AppShellDetalhesTecnicosOverlay(
                    overlayStack = stack,
                    resultadoSpeedtest = null,
                    localizacaoServidor = null,
                    localDevice = null,
                )
            }
        }
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.runOnIdle { assertFalse(AppShellOverlay.DetalhesTecnicos in stack) }
    }

    @Test
    fun `fora da pilha continua sem compor, mesmo sem resultado`() {
        // A inversão não pode ter ido longe demais: sem estar na pilha, nada aparece. Um mutante
        // que trocasse o `visible` por `true` passaria pelos testes acima e falharia aqui.
        val stack = mutableStateListOf<AppShellOverlay>()
        composeRule.setContent {
            SignallQTheme {
                AppShellDetalhesTecnicosOverlay(
                    overlayStack = stack,
                    resultadoSpeedtest = null,
                    localizacaoServidor = null,
                    localDevice = null,
                )
            }
        }
        composeRule.onNodeWithTag("appshell_overlay_detalhes_tecnicos").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(emptyList<AppShellOverlay>(), stack.toList()) }
    }

    // ─── O simétrico: mostrar o vazio na hora ERRADA seria catastrófico ────────

    @Test
    fun `com resultado presente NAO mostra o estado vazio e compoe a tela real`() {
        // Mutante que este teste mata: inverter o `if` — mostrar `ResultadoIndisponivelScreen`
        // quando o resultado EXISTE. Isso passava nos dois testes anteriores (o container compõe,
        // o texto do vazio aparece) e o usuário nunca veria resultado nenhum. Achado de Caio na
        // PR #1718, marcado como bloqueante — e ele tem razão: é a única falha desta mudança que
        // seria invisível em teste e devastadora em produção.
        val stack = mutableStateListOf(AppShellOverlay.DetalhesTecnicos)
        composeRule.setContent {
            SignallQTheme {
                AppShellDetalhesTecnicosOverlay(
                    overlayStack = stack,
                    resultadoSpeedtest = resultadoDeTeste(),
                    localizacaoServidor = "São Paulo, SP",
                    localDevice = null,
                )
            }
        }
        composeRule.onNodeWithTag("appshell_overlay_detalhes_tecnicos").assertExists()
        composeRule.onNodeWithText(titulo).assertDoesNotExist()
        composeRule.onNodeWithText("Detalhes da conexão").assertExists()
    }

    @Test
    fun `resultado que some com o overlay aberto troca para o estado vazio`() {
        // Ressalva 4 de Caio: antes, `&& resultado != null` no `visible` tornava o falso-positivo
        // estruturalmente impossível. Agora depende de nenhum caminho publicar `resultado = null`
        // com o overlay na pilha — invariante que hoje se sustenta e que ninguém guardava.
        //
        // Este teste guarda: com o overlay aberto e resultado presente, zerar o resultado tem que
        // trocar para o estado vazio, não deixar a tela antiga nem sumir com tudo.
        val stack = mutableStateListOf(AppShellOverlay.DetalhesTecnicos)
        val resultado = mutableStateOf<ResultadoSpeedtest?>(resultadoDeTeste())
        composeRule.setContent {
            SignallQTheme {
                AppShellDetalhesTecnicosOverlay(
                    overlayStack = stack,
                    resultadoSpeedtest = resultado.value,
                    localizacaoServidor = null,
                    localDevice = null,
                )
            }
        }
        composeRule.onNodeWithText("Detalhes da conexão").assertExists()

        composeRule.runOnIdle { resultado.value = null }

        composeRule.onNodeWithText(titulo).assertExists()
        composeRule.onNodeWithTag("appshell_overlay_detalhes_tecnicos").assertExists()
    }

    private fun resultadoDeTeste(): ResultadoSpeedtest =
        ResultadoSpeedtest(
            timestampEpochMs = 0L,
            specVersion = "1",
            modo = ModoSpeedtest.complete,
            connectionTypeStart = "wifi",
            connectionTypeEnd = "wifi",
            contaminado = false,
            latenciaMs = 10.0,
            jitterMs = 1.0,
            perdaPercentual = 0.0,
            bufferbloatMs = 5.0,
            severidadeBufferbloat = SeveridadeBufferbloat.none,
            downloadMbps = 100.0,
            uploadMbps = 50.0,
            latencyDownloadMs = 10.0,
            latencyUploadMs = 10.0,
            stabilityScore = 1.0,
            peakDownloadMbps = 110.0,
            peakUploadMbps = 55.0,
            packetLossSource = "download",
            dnsLatencyMs = null,
            dnsResolverIp = null,
            dnsProvider = null,
            diagnosticoQualidade =
                DiagnosticoQualidadeSpeedtest(
                    vereditoStreaming = VereditoUso.good,
                    vereditoGamer = VereditoUso.good,
                    vereditoVideoChamada = VereditoUso.good,
                    gargaloPrimario = GargaloPrimario.none,
                ),
            diagnosticoFases =
                DiagnosticoFasesSpeedtest(
                    faseInterrompida = "",
                    latenciaAmostrasTotais = 0,
                    latenciaAmostrasValidas = 0,
                    latenciaTimeouts = 0,
                    downloadBytesTotal = 0L,
                    downloadAmostrasValidas = 0,
                    downloadRequisicoesSucesso = 0,
                    downloadRequisicoesErro = 0,
                    downloadEncerradaPor = "",
                    downloadThroughputOrigem = "",
                    downloadUltimoErro = null,
                    uploadBytesTotal = 0L,
                    uploadAmostrasValidas = 0,
                    uploadRequisicoesSucesso = 0,
                    uploadRequisicoesErro = 0,
                    uploadEncerradaPor = "",
                    uploadThroughputOrigem = "",
                    uploadUltimoErro = null,
                    dnsErroMensagem = null,
                ),
            status = MeasurementStatus.COMPLETE,
        )
}
