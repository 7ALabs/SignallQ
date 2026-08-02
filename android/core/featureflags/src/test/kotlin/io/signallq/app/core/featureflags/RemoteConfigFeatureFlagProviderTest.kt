package io.signallq.app.core.featureflags

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre os criterios de aceite da issue #1477: defaults aplicados antes de
 * qualquer consulta remota, `fetchAndActivate()==false` nao tratado como erro,
 * e ultima configuracao valida preservada em falha de rede -- mesma logica ja
 * validada (GH#1224) em `io.signallq.app.ads.AdsRemoteConfigRepositoryTest`,
 * agora aplicada ao provider de feature flags do Consumer.
 */
class RemoteConfigFeatureFlagProviderTest {
    private val definicaoSpeedtest =
        FeatureFlagDefinition(
            key = FeatureFlagKeys.CONSUMER_SPEEDTEST_ENABLED,
            module = ":featureSpeedtest",
            type = FeatureFlagType.BOOLEAN,
            defaultValue = FeatureFlagRawValue.BooleanValue(true),
            criticality = FeatureFlagCriticality.HIGH,
            owner = "product-signallq",
            description = "desc",
            disabledBehavior = FeatureFlagDisabledBehavior.HIDE_ENTRY_AND_BLOCK_ROUTE,
            disabledMessage = null,
            dependencies = emptyList(),
            androidImplemented = false,
            adminManaged = true,
            analyticsEvent = null,
        )
    private val catalog = FeatureFlagCatalog(listOf(definicaoSpeedtest))

    @Test
    fun `isEnabled retorna default local antes de qualquer refresh -- nunca bloqueia esperando rede`() {
        val remoteConfig = mockk<FirebaseRemoteConfig>()
        val provider = RemoteConfigFeatureFlagProvider({ remoteConfig }, catalog)

        assertTrue(provider.isEnabled(FeatureFlagKeys.CONSUMER_SPEEDTEST_ENABLED))
    }

    @Test
    fun `isEnabled retorna false para chave desconhecida, nunca lanca excecao`() {
        val remoteConfig = mockk<FirebaseRemoteConfig>()
        val provider = RemoteConfigFeatureFlagProvider({ remoteConfig }, catalog)

        assertFalse(provider.isEnabled(FeatureFlagKey("consumer.flag.inexistente")))
    }

    @Test
    fun `fetchAndActivate retornando false nao e erro -- le valores ja ativos`() =
        runTest {
            val remoteConfig = mockFirebaseRemoteConfig(fetchAndActivateResult = fakeCompletedTask(false), valorAtivoSpeedtest = false)
            val provider = RemoteConfigFeatureFlagProvider({ remoteConfig }, catalog)

            val resultado = provider.refresh()

            assertTrue(resultado is FeatureFlagRefreshResult.Success)
            assertFalse((resultado as FeatureFlagRefreshResult.Success).activated)
            assertFalse(provider.isEnabled(FeatureFlagKeys.CONSUMER_SPEEDTEST_ENABLED))
        }

    @Test
    fun `excecao no fetch preserva ultima configuracao valida em memoria`() =
        runTest {
            val remoteConfig = mockFirebaseRemoteConfig(fetchAndActivateResult = fakeFailedTask(RuntimeException("sem rede")), valorAtivoSpeedtest = true)
            val provider = RemoteConfigFeatureFlagProvider({ remoteConfig }, catalog)

            val resultado = provider.refresh()

            assertTrue(resultado is FeatureFlagRefreshResult.Failure)
            // Mesmo com fetch falho, a leitura dos valores ATIVOS do SDK ainda roda e
            // atualiza o estado -- comportamento identico ao AdsRemoteConfigRepository (GH#1224).
            assertTrue(provider.isEnabled(FeatureFlagKeys.CONSUMER_SPEEDTEST_ENABLED))
        }

    @Test
    fun `excecao no fetch e leitura de valores ativos tambem falha -- mantem default local anterior`() =
        runTest {
            val remoteConfig = mockk<FirebaseRemoteConfig>()
            every { remoteConfig.fetchAndActivate() } returns fakeFailedTask(RuntimeException("sem rede"))
            every { remoteConfig.getValue(any()) } throws IllegalStateException("Remote Config nao inicializado")
            val provider = RemoteConfigFeatureFlagProvider({ remoteConfig }, catalog)

            provider.refresh()

            // Nenhuma leitura nova aplicada -- continua no default local (true).
            assertTrue(provider.isEnabled(FeatureFlagKeys.CONSUMER_SPEEDTEST_ENABLED))
        }

    @Test
    fun `refresh force=true usa fetch(0) + activate em vez de fetchAndActivate`() =
        runTest {
            val remoteConfig = mockk<FirebaseRemoteConfig>()
            every { remoteConfig.fetch(0) } returns fakeCompletedTask<Void?>(null)
            every { remoteConfig.activate() } returns fakeCompletedTask(true)
            every { remoteConfig.getValue(definicaoSpeedtest.key.id) } returns
                fakeRemoteConfigValue(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE)
            every { remoteConfig.getBoolean(definicaoSpeedtest.key.id) } returns true
            every { remoteConfig.info } returns mockk { every { fetchTimeMillis } returns 123L }
            val provider = RemoteConfigFeatureFlagProvider({ remoteConfig }, catalog)

            val resultado = provider.refresh(force = true)

            assertTrue(resultado is FeatureFlagRefreshResult.Success)
            assertEquals(123L, (resultado as FeatureFlagRefreshResult.Success).fetchTimeMillis)
            assertTrue(provider.isEnabled(FeatureFlagKeys.CONSUMER_SPEEDTEST_ENABLED))
        }

    private fun mockFirebaseRemoteConfig(
        fetchAndActivateResult: Task<Boolean>,
        valorAtivoSpeedtest: Boolean,
    ): FirebaseRemoteConfig {
        val remoteConfig = mockk<FirebaseRemoteConfig>()
        every { remoteConfig.fetchAndActivate() } returns fetchAndActivateResult
        every { remoteConfig.getBoolean(definicaoSpeedtest.key.id) } returns valorAtivoSpeedtest
        every { remoteConfig.getValue(definicaoSpeedtest.key.id) } returns
            fakeRemoteConfigValue(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE)
        return remoteConfig
    }

    private fun fakeRemoteConfigValue(sourceValue: Int): FirebaseRemoteConfigValue =
        mockk {
            every { source } returns sourceValue
        }

    /** Task fake que ja chega "completa" -- invoca o listener sincronamente, sem
     *  depender de looper/Robolectric (mesmo padrao de AdsRemoteConfigRepositoryTest). */
    private fun <T> fakeCompletedTask(result: T): Task<T> {
        val task = mockk<Task<T>>()
        every { task.isComplete } returns true
        every { task.exception } returns null
        every { task.result } returns result
        every { task.addOnCompleteListener(any<OnCompleteListener<T>>()) } answers {
            firstArg<OnCompleteListener<T>>().onComplete(task)
            task
        }
        return task
    }

    private fun <T> fakeFailedTask(erro: Exception): Task<T> {
        val task = mockk<Task<T>>()
        every { task.isComplete } returns true
        every { task.exception } returns erro
        every { task.addOnCompleteListener(any<OnCompleteListener<T>>()) } answers {
            firstArg<OnCompleteListener<T>>().onComplete(task)
            task
        }
        return task
    }
}
