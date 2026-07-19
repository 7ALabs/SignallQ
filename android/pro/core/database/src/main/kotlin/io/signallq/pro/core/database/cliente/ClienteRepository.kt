package io.signallq.pro.core.database.cliente

import androidx.room.withTransaction
import io.signallq.pro.core.database.SignallQProDatabase
import io.signallq.pro.core.database.local.LocalRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class ClienteRepository
    @Inject
    constructor(
        private val db: SignallQProDatabase,
        private val dao: ClienteDao,
        private val localRepository: LocalRepository,
    ) {
        fun observarClientes(): Flow<List<ClienteEntity>> = dao.observarTodos()

        suspend fun buscarPorId(id: String): ClienteEntity? = dao.buscarPorId(id)

        /**
         * Cria o cliente e o local "Principal" associado, na MESMA transacao Room -- achado do
         * Rhodolfo na PR #1167: sem transacao, morte do processo entre os dois inserts deixava
         * um cliente persistido sem local, cenario alcancavel em producao (nao teorico) que
         * quebra o invariante que [io.signallq.pro.core.database.visita.VisitaRepository]
         * assume (todo cliente tem >= 1 local). [endereco] pode ficar em branco (cadastro
         * rapido, doc 09 §11: "endereco completo pode ser concluido depois").
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
            db.withTransaction {
                dao.salvar(entidade)
                localRepository.criarLocal(clienteId = entidade.id, nome = "Principal", endereco = endereco)
            }
            return entidade.id
        }
    }
