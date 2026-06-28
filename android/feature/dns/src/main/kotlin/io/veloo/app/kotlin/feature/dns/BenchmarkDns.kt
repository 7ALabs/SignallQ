package io.signallq.app.feature.dns

import kotlinx.coroutines.flow.StateFlow

interface BenchmarkDns {
    val snapshotFlow: StateFlow<SnapshotBenchmarkDns>

    suspend fun executar(
        hostConsulta: String = "example.com",
        resolvedoresAtivos: List<String> = emptyList(),
        privateDnsHostname: String? = null,
    )
}
