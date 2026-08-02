package io.signallq.app.feature.diagnostico.remote

import io.signallq.app.core.database.provider.ProviderDirectoryCacheDao
import io.signallq.app.core.database.provider.ProviderDirectoryCacheEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fake em memoria de [ProviderDirectoryCacheDao] — evita depender de Room instrumentado
 *  (`androidTest`) so pra validar o mapeamento Entity<->[RemoteProviderInfo] de
 *  [RoomProviderDirectoryCache], que e a unica responsabilidade real desta classe (o
 *  SQL do DAO em si e trivial demais pra justificar um teste instrumentado dedicado). */
private class FakeProviderDirectoryCacheDao : ProviderDirectoryCacheDao {
    val armazenado = mutableMapOf<String, ProviderDirectoryCacheEntity>()
    var falharNaProximaEscrita = false

    override suspend fun buscarPorChave(cacheKey: String): ProviderDirectoryCacheEntity? = armazenado[cacheKey]

    override suspend fun salvar(entidade: ProviderDirectoryCacheEntity) {
        if (falharNaProximaEscrita) throw IllegalStateException("falha simulada de escrita")
        armazenado[entidade.cacheKey] = entidade
    }
}

class RoomProviderDirectoryCacheTest {

    private fun info(
        providerId: String = "regional_teste",
        cacheVersion: Int? = 3,
        cacheExpiresAtMs: Long? = 1_800_000_000_000L,
    ) = RemoteProviderInfo(
        providerId = providerId,
        displayName = "Regional Teste",
        logoUrl = "https://assets.signallq.com/providers/$providerId/logo.webp",
        sacPhone = "10000",
        technicalSupportPhone = null,
        whatsappUrl = "https://wa.me/5511999999999",
        websiteUrl = "https://regional.example.com",
        customerAreaUrl = null,
        ombudsmanPhone = null,
        status = "VERIFIED",
        cacheVersion = cacheVersion,
        cacheExpiresAtMs = cacheExpiresAtMs,
    )

    @Test
    fun `get sem entrada previa devolve null (cache miss)`() = runTest {
        val cache = RoomProviderDirectoryCache(FakeProviderDirectoryCacheDao())
        assertNull(cache.get("id:desconhecido"))
    }

    @Test
    fun `put seguido de get devolve o mesmo RemoteProviderInfo (cache hit)`() = runTest {
        val cache = RoomProviderDirectoryCache(FakeProviderDirectoryCacheDao())
        val original = info()

        cache.put("id:regional_teste", original)
        val resultado = cache.get("id:regional_teste")

        assertEquals(original, resultado?.info)
    }

    @Test
    fun `put sobrescreve entrada existente para a mesma chave`() = runTest {
        val dao = FakeProviderDirectoryCacheDao()
        val cache = RoomProviderDirectoryCache(dao)

        cache.put("id:regional_teste", info(cacheVersion = 1))
        cache.put("id:regional_teste", info(cacheVersion = 2))

        assertEquals(2, cache.get("id:regional_teste")?.info?.cacheVersion)
        assertEquals(1, dao.armazenado.size)
    }

    @Test
    fun `isExpired verdadeiro quando cacheExpiresAtMs esta no passado`() = runTest {
        val cache = RoomProviderDirectoryCache(FakeProviderDirectoryCacheDao())
        cache.put("id:expirado", info(cacheExpiresAtMs = 1_000L))

        val entrada = cache.get("id:expirado")
        assertTrue(entrada!!.isExpired(nowMs = 2_000L))
    }

    @Test
    fun `isExpired falso quando cacheExpiresAtMs esta no futuro`() = runTest {
        val cache = RoomProviderDirectoryCache(FakeProviderDirectoryCacheDao())
        cache.put("id:valido", info(cacheExpiresAtMs = 5_000L))

        val entrada = cache.get("id:valido")
        assertTrue(!entrada!!.isExpired(nowMs = 2_000L))
    }

    @Test
    fun `isExpired sempre falso quando cacheExpiresAtMs e nulo`() = runTest {
        val cache = RoomProviderDirectoryCache(FakeProviderDirectoryCacheDao())
        cache.put("id:sem-expiracao", info(cacheExpiresAtMs = null))

        val entrada = cache.get("id:sem-expiracao")
        assertTrue(!entrada!!.isExpired(nowMs = Long.MAX_VALUE))
    }

    @Test
    fun `falha ao ler do dao nunca lanca excecao, devolve null`() = runTest {
        val dao = object : ProviderDirectoryCacheDao {
            override suspend fun buscarPorChave(cacheKey: String): ProviderDirectoryCacheEntity? =
                throw IllegalStateException("falha simulada de leitura")

            override suspend fun salvar(entidade: ProviderDirectoryCacheEntity) {}
        }
        val cache = RoomProviderDirectoryCache(dao)

        assertNull(cache.get("id:qualquer"))
    }

    @Test
    fun `falha ao gravar no dao nunca lanca excecao`() = runTest {
        val dao = FakeProviderDirectoryCacheDao().apply { falharNaProximaEscrita = true }
        val cache = RoomProviderDirectoryCache(dao)

        cache.put("id:qualquer", info())
        assertNull(cache.get("id:qualquer"))
    }
}
