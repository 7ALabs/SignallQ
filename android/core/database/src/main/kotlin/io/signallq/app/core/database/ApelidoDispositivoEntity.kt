package io.signallq.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apelido_dispositivo")
data class ApelidoDispositivoEntity(
    /**
     * #853 — endereco MAC quando resolvivel via ARP, ou a chave sintetica
     * "ipnome:<ip>:<nome>" (ver `DispositivoRede.chaveApelido()`) quando o Android
     * nao consegue resolver o MAC do dispositivo — cenario comum, ja documentado
     * em [io.signallq.app.feature.devices.DispositivosIdentidadeHelper].
     */
    @PrimaryKey
    val mac: String,
    /** Apelido definido pelo usuario. Null indica dispositivo conhecido sem apelido atribuido. */
    val apelido: String?,
)
