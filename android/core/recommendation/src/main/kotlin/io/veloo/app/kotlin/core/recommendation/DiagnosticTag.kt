package io.signallq.app.core.recommendation

/**
 * Tag estruturada emitida pelo motor de diagnostico e usada pelo Recommendation Engine
 * para casar contexto com recomendacoes do catalogo.
 *
 * Modelada como value class sobre String (em vez de enum fechado) porque o catalogo remoto
 * pode introduzir tags novas sem exigir release do app.
 */
@JvmInline
value class DiagnosticTag(val id: String) {

    companion object {
        val WIFI_FRACO = DiagnosticTag("wifi_fraco")
        val BUFFERBLOAT_ALTO = DiagnosticTag("bufferbloat_alto")
        val DNS_LENTO = DiagnosticTag("dns_lento")
        val SINAL_BAIXO = DiagnosticTag("sinal_baixo")
        val MUITOS_DISPOSITIVOS = DiagnosticTag("muitos_dispositivos")
        val LATENCIA_ALTA = DiagnosticTag("latencia_alta")
        val PERDA_PACOTES_ALTA = DiagnosticTag("perda_pacotes_alta")
        val VELOCIDADE_ABAIXO_DO_CONTRATADO = DiagnosticTag("velocidade_abaixo_do_contratado")
    }
}
