package io.signallq.app.core.network

// Só existe uma origem hoje: o Assist não é alcançável em AppShellMode.Legacy (review da PR
// #1683 — "FluxoLegado" tinha zero call sites, código morto). Mantido como enum, não String
// solta, porque um segundo ponto de entrada é um cenário real e próximo (Ferramentas, por
// exemplo) — adicionar uma constante nova é mais seguro que introduzir o tipo depois.
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
