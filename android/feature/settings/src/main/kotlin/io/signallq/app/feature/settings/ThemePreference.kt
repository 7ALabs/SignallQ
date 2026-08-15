package io.signallq.app.feature.settings

/**
 * GH#1227 item 14/RF-I — antes o tema era comparado por string livre
 * (`temaSelecionado == "sistema"`/`"claro"`/`"escuro"`); qualquer valor inesperado (dado
 * corrompido, migração futura, string vazia) fazia NENHUMA opção aparecer selecionada na UI
 * (`ThemeSelector` comparava igualdade direta, sem fallback). Este enum + [parse] garantem que
 * sempre existe uma opção válida e visualmente selecionada.
 */
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    /** Valor persistido no DataStore -- preserva as chaves já gravadas em produção
     *  ("sistema"/"claro"/"escuro"), só a comparação na UI passa a ser seguro. */
    val chaveDataStore: String
        get() = when (this) {
            SYSTEM -> "sistema"
            LIGHT -> "claro"
            DARK -> "escuro"
        }

    companion object {
        /** Nunca lança exceção e nunca devolve "nenhuma opção" -- valor não reconhecido
         *  sempre cai em [SYSTEM] (mesmo default que já era usado no DataStore). */
        fun parse(raw: String?): ThemePreference =
            when (raw?.trim()?.lowercase()) {
                "claro" -> LIGHT
                "escuro" -> DARK
                else -> SYSTEM
            }
    }
}
