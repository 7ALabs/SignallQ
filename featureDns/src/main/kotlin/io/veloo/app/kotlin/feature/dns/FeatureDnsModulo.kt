package io.veloo.app.feature.dns

object FeatureDnsModulo {
    fun criarBenchmarkDns(): BenchmarkDns {
        return BenchmarkDnsDoh()
    }
}

