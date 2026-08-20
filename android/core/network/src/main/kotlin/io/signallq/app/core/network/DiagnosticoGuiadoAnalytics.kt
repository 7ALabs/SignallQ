package io.signallq.app.core.network

/**
 * Funil do diagnostico guiado 2.0 — issue #1706 (2.0.09d), spec §12, epico #1647.
 *
 * Complementa os eventos do Assist (`AssistAnalytics.kt`), que cobrem a escolha do sintoma e o
 * abandono. Aqui entra o passo 3 do funil minimo: o plano de analise sendo apresentado. O passo 4
 * (bloqueio encontrado) nao entra nesta fatia — ver o comentario no fim deste arquivo.
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

// O passo 4 do funil (`diagnostico_bloqueio_encontrado`) NAO entra nesta fatia. Ele existia aqui e
// saiu junto com a preparacao contextual: a premissa de que o sinal Wi-Fi depende da permissao de
// localizacao era falsa (bloqueio B5 de Caio na PR #1732 — o RSSI e lido incondicionalmente), e a
// unica capacidade que de fato exige localizacao, `CANAIS_WIFI`, o motor ainda nao avalia.
//
// Sem gatilho vivo, o evento nao teria produtor e o vocabulario de tipo/resolucao seria enum morto.
// Decisao de Luiz em 2026-08-18: entregar o plano sem a preparacao. Volta com ela.

/**
 * Passo 8 do funil (GH#1707, Task 2.0.09e, épico #1647, spec §12): o usuário acionou "Testar
 * novamente" **vinculado** a uma análise anterior — nunca "recomeçar do zero" (isso é o evento 1
 * com `origem` própria, ver `AssistAnalytics`/`DiagnosticoGuiadoAnalytics`).
 *
 * [acaoAnteriorId] vazio quando a pessoa retestou sem executar nenhuma ação antes.
 * [mesmoContextoRede] só é `true` quando o `networkId` atual bate com o da medição original —
 * calculado no mesmo resolvedor que decide comparabilidade (`ResolvedorNetworkId`,
 * `core/database`), nunca inferido de outra forma.
 */
data class DiagnosticoRetesteIniciado(
    val analiseId: String,
    val retesteId: String,
    val acaoAnteriorId: String,
    val intervaloMs: Long,
    val mesmoContextoRede: Boolean,
)

/**
 * Passo 9 do funil (GH#1707, Task 2.0.09e) — a comparação entre a análise original e o reteste foi
 * calculada e exibida. [veredito] reusa o vocabulário de `TendenciaEstado`
 * (`melhorou`/`nao_mudou`/`piorou`) mais `inconclusiva` quando [comparavel] é `false` — spec §8.8 /
 * decisão de Luiz em 2026-08-20 (§14.6): só o veredito simples, nunca números lado a lado.
 *
 * [statusAnterior]/[statusNovo] usam o vocabulário de `DiagnosticStatus`
 * (`ok`/`info`/`attention`/`critical`/`inconclusive`).
 */
data class DiagnosticoComparacaoConcluida(
    val analiseId: String,
    val retesteId: String,
    val veredito: String,
    val comparavel: Boolean,
    val statusAnterior: String,
    val statusNovo: String,
)
