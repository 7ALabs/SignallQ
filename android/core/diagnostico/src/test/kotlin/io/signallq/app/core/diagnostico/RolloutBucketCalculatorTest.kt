package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GH#1445 (parte de #952) — bucketing deterministico por instalacao.
 * Criterio de aceite central: mesma instalacao SEMPRE cai no mesmo bucket
 * (nao pode "piscar" entre execucoes).
 */
class RolloutBucketCalculatorTest {
    @Test
    fun `bucketOf e deterministico para a mesma instalacao`() {
        val id = "11111111-1111-1111-1111-111111111111"
        val primeiro = RolloutBucketCalculator.bucketOf(id)
        repeat(20) {
            assertEquals(primeiro, RolloutBucketCalculator.bucketOf(id))
        }
    }

    @Test
    fun `bucketOf sempre devolve valor entre 0 e 99`() {
        repeat(500) { index ->
            val bucket = RolloutBucketCalculator.bucketOf("installation-$index")
            assertTrue("bucket=$bucket fora de 0..99", bucket in 0..99)
        }
    }

    @Test
    fun `bucketOf distribui instalacoes diferentes de forma razoavelmente uniforme`() {
        // Nao precisa ser exato -- so garantir que nao caia tudo no mesmo bucket
        // (o que indicaria hash quebrado/constante).
        val buckets = (0 until 1000).map { RolloutBucketCalculator.bucketOf("installation-$it") }.toSet()
        assertTrue("esperado varios buckets distintos, obteve ${buckets.size}", buckets.size > 50)
    }

    @Test
    fun `salt diferente muda o bucket (namespacing entre mecanismos de rollout)`() {
        val id = "22222222-2222-2222-2222-222222222222"
        val bucketA = RolloutBucketCalculator.bucketOf(id, salt = "mecanismo_a")
        val bucketB = RolloutBucketCalculator.bucketOf(id, salt = "mecanismo_b")
        // Nao ha garantia matematica de que sejam diferentes para todo id, mas
        // para este id fixo (case fixture) sabemos que sao -- documenta a intencao
        // do parametro sem depender de uma unica amostra aleatoria em CI.
        assertTrue(bucketA != bucketB || RolloutBucketCalculator.bucketOf(id, salt = "mecanismo_c") != bucketA)
    }

    @Test
    fun `isInRollout com 0 por cento nunca inclui ninguem`() {
        repeat(50) { index ->
            assertFalse(RolloutBucketCalculator.isInRollout("installation-$index", rolloutPercent = 0))
        }
    }

    @Test
    fun `isInRollout com 100 por cento sempre inclui`() {
        repeat(50) { index ->
            assertTrue(RolloutBucketCalculator.isInRollout("installation-$index", rolloutPercent = 100))
        }
    }

    @Test
    fun `isInRollout com installationId em branco nunca participa (fail closed)`() {
        assertFalse(RolloutBucketCalculator.isInRollout("", rolloutPercent = 100))
        assertFalse(RolloutBucketCalculator.isInRollout("   ", rolloutPercent = 50))
    }

    @Test
    fun `isInRollout com percentual fora de 0 a 100 e limitado (coerceIn)`() {
        val id = "33333333-3333-3333-3333-333333333333"
        assertFalse(RolloutBucketCalculator.isInRollout(id, rolloutPercent = -10))
        assertTrue(RolloutBucketCalculator.isInRollout(id, rolloutPercent = 150))
    }

    @Test
    fun `isInRollout e consistente com bucketOf`() {
        val id = "44444444-4444-4444-4444-444444444444"
        val bucket = RolloutBucketCalculator.bucketOf(id)
        assertTrue(RolloutBucketCalculator.isInRollout(id, rolloutPercent = bucket + 1))
        assertFalse(RolloutBucketCalculator.isInRollout(id, rolloutPercent = bucket))
    }
}
