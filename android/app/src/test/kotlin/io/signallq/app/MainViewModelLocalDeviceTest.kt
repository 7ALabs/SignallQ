package io.signallq.app

import io.signallq.app.core.network.contracts.localdevice.DeviceType
import io.signallq.app.feature.fibra.EstadoFibra
import io.signallq.app.feature.fibra.GponStatus
import io.signallq.app.feature.fibra.NokiaLocalDeviceMapper
import io.signallq.app.feature.fibra.SnapshotFibra
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Reproduz o mapeamento exato de `MainViewModel.localDeviceSnapshot`
 * (`executorFibra.snapshotFlow.map { NokiaLocalDeviceMapper.map(it, ...) }`)
 * de forma isolada — MainViewModel nao e instanciavel em unit test puro
 * (dependencias Hilt/AndroidViewModel, ver MainViewModelHistoricoTest).
 *
 * GH#865 Fase 1.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelLocalDeviceTest {
    private val gponOk =
        GponStatus(
            status = "up",
            mode = "VlanMuxMode",
            rxPowerDbm = -19.8,
            txPowerDbm = 2.1,
            temperatureCelsius = 48.5,
            serial = "ALCL12345678",
            voltageV = 3.25,
            laserCurrentMa = 13.99,
        )

    @Test
    fun `localDeviceSnapshot fica nao-nulo apos leitura de fibra concluida com sucesso`() =
        runTest {
            val snapshotFibraFlow =
                MutableStateFlow(
                    SnapshotFibra(
                        estado = EstadoFibra.idle,
                        gpon = null,
                        wan = null,
                        ppp = null,
                        deviceInfo = null,
                        erroMensagem = null,
                    ),
                )
            val localDeviceFlow = snapshotFibraFlow.map { NokiaLocalDeviceMapper.map(it, System.currentTimeMillis()) }

            assertNull(localDeviceFlow.first())

            snapshotFibraFlow.value =
                SnapshotFibra(
                    estado = EstadoFibra.concluido,
                    gpon = gponOk,
                    wan = null,
                    ppp = null,
                    deviceInfo = null,
                    erroMensagem = null,
                )

            val resultado = localDeviceFlow.first()
            assertNotNull(resultado)
            assertEquals(DeviceType.ONT_GPON, resultado?.deviceType)
        }

    @Test
    fun `localDeviceSnapshot permanece nulo quando a leitura de fibra falha`() =
        runTest {
            val snapshotFibraFlow =
                MutableStateFlow(
                    SnapshotFibra(
                        estado = EstadoFibra.erro,
                        gpon = null,
                        wan = null,
                        ppp = null,
                        deviceInfo = null,
                        erroMensagem = "erroModemInacessivel",
                    ),
                )
            val localDeviceFlow = snapshotFibraFlow.map { NokiaLocalDeviceMapper.map(it, System.currentTimeMillis()) }

            assertNull(localDeviceFlow.first())
        }
}
