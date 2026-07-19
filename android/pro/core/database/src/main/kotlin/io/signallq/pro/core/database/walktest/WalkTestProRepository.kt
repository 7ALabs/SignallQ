package io.signallq.pro.core.database.walktest

import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class WalkTestProRepository
    @Inject
    constructor(
        private val dao: PontoWalkTestDao,
    ) {
        fun observarPorAmbiente(ambienteId: String): Flow<List<PontoWalkTestEntity>> = dao.observarPorAmbiente(ambienteId)

        suspend fun salvarPonto(
            ambienteId: String,
            rssiDbm: Int,
            candidato: Boolean,
        ): String {
            val entidade =
                PontoWalkTestEntity(
                    id = UUID.randomUUID().toString(),
                    ambienteId = ambienteId,
                    rssiDbm = rssiDbm,
                    candidato = candidato,
                    criadoEmEpochMs = System.currentTimeMillis(),
                )
            dao.salvar(entidade)
            return entidade.id
        }
    }
