package io.linka.app.kotlin.feature.dns

object FeatureDnsModulo {
    fun criarBenchmarkDns(): BenchmarkDns {
        return BenchmarkDnsDoh()
    }
}

