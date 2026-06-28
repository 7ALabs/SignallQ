package io.signallq.app.feature.speedtest

data class DiagnosticoFasesSpeedtest(
    val faseInterrompida: String,
    val latenciaAmostrasTotais: Int,
    val latenciaAmostrasValidas: Int,
    val latenciaTimeouts: Int,
    val downloadBytesTotal: Long,
    val downloadAmostrasValidas: Int,
    val downloadRequisicoesSucesso: Int,
    val downloadRequisicoesErro: Int,
    val downloadEncerradaPor: String,
    val downloadThroughputOrigem: String,
    val downloadUltimoErro: String?,
    val uploadBytesTotal: Long,
    val uploadAmostrasValidas: Int,
    val uploadRequisicoesSucesso: Int,
    val uploadRequisicoesErro: Int,
    val uploadEncerradaPor: String,
    val uploadThroughputOrigem: String,
    val uploadUltimoErro: String?,
    val dnsErroMensagem: String?,
)
