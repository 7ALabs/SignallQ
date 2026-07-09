package io.signallq.app.feature.diagnostico

data class DiagnosticResult(
    val id: String,
    val titulo: String,
    val status: DiagnosticStatus,
    val evidencia: String?,
    val mensagemUsuario: String,
    val recomendacao: String?,
    val categoria: String,
    val podeConcluir: Boolean = false,
    /** Categoria da causa raiz real do achado/decisao de SAIDA (ex.: "isp", "fibra",
     *  "wifi", "dns", "roteador", "local") -- usada para decidir se a UI deve
     *  oferecer contato com a operadora (GH#836). Diferente de [categoria], que
     *  classifica resultados de ENTRADA (dns/historico/wifi-canal) e continua com
     *  seu uso atual inalterado. Nulo quando a causa e genuinamente ambigua/nao
     *  atribuivel a uma origem especifica. */
    val categoriaOrigem: String? = null,
)
