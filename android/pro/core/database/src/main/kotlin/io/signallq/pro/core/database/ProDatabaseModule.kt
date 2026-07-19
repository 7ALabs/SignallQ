package io.signallq.pro.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.signallq.pro.core.database.ambiente.AmbienteDao
import io.signallq.pro.core.database.checklist.ChecklistItemDao
import io.signallq.pro.core.database.cliente.ClienteDao
import io.signallq.pro.core.database.diagnostico.DiagnosticoProDao
import io.signallq.pro.core.database.evidencia.EvidenciaDao
import io.signallq.pro.core.database.local.LocalDao
import io.signallq.pro.core.database.medicao.MedicaoProDao
import io.signallq.pro.core.database.profissional.ProfissionalDao
import io.signallq.pro.core.database.visita.VisitaDao
import javax.inject.Singleton

/**
 * Issue #1166: cria a tabela `local` (ausente ate aqui) e backfilla um local "Principal" por
 * cliente ja existente -- os dados de dev/teste anteriores a essa migracao nao tinham
 * endereco, entao o backfill usa string vazia (unico valor disponivel) em vez de perder a
 * visita/cliente. `visita.localId` e resolvido por join com o `local` recem-criado do mesmo
 * cliente (MVP0: um cliente tem exatamente um local nesse momento).
 */
private val migracaoLocal1Para2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `local` (`id` TEXT NOT NULL, `clienteId` TEXT NOT NULL, " +
                    "`nome` TEXT NOT NULL, `endereco` TEXT NOT NULL, `criadoEmEpochMs` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "INSERT INTO local (id, clienteId, nome, endereco, criadoEmEpochMs) " +
                    "SELECT id || '-local', id, 'Principal', '', criadoEmEpochMs FROM cliente",
            )
            db.execSQL("ALTER TABLE visita ADD COLUMN localId TEXT NOT NULL DEFAULT ''")
            db.execSQL(
                "UPDATE visita SET localId = (SELECT id FROM local WHERE local.clienteId = visita.clienteId LIMIT 1) " +
                    "WHERE localId = ''",
            )
        }
    }

@Module
@InstallIn(SingletonComponent::class)
object ProDatabaseModule {
    @Provides
    @Singleton
    fun provideSignallQProDatabase(
        @ApplicationContext context: Context,
    ): SignallQProDatabase =
        Room
            .databaseBuilder(context, SignallQProDatabase::class.java, SignallQProDatabase.NOME_ARQUIVO)
            .addMigrations(migracaoLocal1Para2)
            .build()

    @Provides
    fun provideProfissionalDao(db: SignallQProDatabase): ProfissionalDao = db.profissionalDao()

    @Provides
    fun provideClienteDao(db: SignallQProDatabase): ClienteDao = db.clienteDao()

    @Provides
    fun provideLocalDao(db: SignallQProDatabase): LocalDao = db.localDao()

    @Provides
    fun provideVisitaDao(db: SignallQProDatabase): VisitaDao = db.visitaDao()

    @Provides
    fun provideAmbienteDao(db: SignallQProDatabase): AmbienteDao = db.ambienteDao()

    @Provides
    fun provideMedicaoProDao(db: SignallQProDatabase): MedicaoProDao = db.medicaoProDao()

    @Provides
    fun provideDiagnosticoProDao(db: SignallQProDatabase): DiagnosticoProDao = db.diagnosticoProDao()

    @Provides
    fun provideEvidenciaDao(db: SignallQProDatabase): EvidenciaDao = db.evidenciaDao()

    @Provides
    fun provideChecklistItemDao(db: SignallQProDatabase): ChecklistItemDao = db.checklistItemDao()
}
