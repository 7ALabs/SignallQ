package io.signallq.app.ui.screen

import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.core.network.EstadoConexao
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

    // §8.4, a metade que costuma ser esquecida: adaptar em silencio e falhar em silencio. O
    // cenario vivo hoje e a rede, nao a permissao — ver `sinal wifi nao depende de permissao`.
    @Test
    fun `fora do wifi o plano e reduzido e diz o que ficou de fora`() {
        val semWifi = comTudo.copy(conectadoPorWifi = false)

        // JOGOS_COM_LAG e o caso vivo: convoca `SINAL_WIFI` (resposta "Wi-Fi") e perde fora dele.
        // `WIFI_VS_OPERADORA` nao serve mais — desde o B7 ele troca de capacidade em vez de perder.
        val plano = montarPlano(ObjetivoDiagnostico.JOGOS_COM_LAG, semWifi, respostas = listOf(0))

        assertNotNull("plano adaptado sem limite declarado e falha silenciosa", plano.limite)
        assertFalse(Capacidade.SINAL_WIFI in plano.capacidades)
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
    fun `o limite fala da consequencia, nao do vocabulario do sistema`() {
        val plano =
            montarPlano(
                ObjetivoDiagnostico.JOGOS_COM_LAG,
                comTudo.copy(conectadoPorWifi = false),
                respostas = listOf(0),
            )

        val limite = plano.limite!!.lowercase()
        listOf("permissão", "localização", "gps", "android").forEach { termo ->
            assertFalse("o limite virou vocabulario de sistema: \"$limite\"", limite.contains(termo))
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

    // BLOQUEIO B3: a tabela nao era travada por teste, e divergia do motor. Espelha as dimensoes
    // que cada `avaliar*` produz — inclusive as CONDICOES, que a primeira correcao ignorou
    // (bloqueios B6 e B7 de Caio: `JOGOS_COM_LAG` descarta Wi-Fi no cabo, e `WIFI_VS_OPERADORA` e
    // um `when` exclusivo por tipo de conexao).
    //
    // RESSALVA R11 de Caio na PR #1732: este teste chama `montarPlano` sem `respostas`, entao a
    // linha de `JOGOS_COM_LAG` so cobre o ramo "nao e cabo" (`respostas.getOrNull(0) != RESPOSTA_
    // JOGA_POR_CABO` com lista vazia da `null`, que satisfaz o `!=`). O ramo "e cabo" (sem
    // `SINAL_WIFI`) esta coberto pelo teste dedicado `jogos por cabo nao promete sinal wifi`
    // abaixo — esta tabela nao e mais um espelho incondicional do motor, e sim o espelho do caso
    // sem respostas.
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
                    listOf(Capacidade.ESTADO_CONEXAO, Capacidade.SINAL_WIFI),
                ObjetivoDiagnostico.OUTRO_PROBLEMA to
                    listOf(Capacidade.ESTADO_CONEXAO, Capacidade.LATENCIA_VARIACAO, Capacidade.DOWNLOAD_UPLOAD),
            )

        ObjetivoDiagnostico.entries.forEach { objetivo ->
            assertEquals(
                "plano de $objetivo divergiu",
                esperado.getValue(objetivo),
                montarPlano(objetivo, comTudo).capacidades,
            )
        }
    }

    // BLOQUEIO B6: `avaliarJogosComLag` descarta a dimensao de Wi-Fi quando a resposta 0 e "Cabo de
    // rede" — a suite ja assere essa supressao no resultado desde a #1683. O plano prometia mesmo
    // assim, e o texto na tela nomeava o que o motor ia jogar fora.
    @Test
    fun `jogos por cabo nao promete sinal wifi`() {
        val porCabo = montarPlano(ObjetivoDiagnostico.JOGOS_COM_LAG, comTudo, respostas = listOf(1))
        val porWifi = montarPlano(ObjetivoDiagnostico.JOGOS_COM_LAG, comTudo, respostas = listOf(0))

        assertFalse("no cabo o motor descarta o Wi-Fi", Capacidade.SINAL_WIFI in porCabo.capacidades)
        assertTrue("no Wi-Fi ele avalia", Capacidade.SINAL_WIFI in porWifi.capacidades)
    }

    // BLOQUEIO B7: `avaliarWifiVsOperadora` e um `when` exclusivo por `connectionType` — nunca
    // produz as duas dimensoes. O plano prometia as duas incondicionalmente.
    @Test
    fun `wifi vs operadora promete so o lado que o motor vai medir`() {
        val noWifi = montarPlano(ObjetivoDiagnostico.WIFI_VS_OPERADORA, comTudo)
        val noMovel =
            montarPlano(
                ObjetivoDiagnostico.WIFI_VS_OPERADORA,
                comTudo.copy(conectadoPorWifi = false, estadoConexao = EstadoConexao.movel),
            )

        assertTrue(Capacidade.SINAL_WIFI in noWifi.capacidades)
        assertFalse("no Wi-Fi o motor nao mede a operadora", Capacidade.REDE_MOVEL in noWifi.capacidades)
        assertTrue(Capacidade.REDE_MOVEL in noMovel.capacidades)
        assertFalse("no movel ele nao mede o Wi-Fi", Capacidade.SINAL_WIFI in noMovel.capacidades)
    }

    // BLOQUEIO B10 de Caio na PR #1732 (Rodada 4): o `else` que sobrou do B7 nao era "estou no
    // movel", era "nao tenho snapshot de Wi-Fi" — cobria tambem ethernet, desconectado e estado
    // desconhecido. Nesses tres o motor cai no `else -> Unit` de `avaliarWifiVsOperadora` e nao
    // produz dimensao nenhuma, mas o plano prometia `REDE_MOVEL` do mesmo jeito.
    @Test
    fun `wifi vs operadora fora do movel e do wifi nao promete rede movel`() {
        listOf(EstadoConexao.ethernet, EstadoConexao.desconectado, EstadoConexao.desconhecido)
            .forEach { estado ->
                val contexto = comTudo.copy(conectadoPorWifi = false, estadoConexao = estado)

                val plano = montarPlano(ObjetivoDiagnostico.WIFI_VS_OPERADORA, contexto)

                assertFalse(
                    "em $estado o motor nao mede rede movel — o plano nao pode prometer",
                    Capacidade.REDE_MOVEL in plano.capacidades,
                )
                assertFalse("nem Wi-Fi, ja que nao ha snapshot", Capacidade.SINAL_WIFI in plano.capacidades)
            }
    }

    // Segundo teste do B10: Wi-Fi atras de VPN tem `estadoConexao == wifi` mas
    // `wifiLinkSnapshot == null` (a API nao expoe `WifiInfo` quando o transporte e VPN) —
    // `conectadoPorWifi` fica falso mesmo estando no Wi-Fi de fato. Nesse caso nada foi
    // "removido por rede": a capacidade nunca chegou a ser prometida, entao nao ha limite
    // culpando a rede por algo que continua sendo Wi-Fi.
    @Test
    fun `wifi sem snapshot nao gera limite que culpe a rede`() {
        val wifiComVpn =
            comTudo.copy(conectadoPorWifi = false, estadoConexao = EstadoConexao.wifi)

        val plano = montarPlano(ObjetivoDiagnostico.WIFI_VS_OPERADORA, wifiComVpn)

        assertFalse(Capacidade.REDE_MOVEL in plano.capacidades)
        assertFalse(Capacidade.SINAL_WIFI in plano.capacidades)
        assertTrue("nada foi removido por rede aqui", plano.removidasPorRede.isEmpty())
        assertNull("nao ha causa de rede a declarar", plano.limite)
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

    // BLOQUEIO B5 de Caio na PR #1732: `SINAL_WIFI` NAO depende de permissao de localizacao. O
    // RSSI e lido incondicionalmente por `MonitorRedeAndroid`; o que a localizacao gateia e
    // ssid/bssid, e por `locationManager.isLocationEnabled`, que e outro sinal.
    //
    // A fatia inteira de "preparacao contextual" estava sobre essa premissa falsa. Restou
    // `CANAIS_WIFI`, que o motor ainda nao avalia — entao HOJE nenhuma capacidade e recuperavel por
    // permissao, e foi isso que tirou o CTA desta entrega (decisao de Luiz em 2026-08-18).
    //
    // Este teste trava a premissa corrigida: se alguem recolocar `SINAL_WIFI` na dependencia de
    // localizacao, o plano volta a mentir sobre o que consegue medir.
    @Test
    fun `sinal wifi nao depende de permissao de localizacao`() {
        val noWifiSemPermissao =
            ContextoDoPlano(temPermissaoLocalizacao = false, conectadoPorWifi = true)

        val plano = montarPlano(ObjetivoDiagnostico.WIFI_VS_OPERADORA, noWifiSemPermissao)

        assertTrue(
            "o RSSI e lido sem a permissao — o plano nao pode remover SINAL_WIFI por isso",
            Capacidade.SINAL_WIFI in plano.capacidades,
        )
        assertTrue("nada a declarar", plano.removidasPorPermissao.isEmpty())
        assertNull("nao ha limite quando nada foi removido", plano.limite)
    }

    // Enquanto o motor nao avaliar canais Wi-Fi, nenhuma capacidade e recuperavel por permissao —
    // e por isso a preparacao contextual saiu desta fatia. Se este teste comecar a falhar, o gatilho
    // voltou a existir e o CTA pode voltar junto.
    @Test
    fun `hoje nenhum plano perde capacidade por falta de permissao`() {
        val semPermissao = ContextoDoPlano(temPermissaoLocalizacao = false, conectadoPorWifi = true)

        ObjetivoDiagnostico.entries.forEach { objetivo ->
            assertTrue(
                "$objetivo perdeu capacidade por permissao — a preparacao precisa voltar",
                montarPlano(objetivo, semPermissao).removidasPorPermissao.isEmpty(),
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

    // Com `SINAL_WIFI` fora da dependencia de localizacao, nenhuma capacidade falha nos dois
    // criterios hoje. O teste guarda a consequencia: a atribuicao de causa nunca conta duas vezes.
    @Test
    fun `nenhuma capacidade e contada nas duas causas`() {
        val semNada = ContextoDoPlano(temPermissaoLocalizacao = false, conectadoPorWifi = false)

        ObjetivoDiagnostico.entries.forEach { objetivo ->
            val plano = montarPlano(objetivo, semNada)
            plano.removidasPorPermissao.forEach { capacidade ->
                assertFalse(
                    "$objetivo contou $capacidade nas duas causas",
                    capacidade in plano.removidasPorRede,
                )
            }
        }
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
