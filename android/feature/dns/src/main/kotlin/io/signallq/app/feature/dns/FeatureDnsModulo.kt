package io.signallq.app.feature.dns

object FeatureDnsModulo {
    fun criarBenchmarkDns(): BenchmarkDns {
        return BenchmarkDnsDoh()
    }
}

