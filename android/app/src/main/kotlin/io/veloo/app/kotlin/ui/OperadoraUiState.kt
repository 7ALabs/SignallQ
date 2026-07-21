package io.signallq.app.ui

/**
 * GH#1226 item 6/RF-A — antes identidade e contato eram resolvidos separadamente, mas a tela
 * só considerava a operadora "detectada" quando havia contato acionável
 * (`contato?.hasAnyContact == true`). Isso fazia o app esconder uma identidade que já sabia
 * (ex.: só o site veio do diretório) atrás de "Não foi possível identificar sua operadora
 * automaticamente" — uma mentira: a operadora FOI identificada, só não tinha contato. Este
 * estado formaliza a distinção.
 */
sealed interface OperadoraUiState {
    /** Resolução (local síncrona + diretório remoto assíncrono) ainda em andamento. */
    data object Loading : OperadoraUiState

    /** Identidade resolvida E há pelo menos um canal de contato (WhatsApp/telefone/site/app). */
    data class IdentifiedWithContacts(
        val identity: ResolvedOperadoraIdentity,
        val contact: ResolvedOperadoraContact,
    ) : OperadoraUiState

    /** Identidade resolvida, mas nenhum canal de contato disponível — a tela pode saber quem
     *  é a operadora e ainda assim não ter WhatsApp/telefone/site/app pra oferecer. */
    data class IdentifiedWithoutContacts(
        val identity: ResolvedOperadoraIdentity,
    ) : OperadoraUiState

    /** Nem identidade local nem diretório remoto encontraram nada — a lista "Outras
     *  operadoras" (sempre local) é a única ação disponível. */
    data object NotIdentified : OperadoraUiState

    /** Falha técnica real na resolução (não "não identificada") — [recoverable] indica se
     *  vale oferecer retry (ex.: timeout de rede) ou não (erro de programação/dado inválido). */
    data class Error(
        val recoverable: Boolean,
    ) : OperadoraUiState
}

/**
 * GH#1226 item 6 — resolve o [OperadoraUiState] a partir da identidade/contato já resolvidos
 * pela cadeia local → diretório remoto → fallback ([OperadoraDirectoryResolver]). Função pura,
 * sem I/O — só decide QUAL estado representar dado o que já foi resolvido.
 */
fun resolverOperadoraUiState(
    carregando: Boolean,
    identidade: ResolvedOperadoraIdentity?,
    contato: ResolvedOperadoraContact?,
): OperadoraUiState {
    if (carregando) return OperadoraUiState.Loading
    // FALLBACK = nem local nem diretório remoto encontraram nada -- trata como "não
    // identificada" (preserva o comportamento já existente antes desta issue).
    val identidadeValida = identidade?.takeIf { it.source != OperadoraSource.FALLBACK }
    val contatoValido = contato?.takeIf { it.source != OperadoraSource.FALLBACK }
    return when {
        identidadeValida == null -> OperadoraUiState.NotIdentified
        contatoValido != null && contatoValido.hasAnyContact ->
            OperadoraUiState.IdentifiedWithContacts(identidadeValida, contatoValido)
        else -> OperadoraUiState.IdentifiedWithoutContacts(identidadeValida)
    }
}
