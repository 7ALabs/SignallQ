package io.veloo.app.feature.dns

data class ResultadoBenchmarkDns(
    val nomeProvedor: String,
    val hostConsulta: String,
    val tempoMs: Double?,
    val amostrasMs: List<Double>,
    val tentativas: Int,
    val sucessos: Int,
    val taxaSucessoPercentual: Double,
    val erroMensagem: String?,
    val gradeRapidez: String?,   // "A" <=15ms | "B" <=30ms | "C" <=50ms | "D" >50ms
    // true quando o resolvedor detectado é IP privado (roteador/gateway local).
    // Latência local não é comparável com DNS públicos externos.
    val isGatewayLocal: Boolean = false,
)
