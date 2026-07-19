package io.signallq.pro.core.database.cliente

import io.signallq.pro.core.database.local.LocalRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class ClienteRepository
    @Inject
    constructor(
        private val dao: ClienteDao,
        private val localRepository: LocalRepository,
    ) {
        fun observarClientes(): Flow<List<ClienteEntity>> = dao.observarTodos()

        suspend fun buscarPorId(id: String): ClienteEntity? = dao.buscarPorId(id)

        /**
         * Cria o cliente e o local "Principal" associado -- todo cliente do MVP0 nasce com
         * exatamente um local (issue #1166: "local" e entidade propria no dicionario
         * canonico, ausente ate aqui do fluxo cliente -> visita). [endereco] pode ficar em
         * branco (cadastro rapido, doc 09 §11: "endereco completo pode ser concluido depois").
         * @return id do cliente criado.
         */
        suspend fun criarCliente(
            nome: String,
            telefone: String?,
            endereco: String,
        ): String {
            val entidade =
                ClienteEntity(
                    id = UUID.randomUUID().toString(),
                    nome = nome,
                    telefone = telefone,
                    criadoEmEpochMs = System.currentTimeMillis(),
                )
            dao.salvar(entidade)
            localRepository.criarLocal(clienteId = entidade.id, nome = "Principal", endereco = endereco)
            return entidade.id
        }
    }
