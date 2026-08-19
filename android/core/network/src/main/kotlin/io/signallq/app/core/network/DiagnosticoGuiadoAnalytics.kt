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
