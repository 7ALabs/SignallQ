package io.signallq.pro.core.database.walktest

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ponto amostrado durante a Walk Test (tela 2.11) -- issue #1176. Tabela nova, aditiva
 * (migração `migracaoWalkTest2Para3`), sem relação com `medicao_pro` (que é resultado de
 * speedtest, não amostra de RSSI ao caminhar pelo ambiente).
 */
@Entity(tableName = "ponto_walktest_pro")
data class PontoWalkTestEntity(
    @PrimaryKey val id: String,
    val ambienteId: String,
    val rssiDbm: Int,
    /** true = "Marcar ponto candidato" (sinal notavelmente melhor que o pior da sessão);
     *  false = "Salvar medição" (ponto simples, sem qualificar como candidato). */
    val candidato: Boolean,
    val criadoEmEpochMs: Long,
)
