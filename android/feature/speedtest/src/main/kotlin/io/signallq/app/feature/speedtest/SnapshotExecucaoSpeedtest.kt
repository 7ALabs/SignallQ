package io.signallq.app.feature.speedtest

data class ResultadoRodadaTriplo(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val latenciaMs: Double = 0.0,
)

data class SnapshotExecucaoSpeedtest(
    val estado: EstadoExecucaoSpeedtest,
    val progressoPercentual: Int,
    val resultado: ResultadoSpeedtest?,
    val erroMensagem: String?,
    val faseAtual: FaseSpeedtest = FaseSpeedtest.idle,
    val velocidadeAtualMbps: Double = 0.0,
    val bytesConsumidos: Long = 0L,
    val progressoGlobal: Float = 0f,
    val pontosAoVivo: List<PontoAoVivo> = emptyList(),
    // Teste Triplo
    val rodadaAtual: Int = 0,
    val aguardandoProximaRodada: Boolean = false,
    val rodadasTriplo: List<ResultadoRodadaTriplo> = emptyList(),
)
