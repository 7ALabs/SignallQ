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

    // BLOQUEIO B3 de Caio na PR #1732: a tabela por objetivo nao era travada por teste nenhum —
    // trocar as capacidades de um objetivo deixava a suite verde. E ela divergia do motor: 3 das 10
    // capacidades nao eram avaliadas por objetivo nenhum, e era essa divergencia que fazia o app
    // pedir permissao de localizacao em jornadas onde conceder nao muda o resultado.
    //
    // Esta tabela espelha as dimensoes que cada `avaliar*` do `DiagnosticoGuiadoEngine` produz.
    // Mudou o motor? Mude aqui junto — e ao contrario tambem.
    @Test
    fun `a tabela de capacidades espelha o que o motor avalia`() {
        val esperado =
            mapOf(
                ObjetivoDiagnostico.INTERNET_CAI_OSCILA to
                    listOf(Capacidade.ESTADO_CONEXAO, Capacidade.LATENCIA_VARIACAO),
                ObjetivoDiagnostico.VIDEOS_TRAVAM to
                    listOf(
                        Capacidade.ESTADO_CONEXAO,
                        Capacidade.COMPORTAMENTO_SOB_CARGA,
                        Capacidade.DOWNLOAD_UPLOAD,
                    ),
                ObjetivoDiagnostico.JOGOS_COM_LAG to
                    listOf(
                        Capacidade.ESTADO_CONEXAO,
                        Capacidade.LATENCIA_VARIACAO,
                        Capacidade.COMPORTAMENTO_SOB_CARGA,
                        Capacidade.SINAL_WIFI,
                    ),
                ObjetivoDiagnostico.CHAMADAS_CONGELAM to
                    listOf(
                        Capacidade.ESTADO_CONEXAO,
                        Capacidade.LATENCIA_VARIACAO,
                        Capacidade.DOWNLOAD_UPLOAD,
                    ),
                ObjetivoDiagnostico.SITES_DEMORAM to
                    listOf(Capacidade.ESTADO_CONEXAO, Capacidade.DNS, Capacidade.LATENCIA_VARIACAO),
                ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA to
                    listOf(Capacidade.ESTADO_CONEXAO, Capacidade.DOWNLOAD_UPLOAD),
                ObjetivoDiagnostico.WIFI_VS_OPERADORA to
                    listOf(Capacidade.ESTADO_CONEXAO, Capacidade.SINAL_WIFI, Capacidade.REDE_MOVEL),
            )

        ObjetivoDiagnostico.entries.forEach { objetivo ->
            assertEquals(
                "plano de $objetivo divergiu",
                esperado.getValue(objetivo),
                montarPlano(objetivo, comTudo).capacidades,
            )
        }
    }

    // As tres capacidades que a spec §7 preve e o motor ainda nao implementa nao podem ser
    // prometidas a ninguem ate existirem. O enum as mantem porque os ids sao contrato de
    // telemetria; o que nao pode e um plano convoca-las.
    @Test
    fun `capacidades que o motor nao avalia nao entram em plano nenhum`() {
        val naoImplementadas =
            setOf(Capacidade.CANAIS_WIFI, Capacidade.DISPOSITIVOS, Capacidade.EQUIPAMENTO_INTERNET)

        ObjetivoDiagnostico.entries.forEach { objetivo ->
            val plano = montarPlano(objetivo, comTudo)
            naoImplementadas.forEach { capacidade ->
                assertFalse(
                    "$objetivo promete $capacidade, que o motor nunca avalia",
                    capacidade in plano.capacidades,
                )
            }
        }
    }

    // BLOQUEIO B1: o botao de permissao seguia `limite != null`, e `limite` tambem vem de reducao
    // por REDE. Em `VELOCIDADE_NAO_CHEGA` sem Wi-Fi o botao aparecia e conceder devolvia zero
    // capacidade — e ainda emitia um evento de bloqueio que nunca existiu.
    @Test
    fun `so oferece permissao quando conceder devolve capacidade`() {
        val semNada = ContextoDoPlano(temPermissaoLocalizacao = false, conectadoPorWifi = false)

        ObjetivoDiagnostico.entries.forEach { objetivo ->
            val plano = montarPlano(objetivo, semNada)
            val comPermissao = montarPlano(objetivo, semNada.copy(temPermissaoLocalizacao = true))

            val ganharia = comPermissao.capacidades.size > plano.capacidades.size
            assertEquals(
                "$objetivo: oferecer permissao so faz sentido se ela devolve capacidade",
                ganharia,
                plano.podeMelhorarComLocalizacao,
            )
        }
    }

    // BLOQUEIO B4 do parecer: o `when` de `limiteDoPlano` retornava so a primeira causa, e a outra
    // capacidade saia em silencio — o que a §8.4 proibe. A funcao agora declara as duas.
    //
    // Achado ao escrever este teste: com os conjuntos de dependencia atuais as duas causas NAO
    // coexistem. Toda capacidade que depende de localizacao (`SINAL_WIFI`, `CANAIS_WIFI`) tambem
    // depende de Wi-Fi, entao fora do Wi-Fi a remocao e sempre por rede, e no Wi-Fi nunca ha
    // remocao por rede dessas duas. O tratamento das duas causas em `limiteDoPlano` fica como
    // defesa para quando os conjuntos mudarem — e este teste trava o invariante enquanto nao mudam,
    // em vez de fingir que exercita um cenario inalcancavel.
    @Test
    fun `as duas causas nao coexistem com os conjuntos de dependencia atuais`() {
        val contextos =
            listOf(
                ContextoDoPlano(temPermissaoLocalizacao = false, conectadoPorWifi = false),
                ContextoDoPlano(temPermissaoLocalizacao = false, conectadoPorWifi = true),
                ContextoDoPlano(temPermissaoLocalizacao = true, conectadoPorWifi = false),
            )

        contextos.forEach { contexto ->
            ObjetivoDiagnostico.entries.forEach { objetivo ->
                val plano = montarPlano(objetivo, contexto)
                assertTrue(
                    "$objetivo em $contexto teve as duas causas — o limite precisa declarar as duas",
                    plano.removidasPorPermissao.isEmpty() || plano.removidasPorRede.isEmpty(),
                )
            }
        }
    }

    @Test
    fun `fora do wifi o limite fala de rede, nao de permissao`() {
        val semNada = ContextoDoPlano(temPermissaoLocalizacao = false, conectadoPorWifi = false)

        val limite = montarPlano(ObjetivoDiagnostico.JOGOS_COM_LAG, semNada).limite!!

        assertTrue("a causa que manda e a rede: \"$limite\"", limite.contains("não está no Wi-Fi"))
        assertFalse("nao pode culpar a permissao: \"$limite\"", limite.contains("redes próximas"))
    }

    // RESSALVA R1: a ordem entre permissao e rede nao era travada. Ela decide qual causa a pessoa
    // le primeiro, e capacidade que falha nos dois criterios conta como de PERMISSAO — que e a que
    // ela pode resolver ali mesmo.
    @Test
    fun `capacidade que falha nos dois criterios conta como removida por rede`() {
        val semNada = ContextoDoPlano(temPermissaoLocalizacao = false, conectadoPorWifi = false)

        val plano = montarPlano(ObjetivoDiagnostico.WIFI_VS_OPERADORA, semNada)

        assertTrue("rede e a restricao que manda", Capacidade.SINAL_WIFI in plano.removidasPorRede)
        assertFalse("nao pode contar duas vezes", Capacidade.SINAL_WIFI in plano.removidasPorPermissao)
    }

    // RESSALVA R8: a frase juntava trechos com "e", e trechos que ja traziam "e" produziam
    // "A e B e C". Lia mal em voz alta na maioria dos objetivos.
    @Test
    fun `a frase nao encadeia dois e`() {
        ObjetivoDiagnostico.entries.forEach { objetivo ->
            val frase = fraseDoPlano(montarPlano(objetivo, comTudo))
            val ocorrencias = Regex(" e ").findAll(frase).count()
            assertTrue("frase de $objetivo tem $ocorrencias \" e \": \"$frase\"", ocorrencias <= 1)
        }
    }
}
