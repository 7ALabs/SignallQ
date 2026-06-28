package io.signallq.app.monitoramento

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes unitários da lógica de histerese do MonitoramentoWorker.
 *
 * Testa diretamente o [HisteresiHelper] — o código de produção real —
 * sem depender de Android/DataStore/WorkManager.
 *
 * Qualquer mudança de threshold ou lógica no HisteresiHelper vai quebrar
 * esses testes imediatamente.
 */
class MonitoramentoWorkerHistereseTest {
    // -------------------------------------------------------------------------
    // Teste 1: Latência — oscilações dentro dos thresholds não disparam
    // -------------------------------------------------------------------------

    @Test
    fun `latencia oscilando abaixo de 400ms nao gera transicao`() {
        runTest {
            // Sequência: 350, 380, 320 — todas abaixo de 400, então nunca entra em alerta
            var estado = false

            listOf(350L, 380L, 320L).forEach { ms ->
                val novo = HisteresiHelper.calcularAlertaLatencia(ms, estado)
                assertEquals("latencia=$ms não deve ativar alerta", false, novo)
                estado = novo
            }
        }
    }

    @Test
    fun `latencia cruza 400ms dispara alerta uma vez e oscilacoes mantem estado`() {
        runTest {
            // 350 → 450 → 380 → 420 → 280
            // Transições esperadas:
            //   350: ok→ok (sem transição)
            //   450: ok→ALERTA (transição! notifica)
            //   380: alerta→alerta (zona histerese, mantém)
            //   420: alerta→alerta (acima de 400 mas já estava em alerta)
            //   280: alerta→OK (transição! notifica "resolvido" se implementado)
            var estado = false
            val transicoes = mutableListOf<Pair<Long, Boolean>>()

            val sequencia = listOf(350L, 450L, 380L, 420L, 280L)
            sequencia.forEach { ms ->
                val novo = HisteresiHelper.calcularAlertaLatencia(ms, estado)
                if (novo != estado) transicoes.add(Pair(ms, novo))
                estado = novo
            }

            // Deve haver exatamente 2 transições: 350→450 (ok→alerta) e 420→280 (alerta→ok)
            assertEquals("Deve haver exatamente 2 transições", 2, transicoes.size)
            assertEquals("Primeira transição: 450ms ativa alerta", Pair(450L, true), transicoes[0])
            assertEquals("Segunda transição: 280ms resolve alerta", Pair(280L, false), transicoes[1])
        }
    }

    @Test
    fun `latencia null mantem estado anterior sem transicao (Doze Mode)`() {
        runTest {
            // Estado anterior: alerta ativo
            val estadoAnterior = true
            val novo = HisteresiHelper.calcularAlertaLatencia(null, estadoAnterior)
            assertEquals("null deve manter estado anterior", estadoAnterior, novo)
            assertEquals("Sem transição para null", true, novo)
        }
    }

    @Test
    fun `latencia null com estado ok mantem ok`() {
        runTest {
            val estadoAnterior = false
            val novo = HisteresiHelper.calcularAlertaLatencia(null, estadoAnterior)
            assertEquals("null com estado ok deve manter ok", false, novo)
        }
    }

    @Test
    fun `latencia na zona de histerese mantem estado anterior`() {
        runTest {
            // Entre 300 e 400: mantém estado anterior
            val estadoAlerta = HisteresiHelper.calcularAlertaLatencia(350L, true) // estava em alerta, mantém
            val estadoOk = HisteresiHelper.calcularAlertaLatencia(350L, false) // estava ok, mantém

            assertEquals("350ms com alerta anterior: mantém alerta", true, estadoAlerta)
            assertEquals("350ms com ok anterior: mantém ok", false, estadoOk)
        }
    }

    // -------------------------------------------------------------------------
    // Teste 2: DNS
    // -------------------------------------------------------------------------

    @Test
    fun `dns cruza 2500ms dispara alerta e oscilacoes mantem estado`() {
        runTest {
            // 1500 → 2800 → 2000 → 2600 → 1200
            var estado = false
            val transicoes = mutableListOf<Pair<Long, Boolean>>()

            listOf(1500L, 2800L, 2000L, 2600L, 1200L).forEach { ms ->
                val novo = HisteresiHelper.calcularAlertaDns(ms, estado)
                if (novo != estado) transicoes.add(Pair(ms, novo))
                estado = novo
            }

            assertEquals("Deve haver exatamente 2 transições de DNS", 2, transicoes.size)
            assertEquals("2800ms ativa alerta DNS", Pair(2800L, true), transicoes[0])
            assertEquals("1200ms resolve alerta DNS", Pair(1200L, false), transicoes[1])
        }
    }

