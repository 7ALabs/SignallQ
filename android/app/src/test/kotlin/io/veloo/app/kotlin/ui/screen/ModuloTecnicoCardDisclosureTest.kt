package io.signallq.app.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.signallq.app.ui.SignallQTheme
import io.signallq.app.ui.component.EquipamentoItemTecnico
import io.signallq.app.ui.component.EquipamentoSecaoTecnica
import io.signallq.app.ui.lightTokens
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Teste de caracterização da disclosure progressiva genérica em ModuloTecnicoCard
 * (PR #1364, revisão Lia 2026-07-24).
 *
 * Regra: qualquer seção com mais de 2 itens nasce colapsada, mostrando só os
 * 2 primeiros; seção com ≤2 itens nasce expandida. O toggle "Ver detalhes
 * técnicos" aparece só quando há >2 itens.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ModuloTecnicoCardDisclosureTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val tokens = lightTokens()

    private fun assertNodeDoesNotExist(text: String) {
        try {
            composeRule.onNodeWithText(text).assertIsDisplayed()
            Assert.fail(
                "Node with text '$text' should not exist, but was found",
            )
        } catch (
            @Suppress("SwallowedException") e: AssertionError,
        ) {
            // esperado — nó não existe ou não é exibido
        }
    }

    private fun secaoComDoisItens(): EquipamentoSecaoTecnica =
        EquipamentoSecaoTecnica(
            titulo = "Rede local (LAN)",
            icone = Icons.Outlined.Devices,
            itens =
                listOf(
                    EquipamentoItemTecnico(label = "IP do roteador", valor = "192.168.1.1"),
                    EquipamentoItemTecnico(label = "Máscara", valor = "255.255.255.0"),
                ),
            overline = null,
            clientes = emptyList(),
            trailing = null,
        )

    private fun secaoComQuatroItens(): EquipamentoSecaoTecnica =
        EquipamentoSecaoTecnica(
            titulo = "Internet (WAN)",
            icone = Icons.Outlined.Devices,
            itens =
                listOf(
                    EquipamentoItemTecnico(label = "IP WAN", valor = "203.0.113.42"),
                    EquipamentoItemTecnico(label = "Gateway", valor = "203.0.113.1"),
                    EquipamentoItemTecnico(label = "DNS primário", valor = "8.8.8.8"),
                    EquipamentoItemTecnico(label = "DNS secundário", valor = "1.1.1.1"),
                ),
            overline = null,
            clientes = emptyList(),
            trailing = null,
        )

    private fun secaoComUmItem(): EquipamentoSecaoTecnica =
        EquipamentoSecaoTecnica(
            titulo = "Teste",
            icone = Icons.Outlined.Devices,
            itens =
                listOf(
                    EquipamentoItemTecnico(label = "Campo único", valor = "valor"),
                ),
            overline = null,
            clientes = emptyList(),
            trailing = null,
        )

    @Test
    fun `secao com 2 itens nasce expandida e toggle nao aparece`() {
        composeRule.setContent {
            SignallQTheme {
                ModuloTecnicoCard(secao = secaoComDoisItens(), c = tokens)
            }
        }

        // Ambos os itens devem estar visíveis
        composeRule.onNodeWithText("IP do roteador").assertIsDisplayed()
        composeRule.onNodeWithText("192.168.1.1").assertIsDisplayed()
        composeRule.onNodeWithText("Máscara").assertIsDisplayed()
        composeRule.onNodeWithText("255.255.255.0").assertIsDisplayed()

        // Toggle "Ver detalhes técnicos" não deve ser exibido (pois tem exatamente 2 itens)
        assertNodeDoesNotExist("Ver detalhes técnicos")
    }

    @Test
    fun `secao com 1 item nasce expandida e toggle nao aparece`() {
        composeRule.setContent {
            SignallQTheme {
                ModuloTecnicoCard(secao = secaoComUmItem(), c = tokens)
            }
        }

        // O item deve estar visível
        composeRule.onNodeWithText("Campo único").assertIsDisplayed()
        composeRule.onNodeWithText("valor").assertIsDisplayed()

        // Toggle não deve existir (pois tem apenas 1 item)
        assertNodeDoesNotExist("Ver detalhes técnicos")
    }

    @Test
    fun `secao com 4 itens nasce colapsada mostrando apenas os 2 primeiros`() {
        composeRule.setContent {
            SignallQTheme {
                ModuloTecnicoCard(secao = secaoComQuatroItens(), c = tokens)
            }
        }

        // Os 2 primeiros itens devem estar visíveis
        composeRule.onNodeWithText("IP WAN").assertIsDisplayed()
        composeRule.onNodeWithText("203.0.113.42").assertIsDisplayed()
        composeRule.onNodeWithText("Gateway").assertIsDisplayed()
        composeRule.onNodeWithText("203.0.113.1").assertIsDisplayed()

        // Os 2 últimos itens NÃO devem estar visíveis (seção colapsada)
        assertNodeDoesNotExist("DNS primário")
        assertNodeDoesNotExist("8.8.8.8")
        assertNodeDoesNotExist("DNS secundário")
        assertNodeDoesNotExist("1.1.1.1")

        // Toggle deve existir e estar visível
        composeRule.onNodeWithText("Ver detalhes técnicos").assertIsDisplayed()
    }

    @Test
    fun `clicar no toggle Ver detalhes tecnico expande a secao mostrando todos os itens`() {
        composeRule.setContent {
            SignallQTheme {
                ModuloTecnicoCard(secao = secaoComQuatroItens(), c = tokens)
            }
        }

        // Estado inicial: apenas os 2 primeiros visíveis
        assertNodeDoesNotExist("DNS primário")

        // Clica no toggle
        composeRule.onNodeWithText("Ver detalhes técnicos").performClick()

        // Agora todos os 4 itens devem estar visíveis
        composeRule.onNodeWithText("IP WAN").assertIsDisplayed()
        composeRule.onNodeWithText("203.0.113.42").assertIsDisplayed()
        composeRule.onNodeWithText("Gateway").assertIsDisplayed()
        composeRule.onNodeWithText("203.0.113.1").assertIsDisplayed()
        composeRule.onNodeWithText("DNS primário").assertIsDisplayed()
        composeRule.onNodeWithText("8.8.8.8").assertIsDisplayed()
        composeRule.onNodeWithText("DNS secundário").assertIsDisplayed()
        composeRule.onNodeWithText("1.1.1.1").assertIsDisplayed()

        // Toggle deve mudar para "Ocultar"
        composeRule.onNodeWithText("Ocultar").assertIsDisplayed()
        assertNodeDoesNotExist("Ver detalhes técnicos")
    }

    @Test
    fun `clicar no toggle Ocultar colapsa a secao mostrando apenas os 2 primeiros itens`() {
        composeRule.setContent {
            SignallQTheme {
                ModuloTecnicoCard(secao = secaoComQuatroItens(), c = tokens)
            }
        }

        // Expande clicando em "Ver detalhes técnicos"
        composeRule.onNodeWithText("Ver detalhes técnicos").performClick()

        // Todos os itens visíveis
        composeRule.onNodeWithText("DNS primário").assertIsDisplayed()

        // Colapsa clicando em "Ocultar"
        composeRule.onNodeWithText("Ocultar").performClick()

        // Apenas os 2 primeiros visíveis novamente
        composeRule.onNodeWithText("IP WAN").assertIsDisplayed()
        composeRule.onNodeWithText("Gateway").assertIsDisplayed()
        assertNodeDoesNotExist("DNS primário")
        assertNodeDoesNotExist("DNS secundário")

        // Toggle volta para "Ver detalhes técnicos"
        composeRule.onNodeWithText("Ver detalhes técnicos").assertIsDisplayed()
        assertNodeDoesNotExist("Ocultar")
    }
}
