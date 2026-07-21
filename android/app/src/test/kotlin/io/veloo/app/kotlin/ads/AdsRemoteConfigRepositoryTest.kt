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
 * Issue #555 -- qualquer FALHA REAL de Remote Config (excecao, timeout) cai no
 * fallback local seguro [AdsFlags.DESLIGADO] quando tambem nao ha valor ativo
 * utilizavel. Nunca deve mostrar anuncio "por acidente" so porque o Firebase nao
 * respondeu.
 *
 * GH#1224 -- correcao do bug real: `fetchAndActivate()` retornar `false` significa
 * apenas "nenhuma configuracao NOVA precisou ser ativada" (valores atuais ja
 * ativos/cacheados), nao uma falha. Antes disso ser corrigido, esse `false` desligava
 * TODAS as flags (ver o teste que documentava esse comportamento errado, agora
 * substituido pelo teste equivalente correto abaixo).
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
    fun `fetch retornando false (sem config nova) ainda le os valores ja ativos -- GH#1224`() =
        runTest {
            // Esse e exatamente o bug real: false so significa "nada novo pra ativar",
            // os valores continuam la e precisam ser lidos normalmente.
            val remoteConfig = mockk<FirebaseRemoteConfig>()
            every { remoteConfig.fetchAndActivate() } returns fakeCompletedTask(false)
            every { remoteConfig.getBoolean(AdsRemoteConfigRepository.CHAVE_MASTER) } returns true
            every { remoteConfig.getBoolean(AdsRemoteConfigRepository.CHAVE_VELOCIDADE) } returns true
            every { remoteConfig.getBoolean(AdsRemoteConfigRepository.CHAVE_RESULTADO) } returns true
            every { remoteConfig.getBoolean(AdsRemoteConfigRepository.CHAVE_DISPOSITIVOS) } returns true
            every { remoteConfig.getBoolean(AdsRemoteConfigRepository.CHAVE_HISTORICO) } returns true
            every { remoteConfig.getBoolean(AdsRemoteConfigRepository.CHAVE_JOGOS) } returns true

            val flags = AdsRemoteConfigRepository(Lazy { remoteConfig }).buscarFlags()

            assertEquals(
                AdsFlags(
                    masterEnabled = true,
                    velocidade = true,
                    resultado = true,
                    dispositivos = true,
                    historico = true,
                    jogos = true,
                ),
                flags,
            )
        }

    @Test
    fun `excecao no fetch mas valores ja ativos utilizaveis -- usa os valores ativos, nao desliga`() =
        runTest {
            // RF-03 -- falha temporaria de rede nao pode apagar uma config valida ja
            // carregada anteriormente (o SDK do Remote Config mantem os ultimos valores
            // ativados mesmo quando um fetch novo falha).
            val remoteConfig = mockk<FirebaseRemoteConfig>()
            every { remoteConfig.fetchAndActivate() } returns fakeFailedTask(RuntimeException("sem rede"))
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
    fun `excecao no fetch e leitura dos valores ativos tambem falha -- cai no fallback desligado`() =
        runTest {
            val remoteConfig = mockk<FirebaseRemoteConfig>()
            every { remoteConfig.fetchAndActivate() } returns fakeFailedTask(RuntimeException("sem rede"))
            every { remoteConfig.getBoolean(any()) } throws IllegalStateException("Remote Config nao inicializado")

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
