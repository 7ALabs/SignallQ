package io.signallq.app.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache em memoria do ultimo provedor de internet (ISP) resolvido pelo
 * [io.signallq.app.MainViewModel] (via ipapi.co, ver `coletarIspInfo`).
 *
 * Existe para que singletons fora do escopo do ViewModel — como o
 * [io.signallq.app.speedtest.SpeedtestPersistenceCoordinator] — possam ler o
 * ISP ja identificado sem repetir a chamada de rede. GH#412: o provedor Wi-Fi
 * ficava "preso" dentro do ViewModel e nunca chegava ao evento de ingest do
 * Painel Admin.
 */
@Singleton
class IspInfoCache
    @Inject
    constructor() {
        @Volatile
        var ultimoIspNome: String? = null
            private set

        fun atualizar(ispNome: String?) {
            if (!ispNome.isNullOrBlank()) {
                ultimoIspNome = ispNome
            }
        }
    }
