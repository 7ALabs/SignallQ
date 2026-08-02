package io.signallq.app.ui

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.signallq.app.core.network.FeatureFlagProvider
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
 * diretorio remoto encontrado, e nenhum dos dois (fallback generico). Cobre
 * tambem GH#1464 (parte de #951): propagacao do `status` do worker para
 * [ResolvedOperadoraIdentity]/[ResolvedOperadoraContact], e o kill switch
 * `feature_provider_directory_enabled`.
 */
class OperadoraDirectoryResolverTest {
    private fun remoteInfo(
        id: String = "regional_teste",
        logoUrl: String? = "https://assets.signallq.com/providers/regional_teste/logo-square-v1.webp",
        sacPhone: String? = "10000",
        whatsapp: String? = "https://wa.me/5511999999999",
        website: String? = "https://regional.example.com",
        status: String? = "VERIFIED",
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
        status = status,
    )

    /** Flag sempre no valor informado — usado pra cobrir os dois cenarios do kill switch. */
    private fun flagProvider(providerDirectoryEnabled: Boolean): FeatureFlagProvider =
        object : FeatureFlagProvider {
            override fun isEnabled(key: String): Boolean =
                if (key == "feature_provider_directory_enabled") providerDirectoryEnabled else true
        }

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

    // ── GH#1464 (parte de #951): status de curadoria (verificado/nao verificado) ───────────

    @Test
    fun `remoto com status VERIFIED nao e marcado como nao verificado`() =
        runTest {
            val repo = mockk<ProviderDirectoryRepository>()
            coEvery { repo.searchByName("Regional Verificada") } returns remoteInfo(status = "VERIFIED")
            val resolver = OperadoraDirectoryResolver(repo)

            val identidade = resolver.resolveIdentity("Regional Verificada")
            assertEquals("VERIFIED", identidade.status)
            assertFalse(identidade.isNaoVerificado)

            val contato = resolver.resolveContact("Regional Verificada")
            assertEquals("VERIFIED", contato.status)
            assertFalse(contato.isNaoVerificado)
        }

    @Test
    fun `remoto com status PENDING_REVIEW e marcado como nao verificado`() =
        runTest {
            val repo = mockk<ProviderDirectoryRepository>()
            coEvery { repo.searchByName("Regional Pendente") } returns remoteInfo(status = "PENDING_REVIEW")
            val resolver = OperadoraDirectoryResolver(repo)

            val identidade = resolver.resolveIdentity("Regional Pendente")
            assertEquals("PENDING_REVIEW", identidade.status)
            assertTrue(identidade.isNaoVerificado)

            val contato = resolver.resolveContact("Regional Pendente")
            assertEquals("PENDING_REVIEW", contato.status)
            assertTrue(contato.isNaoVerificado)
        }

    @Test
    fun `remoto sem status (nulo) e tratado como nao verificado`() =
        runTest {
            val repo = mockk<ProviderDirectoryRepository>()
            coEvery { repo.searchByName("Regional Sem Status") } returns remoteInfo(status = null)
            val resolver = OperadoraDirectoryResolver(repo)

            val contato = resolver.resolveContact("Regional Sem Status")
            assertNull(contato.status)
            assertTrue(contato.isNaoVerificado)
        }

    @Test
    fun `operadora local nunca e marcada como nao verificada, mesmo sem status`() =
        runTest {
            val repo = mockk<ProviderDirectoryRepository>()
            val resolver = OperadoraDirectoryResolver(repo)

            val contato = resolver.resolveContact("Claro")
            assertEquals(OperadoraSource.LOCAL, contato.source)
            assertNull(contato.status)
            assertFalse(contato.isNaoVerificado)
        }

    // ── GH#1464 (parte de #951): kill switch feature_provider_directory_enabled ────────────

    @Test
    fun `flag ligada consulta o diretorio remoto normalmente`() =
        runTest {
            val repo = mockk<ProviderDirectoryRepository>()
            coEvery { repo.searchByName("Regional Teste ISP") } returns remoteInfo()
            val resolver = OperadoraDirectoryResolver(repo, flagProvider(providerDirectoryEnabled = true))

            val identidade = resolver.resolveIdentity("Regional Teste ISP")
            assertEquals(OperadoraSource.REMOTE, identidade.source)
            coVerify(exactly = 1) { repo.searchByName("Regional Teste ISP") }
        }

    @Test
    fun `flag desligada pula o diretorio remoto e cai direto no fallback, sem chamar o repositorio`() =
        runTest {
            val repo = mockk<ProviderDirectoryRepository>()
            coEvery { repo.searchByName(any()) } returns remoteInfo()
            val resolver = OperadoraDirectoryResolver(repo, flagProvider(providerDirectoryEnabled = false))

            val identidade = resolver.resolveIdentity("Regional Teste ISP")
            assertEquals(OperadoraSource.FALLBACK, identidade.source)

            val contato = resolver.resolveContact("Regional Teste ISP")
            assertEquals(OperadoraSource.FALLBACK, contato.source)
            assertFalse(contato.hasAnyContact)

            coVerify(exactly = 0) { repo.searchByName(any()) }
        }

    @Test
    fun `flag desligada preserva catalogo local (nivel 1) funcionando normalmente`() =
        runTest {
            val repo = mockk<ProviderDirectoryRepository>()
            val resolver = OperadoraDirectoryResolver(repo, flagProvider(providerDirectoryEnabled = false))

            val contato = resolver.resolveContact("Claro")
            assertEquals(OperadoraSource.LOCAL, contato.source)
            assertEquals("10621", contato.sacPhone)
        }
}
