package io.signallq.pro.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import io.signallq.pro.core.database.ambiente.AmbienteDao
import io.signallq.pro.core.database.ambiente.AmbienteEntity
import io.signallq.pro.core.database.checklist.ChecklistItemDao
import io.signallq.pro.core.database.checklist.ChecklistItemEntity
import io.signallq.pro.core.database.cliente.ClienteDao
import io.signallq.pro.core.database.cliente.ClienteEntity
import io.signallq.pro.core.database.diagnostico.DiagnosticoAchadoProEntity
import io.signallq.pro.core.database.diagnostico.DiagnosticoProDao
import io.signallq.pro.core.database.diagnostico.DiagnosticoProEntity
import io.signallq.pro.core.database.evidencia.EvidenciaDao
import io.signallq.pro.core.database.evidencia.EvidenciaEntity
import io.signallq.pro.core.database.evidencia.TipoEvidencia
import io.signallq.pro.core.database.local.LocalDao
import io.signallq.pro.core.database.local.LocalEntity
import io.signallq.pro.core.database.medicao.MedicaoProDao
import io.signallq.pro.core.database.medicao.MedicaoProEntity
import io.signallq.pro.core.database.profissional.ProfissionalDao
import io.signallq.pro.core.database.profissional.ProfissionalEntity
import io.signallq.pro.core.database.visita.EtapaVisita
import io.signallq.pro.core.database.visita.StatusVisita
import io.signallq.pro.core.database.visita.TipoVisita
import io.signallq.pro.core.database.visita.VisitaDao
import io.signallq.pro.core.database.visita.VisitaEntity

/**
 * Banco Room greenfield do SignallQ Pro -- issue #1161 (Fase 2). Sem qualquer relacao com
 * `linkaKotlin.db` do consumidor: produto, Firebase/Play e ciclo de vida de dados separados
 * (ver .claude/CLAUDE.md, "Nao-negociaveis por produto").
 */
@Database(
    entities = [
        ProfissionalEntity::class,
        ClienteEntity::class,
        LocalEntity::class,
        VisitaEntity::class,
        AmbienteEntity::class,
        MedicaoProEntity::class,
        DiagnosticoProEntity::class,
        DiagnosticoAchadoProEntity::class,
        EvidenciaEntity::class,
        ChecklistItemEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(SignallQProTypeConverters::class)
abstract class SignallQProDatabase : RoomDatabase() {
    abstract fun profissionalDao(): ProfissionalDao

    abstract fun clienteDao(): ClienteDao

    abstract fun localDao(): LocalDao

    abstract fun visitaDao(): VisitaDao

    abstract fun ambienteDao(): AmbienteDao

    abstract fun medicaoProDao(): MedicaoProDao

    abstract fun diagnosticoProDao(): DiagnosticoProDao

    abstract fun evidenciaDao(): EvidenciaDao

    abstract fun checklistItemDao(): ChecklistItemDao

    companion object {
        const val NOME_ARQUIVO = "signallqPro.db"
    }
}

class SignallQProTypeConverters {
    @TypeConverter
    fun statusVisitaParaTexto(status: StatusVisita): String = status.name

    @TypeConverter
    fun textoParaStatusVisita(valor: String): StatusVisita = StatusVisita.valueOf(valor)

    @TypeConverter
    fun tipoVisitaParaTexto(tipo: TipoVisita): String = tipo.name

    @TypeConverter
    fun textoParaTipoVisita(valor: String): TipoVisita = TipoVisita.valueOf(valor)

    @TypeConverter
    fun etapaVisitaParaTexto(etapa: EtapaVisita): String = etapa.name

    @TypeConverter
    fun textoParaEtapaVisita(valor: String): EtapaVisita = EtapaVisita.valueOf(valor)

    @TypeConverter
    fun tipoEvidenciaParaTexto(tipo: TipoEvidencia): String = tipo.name

    @TypeConverter
    fun textoParaTipoEvidencia(valor: String): TipoEvidencia = TipoEvidencia.valueOf(valor)
}
