package io.linka.app.kotlin.feature.diagnostico.pulse

object RotatingMessageProvider {

    private val messages: Map<OrbitState, List<String>> = mapOf(
        OrbitState.Collecting to listOf(
            "Verificando sua conexão...",
            "Medindo a velocidade real...",
            "Checando a qualidade do sinal...",
            "Olhando como sua rede está se comportando...",
        ),
        OrbitState.Thinking to listOf(
            "Conectando os pontos...",
            "Entendendo o que aconteceu...",
            "Identificando a causa...",
        ),
        OrbitState.Analyzing to listOf(
            "Quase lá...",
            "Preparando seu diagnóstico...",
            "Só mais um instante...",
        ),
        OrbitState.AwaitingInput to listOf(
            "Pronto para aprofundar a análise…",
            "Selecione o que melhor descreve seu problema…",
        ),
        OrbitState.Success to listOf("Análise concluída."),
        OrbitState.Warning to listOf("Análise concluída com alertas."),
        OrbitState.Critical to listOf("Problemas críticos identificados."),
        OrbitState.Idle to listOf("Toque para iniciar o diagnóstico inteligente."),
    )

    fun first(state: OrbitState): String = messages[state]?.firstOrNull() ?: ""

    fun next(state: OrbitState, current: String): String {
        val list = messages[state] ?: return current
        if (list.size <= 1) return current
        val idx = list.indexOf(current)
        return if (idx < 0 || idx >= list.size - 1) list[0] else list[idx + 1]
    }

    fun all(state: OrbitState): List<String> = messages[state] ?: emptyList()
}
