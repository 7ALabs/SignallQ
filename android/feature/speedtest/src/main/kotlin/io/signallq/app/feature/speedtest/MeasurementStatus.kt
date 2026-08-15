package io.signallq.app.feature.speedtest

/**
 * Integridade de uma execucao de speedtest — GH#1221/#1225.
 *
 * Antes desta issue, a unica sinalizacao de integridade era o par
 * `contaminado: Boolean` + `faseInterrompida: String` em [ResultadoSpeedtest], sem uma
 * fonte unica que os consumidores (tela de Resultado, diagnostico, IA, recomendacao,
 * PDF, historico) pudessem checar para decidir "posso tratar isto como uma medicao
 * valida?". [status] é essa fonte unica — computado uma vez em
 * `ExecutorSpeedtestCloudflare.construirResultado`, nunca recalculado pelos
 * consumidores.
 *
 * Regras de consumo (GH#1225, criterio D):
 * - [COMPLETE]: unico status que libera diagnostico conclusivo, IA, Recommendation
 *   Engine, contato com operadora, historico e PDF completo.
 * - [PARTIAL]: alguma fase relevante falhou (ex.: upload nao detectado apos retry) —
 *   mostrar so as metricas validas, sem conclusao causal forte.
 * - [INCONCLUSIVE]: amostras validas abaixo do minimo estatistico (RF-08) — priorizar
 *   repetir o teste.
 * - [CONTAMINATED]: rede mudou durante o teste — nao persistir como medicao valida,
 *   nao alimentar diagnostico/IA/recomendacao.
 * - [CANCELLED]: usuario cancelou — sem resultado a apresentar (nao chega a gerar
 *   [ResultadoSpeedtest] hoje, mantido aqui para os consumidores que modelam o ciclo
 *   de vida inteiro da execucao, ex.: PostSpeedtestUiState).
 */
enum class MeasurementStatus {
    COMPLETE,
    PARTIAL,
    INCONCLUSIVE,
    CONTAMINATED,
    CANCELLED,
    ;

    /** `true` quando o resultado pode alimentar diagnostico conclusivo, IA,
     *  Recommendation Engine, contato com operadora, historico e PDF completo. */
    val liberaConclusaoCompleta: Boolean get() = this == COMPLETE
}

/** Label PT-BR do [MeasurementStatus], usado por telas e PDF (GH#1219/#1225) — unico
 *  ponto de traducao, nao reimplementar em cada consumidor. */
fun MeasurementStatus.labelPt(): String = when (this) {
    MeasurementStatus.COMPLETE -> "Completo"
    MeasurementStatus.PARTIAL -> "Parcial"
    MeasurementStatus.INCONCLUSIVE -> "Inconclusivo"
    MeasurementStatus.CONTAMINATED -> "Contaminado"
    MeasurementStatus.CANCELLED -> "Cancelado"
}

/** Minimo de amostras validas de latencia para o calculo nao ser [MeasurementStatus.INCONCLUSIVE]
 *  (RF-08). Abaixo disso, jitter/perda calculados sobre poucas amostras nao tem confianca
 *  estatistica minima — valor escolhido como piso conservador (jitter com 3 amostras nao é
 *  comparavel a um calculo com 30, mas exigir 30 tornaria o teste `fast` sempre inconclusivo). */
internal const val MINIMO_AMOSTRAS_LATENCIA_VALIDAS = 5

/**
 * Calcula o [MeasurementStatus] de uma execucao a partir dos sinais ja existentes no
 * motor — funcao pura e testável isoladamente, sem depender de OkHttp/coroutines.
 *
 * Ordem de prioridade (do mais para o menos severo): [MeasurementStatus.CONTAMINATED]
 * (rede mudou — o resultado nem deveria ter sido consolidado) > [MeasurementStatus.INCONCLUSIVE]
 * (poucas amostras de latencia) > [MeasurementStatus.PARTIAL] (upload nao detectado ou alguma
 * fase encerrada por falha) > [MeasurementStatus.COMPLETE].
 */
internal fun calcularMeasurementStatus(
    contaminado: Boolean,
    amostrasValidasLatencia: Int,
    uploadNaoDetectado: Boolean,
    downloadEncerradaPor: String,
    uploadEncerradaPor: String,
): MeasurementStatus = when {
    contaminado -> MeasurementStatus.CONTAMINATED
    amostrasValidasLatencia < MINIMO_AMOSTRAS_LATENCIA_VALIDAS -> MeasurementStatus.INCONCLUSIVE
    uploadNaoDetectado -> MeasurementStatus.PARTIAL
    downloadEncerradaPor == "download_bloqueado_429" -> MeasurementStatus.PARTIAL
    else -> MeasurementStatus.COMPLETE
}
