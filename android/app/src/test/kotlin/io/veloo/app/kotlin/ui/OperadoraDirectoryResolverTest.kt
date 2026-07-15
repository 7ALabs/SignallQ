package io.signallq.app.ui

import io.mockk.coEvery
import io.mockk.mockk
import io.signallq.app.feature.diagnostico.remote.ProviderDirectoryRepository
import io.signallq.app.feature.diagnostico.remote.RemoteProviderInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre os 3 niveis de fallback do GH#965: catalogo local encontrado, so
 * diretorio remoto encontrado, e nenhum dos dois (fallback generico).
 */
class OperadoraDirectoryResolverTest {
    private fun remoteInfo(
        id: String = "regional_teste",
        logoUrl: String? = "https://assets.signallq.com/providers/regional_teste/logo-square-v1.webp",
        sacPhone: String? = "10000",
        whatsapp: String? = "https://wa.me/5511999999999",
        website: String? = "https://regional.example.com",
    ) = RemoteProviderInfo(
        providerId = id,
        displayName = "Regional Teste",
        logoUrl = logoUrl,
        sacPhone = sacPhone,
        technicalSupportPhone = null,
        whatsappUrl = whatsapp,
        websiteUrl = website,
        customerAreaUrl = null,
        ombudsmanPhone = null,
    )

    // ── Nivel 1: catalogo local ────────────────────────────────────────────

    @Test
    fun `operadora principal resolve totalmente local, nunca chama o repositorio remoto`() =
        runTest {
            val repo = mockk<ProviderDirectoryRepository>()
            val resolver = OperadoraDirectoryResolver(repo)

            val identidade = resolver.resolveIdentity("Claro")
            assertEquals(OperadoraSource.LOCAL, identidade.source)
            assertEquals("C", identidade.monograma)
            assertTrue(identidade.logoRes != null)
            assertNull(identidade.logoUrl)

            val contato = resolver.resolveContact("Claro")
            assertEquals(OperadoraSource.LOCAL, contato.source)
            assertEquals("10621", contato.sacPhone)
            assertTrue(contato.hasAnyContact)
        }

    @Test
    fun `operadora movel principal usa resolverMovel quando viaMovel=true`() =
        runTest {
            val repo = mockk<ProviderDirectoryRepository>()
            val resolver = OperadoraDirectoryResolver(repo)

            val contato = resolver.resolveContact("TIMBRASIL", viaMovel = true)
            assertEquals(OperadoraSource.LOCAL, contato.source)
            assertEquals("TIM", contato.displayName)
        }

    // ── Nivel 2: diretorio remoto (so quando local nao achou) ──────────────

    @Test
    fun `operadora nao catalogada localmente com match remoto resolve via worker`() =
        runTest {
            val repo = mockk<ProviderDirectoryRepository>()
            coEvery { repo.searchByName("Regional Teste ISP") } returns remoteInfo()
            val resolver = OperadoraDirectoryResolver(repo)

            val identidade = resolver.resolveIdentity("Regional Teste ISP")
            assertEquals(OperadoraSource.REMOTE, identidade.source)
            assertEquals("https://assets.signallq.com/providers/regional_teste/logo-square-v1.webp", identidade.logoUrl)
            assertNull(identidade.logoRes)

            val contato = resolver.resolveContact("Regional Teste ISP")
            assertEquals(OperadoraSource.REMOTE, contato.source)
            assertEquals("10000", contato.sacPhone)
            assertEquals("https://wa.me/5511999999999", contato.whatsapp)
            assertTrue(contato.hasAnyContact)
        }

    @Test
    fun `remoto encontrado mas sem logoUrl nao inventa logo - identidade cai pro fallback generico`() =
        runTest {
            val repo = mockk<ProviderDirectoryRepository>()
            coEvery { repo.searchByName("Regional Sem Logo") } returns remoteInfo(logoUrl = null)
            val resolver = OperadoraDirectoryResolver(repo)

            val identidade = resolver.resolveIdentity("Regional Sem Logo")
            assertEquals(OperadoraSource.FALLBACK, identidade.source)
            assertNull(identidade.logoRes)
            assertNull(identidade.logoUrl)
        }

    // ── Nivel 3: fallback final (nem local, nem remoto) ─────────────────────

    @Test
    fun `nao encontrado em lugar nenhum cai pro fallback generico, nunca quebra`() =
        runTest {
            val repo = mockk<ProviderDirectoryRepository>()
            coEvery { repo.searchByName(any()) } returns null
            val resolver = OperadoraDirectoryResolver(repo)

            val identidade = resolver.resolveIdentity("Provedor Totalmente Desconhecido")
            assertEquals(OperadoraSource.FALLBACK, identidade.source)
            assertNull(identidade.logoRes)
            assertNull(identidade.logoUrl)

            val contato = resolver.resolveContact("Provedor Totalmente Desconhecido")
            assertEquals(OperadoraSource.FALLBACK, contato.source)
            assertFalse(contato.hasAnyContact)
            assertNull(contato.sacPhone)
        }

    @Test
    fun `nome nulo ou em branco nunca chama o repositorio remoto e cai direto no fallback`() =
        runTest {
            val repo = mockk<ProviderDirectoryRepository>()
            val resolver = OperadoraDirectoryResolver(repo)

            val identidade = resolver.resolveIdentity(null)
            assertEquals(OperadoraSource.FALLBACK, identidade.source)

            val contato = resolver.resolveContact("   ")
            assertEquals(OperadoraSource.FALLBACK, contato.source)
            assertFalse(contato.hasAnyContact)
        }

    @Test
    fun `falha do repositorio remoto (retorno null) nunca lanca excecao, cai pro fallback`() =
        runTest {
            val repo = mockk<ProviderDirectoryRepository>()
            coEvery { repo.searchByName(any()) } returns null
            val resolver = OperadoraDirectoryResolver(repo)

            val identidade = resolver.resolveIdentity("Sem Rede Nenhuma")
            assertEquals(OperadoraSource.FALLBACK, identidade.source)
        }
}
