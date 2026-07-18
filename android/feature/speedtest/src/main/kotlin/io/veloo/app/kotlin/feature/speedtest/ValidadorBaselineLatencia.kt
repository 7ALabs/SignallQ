package io.signallq.app.feature.speedtest

/**
 * Regras puras da guarda de sanidade da latência base do speedtest (GH#1118).
 *
 * Extraído para ser testável sem depender de OkHttp/rede — segue o mesmo padrão de
 * [AnalisadorAmostragemPing] (GH#1019): algoritmo puro, sem I/O, reusável e coberto por
 * teste unitário direto.
 */
object ValidadorBaselineLatencia {
    /**
     * `true` quando a amostragem de latência não teve NENHUMA resposta válida — sinal de
     * que o host/probe usado está fora do ar (não apenas com perda parcial de pacote).
     */
    fun probeIndisponivel(resultado: ResultadoAmostragemPing): Boolean =
        resultado.totalAmostras > 0 && resultado.timeouts >= resultado.totalAmostras

    /**
     * `true` quando a latência base medida é maior que a latência sob carga — fisicamente
     * invertido (a rede não fica mais rápida sob carga), sinal de que o baseline está
     * contaminado. `latenciaSobCargaMs` igual a 0 significa "sem dado sob carga disponível"
     * (ex.: download/upload não geraram amostras) — nesse caso não há o que comparar.
     */
    fun baselineImplausivel(
        latenciaBaseMs: Double,
        latenciaSobCargaMs: Double,
    ): Boolean = latenciaSobCargaMs > 0.0 && latenciaBaseMs > latenciaSobCargaMs
}
