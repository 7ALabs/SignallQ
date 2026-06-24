package io.veloo.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apelido_dispositivo")
data class ApelidoDispositivoEntity(
    @PrimaryKey
    val mac: String,
    /** Apelido definido pelo usuario. Null indica dispositivo conhecido sem apelido atribuido. */
    val apelido: String?,
)
