package io.signallq.app.core.telephony

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MonitorTelephonyTest {

    @Test
    fun fake_iniciaENotaContagem() {
        val fake = MonitorTelephonyFake()
        assertEquals(0, fake.vezesIniciado)
        fake.iniciar()
        fake.iniciar()
        assertEquals(2, fake.vezesIniciado)
    }

    @Test
    fun fake_emiteSnapshotValido() {
        val snap = MovelSnapshot(
            operadora = "Vivo",
            tecnologia = "5G NSA",
            rsrpDbm = -98,
            rsrqDb = -11,
            sinrDb = 8,
            ecnoDb = null,
            bandaMovel = "n78 (3.5 GHz)",
            cellId = 123_456_789L,
            mcc = "724",
            mnc = "06",
            tac = 4321,
            roaming = false,
            timestampMs = 1700000000000L,
        )
        val fake = MonitorTelephonyFake(snapshotInicial = snap)
        assertEquals(snap, fake.snapshotFlow.value)
    }

    @Test
    fun fake_emiteNullPorPadrao() {
        val fake = MonitorTelephonyFake()
        assertNull(fake.snapshotFlow.value)
    }

    @Test
    fun fake_atualizaSnapshotAoEmitir() {
        val fake = MonitorTelephonyFake()
        assertNull(fake.snapshotFlow.value)
        fake.emitir(MovelSnapshot(
            operadora = "TIM", tecnologia = "4G", rsrpDbm = -110, rsrqDb = -15,
            sinrDb = -2, ecnoDb = null, bandaMovel = "B3 (1800 MHz)",
            cellId = 999L, mcc = "724", mnc = "04", tac = 100,
            roaming = false, timestampMs = 1L,
        ))
        assertNotNull(fake.snapshotFlow.value)
        assertEquals("TIM", fake.snapshotFlow.value?.operadora)
        assertEquals("4G", fake.snapshotFlow.value?.tecnologia)
    }

    @Test
    fun snapshot_radioDesligado_naoTemMetricasDeSinal() {
        // #393 — em modo aviao o radio esta desligado: nao deve haver
        // RSRP/tecnologia/operadora, apenas o flag explicito.
        val snap = MovelSnapshot(
            operadora = null, tecnologia = null, rsrpDbm = null, rsrqDb = null,
            sinrDb = null, ecnoDb = null, bandaMovel = null, cellId = null,
            mcc = null, mnc = null, tac = null, roaming = null,
            radioDesligado = true, timestampMs = 1L,
        )
        assertNull(snap.rsrpDbm)
        assertEquals(true, snap.radioDesligado)
    }

    @Test
    fun snapshot_radioDesligado_padraoFalse() {
        val snap = MovelSnapshot(
            operadora = "Vivo", tecnologia = "4G", rsrpDbm = -90, rsrqDb = -10,
            sinrDb = 5, ecnoDb = null, bandaMovel = null, cellId = null,
            mcc = null, mnc = null, tac = null, roaming = false, timestampMs = 1L,
        )
        assertEquals(false, snap.radioDesligado)
    }
}
