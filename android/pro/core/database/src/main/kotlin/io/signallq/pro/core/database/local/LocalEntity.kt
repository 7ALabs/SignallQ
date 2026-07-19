package io.signallq.pro.core.database.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local de atendimento de um cliente (residencia, filial, loja etc.) -- entidade propria no
 * dicionario canonico (`00_CANONICO_v5.md`): um cliente pode ter mais de um local. O MVP0
 * (issue #1119/#1166) cria exatamente um local "Principal" junto com o cadastro rapido do
 * cliente (`NovoClienteScreen`) -- multiplos locais por cliente e edicao de local ficam para
 * uma fase futura, sem editor dedicado por ora.
 */
@Entity(tableName = "local")
data class LocalEntity(
    @PrimaryKey val id: String,
    val clienteId: String,
    val nome: String,
    val endereco: String,
    val criadoEmEpochMs: Long,
)
