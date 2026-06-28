package io.signallq.app.feature.dns

data class SnapshotBenchmarkDns(
    val estado: EstadoBenchmarkDns,
    val progressoPercentual: Int,
    val resultados: List<ResultadoBenchmarkDns>,
    val erroMensagem: String?,
)

