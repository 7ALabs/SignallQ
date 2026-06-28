package io.signallq.app.core.network

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Abstrai os dispatchers de coroutine para permitir substituição em testes.
 *
 * Uso em producao: injete [DefaultDispatcherProvider] via Hilt.
 * Uso em testes: injete [TestDispatcherProvider] com [UnconfinedTestDispatcher].
 */
interface DispatcherProvider {
    val io: CoroutineDispatcher
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val default: CoroutineDispatcher = Dispatchers.Default
}
