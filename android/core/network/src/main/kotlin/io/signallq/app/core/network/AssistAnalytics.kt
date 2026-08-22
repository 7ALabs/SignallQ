package io.signallq.app.core.network

// Só existe uma origem hoje: o Assist é aberto pela jornada de diagnóstico no Início. Mantido como
// enum, não String solta, porque Ferramentas pode ganhar uma entrada própria sem quebrar analytics.
enum class AssistOrigem(val analyticsId: String) { Inicio2("inicio_2") }

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
