package io.signallq.app.feature.dns

/**
 * [tentativas] é o total de rounds executados (inclui o warm-up descartado).
 * [tentativasAvaliadas] é quantos desses rounds efetivamente contam pro resultado
 * (pós-warmup) — GH#1212 item 5: antes o contrato registrava `tentativas=3` mas calculava
 * taxa de sucesso sobre só 2 rounds avaliados, sem separar os dois conceitos.
 * [respostaInvalida] é true quando a resposta veio com HTTP 200 mas o corpo indica
 * NXDOMAIN/SERVFAIL/sem resposta do tipo pedido — GH#1212 item 6, essas respostas não
 * contam como sucesso mesmo com HTTP OK.
 */
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
    val tentativasAvaliadas: Int = (tentativas - 1).coerceAtLeast(0),
    val respostaInvalida: Boolean = false,
)
