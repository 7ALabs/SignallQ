package io.signallq.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import io.signallq.app.R

/**
 * Identidade visual local de uma operadora: cor de marca + monograma, com logo oficial
 * opcional (SIG-292 fase 2).
 *
 * [logoRes] e nullable de proposito: nem toda operadora tem um asset oficial baixavel com
 * qualidade/seguranca juridica suficiente (ver `docs/brand-assets/operators-sources.md`).
 * Quando nulo, a UI cai no badge de cor+monograma abaixo.
 *
 * Uso apenas identificativo — nao e endosso, parceria ou patrocinio das operadoras ao
 * SignallQ. Ver nota completa no arquivo de fontes.
 */
data class OperadoraVisualIdentity(
    val corMarca: Color,
    val monograma: String,
    @DrawableRes val logoRes: Int? = null,
)

/**
 * Catalogo local de identidade visual por operadora (SIG-292).
 *
 * Reaproveita os ids do catalogo/matcher de [BancoOperadoras] (ver SIG-293 / GH#411) —
 * nao duplica lista de operadoras nem regra de deteccao, so mapeia `id -> identidade visual`.
 *
 * Deliberadamente 100% local (sem chamada de rede): app de diagnostico de conectividade
 * nao pode depender de internet para exibir a propria identificacao de operadora quando o
 * diagnostico e justamente sobre a rede estar ruim ou fora do ar. Os `logoRes` sao drawables
 * bundled (vector ou webp), nunca URL remota — ver `docs/brand-assets/operators-sources.md`
 * para fonte, data de coleta e risco de cada asset.
 */
object OperadoraLogoCatalog {
    private val identidades: Map<String, OperadoraVisualIdentity> =
        mapOf(
            "vivo_fibra" to OperadoraVisualIdentity(Color(0xFF660099), "V"), // fallback manual — ver docs/brand-assets
            "claro_net" to OperadoraVisualIdentity(Color(0xFFED1C24), "C", R.drawable.operator_claro_net),
            "tim_live" to OperadoraVisualIdentity(Color(0xFF003D8F), "T", R.drawable.operator_tim_live),
            "oi_fibra" to OperadoraVisualIdentity(Color(0xFFFF8C00), "O", R.drawable.operator_oi_fibra),
            "nio" to OperadoraVisualIdentity(Color(0xFF00B4D8), "N", R.drawable.operator_nio),
            "algar" to OperadoraVisualIdentity(Color(0xFF0066CC), "A", R.drawable.operator_algar),
            "unifique" to OperadoraVisualIdentity(Color(0xFF00A651), "U", R.drawable.operator_unifique),
            "brisanet" to OperadoraVisualIdentity(Color(0xFFFF6600), "B"), // fallback manual — ver docs/brand-assets
            "desktop" to OperadoraVisualIdentity(Color(0xFF1E3A5F), "D", R.drawable.operator_desktop),
            "ligga" to OperadoraVisualIdentity(Color(0xFF8BC53F), "L", R.drawable.operator_ligga),
            "vero" to OperadoraVisualIdentity(Color(0xFF7B2D8E), "V", R.drawable.operator_vero),
            "giga_mais" to OperadoraVisualIdentity(Color(0xFF00AEEF), "G", R.drawable.operator_giga_mais),
        )

    private val padrao = OperadoraVisualIdentity(LkColors.accent, "?", R.drawable.operator_generic)

    /** Identidade visual local para o [operadora]. Nunca falha — cai no padrao se o id nao estiver no catalogo. */
    fun identidadePara(operadora: ContatoOperadora): OperadoraVisualIdentity =
        identidades[operadora.id] ?: padrao.copy(monograma = operadora.nome.firstOrNull()?.uppercase() ?: "?")
}
