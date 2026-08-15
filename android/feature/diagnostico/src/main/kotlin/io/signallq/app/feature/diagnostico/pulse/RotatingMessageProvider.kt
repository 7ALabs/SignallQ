package io.signallq.app.feature.diagnostico.pulse

object RotatingMessageProvider {

    private val messages: Map<SignallQState, List<String>> = mapOf(
        SignallQState.Collecting to listOf(
            "Verificando sua conexão...",
            "Medindo a velocidade real...",
            "Checando a qualidade do sinal...",
            "Olhando como sua rede está se comportando...",
        ),
        SignallQState.Thinking to listOf(
            "Conectando os pontos...",
            "Entendendo o que aconteceu...",
            "Identificando a causa...",
        ),
        SignallQState.Analyzing to listOf(
            "Quase lá...",
            "Preparando seu diagnóstico...",
            "Só mais um instante...",
        ),
        SignallQState.AwaitingInput to listOf(
            "Pronto para aprofundar a análise…",
            "Selecione o que melhor descreve seu problema…",
        ),
        SignallQState.Success to listOf("Análise concluída."),
        SignallQState.Warning to listOf("Análise concluída com alertas."),
        SignallQState.Critical to listOf("Problemas críticos identificados."),
        SignallQState.Idle to listOf("Toque para iniciar o diagnóstico inteligente."),
    )

    fun first(state: SignallQState): String = messages[state]?.firstOrNull() ?: ""

    fun next(state: SignallQState, current: String): String {
        val list = messages[state] ?: return current
        if (list.size <= 1) return current
        val idx = list.indexOf(current)
        return if (idx < 0 || idx >= list.size - 1) list[0] else list[idx + 1]
    }

    fun all(state: SignallQState): List<String> = messages[state] ?: emptyList()
}
