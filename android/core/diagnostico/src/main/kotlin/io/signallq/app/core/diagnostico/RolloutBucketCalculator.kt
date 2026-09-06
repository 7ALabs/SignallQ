package io.signallq.app.core.diagnostico

import java.security.MessageDigest

/**
 * Bucketing deterministico por instalacao — GH#1445, parte de #952.
 *
 * Puro e deterministico (sem IO, sem `Random`): a mesma instalacao (mesmo
 * [installationId]) SEMPRE cai no mesmo bucket 0..99, em qualquer execucao —
 * criterio de aceite "estabilidade por instalacao" da issue (nao pode "piscar"
 * entre execucoes do app). [salt] namespaça o bucket por mecanismo de rollout
 * (ex.: shadow mode usa um salt, um rollout futuro independente usaria outro),
 * evitando que duas decisoes de rollout diferentes fiquem sempre correlacionadas
 * pra a mesma instalacao.
 *
 * Hash usado (SHA-256, 4 primeiros bytes como inteiro) e so pra distribuir os
 * buckets de forma uniforme — nao e criptografia, [installationId] ja e um
 * identificador anonimo sem PII (ver `PreferenciasAppRepository.buscarOuGerarAnonDeviceId`,
 * `:core:datastore`).
 */
object RolloutBucketCalculator {
    /** Bucket estavel 0..99 para [installationId] sob o namespace [salt]. */
    fun bucketOf(
        installationId: String,
        salt: String = DEFAULT_SALT,
    ): Int {
        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest("$salt:$installationId".toByteArray(Charsets.UTF_8))
        val value =
            ((digest[0].toInt() and 0xFF) shl 24) or
                ((digest[1].toInt() and 0xFF) shl 16) or
                ((digest[2].toInt() and 0xFF) shl 8) or
                (digest[3].toInt() and 0xFF)
        return (value and 0x7FFFFFFF) % 100
    }

    /**
     * `true` quando [installationId] esta dentro do [rolloutPercent] atual.
     * `installationId` em branco nunca participa (fail closed — sem identificador
     * estavel, nao ha como garantir nao-piscar entre execucoes).
     */
    fun isInRollout(
        installationId: String,
        rolloutPercent: Int,
        salt: String = DEFAULT_SALT,
    ): Boolean {
        if (installationId.isBlank()) return false
        val clamped = rolloutPercent.coerceIn(0, 100)
        if (clamped <= 0) return false
        if (clamped >= 100) return true
        return bucketOf(installationId, salt) < clamped
    }

    /** Salt padrao — hoje so o shadow mode consome [RolloutBucketCalculator] (ver [DiagnosticRolloutEligibility]). */
    const val DEFAULT_SALT: String = "diagnostic_shadow_mode_rollout"
}
