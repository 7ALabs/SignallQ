package io.signallq.app.feature.settings

/**
 * GH#1249 (recorte de #1227, item 6/RF-D) — cidade e UF precisam ser salvas em conjunto ou não
 * salvas, nunca uma preenchida sem a outra (a "combinação inválida" que a issue original
 * descrevia). Validação completa de correspondência geográfica real (uma cidade pertence
 * mesmo àquela UF) exigiria um catálogo de municípios (ex.: IBGE) que não existe neste repo —
 * construir esse catálogo agora seria escopo novo não pedido; ver comentário na issue. Esta
 * função cobre o que é possível sem esse catálogo: consistência estrutural entre os dois
 * campos e UF pertencente à lista de siglas reais.
 */
object ValidadorCidadeUf {
    val UFS_VALIDAS =
        setOf(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA",
            "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO",
        )

    /**
     * `true` quando ambos os campos estão vazios (nada pra validar) ou ambos preenchidos com
     * uma UF real. `false` quando só um dos dois está preenchido, ou quando a UF não existe.
     */
    fun ehCombinacaoValida(
        cidade: String?,
        uf: String?,
    ): Boolean {
        val cidadeValida = cidade?.trim()?.takeIf { it.isNotBlank() }
        val ufValida = uf?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
        if (cidadeValida == null && ufValida == null) return true
        if (cidadeValida == null || ufValida == null) return false
        return ufValida in UFS_VALIDAS
    }
}
