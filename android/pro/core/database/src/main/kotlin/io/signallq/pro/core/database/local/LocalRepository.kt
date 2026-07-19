package io.signallq.pro.core.database.local

import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class LocalRepository
    @Inject
    constructor(
        private val dao: LocalDao,
    ) {
        fun observarPorCliente(clienteId: String): Flow<List<LocalEntity>> = dao.observarPorCliente(clienteId)

        suspend fun buscarPorId(id: String): LocalEntity? = dao.buscarPorId(id)

        suspend fun buscarPrimeiroPorCliente(clienteId: String): LocalEntity? = dao.buscarPrimeiroPorCliente(clienteId)

        /** @return id do local criado. */
        suspend fun criarLocal(
            clienteId: String,
            nome: String,
            endereco: String,
        ): String {
            val entidade =
                LocalEntity(
                    id = UUID.randomUUID().toString(),
                    clienteId = clienteId,
                    nome = nome,
                    endereco = endereco,
                    criadoEmEpochMs = System.currentTimeMillis(),
                )
            dao.salvar(entidade)
            return entidade.id
        }
    }
