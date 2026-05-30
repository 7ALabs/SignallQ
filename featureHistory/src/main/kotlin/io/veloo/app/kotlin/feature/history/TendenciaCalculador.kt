package io.veloo.app.feature.history

enum class TendenciaEstado { MELHOROU, PIOROU, ESTAVEL }

fun calcularTendencia(resumo: ResumoHistorico): Pair<TendenciaEstado, Int>? {
    val ultimo = resumo.ultimoDownloadMbps ?: return null
    val media = resumo.mediaDownloadMbps5 ?: return null
    if (resumo.totalMedicoes < 2 || media == 0.0) return null
    val delta = (ultimo - media) / media * 100.0
    val percentual = if (delta < 0) (-delta).toInt() else delta.toInt()
    return when {
        delta > 10.0 -> Pair(TendenciaEstado.MELHOROU, percentual)
        delta < -10.0 -> Pair(TendenciaEstado.PIOROU, percentual)
        else -> Pair(TendenciaEstado.ESTAVEL, percentual)
    }
}
