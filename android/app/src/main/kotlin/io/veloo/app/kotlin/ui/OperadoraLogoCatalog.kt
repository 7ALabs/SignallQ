package io.signallq.app.ui

import androidx.compose.ui.graphics.Color

/**
 * Identidade visual local de uma operadora: cor de marca + monograma.
 *
 * Uso apenas identificativo/didatico — nao e logo oficial da operadora,
 * nao implica parceria, patrocinio ou endosso do SignallQ pela operadora.
 */
data class OperadoraVisualIdentity(
    val corMarca: Color,
    val monograma: String,
)

/**
 * Catalogo local de identidade visual por operadora (SIG-292).
 *
 * Reaproveita os ids do catalogo/matcher de [BancoOperadoras] (ver SIG-293 / GH#411) —
 * nao duplica lista de operadoras nem regra de deteccao, so mapeia `id -> identidade visual`.
 *
 * Deliberadamente 100% local (sem chamada de rede): app de diagnostico de conectividade
 * nao pode depender de internet para exibir a propria identificacao de operadora quando o
 * diagnostico e justamente sobre a rede estar ruim ou fora do ar.
 */
object OperadoraLogoCatalog {
    private val identidades: Map<String, OperadoraVisualIdentity> =
        mapOf(
            "vivo_fibra" to OperadoraVisualIdentity(Color(0xFF660099), "V"),
            "claro_net" to OperadoraVisualIdentity(Color(0xFFED1C24), "C"),
            "tim_live" to OperadoraVisualIdentity(Color(0xFF003D8F), "T"),
            "oi_fibra" to OperadoraVisualIdentity(Color(0xFFFF8C00), "O"),
            "nio" to OperadoraVisualIdentity(Color(0xFF00B4D8), "N"),
            "algar" to OperadoraVisualIdentity(Color(0xFF0066CC), "A"),
            "unifique" to OperadoraVisualIdentity(Color(0xFF00A651), "U"),
            "brisanet" to OperadoraVisualIdentity(Color(0xFFFF6600), "B"),
            "desktop" to OperadoraVisualIdentity(Color(0xFF1E3A5F), "D"),
            "ligga" to OperadoraVisualIdentity(Color(0xFF8BC53F), "L"),
            "vero" to OperadoraVisualIdentity(Color(0xFF7B2D8E), "V"),
            "giga_mais" to OperadoraVisualIdentity(Color(0xFF00AEEF), "G"),
        )

    private val padrao = OperadoraVisualIdentity(LkColors.accent, "?")

    /** Identidade visual local para o [operadora]. Nunca falha — cai no padrao se o id nao estiver no catalogo. */
    fun identidadePara(operadora: ContatoOperadora): OperadoraVisualIdentity =
        identidades[operadora.id] ?: padrao.copy(monograma = operadora.nome.firstOrNull()?.uppercase() ?: "?")
}
