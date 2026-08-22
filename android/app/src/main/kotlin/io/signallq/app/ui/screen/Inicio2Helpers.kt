package io.signallq.app.ui.screen

/** Reduz siglas de tecnologia da rede móvel antes de mostrá-las ao público geral. */
internal fun tecnologiaSimplificada(tecnologia: String?): String? =
    tecnologia
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.replace(Regex("\\s+(NSA|SA)$", RegexOption.IGNORE_CASE), "")
