package io.signallq.app.ads

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.Lazy
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Issue #555 -- qualquer falha de Remote Config (fetch, timeout, excecao) cai no
 * fallback local seguro [AdsFlags.DESLIGADO]. Nunca deve mostrar anuncio "por acidente"
 * so porque o Firebase nao respondeu.
 */
class AdsRemoteConfigRepositoryTest {
    @Test
    fun `fetch com sucesso e chaves ligadas retorna as flags lidas`() =
        runTest {
            val remoteConfig = mockk<FirebaseRemoteConfig>()
            val task = fakeCompletedTask(true)
            every { remoteConfig.fetchAndActivate() } returns task
            every { remoteConfig.getBoolean(AdsRemoteConfigRepository.CHAVE_MASTER) } returns true
            every { remoteConfig.getBoolean(AdsRemoteConfigRepository.CHAVE_VELOCIDADE) } returns true
            every { remoteConfig.getBoolean(AdsRemoteConfigRepository.CHAVE_RESULTADO) } returns false
            every { remoteConfig.getBoolean(AdsRemoteConfigRepository.CHAVE_DISPOSITIVOS) } returns false
            every { remoteConfig.getBoolean(AdsRemoteConfigRepository.CHAVE_HISTORICO) } returns false
            every { remoteConfig.getBoolean(AdsRemoteConfigRepository.CHAVE_JOGOS) } returns false

            val flags = AdsRemoteConfigRepository(Lazy { remoteConfig }).buscarFlags()

            assertEquals(AdsFlags(masterEnabled = true, velocidade = true), flags)
        }

    @Test
    fun `fetch retornando false (nao sucesso) cai no fallback desligado`() =
        runTest {
            val remoteConfig = mockk<FirebaseRemoteConfig>()
            every { remoteConfig.fetchAndActivate() } returns fakeCompletedTask(false)

            val flags = AdsRemoteConfigRepository(Lazy { remoteConfig }).buscarFlags()

            assertEquals(AdsFlags.DESLIGADO, flags)
        }

    @Test
    fun `excecao no fetch cai no fallback desligado sem propagar`() =
        runTest {
            val remoteConfig = mockk<FirebaseRemoteConfig>()
            every { remoteConfig.fetchAndActivate() } returns fakeFailedTask(RuntimeException("sem rede"))

            val flags = AdsRemoteConfigRepository(Lazy { remoteConfig }).buscarFlags()

            assertEquals(AdsFlags.DESLIGADO, flags)
            assertFalse(flags.masterEnabled)
        }

    /** Task fake que ja chega "completa" -- invoca o listener sincronamente, sem
     *  depender de looper/Robolectric. */
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