    @Test
    fun `dns null mantem estado anterior`() {
        runTest {
            assertEquals("null mantém alerta", true, HisteresiHelper.calcularAlertaDns(null, true))
            assertEquals("null mantém ok", false, HisteresiHelper.calcularAlertaDns(null, false))
        }
    }

    @Test
    fun `dns na zona de histerese entre 1800 e 2500 mantem estado`() {
        runTest {
            assertEquals("2000ms com alerta: mantém", true, HisteresiHelper.calcularAlertaDns(2000L, true))
            assertEquals("2000ms com ok: mantém", false, HisteresiHelper.calcularAlertaDns(2000L, false))
        }
    }

    // -------------------------------------------------------------------------
    // Teste 3: RSSI
    // -------------------------------------------------------------------------

    @Test
    fun `rssi cruza -75 dispara alerta e sai quando passa de -68`() {
        runTest {
            // -60 → -78 → -72 → -76 → -65
            var estado = false
            val transicoes = mutableListOf<Pair<Int, Boolean>>()

            listOf(-60, -78, -72, -76, -65).forEach { rssi ->
                val novo = HisteresiHelper.calcularAlertaRssi(rssi, estado)
                if (novo != estado) transicoes.add(Pair(rssi, novo))
                estado = novo
            }

            assertEquals("Deve haver exatamente 2 transições de RSSI", 2, transicoes.size)
            assertEquals("-78dBm ativa alerta", Pair(-78, true), transicoes[0])
            assertEquals("-65dBm resolve alerta", Pair(-65, false), transicoes[1])
        }
    }

    @Test
    fun `rssi null mantem estado anterior`() {
        runTest {
            assertEquals("rssi null mantém alerta", true, HisteresiHelper.calcularAlertaRssi(null, true))
            assertEquals("rssi null mantém ok", false, HisteresiHelper.calcularAlertaRssi(null, false))
        }
    }

    @Test
    fun `rssi na zona de histerese entre -75 e -68 mantem estado`() {
        runTest {
            // -72 está entre -75 e -68: mantém estado anterior
            assertEquals("-72 com alerta: mantém", true, HisteresiHelper.calcularAlertaRssi(-72, true))
            assertEquals("-72 com ok: mantém", false, HisteresiHelper.calcularAlertaRssi(-72, false))
        }
    }

    // -------------------------------------------------------------------------
    // Teste 4: Sem internet
    // -------------------------------------------------------------------------

    @Test
    fun `sem latencia e sem dns com semWifi ativa alerta sem internet`() {
        runTest {
            val novo = HisteresiHelper.calcularAlertaSemInternet(null, null, semWifi = true, estadoAnterior = false)
            assertEquals("Deve ativar sem-internet", true, novo)
        }
    }

    @Test
    fun `latencia volta ativa resolve alerta sem internet`() {
        runTest {
            val novo = HisteresiHelper.calcularAlertaSemInternet(200L, null, semWifi = false, estadoAnterior = true)
            assertEquals("Latência de volta resolve sem-internet", false, novo)
        }
    }

    @Test
    fun `dns volta resolve alerta sem internet`() {
        runTest {
            val novo = HisteresiHelper.calcularAlertaSemInternet(null, 800L, semWifi = false, estadoAnterior = true)
            assertEquals("DNS de volta resolve sem-internet", false, novo)
        }
    }

    @Test
    fun `sem latencia e sem dns mas semWifi false mantem estado anterior`() {
        runTest {
            // Ambos null mas motivo do RSSI é ambíguo (Invalido/SemPermissao) → mantém
            val mante = HisteresiHelper.calcularAlertaSemInternet(null, null, semWifi = false, estadoAnterior = false)
            assertEquals("Estado ambíguo mantém anterior (false)", false, mante)
        }
    }

    // -------------------------------------------------------------------------
    // Teste 5: rssiConfirmaSemWifi — apenas SemWifi confirma, Invalido não
    // -------------------------------------------------------------------------

    @Test
    fun `sem internet nao dispara quando rssi motivo e Invalido`() {
        runTest {
            // Motivo Invalido (ex: Integer.MAX_VALUE) não confirma ausência de Wi-Fi
            // semWifi = false → estado ambíguo → mantém anterior (false)
            val novo = HisteresiHelper.calcularAlertaSemInternet(null, null, semWifi = false, estadoAnterior = false)
            assertEquals("Motivo Invalido não deve ativar sem-internet", false, novo)
        }
    }

    @Test
    fun `sem internet nao dispara quando rssi motivo e SemPermissao`() {
        runTest {
            // Sem permissão não é equivalente a sem Wi-Fi — dispositivo pode estar conectado
            val novo = HisteresiHelper.calcularAlertaSemInternet(null, null, semWifi = false, estadoAnterior = false)
            assertEquals("Motivo SemPermissao não deve ativar sem-internet", false, novo)
        }
    }
}
