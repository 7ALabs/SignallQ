package io.signallq.pro.feature.laudo

import io.signallq.pro.core.database.diagnostico.DiagnosticoAchadoProEntity
import io.signallq.pro.core.database.diagnostico.DiagnosticoProEntity
import io.signallq.pro.core.database.evidencia.EvidenciaEntity
import io.signallq.pro.core.database.medicao.MedicaoProEntity
import io.signallq.pro.core.database.visita.TipoVisita

/** Agregado de um ambiente para compor o laudo -- une medicao, diagnostico/achados e
 *  evidencias do mesmo ambiente (todos ja persistidos nas telas 2.10-2.12/2.15-2.16). */
data class LaudoAmbienteDados(
    val nome: String,
    val medicao: MedicaoProEntity?,
    val diagnostico: DiagnosticoProEntity?,
    val achados: List<DiagnosticoAchadoProEntity>,
    val evidencias: List<EvidenciaEntity>,
)

/** Agregado completo da visita usado para montar o laudo (tela 3.2). Traduz varias
 *  entidades de `:pro:core:database` num unico modelo de dominio, para que
 *  [LaudoHtmlGenerator] permaneca puro e testavel em JVM sem depender de Room. */
data class LaudoDados(
    val visitaId: String,
    val profissionalNome: String,
    val clienteNome: String,
    val clienteTelefone: String?,
    val localNome: String,
    val localEndereco: String,
    val tipoVisita: TipoVisita,
    val dataVisitaEpochMs: Long,
    val ambientes: List<LaudoAmbienteDados>,
) {
    val totalEvidencias: Int get() = ambientes.sumOf { it.evidencias.size }
    val totalAchados: Int get() = ambientes.sumOf { it.achados.size }
}
