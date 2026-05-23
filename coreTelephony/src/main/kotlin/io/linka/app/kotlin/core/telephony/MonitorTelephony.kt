package io.linka.app.kotlin.core.telephony

import kotlinx.coroutines.flow.StateFlow

/**
 * Fonte de dados de telefonia movel para o diagnostico.
 *
 * Uso:
 *  - Chame [iniciar] uma vez quando souber que o usuario esta em rede movel
 *    (ex: MainViewModel detecta connectionType=mobile). Evite no app cold-start
 *    para economizar bateria.
 *  - Leia [snapshotFlow.value] quando precisar (ex: ao montar payload para a IA).
 *  - Chame [encerrar] quando sair da rede movel ou ao destruir o ViewModel.
 *
 * Contrato de erro: nunca lanca. Se a permissao READ_PHONE_STATE nao foi
 * concedida, ou nao ha SIM ativa, ou o ambiente e um emulador sem suporte,
 * snapshotFlow.value continua null (e e logado uma vez por sessao).
 */
interface MonitorTelephony {
    val snapshotFlow: StateFlow<MovelSnapshot?>

    /** Comeca a observar mudancas de sinal/celula. Idempotente. */
    fun iniciar()

    /** Para de observar e libera callbacks. Idempotente. */
    fun encerrar()
}
