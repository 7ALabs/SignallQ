package io.signallq.app.core.diagnostico

/**
 * GH#1207 — identidade e classificacao de um canal no espectro exibido pela tela Sinal > Canal.
 *
 * @property banda item 2 — identidade composta com [canal]: "2.4GHz"/"5GHz"/"6GHz". Sem isso,
 * o modo "Todos" (que combina as 3 bandas) usava so [canal] como chave, colidindo quando o mesmo
 * numero de canal existe em bandas diferentes (chave duplicada em LazyColumn, detalhe da banda
 * errada, canal atual marcado na banda errada).
 * @property fracaoInterferencia item 4 — score de interferencia normalizado (0.0 = livre, cresce
 * conforme a banda fica mais congestionada), a MESMA fonte que decide [nivel] e a recomendacao —
 * usado tambem pra desenhar a barra de ocupacao na UI (antes a barra usava so `count / 8`,
 * independente do [nivel], podendo mostrar uma barra cheia pra um canal classificado como livre).
 * @property larguraEstimada item 3 — true quando pelo menos um vizinho contabilizado neste canal
 * nao tinha largura real reportada pelo scan (assumida 20 MHz por padrao) — a UI deve sinalizar
 * que a analise e uma estimativa nesse caso, reduzindo a confianca exibida.
 */
data class DadoCanal(
    val canal: Int,
    val banda: String,
    val count: Int,
    val countProprios: Int = 0,
    val countTerceiros: Int = count,
    val maxRssiDbm: Int?,
    val nivel: NivelCongestionamento,
    val fracaoInterferencia: Double = 0.0,
    val larguraEstimada: Boolean = false,
    val ehCanalAtual: Boolean,
    val ehCanalRecomendado: Boolean,
)
