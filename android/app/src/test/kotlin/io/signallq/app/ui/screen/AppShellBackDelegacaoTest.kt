package io.signallq.app.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Delegação de back do overlay do topo — issue #1704 (2.0.09b), decisão de arquitetura de Caio.
 *
 * Um fluxo interno de vários passos (o diagnóstico guiado 2.0) precisa recuar UM passo antes de o
 * overlay inteiro sair da pilha. As alternativas foram descartadas com motivo: 7 valores novos em
 * [AppShellOverlay] não funciona porque a pilha é set-like (não admite duplicata, e o fluxo é
 * cíclico) e overlays acumulam em vez de substituir; um navigation graph aninhado seria um segundo
 * motor de navegação, proibido.
 *
 * O que estes testes travam é o contrato mínimo: **só o topo é consultado**, e um "não consumi"
 * precisa cair no `pop` de sempre. Errar qualquer um dos dois produz back preso ou back que pula
 * uma tela — os dois invisíveis para a compilação.
 */
class AppShellBackDelegacaoTest {
    private fun navigatorComPilha(vararg overlays: AppShellOverlay): AppShellNavigator =
        AppShellNavigator(initialTab = AppShellRoot.Home.legacyIndex).apply {
            overlays.forEach { open(it) }
        }

    @Test
    fun `sem overlay na pilha nao ha o que consumir`() {
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Home.legacyIndex)
        assertFalse(navigator.consumirBackDoOverlayTopo())
    }

    @Test
    fun `overlay sem interceptador registrado nao consome`() {
        // Preserva o comportamento anterior à issue: overlay que não sabe de passos internos
        // continua saindo da pilha no primeiro back.
        val navigator = navigatorComPilha(AppShellOverlay.Perfil)
        assertFalse(navigator.consumirBackDoOverlayTopo())
    }

    @Test
    fun `interceptador que devolve true consome o back`() {
        val navigator = navigatorComPilha(AppShellOverlay.DiagnosticoGuiado)
        navigator.registrarBackDoOverlay(AppShellOverlay.DiagnosticoGuiado) { true }
        assertTrue(navigator.consumirBackDoOverlayTopo())
    }

    @Test
    fun `interceptador que devolve false deixa o back seguir para o pop`() {
        // É o fim do fluxo interno: o overlay declara que não tem mais passos e deve sair inteiro.
        val navigator = navigatorComPilha(AppShellOverlay.DiagnosticoGuiado)
        navigator.registrarBackDoOverlay(AppShellOverlay.DiagnosticoGuiado) { false }
        assertFalse(navigator.consumirBackDoOverlayTopo())
    }

    @Test
    fun `so o topo e consultado, nunca um overlay soterrado`() {
        // Mutante que este teste mata: iterar o mapa inteiro, ou consultar por "está na pilha" em
        // vez de "é o topo". Cenário real: Perfil aberto POR CIMA do diagnóstico guiado — o back
        // tem que fechar o Perfil, não recuar um passo do fluxo escondido atrás.
        var guiadoConsultado = false
        val navigator = navigatorComPilha(AppShellOverlay.DiagnosticoGuiado, AppShellOverlay.Perfil)
        navigator.registrarBackDoOverlay(AppShellOverlay.DiagnosticoGuiado) {
            guiadoConsultado = true
            true
        }
        assertFalse("Perfil esta no topo e nao registrou nada", navigator.consumirBackDoOverlayTopo())
        assertFalse("interceptador soterrado nao pode ser consultado", guiadoConsultado)
    }

    @Test
    fun `topo volta a ser consultado quando o overlay de cima sai`() {
        // Continuação do caso anterior: fechado o Perfil, o guiado volta ao topo e recupera a voz.
        val navigator = navigatorComPilha(AppShellOverlay.DiagnosticoGuiado, AppShellOverlay.Perfil)
        navigator.registrarBackDoOverlay(AppShellOverlay.DiagnosticoGuiado) { true }
        navigator.pop()
        assertTrue(navigator.consumirBackDoOverlayTopo())
    }

    @Test
    fun `desregistrar devolve o back ao comportamento padrao`() {
        // Sem isto, um overlay fechado seguiria segurando o back de quem ficou embaixo — o
        // `onDispose` de RegistrarBackDoOverlay depende deste caminho.
        val navigator = navigatorComPilha(AppShellOverlay.DiagnosticoGuiado)
        navigator.registrarBackDoOverlay(AppShellOverlay.DiagnosticoGuiado) { true }
        navigator.desregistrarBackDoOverlay(AppShellOverlay.DiagnosticoGuiado)
        assertFalse(navigator.consumirBackDoOverlayTopo())
    }

    @Test
    fun `interceptador e consultado a cada back, nao uma vez so`() {
        // O fluxo guiado tem N passos: o mesmo interceptador precisa responder `true` várias
        // vezes e `false` só no último. Um mutante que cacheasse a primeira resposta passaria
        // pelos testes acima e prenderia (ou soltaria) o back para sempre.
        val navigator = navigatorComPilha(AppShellOverlay.DiagnosticoGuiado)
        var passosRestantes = 2
        navigator.registrarBackDoOverlay(AppShellOverlay.DiagnosticoGuiado) {
            if (passosRestantes > 0) {
                passosRestantes -= 1
                true
            } else {
                false
            }
        }
        assertTrue(navigator.consumirBackDoOverlayTopo())
        assertTrue(navigator.consumirBackDoOverlayTopo())
        assertFalse("terceiro back deve liberar o pop", navigator.consumirBackDoOverlayTopo())
        assertEquals(0, passosRestantes)
    }

    @Test
    fun `registro e por raiz de navegacao, seguindo a pilha corrente`() {
        // A pilha do navigator é por raiz. Trocar de raiz muda o topo, e o interceptador do
        // overlay que ficou na outra raiz não pode responder por ela.
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Home.legacyIndex)
        navigator.open(AppShellOverlay.DiagnosticoGuiado)
        navigator.registrarBackDoOverlay(AppShellOverlay.DiagnosticoGuiado) { true }
        assertTrue(navigator.consumirBackDoOverlayTopo())

        navigator.select(AppShellRoot.Tools)
        assertFalse("outra raiz tem pilha propria e esta vazia", navigator.consumirBackDoOverlayTopo())
    }
}
