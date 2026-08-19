package io.signallq.app.feature.speedtest

import io.signallq.app.core.network.EstadoConexao

enum class ModoSpeedtest {
    fast,
    complete,
}

/**
 * GH#1737 (Task 2.0.10a, épico #1647) — decisão de produto: o SignallQ 2.0 não oferece mais
 * escolha manual de modo (o antigo seletor tinha 3 opções, incluindo `triplo`, removido). O
 * modo é decidido automaticamente pelo tipo de rede no momento em que a medição é disparada.
 *
 * Rede móvel usa [ModoSpeedtest.fast] — download apenas, ~10 MB, para não pesar no plano de
 * dados. Qualquer outra conexão (Wi-Fi, Ethernet, desconhecida) usa [ModoSpeedtest.complete] —
 * download e upload. `desconectado` cai no mesmo padrão de `complete`, mas na prática nunca
 * chega a ser usado: o botão de iniciar teste já fica desabilitado sem conexão.
 */
fun modoAutomaticoPara(estadoConexao: EstadoConexao): ModoSpeedtest =
    if (estadoConexao == EstadoConexao.movel) ModoSpeedtest.fast else ModoSpeedtest.complete
