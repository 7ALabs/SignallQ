package io.signallq.app.modogamer

import io.signallq.app.core.diagnostico.CatalogoJogosModoGamer
import io.signallq.app.core.diagnostico.CategoriaJogoModoGamer
import io.signallq.app.core.diagnostico.DeviceJogo
import io.signallq.app.core.diagnostico.DiagnosticInput
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes de navegação do [ModoGamerViewModel] (Feature #550, issue #1476) — as 4 etapas do
 * fluxo (jogo → device → config → resultado), fallback pra jogo fora do catálogo, "voltar" em
 * cada etapa e persistência do padrão só quando "Salvar como padrão" é escolhido.
 */
class ModoGamerViewModelTest {
    private val valorant = CatalogoJogosModoGamer.porId("valorant")!!

    private fun viewModel(
        padraoInicial: Pair<SelecaoJogoModoGamer, DeviceJogo>? = null,
        onSalvarPadrao: suspend (String?, String?, String) -> Unit = { _, _, _ -> },
    ) = ModoGamerViewModel(
        padraoInicial = padraoInicial,
        inputAtual = { DiagnosticInput() },
        onSalvarPadrao = onSalvarPadrao,
    )

    @Test
    fun `comeca na etapa de selecao de jogo quando nao ha padrao salvo`() {
        val vm = viewModel()
        assertTrue(vm.etapa.value is ModoGamerEtapa.SelecaoJogo)
    }

    @Test
    fun `busca atualiza o texto da etapa de selecao de jogo`() {
        val vm = viewModel()
        vm.buscar("valo")
        val etapa = vm.etapa.value as ModoGamerEtapa.SelecaoJogo
        assertEquals("valo", etapa.busca)
    }

    @Test
    fun `selecionar jogo catalogado avanca para selecao de device`() {
        val vm = viewModel()
        vm.selecionarJogo(valorant)
        val etapa = vm.etapa.value as ModoGamerEtapa.SelecaoDevice
        val selecao = etapa.selecaoJogo as SelecaoJogoModoGamer.Catalogado
        assertEquals("valorant", selecao.jogo.gameId)
        assertEquals(CategoriaJogoModoGamer.FPS_COMPETITIVO, selecao.categoria)
    }

    @Test
    fun `selecionar categoria fallback avanca para selecao de device sem erro`() {
        val vm = viewModel()
        vm.selecionarCategoriaFallback(CategoriaJogoModoGamer.MOBA)
        val etapa = vm.etapa.value as ModoGamerEtapa.SelecaoDevice
        val selecao = etapa.selecaoJogo as SelecaoJogoModoGamer.ForaDoCatalogo
        assertEquals(CategoriaJogoModoGamer.MOBA, selecao.categoria)
        assertEquals("MOBA", selecao.nomeExibido)
    }

    @Test
    fun `selecionar device avanca para config`() {
        val vm = viewModel()
        vm.selecionarJogo(valorant)
        vm.selecionarDevice(DeviceJogo.PC)
        val etapa = vm.etapa.value as ModoGamerEtapa.Config
        assertEquals(DeviceJogo.PC, etapa.device)
    }

    @Test
    fun `confirmar sem salvar como padrao nao chama onSalvarPadrao`() =
        runTest {
            var chamado = false
            val vm = viewModel(onSalvarPadrao = { _, _, _ -> chamado = true })
            vm.selecionarJogo(valorant)
            vm.selecionarDevice(DeviceJogo.PC)
            vm.confirmar(salvarComoPadrao = false)

            assertFalse(chamado)
            val etapa = vm.etapa.value as ModoGamerEtapa.Resultado
            assertFalse(etapa.salvoComoPadrao)
            assertEquals(CategoriaJogoModoGamer.FPS_COMPETITIVO, etapa.resultado.categoria)
        }

    @Test
    fun `confirmar salvando como padrao persiste jogoId e device`() =
        runTest {
            var jogoIdPersistido: String? = null
            var categoriaPersistida: String? = null
            var devicePersistido: String? = null
            val vm =
                viewModel(
                    onSalvarPadrao = { jogoId, categoria, device ->
                        jogoIdPersistido = jogoId
                        categoriaPersistida = categoria
                        devicePersistido = device
                    },
                )
            vm.selecionarJogo(valorant)
            vm.selecionarDevice(DeviceJogo.XBOX)
            vm.confirmar(salvarComoPadrao = true)

            assertEquals("valorant", jogoIdPersistido)
            assertNull(categoriaPersistida)
            assertEquals("XBOX", devicePersistido)
            assertTrue((vm.etapa.value as ModoGamerEtapa.Resultado).salvoComoPadrao)
        }

    @Test
    fun `confirmar com jogo fora do catalogo persiste categoriaFallback e jogoId nulo`() =
        runTest {
            var jogoIdPersistido: String? = "nao deveria mudar"
            var categoriaPersistida: String? = null
            val vm =
                viewModel(
                    onSalvarPadrao = { jogoId, categoria, _ ->
                        jogoIdPersistido = jogoId
                        categoriaPersistida = categoria
                    },
                )
            vm.selecionarCategoriaFallback(CategoriaJogoModoGamer.CASUAL)
            vm.selecionarDevice(DeviceJogo.SWITCH)
            vm.confirmar(salvarComoPadrao = true)

            assertNull(jogoIdPersistido)
            assertEquals("CASUAL", categoriaPersistida)
        }

    @Test
    fun `voltar para selecao de jogo reseta a busca`() {
        val vm = viewModel()
        vm.selecionarJogo(valorant)
        vm.selecionarDevice(DeviceJogo.PC)
        vm.voltarParaSelecaoJogo()
        assertEquals(ModoGamerEtapa.SelecaoJogo(), vm.etapa.value)
    }

    @Test
    fun `voltar para selecao de device a partir do config preserva o jogo escolhido`() {
        val vm = viewModel()
        vm.selecionarJogo(valorant)
        vm.selecionarDevice(DeviceJogo.PC)
        vm.voltarParaSelecaoDevice()
        val etapa = vm.etapa.value as ModoGamerEtapa.SelecaoDevice
        assertEquals("valorant", (etapa.selecaoJogo as SelecaoJogoModoGamer.Catalogado).jogo.gameId)
    }

    @Test
    fun `trocar jogo ou aparelho a partir do resultado volta para selecao de jogo`() =
        runTest {
            val vm = viewModel()
            vm.selecionarJogo(valorant)
            vm.selecionarDevice(DeviceJogo.PC)
            vm.confirmar(salvarComoPadrao = false)
            vm.trocarJogoOuDevice()
            assertTrue(vm.etapa.value is ModoGamerEtapa.SelecaoJogo)
        }

    // ── Padrão salvo (abre direto no resultado) ───────────────────────────────

    @Test
    fun `com padrao salvo comeca direto no resultado`() {
        val vm =
            viewModel(
                padraoInicial = SelecaoJogoModoGamer.Catalogado(valorant) to DeviceJogo.PLAYSTATION,
            )
        val etapa = vm.etapa.value as ModoGamerEtapa.Resultado
        assertTrue(etapa.salvoComoPadrao)
        assertEquals(DeviceJogo.PLAYSTATION, etapa.device)
        assertEquals("valorant", (etapa.selecaoJogo as SelecaoJogoModoGamer.Catalogado).jogo.gameId)
    }

    @Test
    fun `padrao salvo tambem permite trocar jogo ou aparelho`() {
        val vm =
            viewModel(
                padraoInicial = SelecaoJogoModoGamer.Catalogado(valorant) to DeviceJogo.PLAYSTATION,
            )
        vm.trocarJogoOuDevice()
        assertTrue(vm.etapa.value is ModoGamerEtapa.SelecaoJogo)
    }
}
