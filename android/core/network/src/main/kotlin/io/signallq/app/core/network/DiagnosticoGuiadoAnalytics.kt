package io.signallq.app.core.network

/**
 * Funil do diagnostico guiado 2.0 — issue #1706 (2.0.09d), spec §12, epico #1647.
 *
 * Complementa os eventos do Assist (`AssistAnalytics.kt`), que cobrem a escolha do sintoma e o
 * abandono. Aqui entram os dois passos que faltavam do funil minimo: o plano de analise sendo
 * apresentado e o bloqueio sendo encontrado.
 *
 * Nenhuma propriedade carrega SSID, IP, MAC, descricao livre ou resposta que identifique a pessoa
 * — restricao da spec §12, e o mesmo criterio que `AssistAnalytics` ja aplica.
 */

/**
 * Passo 3 do funil: o plano foi montado e exibido, antes da medicao.
 *
 * [capacidades] usa os ids estaveis de `Capacidade` (spec §7) separados por virgula — sao nomes
 * persistidos, nao rotulos de tela. [planoAdaptado] permite medir quantas jornadas rodam com plano
 * reduzido por permissao ou contexto, que e o indicador que a spec §8.4 torna relevante.
 */
data class DiagnosticoPlanoIniciado(
    val analiseId: String,
    val objetivoId: String,
    val capacidades: String,
    val qtdCapacidades: Long,
    val planoAdaptado: Boolean,
)

/**
 * Passo 4 do funil: um bloqueio foi **apresentado** a pessoa e ela respondeu (ou abandonou).
 *
 * NAO dispara em checagem silenciosa de estado: se a permissao ja estava concedida, nao houve
 * bloqueio. O que se quer medir e quantas jornadas encontram uma parede e o que acontece depois —
 * em especial [planoContinuou], porque a spec §8.4 exige que permissao negada **nao** encerre a
 * jornada, e sem esse dado nao da para saber se a regra esta valendo em campo.
 */
data class DiagnosticoBloqueioEncontrado(
    val analiseId: String,
    val tipo: TipoBloqueioDiagnostico,
    val resolucao: ResolucaoBloqueioDiagnostico,
    val planoContinuou: Boolean,
)

/** Vocabulario fechado da spec de telemetria da 2.0.09a. */
enum class TipoBloqueioDiagnostico(val analyticsId: String) {
    PERMISSAO_LOCALIZACAO("permissao_localizacao"),
    PERMISSAO_TELEFONIA("permissao_telefonia"),
    OFFLINE("offline"),
    REMOTO_INDISPONIVEL("remoto_indisponivel"),
    REDE_MOVEL_DADOS("rede_movel_dados"),
}

/** Vocabulario fechado da spec de telemetria da 2.0.09a. */
enum class ResolucaoBloqueioDiagnostico(val analyticsId: String) {
    CONCEDIDO("concedido"),
    NEGADO("negado"),
    NEGADO_PERMANENTE("negado_permanente"),
    CONTORNADO("contornado"),
    ABANDONOU("abandonou"),
}
