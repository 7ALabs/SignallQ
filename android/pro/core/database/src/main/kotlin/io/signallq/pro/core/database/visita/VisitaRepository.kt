package io.signallq.pro.core.database.visita

import io.signallq.pro.core.database.checklist.ChecklistItemDao
import io.signallq.pro.core.database.checklist.ChecklistItemEntity
import io.signallq.pro.core.database.local.LocalRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/** Checklist padrao por tipo de visita (tela 2.14) -- MVP0 usa um roteiro fixo por tipo,
 *  sem editor de template (fora de escopo da Fase 2, issue #1161). */
private val CHECKLIST_PADRAO: Map<TipoVisita, List<String>> =
    mapOf(
        TipoVisita.INSTALACAO to
            listOf(
                "Conferir sinal na ONT/roteador",
                "Testar velocidade contratada",
                "Posicionar roteador em local adequado",
                "Orientar cliente sobre rede Wi-Fi",
            ),
        TipoVisita.MANUTENCAO to
            listOf(
                "Diagnosticar reclamacao do cliente",
                "Verificar cabeamento e conectores",
                "Testar velocidade e estabilidade",
            ),
        TipoVisita.VISTORIA to
            listOf(
                "Mapear ambientes e equipamentos",
                "Medir sinal por ambiente",
                "Registrar evidencias fotograficas",
            ),
        TipoVisita.SUPORTE to
            listOf(
                "Ouvir reclamacao do cliente",
                "Executar diagnostico rapido",
                "Definir causa raiz",
            ),
    )

class VisitaRepository
    @Inject
    constructor(
        private val visitaDao: VisitaDao,
        private val checklistItemDao: ChecklistItemDao,
        private val localRepository: LocalRepository,
    ) {
        fun observarVisita(id: String): Flow<VisitaEntity?> = visitaDao.observarPorId(id)

        suspend fun buscarVisita(id: String): VisitaEntity? = visitaDao.buscarPorId(id)

        fun observarVisitaEmAndamento(): Flow<VisitaEntity?> = visitaDao.observarEmAndamento()

        suspend fun buscarVisitaEmAndamento(): VisitaEntity? = visitaDao.buscarEmAndamento()

        fun observarRecentes(limite: Int = 10): Flow<List<VisitaEntity>> = visitaDao.observarRecentes(limite)

        /**
         * Cria a visita + o checklist padrao do tipo escolhido. O local da visita e resolvido
         * implicitamente a partir do cliente -- MVP0 garante que todo cliente tem exatamente um
         * local "Principal" (`ClienteRepository.criarCliente`, issue #1166), entao nao ha tela
         * de selecao de local separada por ora.
         * @return id da visita criada.
         */
        suspend fun criarVisita(
            clienteId: String,
            tipo: TipoVisita,
            modoRapido: Boolean = false,
        ): String {
            val localId =
                requireNotNull(localRepository.buscarPrimeiroPorCliente(clienteId)?.id) {
                    "Cliente $clienteId sem local cadastrado -- todo cliente deve ter ao menos " +
                        "um local (issue #1166)."
                }
            val agora = System.currentTimeMillis()
            val visita =
                VisitaEntity(
                    id = UUID.randomUUID().toString(),
                    clienteId = clienteId,
                    localId = localId,
                    tipo = tipo,
                    status = StatusVisita.EM_ANDAMENTO,
                    etapaAtual = EtapaVisita.CHECKLIST,
                    modoRapido = modoRapido,
                    iniciadaEmEpochMs = agora,
                    atualizadaEmEpochMs = agora,
                )
            visitaDao.salvar(visita)
            val itens =
                CHECKLIST_PADRAO.getOrDefault(tipo, emptyList()).mapIndexed { indice, descricao ->
                    ChecklistItemEntity(
                        id = UUID.randomUUID().toString(),
                        visitaId = visita.id,
                        descricao = descricao,
                        ordem = indice,
                    )
                }
            if (itens.isNotEmpty()) checklistItemDao.salvarTodos(itens)
            return visita.id
        }

        suspend fun avancarEtapa(
            visitaId: String,
            etapa: EtapaVisita,
        ) {
            visitaDao.atualizarEtapa(visitaId, etapa, System.currentTimeMillis())
        }

        suspend fun concluirVisita(visitaId: String) {
            visitaDao.atualizarStatus(visitaId, StatusVisita.CONCLUIDA, System.currentTimeMillis())
        }

        /** Retomada de visita interrompida (issue #1119) -- volta o status para EM_ANDAMENTO
         *  sem alterar a etapa salva. */
        suspend fun retomarVisita(visitaId: String) {
            visitaDao.atualizarStatus(visitaId, StatusVisita.EM_ANDAMENTO, System.currentTimeMillis())
        }

        suspend fun marcarInterrompida(visitaId: String) {
            visitaDao.atualizarStatus(visitaId, StatusVisita.INTERROMPIDA, System.currentTimeMillis())
        }
    }
