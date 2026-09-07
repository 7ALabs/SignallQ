package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModoGamerMedicaoElegibilidadeTest {
    private val agora = 1_800_000_000_000L
    private val redeA = "wifi-bssid:aa:bb:cc:dd:ee:ff"

    private fun internet() =
        InternetDiagnosticInput(
            downloadMbps = 150.0,
            uploadMbps = 30.0,
            latencyMs = 20.0,
            jitterMs = 3.0,
            perdaPercentual = 0.0,
        )

    private fun medicaoBase(
        idadeMs: Long = 0,
        networkId: String? = redeA,
        integridade: IntegridadeMedicaoModoGamer = IntegridadeMedicaoModoGamer.COMPLETA,
    ) =
        MedicaoBaseModoGamer(
            internet = internet(),
            medidoEmEpochMs = agora - idadeMs,
            networkId = networkId,
            integridade = integridade,
        )

    @Test
    fun `medicao completa da mesma rede e elegivel ate quinze minutos`() {
        val resultado =
            avaliarElegibilidadeMedicaoBaseModoGamer(
                medicao = medicaoBase(idadeMs = 15 * 60 * 1_000L),
                networkIdAtual = redeA,
                agoraEpochMs = agora,
            )

        assertTrue(resultado is ElegibilidadeMedicaoBaseModoGamer.Elegivel)
    }

    @Test
    fun `medicao expirada exige teste novo`() {
        val resultado =
            avaliarElegibilidadeMedicaoBaseModoGamer(
                medicao = medicaoBase(idadeMs = 15 * 60 * 1_000L + 1),
                networkIdAtual = redeA,
                agoraEpochMs = agora,
            ) as ElegibilidadeMedicaoBaseModoGamer.RequerNovoTeste

        assertEquals(MotivoMedicaoBaseInvalidaModoGamer.EXPIRADA_OU_RELOGIO_INVALIDO, resultado.motivo)
    }

    @Test
    fun `atraso de revalidacao vence imediatamente depois do limite`() {
        val atraso =
            atrasoAteRevalidarMedicaoBaseModoGamer(
                medicao = medicaoBase(idadeMs = 15 * 60 * 1_000L),
                agoraEpochMs = agora,
            )

        assertEquals(1L, atraso)
    }

    @Test
    fun `medicao de outra rede exige teste novo`() {
        val resultado =
            avaliarElegibilidadeMedicaoBaseModoGamer(
                medicao = medicaoBase(networkId = "movel:operadora"),
                networkIdAtual = redeA,
                agoraEpochMs = agora,
            ) as ElegibilidadeMedicaoBaseModoGamer.RequerNovoTeste

        assertEquals(MotivoMedicaoBaseInvalidaModoGamer.REDE_DIFERENTE, resultado.motivo)
    }

    @Test
    fun `identidade ausente nunca e reutilizada`() {
        val resultado =
            avaliarElegibilidadeMedicaoBaseModoGamer(
                medicao = medicaoBase(networkId = null),
                networkIdAtual = redeA,
                agoraEpochMs = agora,
            ) as ElegibilidadeMedicaoBaseModoGamer.RequerNovoTeste

        assertEquals(MotivoMedicaoBaseInvalidaModoGamer.REDE_ATUAL_DESCONHECIDA, resultado.motivo)
    }

    @Test
    fun `medicao nao completa nunca sustenta veredito gamer`() {
        IntegridadeMedicaoModoGamer.entries
            .filter { it != IntegridadeMedicaoModoGamer.COMPLETA }
            .forEach { integridade ->
                val resultado =
                    avaliarElegibilidadeMedicaoBaseModoGamer(
                        medicao = medicaoBase(integridade = integridade),
                        networkIdAtual = redeA,
                        agoraEpochMs = agora,
                    ) as ElegibilidadeMedicaoBaseModoGamer.RequerNovoTeste

                assertEquals(MotivoMedicaoBaseInvalidaModoGamer.INTEGRIDADE_INSUFICIENTE, resultado.motivo)
            }
    }

    @Test
    fun `ping da mesma tentativa e elegivel ate dois minutos`() {
        val ping =
            MedicaoPingEspecificoModoGamer(
                latenciaMs = 20.0,
                jitterMs = 3.0,
                perdaPercentual = 0.0,
                medidoEmEpochMs = agora - 2 * 60 * 1_000L,
                networkId = redeA,
                tentativaId = "tentativa-1",
            )

        val resultado =
            avaliarElegibilidadePingEspecificoModoGamer(
                medicao = ping,
                tentativaAtualId = "tentativa-1",
                networkIdAtual = redeA,
                agoraEpochMs = agora,
            )

        assertTrue(resultado is ElegibilidadePingEspecificoModoGamer.Elegivel)
    }

    @Test
    fun `ping expirado ou de outra tentativa nao e reutilizado`() {
        val ping =
            MedicaoPingEspecificoModoGamer(
                latenciaMs = 20.0,
                jitterMs = 3.0,
                perdaPercentual = 0.0,
                medidoEmEpochMs = agora - 2 * 60 * 1_000L - 1,
                networkId = redeA,
                tentativaId = "tentativa-antiga",
            )

        val tentativaDiferente =
            avaliarElegibilidadePingEspecificoModoGamer(
                medicao = ping,
                tentativaAtualId = "tentativa-nova",
                networkIdAtual = redeA,
                agoraEpochMs = agora,
            ) as ElegibilidadePingEspecificoModoGamer.NaoElegivel
        val expirado =
            avaliarElegibilidadePingEspecificoModoGamer(
                medicao = ping.copy(tentativaId = "tentativa-nova"),
                tentativaAtualId = "tentativa-nova",
                networkIdAtual = redeA,
                agoraEpochMs = agora,
            ) as ElegibilidadePingEspecificoModoGamer.NaoElegivel

        assertEquals(MotivoPingEspecificoInvalidoModoGamer.TENTATIVA_DIFERENTE, tentativaDiferente.motivo)
        assertEquals(MotivoPingEspecificoInvalidoModoGamer.EXPIRADO_OU_RELOGIO_INVALIDO, expirado.motivo)
    }

    @Test
    fun `ping em outra rede nao e reutilizado`() {
        val resultado =
            avaliarElegibilidadePingEspecificoModoGamer(
                medicao =
                    MedicaoPingEspecificoModoGamer(
                        latenciaMs = 20.0,
                        jitterMs = 3.0,
                        perdaPercentual = 0.0,
                        medidoEmEpochMs = agora,
                        networkId = "movel:operadora",
                        tentativaId = "tentativa-1",
                    ),
                tentativaAtualId = "tentativa-1",
                networkIdAtual = redeA,
                agoraEpochMs = agora,
            ) as ElegibilidadePingEspecificoModoGamer.NaoElegivel

        assertEquals(MotivoPingEspecificoInvalidoModoGamer.REDE_DIFERENTE, resultado.motivo)
    }
}
