package io.linka.app.kotlin

import io.linka.app.kotlin.feature.fibra.EstadoFibra
import io.linka.app.kotlin.feature.fibra.GponStatus
import io.linka.app.kotlin.feature.fibra.SnapshotFibra
import io.linka.app.kotlin.ui.screen.FibraModemUiState
import io.linka.app.kotlin.ui.screen.mapearSnapshotFibra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FibraModemUiStateTest {
    private val gponValido =
        GponStatus(
            status = "up",
            mode = "GPON",
            rxPowerDbm = -20.0,
            txPowerDbm = 2.0,
            temperatureCelsius = 45.0,
            serial = "ABCD1234",
            voltageV = 3.3,
            laserCurrentMa = 10.0,
        )

    private val snapshotConcluido =
        SnapshotFibra(
            estado = EstadoFibra.concluido,
            gpon = gponValido,
            wan = null,
            ppp = null,
            deviceInfo = null,
            erroMensagem = null,
        )

    @Test
    fun `retorna SemWifi quando temWifi e false`() {
        val resultado =
            mapearSnapshotFibra(
                snapshot = snapshotConcluido,
                temWifi = false,
                temCredenciais = true,
            )
        assertEquals(FibraModemUiState.SemWifi, resultado)
    }

    @Test
    fun `retorna SemCredenciais quando sem credenciais`() {
        val resultado =
            mapearSnapshotFibra(
                snapshot = snapshotConcluido,
                temWifi = true,
                temCredenciais = false,
            )
        assertEquals(FibraModemUiState.SemCredenciais, resultado)
    }

    @Test
    fun `retorna Conectando quando snapshot e null`() {
        val resultado =
            mapearSnapshotFibra(
                snapshot = null,
                temWifi = true,
                temCredenciais = true,
            )
        assertEquals(FibraModemUiState.Conectando, resultado)
    }

    @Test
    fun `retorna Conectando quando estado e idle`() {
        val snapshot = snapshotConcluido.copy(estado = EstadoFibra.idle, gpon = null)
        val resultado =
            mapearSnapshotFibra(
                snapshot = snapshot,
                temWifi = true,
                temCredenciais = true,
            )
        assertEquals(FibraModemUiState.Conectando, resultado)
    }

    @Test
    fun `retorna Conectando quando estado e conectando`() {
        val snapshot = snapshotConcluido.copy(estado = EstadoFibra.conectando, gpon = null)
        val resultado =
            mapearSnapshotFibra(
                snapshot = snapshot,
                temWifi = true,
                temCredenciais = true,
            )
        assertEquals(FibraModemUiState.Conectando, resultado)
    }

    @Test
    fun `retorna Concluido com gpon valido`() {
        val resultado =
            mapearSnapshotFibra(
                snapshot = snapshotConcluido,
                temWifi = true,
                temCredenciais = true,
            )
        assertTrue(resultado is FibraModemUiState.Concluido)
        val concluido = resultado as FibraModemUiState.Concluido
        assertEquals(gponValido, concluido.gpon)
    }

    @Test
    fun `retorna Erro com chave fibra sem gpon quando concluido sem gpon`() {
        val snapshot = snapshotConcluido.copy(gpon = null)
        val resultado =
            mapearSnapshotFibra(
                snapshot = snapshot,
                temWifi = true,
                temCredenciais = true,
            )
        assertEquals(FibraModemUiState.Erro("fibra.sem_gpon"), resultado)
    }

    @Test
    fun `retorna Erro com mensagem do snapshot quando estado e erro`() {
        val snapshot =
            snapshotConcluido.copy(
                estado = EstadoFibra.erro,
                erroMensagem = "timeout",
            )
        val resultado =
            mapearSnapshotFibra(
                snapshot = snapshot,
                temWifi = true,
                temCredenciais = true,
            )
        assertEquals(FibraModemUiState.Erro("timeout"), resultado)
    }

    @Test
    fun `retorna Erro com chave generica quando estado e erro sem mensagem`() {
        val snapshot =
            snapshotConcluido.copy(
                estado = EstadoFibra.erro,
                erroMensagem = null,
            )
        val resultado =
            mapearSnapshotFibra(
                snapshot = snapshot,
                temWifi = true,
                temCredenciais = true,
            )
        assertEquals(FibraModemUiState.Erro("fibra.erro_desconhecido"), resultado)
    }
}
