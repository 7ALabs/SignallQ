package io.veloo.app.core.telephony

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * Fake usado em testes unitarios. Permite injetar um snapshot pre-fabricado
 * (ou null) e contar quantas vezes [iniciar] foi chamado — util para validar
 * que o monitor NAO e iniciado em rede Wi-Fi.
 */
class MonitorTelephonyFake(
    snapshotInicial: MovelSnapshot? = null,
) : MonitorTelephony {

    private val mutable = MutableStateFlow(snapshotInicial)
    override val snapshotFlow: StateFlow<MovelSnapshot?> = mutable.asStateFlow()

    private val iniciarCount = AtomicInteger(0)
    private val encerrarCount = AtomicInteger(0)

    val vezesIniciado: Int get() = iniciarCount.get()
    val vezesEncerrado: Int get() = encerrarCount.get()

    override fun iniciar() {
        iniciarCount.incrementAndGet()
    }

    override fun encerrar() {
        encerrarCount.incrementAndGet()
    }

    // simsAtivos configuravel para testes de dual SIM
    var simsAtivosParaTeste: List<MovelSimSnapshot> = emptyList()

    override fun captureSimsAtivos(context: Context): List<MovelSimSnapshot> = simsAtivosParaTeste

    fun emitir(snap: MovelSnapshot?) {
        mutable.value = snap
    }
}
