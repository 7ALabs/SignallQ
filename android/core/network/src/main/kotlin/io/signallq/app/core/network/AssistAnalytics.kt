package io.signallq.app.core.network

enum class AssistOrigem(val analyticsId: String) { Inicio2("inicio_2"), FluxoLegado("fluxo_legado") }

enum class AssistEtapa(val analyticsId: String) { Objetivo("objetivo"), Contexto("contexto") }

data class AssistObjetivoSelecionado(
    val objetivoId: String,
    val origem: AssistOrigem,
    val retomada: Boolean,
)

data class AssistPerguntaRespondida(
    val objetivoId: String,
    val perguntaId: String,
    val respostaId: String,
    val retomada: Boolean,
)

data class AssistAbandonado(
    val etapa: AssistEtapa,
    val objetivoId: String?,
    val retomavel: Boolean,
)
