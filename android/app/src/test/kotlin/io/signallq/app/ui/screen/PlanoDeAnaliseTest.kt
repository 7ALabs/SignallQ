package io.signallq.app.ui.screen

import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes do plano de análise — GH#1706 (2.0.09d), spec 2.0 §7 e §8.4.
 *
 * As duas regras que a spec enuncia e que um teste de "não crashou" não pegaria:
 *
 * 1. **permissão recusada não encerra a jornada** — o plano se adapta e **informa o limite**;
 * 2. **o plano é frase curta, nunca checklist** — mesmo escrita em prosa, enumerar tudo é a
 *    checklist que a spec recusa.
 */
class PlanoDeAnaliseTest {
    private val comTudo = ContextoDoPlano(temPermissaoLocalizacao = true, conectadoPorWifi = true)

    @Test
    fun `todo objetivo produz plano com pelo menos o estado da conexao`() {
        ObjetivoDiagnostico.entries.forEach { objetivo ->
            val plano = montarPlano(objetivo, comTudo)

            assertTrue("objetivo $objetivo saiu com plano vazio", plano.capacidades.isNotEmpty())
            assertTrue(
                "a spec §7 marca estado da conexão como 'sempre', e $objetivo não a convocou",
                Capacidade.ESTADO_CONEXAO in plano.capacidades,
            )
        }
    }

    @Test
    fun `com tudo disponivel o plano nao e adaptado e nao declara limite`() {
        ObjetivoDiagnostico.entries.forEach { objetivo ->
            val plano = montarPlano(objetivo, comTudo)

            assertFalse("objetivo $objetivo veio adaptado sem motivo", plano.adaptado)
            assertNull("objetivo $objetivo declarou limite sem ter perdido nada", plano.limite)
        }
    }

    // §8.4, a metade que costuma ser esquecida: adaptar em silêncio é falhar em silêncio.
    // O mutante que este teste mata: devolver `limite = null` mesmo com capacidade removida.
    @Test
    fun `permissao negada reduz o plano e diz o que ficou de fora`() {
        val semLocalizacao = comTudo.copy(temPermissaoLocalizacao = false)

        val plano = montarPlano(ObjetivoDiagnostico.WIFI_VS_OPERADORA, semLocalizacao)

        assertTrue("o plano tinha que ter sido reduzido", plano.adaptado)
        assertNotNull("plano adaptado sem limite declarado é falha silenciosa", plano.limite)
        assertFalse(Capacidade.SINAL_WIFI in plano.capacidades)
        assertFalse(Capacidade.CANAIS_WIFI in plano.capacidades)
    }

    // A regra inteira de §8.4 num teste só: a jornada CONTINUA.
    @Test
    fun `sem permissao e sem wifi a analise ainda acontece`() {
        val semNada = ContextoDoPlano(temPermissaoLocalizacao = false, conectadoPorWifi = false)

        ObjetivoDiagnostico.entries.forEach { objetivo ->
            val plano = montarPlano(objetivo, semNada)

            assertTrue(
                "objetivo $objetivo ficou sem plano — permissão negada não pode encerrar a jornada",
                plano.capacidades.isNotEmpty(),
            )
        }
    }

    @Test
    fun `o limite fala da consequencia, nao da permissao do sistema`() {
        val plano = montarPlano(ObjetivoDiagnostico.WIFI_VS_OPERADORA, comTudo.copy(temPermissaoLocalizacao = false))

        val limite = plano.limite!!.lowercase()
        listOf("permissão", "localização", "gps", "android").forEach { termo ->
            assertFalse("o limite virou vocabulário de sistema: \"$limite\"", limite.contains(termo))
        }
    }

    // Spec §7: "Não apresenta uma checklist técnica completa por padrão."
    //
    // O mutante que este teste mata: `take(MAXIMO_DE_TRECHOS_NA_FRASE)` virar `take(capacidades
    // .size)` — a frase passaria a enumerar as 4 capacidades, que é a checklist recusada, só que
    // escrita em prosa.
    @Test
    fun `a frase nunca enumera o plano inteiro`() {
        val plano = montarPlano(ObjetivoDiagnostico.JOGOS_COM_LAG, comTudo)
        assertTrue("o caso de teste precisa de um plano com mais de 2", plano.capacidades.size > 2)

        val frase = fraseDoPlano(plano)

        val trechosNaFrase = plano.capacidades.count { frase.contains(it.trecho) }
        assertTrue(
            "a frase nomeou $trechosNaFrase capacidades — isso é checklist em prosa: \"$frase\"",
            trechosNaFrase <= 2,
        )
    }

    @Test
    fun `a frase e uma sentenca, nao uma lista`() {
        ObjetivoDiagnostico.entries.forEach { objetivo ->
            val frase = fraseDoPlano(montarPlano(objetivo, comTudo))

            assertTrue("frase de $objetivo não começa com o convite: \"$frase\"", frase.startsWith("Vamos verificar "))
            assertTrue("frase de $objetivo não termina em ponto: \"$frase\"", frase.endsWith("."))
            assertFalse("frase de $objetivo virou lista com marcadores", frase.contains("•") || frase.contains("\n"))
        }
    }

    @Test
    fun `nenhum trecho da frase usa jargao de rede`() {
        val jargao = listOf("latência", "jitter", "throughput", "dns", "rssi", "bufferbloat", "mbps")

        Capacidade.entries.forEach { capacidade ->
            val trecho = capacidade.trecho.lowercase()
            jargao.forEach { termo ->
                assertFalse("capacidade $capacidade vazou '$termo': \"$trecho\"", trecho.contains(termo))
            }
        }
    }

    // Contrato de telemetria: `diagnostico_plano_iniciado` envia estes ids em `capacidades`.
    // Renomear um id quebra a série histórica, então os valores são travados aqui.
    @Test
    fun `os ids de capacidade sao estaveis`() {
        assertEquals(
            listOf(
                "estado_conexao",
                "latencia_variacao",
                "download_upload",
                "comportamento_sob_carga",
                "sinal_wifi",
                "canais_wifi",
                "dns",
                "rede_movel",
                "dispositivos",
                "equipamento_internet",
            ),
            Capacidade.entries.map { it.id },
        )
    }

    @Test
    fun `a lista para telemetria segue a ordem do plano`() {
        val plano = montarPlano(ObjetivoDiagnostico.SITES_DEMORAM, comTudo)

        assertEquals(plano.capacidades.joinToString(",") { it.id }, plano.idsParaTelemetria)
    }
}
