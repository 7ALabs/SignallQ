package io.signallq.app.feature.diagnostico

data class DadoCanal(
    val canal: Int,
    val count: Int,
    val countProprios: Int = 0,
    val countTerceiros: Int = count,
    val maxRssiDbm: Int?,
    val nivel: NivelCongestionamento,
    val ehCanalAtual: Boolean,
    val ehCanalRecomendado: Boolean,
)
