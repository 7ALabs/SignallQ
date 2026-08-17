package io.signallq.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.lightTokens
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppShellNavigationComposeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `rememberSaveable restores root and overlay through registry recreation`() {
        val restoration = StateRestorationTester(composeRule)
        lateinit var navigator: AppShellNavigator
        restoration.setContent {
            navigator = rememberAppShellNavigator(AppShellMode.Guided2)
            Text(navigator.selectedRoot.name)
        }
        composeRule.runOnIdle {
            navigator.select(AppShellRoot.Tools)
            navigator.open(AppShellOverlay.Dns)
        }

        restoration.emulateSavedInstanceStateRestore()

        composeRule.runOnIdle {
            assertEquals(AppShellRoot.Tools, navigator.selectedRoot)
            assertEquals(AppShellOverlay.Dns, navigator.overlayStack.single())
        }
    }

    @Test
    fun `system back pops overlay then History returns Home`() {
        lateinit var navigator: AppShellNavigator
        composeRule.setContent {
            navigator = rememberAppShellNavigator(AppShellMode.Guided2)
            AppShellBackHandlers(navigator)
            Text(navigator.selectedRoot.name + navigator.overlayStack.joinToString())
        }
        composeRule.runOnIdle {
            navigator.select(AppShellRoot.History)
            navigator.open(AppShellOverlay.Perfil)
        }

        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.runOnIdle { assertEquals(emptyList<AppShellOverlay>(), navigator.overlayStack) }
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.runOnIdle { assertEquals(AppShellRoot.Home, navigator.selectedRoot) }
    }

    @Test
    fun `bottom bar exposes mode roots and respects feature flags`() {
        composeRule.setContent {
            CompositionLocalProvider(LocalLkTokens provides lightTokens()) {
                AppShellBottomBar(
                    c = lightTokens(),
                    mode = AppShellMode.Guided2,
                    selectedTab = 0,
                    featureFlags = AppShellFeatureFlagsState(historyEnabled = false),
                    onRootSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Início").assertExists()
        composeRule.onNodeWithText("Velocidade").assertExists()
        composeRule.onNodeWithText("Histórico").assertExists()
        composeRule.onNodeWithText("Histórico").assertIsNotEnabled()
        composeRule.onNodeWithText("Ferramentas").assertExists()
        composeRule.onNodeWithText("Sinal").assertDoesNotExist()
    }
    // ─── Delegação de back (GH#1704) ────────────────────────────────────────────
    //
    // Bloqueios 1 e 2 do parecer de Caio na PR #1710: os 9 testes de
    // `AppShellBackDelegacaoTest` chamam `consumirBackDoOverlayTopo()` DIRETO no navigator e
    // nenhum passa pelo `BackHandler`. Ele apagou a linha que liga a primitiva à produção
    // (`if (navigator.consumirBackDoOverlayTopo()) return@BackHandler`) e a suíte ficou verde —
    // a primitiva podia ser desconectada do app sem nada acusar. Estes testes fecham o caminho
    // real, via `onBackPressedDispatcher`.

    @Test
    fun `back real com interceptador que consome nao desempilha o overlay`() {
        lateinit var navigator: AppShellNavigator
        composeRule.setContent {
            navigator = rememberAppShellNavigator(AppShellMode.Guided2)
            AppShellBackHandlers(navigator)
            RegistrarBackDoOverlay(navigator, AppShellOverlay.DiagnosticoGuiado) { true }
            Text(navigator.selectedRoot.name + navigator.overlayStack.joinToString())
        }
        composeRule.runOnIdle { navigator.open(AppShellOverlay.DiagnosticoGuiado) }

        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.runOnIdle {
            assertEquals(listOf(AppShellOverlay.DiagnosticoGuiado), navigator.overlayStack)
        }
    }

    @Test
    fun `back real com interceptador que nao consome desempilha normalmente`() {
        // O outro lado do mesmo contrato: `false` tem que cair no `pop` de sempre. Sem este,
        // um interceptador que sempre consumisse prenderia o usuário dentro do overlay.
        lateinit var navigator: AppShellNavigator
        composeRule.setContent {
            navigator = rememberAppShellNavigator(AppShellMode.Guided2)
            AppShellBackHandlers(navigator)
            RegistrarBackDoOverlay(navigator, AppShellOverlay.DiagnosticoGuiado) { false }
            Text(navigator.selectedRoot.name + navigator.overlayStack.joinToString())
        }
        composeRule.runOnIdle { navigator.open(AppShellOverlay.DiagnosticoGuiado) }

        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.runOnIdle { assertEquals(emptyList<AppShellOverlay>(), navigator.overlayStack) }
    }

    @Test
    fun `overlay soterrado nao registra e o back fecha o de cima`() {
        // Bloqueio 2: remover a guarda `estaNoTopo` de `RegistrarBackDoOverlay` sobrevivia à
        // suíte. Aqui o guiado está EMBAIXO do Perfil e declara que consumiria o back — se ele
        // conseguir registrar, o Perfil nunca fecha e o usuário fica preso.
        lateinit var navigator: AppShellNavigator
        composeRule.setContent {
            navigator = rememberAppShellNavigator(AppShellMode.Guided2)
            AppShellBackHandlers(navigator)
            RegistrarBackDoOverlay(navigator, AppShellOverlay.DiagnosticoGuiado) { true }
            Text(navigator.selectedRoot.name + navigator.overlayStack.joinToString())
        }
        composeRule.runOnIdle {
            navigator.open(AppShellOverlay.DiagnosticoGuiado)
            navigator.open(AppShellOverlay.Perfil)
        }

        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.runOnIdle {
            assertEquals(listOf(AppShellOverlay.DiagnosticoGuiado), navigator.overlayStack)
        }
    }

    @Test
    fun `interceptador volta a valer quando o overlay reassume o topo`() {
        // Continuação: fechado o Perfil, o guiado volta ao topo, o efeito re-registra, e o back
        // seguinte passa a ser consumido. Trava o ciclo desregistro/re-registro do
        // `DisposableEffect`, que é o mecanismo que impede o vazamento no mapa.
        lateinit var navigator: AppShellNavigator
        composeRule.setContent {
            navigator = rememberAppShellNavigator(AppShellMode.Guided2)
            AppShellBackHandlers(navigator)
            RegistrarBackDoOverlay(navigator, AppShellOverlay.DiagnosticoGuiado) { true }
            Text(navigator.selectedRoot.name + navigator.overlayStack.joinToString())
        }
        composeRule.runOnIdle {
            navigator.open(AppShellOverlay.DiagnosticoGuiado)
            navigator.open(AppShellOverlay.Perfil)
        }
        // 1o back: fecha o Perfil (guiado soterrado não registrou).
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        // 2o back: guiado agora é o topo e consome.
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.runOnIdle {
            assertEquals(listOf(AppShellOverlay.DiagnosticoGuiado), navigator.overlayStack)
        }
    }

    @Test
    fun `overlay soterrado nao fica registrado no mapa`() {
        // Este é o teste que DE FATO mata o mutante da ressalva B2 (remover a guarda
        // `estaNoTopo`). Descobri isso rodando a mutação em vez de presumir: o teste de
        // comportamento acima (`overlay soterrado nao registra e o back fecha o de cima`)
        // continuava VERDE com a guarda removida, porque `consumirBackDoOverlayTopo` consulta por
        // chave do topo — um registro de overlay soterrado dá miss e não muda o back.
        //
        // O efeito real de remover a guarda é o mapa acumular entradas de overlays fora do topo:
        // vazamento silencioso, invisível para qualquer asserção de navegação. Por isso a
        // asserção aqui é sobre o mapa, não sobre a pilha.
        lateinit var navigator: AppShellNavigator
        composeRule.setContent {
            navigator = rememberAppShellNavigator(AppShellMode.Guided2)
            AppShellBackHandlers(navigator)
            RegistrarBackDoOverlay(navigator, AppShellOverlay.DiagnosticoGuiado) { true }
            Text(navigator.selectedRoot.name + navigator.overlayStack.joinToString())
        }
        composeRule.runOnIdle { navigator.open(AppShellOverlay.DiagnosticoGuiado) }
        composeRule.runOnIdle {
            assertEquals(setOf(AppShellOverlay.DiagnosticoGuiado), navigator.overlaysComInterceptador())
        }

        // Perfil por cima: o guiado sai do topo e o registro dele tem que sumir.
        composeRule.runOnIdle { navigator.open(AppShellOverlay.Perfil) }
        composeRule.runOnIdle {
            assertEquals(emptySet<AppShellOverlay>(), navigator.overlaysComInterceptador())
        }
    }

    @Test
    fun `troca de raiz desregistra o interceptador da raiz anterior`() {
        // A pilha é por raiz, o mapa é global (invariante documentada no KDoc de
        // RegistrarBackDoOverlay). O que impede a entrada de vazar entre raízes é o mesmo
        // predicado de topo — `overlayStack` lê `selectedTab` por snapshot, então trocar de raiz
        // recompõe e desregistra.
        lateinit var navigator: AppShellNavigator
        composeRule.setContent {
            navigator = rememberAppShellNavigator(AppShellMode.Guided2)
            AppShellBackHandlers(navigator)
            RegistrarBackDoOverlay(navigator, AppShellOverlay.DiagnosticoGuiado) { true }
            Text(navigator.selectedRoot.name + navigator.overlayStack.joinToString())
        }
        composeRule.runOnIdle { navigator.open(AppShellOverlay.DiagnosticoGuiado) }
        composeRule.runOnIdle {
            assertEquals(setOf(AppShellOverlay.DiagnosticoGuiado), navigator.overlaysComInterceptador())
        }

        composeRule.runOnIdle { navigator.select(AppShellRoot.Tools) }
        composeRule.runOnIdle {
            assertEquals(emptySet<AppShellOverlay>(), navigator.overlaysComInterceptador())
        }
    }

    @Test
    fun `interceptador atualizado e usado sem re-registro manual`() {
        // Ressalva R2: `rememberUpdatedState` no lugar de `onBack` nas chaves. O interceptador
        // captura estado que muda entre backs — se ficasse congelado na primeira lambda, o fluxo
        // travaria no passo em que estava, sem erro de compilação.
        lateinit var navigator: AppShellNavigator
        var passosRestantes by mutableIntStateOf(2)
        composeRule.setContent {
            navigator = rememberAppShellNavigator(AppShellMode.Guided2)
            AppShellBackHandlers(navigator)
            // `passosCapturados` é lido NA COMPOSIÇÃO e é inerte — a lambda captura um Int, não
            // o `MutableIntState`. Isso é o que faz o teste medir o que promete. Com
            // `passosRestantes` capturado direto (versão anterior), a lambda congelada continuava
            // lendo estado vivo, o congelamento não produzia sintoma, e o mutante
            // `registrarBackDoOverlay(overlay, onBack)` — a regressão que a R2 existe para
            // impedir — SOBREVIVIA. Ressalva R5 de Caio na PR #1710, e é a mesma lição do B2 um
            // nível acima: capturar estado observável neutraliza o mutante.
            val passosCapturados = passosRestantes
            RegistrarBackDoOverlay(navigator, AppShellOverlay.DiagnosticoGuiado) {
                if (passosCapturados > 0) {
                    passosRestantes = passosCapturados - 1
                    true
                } else {
                    false
                }
            }
            Text("passos=$passosRestantes" + navigator.overlayStack.joinToString())
        }
        composeRule.runOnIdle { navigator.open(AppShellOverlay.DiagnosticoGuiado) }

        repeat(2) {
            composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        }
        composeRule.runOnIdle {
            assertEquals("dois backs consumidos", 0, passosRestantes)
            assertEquals(listOf(AppShellOverlay.DiagnosticoGuiado), navigator.overlayStack)
        }

        // Terceiro back: o fluxo acabou, o interceptador devolve false e o overlay sai.
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.runOnIdle { assertEquals(emptyList<AppShellOverlay>(), navigator.overlayStack) }
    }
}
