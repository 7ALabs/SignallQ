package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes do [DiagnosticoGuiadoEngine] (Feature #550, issue #1475) — cobre os 7
 * objetivos fechados: faixa ok/atenção/crítica por métrica priorizada, dados
 * insuficientes (nunca inventa evidência) e o branching por resposta que de fato
 * muda a avaliação (jogos por cabo, velocidade vs. plano contratado, Wi-Fi vs.
 * operadora).
 */
class DiagnosticoGuiadoEngineTest {

    private fun internet(
        download: Double? = 100.0,
        upload: Double? = 20.0,
        latencia: Double? = 20.0,
        jitter: Double? = 5.0,
        perda: Double? = 0.0,
        bufferbloat: Double? = 10.0,
    ) = InternetDiagnosticInput(
        downloadMbps = download,
        uploadMbps = upload,
        latencyMs = latencia,
        jitterMs = jitter,
        perdaPercentual = perda,
        bufferbloatMs = bufferbloat,
    )

    // ── Internet cai ou oscila (perda + jitter) ──────────────────────────────

    @Test
    fun `internet cai oscila fica ok sem perda nem jitter fora da faixa`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.INTERNET_CAI_OSCILA,
            emptyList(),
            DiagnosticInput(internet = internet(perda = 0.0, jitter = 3.0)),
        )
        assertEquals(DiagnosticStatus.ok, r.status)
        assertTrue(r.acoes.isEmpty())
    }

    @Test
    fun `internet cai oscila fica critica com perda alta`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.INTERNET_CAI_OSCILA,
            emptyList(),
            DiagnosticInput(internet = internet(perda = 3.8, jitter = 3.0)),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
        assertEquals(2, r.evidencias.size)
        assertTrue(r.acoes.isNotEmpty())
    }

    @Test
    fun `internet cai oscila fica inconclusiva sem dados de internet`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.INTERNET_CAI_OSCILA,
            emptyList(),
            null,
        )
        assertEquals(DiagnosticStatus.inconclusive, r.status)
        assertTrue(r.dadosInsuficientes)
        assertTrue(r.evidencias.isEmpty())
        assertTrue(r.acoes.isEmpty())
    }

    // ── Vídeos travam (bufferbloat + download) ───────────────────────────────

    @Test
    fun `videos travam fica atencao com bufferbloat moderado`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.VIDEOS_TRAVAM,
            emptyList(),
            DiagnosticInput(internet = internet(bufferbloat = 60.0)),
        )
        assertEquals(DiagnosticStatus.attention, r.status)
    }

    @Test
    fun `videos travam fica critica com bufferbloat severo`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.VIDEOS_TRAVAM,
            emptyList(),
            DiagnosticInput(internet = internet(bufferbloat = 210.0)),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
    }

    // ── Jogos com lag (latência + jitter + perda, Wi-Fi condicionado à resposta) ─

    @Test
    fun `jogos com lag fica critica com latencia alta`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.JOGOS_COM_LAG,
            listOf(0), // "Wi-Fi"
            DiagnosticInput(
                internet = internet(latencia = 187.0, jitter = 44.0),
                wifi = WifiDiagnosticInput(rssiDbm = -50, linkSpeedMbps = 300, frequenciaMhz = 5200),
            ),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
        assertTrue(r.evidencias.any { it.label == "Força do sinal Wi-Fi" })
        assertTrue(r.acoes.any { it.contains("cabo", ignoreCase = true) })
    }

    @Test
    fun `jogos com lag por cabo nao usa sinal wifi como evidencia nem recomenda cabo`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.JOGOS_COM_LAG,
            listOf(1), // "Cabo de rede"
            DiagnosticInput(
                internet = internet(latencia = 150.0, jitter = 40.0),
                wifi = WifiDiagnosticInput(rssiDbm = -85, linkSpeedMbps = 20, frequenciaMhz = 2400),
            ),
        )
        assertTrue(r.evidencias.none { it.label == "Força do sinal Wi-Fi" })
        assertTrue(r.acoes.none { it.contains("cabo", ignoreCase = true) && it.contains("em vez de Wi-Fi") })
    }

    @Test
    fun `jogos com lag fica ok dentro das faixas de fps competitivo`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.JOGOS_COM_LAG,
            listOf(0),
            DiagnosticInput(
                internet = internet(latencia = 30.0, jitter = 5.0, perda = 0.0),
                wifi = WifiDiagnosticInput(rssiDbm = -45, linkSpeedMbps = 300, frequenciaMhz = 5200),
            ),
        )
        assertEquals(DiagnosticStatus.ok, r.status)
    }

    // ── Chamadas congelam (jitter + perda + upload) ──────────────────────────

    @Test
    fun `chamadas congelam fica critica com jitter alto`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.CHAMADAS_CONGELAM,
            emptyList(),
            DiagnosticInput(internet = internet(jitter = 38.0)),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
    }

    // ── Sites demoram (DNS quando disponível, latência geral como fallback) ──

    @Test
    fun `sites demoram usa latencia dns quando disponivel`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.SITES_DEMORAM,
            emptyList(),
            DiagnosticInput(
                internet = internet(latencia = 54.0),
                dns = DnsDiagnosticInput(currentDnsLatencyMs = 220),
            ),
        )
        assertEquals(1, r.evidencias.size)
        assertEquals("Tempo para localizar sites", r.evidencias.first().label)
        assertEquals(DiagnosticStatus.attention, r.status)
    }

    @Test
    fun `sites demoram cai para latencia geral sem dado de dns`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.SITES_DEMORAM,
            emptyList(),
            DiagnosticInput(internet = internet(latencia = 54.0), dns = null),
        )
        assertEquals("Tempo de resposta", r.evidencias.first().label)
    }

    // ── Velocidade não chega (download vs. contratado) ───────────────────────

    @Test
    fun `velocidade nao chega compara com plano contratado quando informado`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA,
            emptyList(),
            DiagnosticInput(internet = internet(download = 38.0), velocidadeContratadaMbps = 100),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
        assertEquals("Velocidade recebida do plano", r.evidencias.first().label)
    }

    @Test
    fun `velocidade nao chega cai para regua generica sem plano informado`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA,
            emptyList(),
            DiagnosticInput(internet = internet(download = 15.0), velocidadeContratadaMbps = null),
        )
        assertEquals("Download", r.evidencias.first().label)
    }

    // ── Wi-Fi vs. operadora (sinal medido + auto-relato) ─────────────────────

    @Test
    fun `wifi vs operadora aponta wifi quando melhora muito sem wifi`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.WIFI_VS_OPERADORA,
            listOf(0), // "Sim, melhora muito"
            DiagnosticInput(
                connectionType = ConnectionType.wifi,
                internet = internet(),
                wifi = WifiDiagnosticInput(rssiDbm = -50, linkSpeedMbps = 300, frequenciaMhz = 5200),
            ),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
        assertTrue(r.evidencias.any { it.label == "Ao usar a rede móvel" })
    }

    @Test
    fun `wifi vs operadora fica ok quando nao muda nada e sinal bom`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.WIFI_VS_OPERADORA,
            listOf(2), // "Não muda nada"
            DiagnosticInput(
                connectionType = ConnectionType.wifi,
                internet = internet(),
                wifi = WifiDiagnosticInput(rssiDbm = -50, linkSpeedMbps = 300, frequenciaMhz = 5200),
            ),
        )
        assertEquals(DiagnosticStatus.ok, r.status)
    }

    @Test
    fun `wifi vs operadora sem resposta ainda nao testei nao adiciona evidencia de relato`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.WIFI_VS_OPERADORA,
            listOf(3), // "Ainda não testei"
            DiagnosticInput(
                connectionType = ConnectionType.wifi,
                internet = internet(),
                wifi = WifiDiagnosticInput(rssiDbm = -50, linkSpeedMbps = 300, frequenciaMhz = 5200),
            ),
        )
        assertTrue(r.evidencias.none { it.label == "Ao usar a rede móvel" })
    }

    @Test
    fun `wifi vs operadora aponta rede movel quando teste foi feito em rede movel e melhora ao trocar pro wifi`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.WIFI_VS_OPERADORA,
            listOf(0), // "Sim, melhora muito"
            DiagnosticInput(
                connectionType = ConnectionType.mobile,
                internet = internet(),
                mobile = MobileDiagnosticInput(signalStrengthDbm = -95),
            ),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
        assertTrue(r.evidencias.any { it.label == "Ao usar o Wi-Fi" })
        assertTrue(r.acoes.none { it.contains("roteador", ignoreCase = true) || it.contains("canal Wi-Fi", ignoreCase = true) })
    }

    // ── Perguntas: 7 objetivos, roteiro reduzido a 1 pergunta fechada cada ───

    @Test
    fun `todo objetivo fechado tem exatamente uma pergunta com pelo menos duas opcoes`() {
        // Roteiro reduzido de 2 para 1 pergunta por objetivo (issue "muitas
        // perguntas, dificil escolher", 2026-08) — nenhum dos 7 objetivos ficou
        // com 2 perguntas: o motor nunca leu o indice da 2a pergunta em nenhum
        // deles (ver kdoc de PerguntasDiagnosticoGuiado).
        //
        // OUTRO_PROBLEMA fica fora: não tem pergunta fechada por definição (ver kdoc do enum) —
        // coberto pelo teste dedicado logo abaixo.
        ObjetivoDiagnostico.entries.filter { it != ObjetivoDiagnostico.OUTRO_PROBLEMA }.forEach { objetivo ->
            val perguntas = PerguntasDiagnosticoGuiado.perguntas(objetivo)
            assertEquals("objetivo $objetivo deveria ter exatamente 1 pergunta", 1, perguntas.size)
            perguntas.forEach { pergunta ->
                assertTrue("pergunta '${pergunta.texto}' com menos de 2 opcoes", pergunta.opcoes.size >= 2)
            }
        }
    }

    @Test
    fun `outro problema nao tem pergunta fechada`() {
        assertTrue(PerguntasDiagnosticoGuiado.perguntas(ObjetivoDiagnostico.OUTRO_PROBLEMA).isEmpty())
    }

    @Test
    fun `outro problema avalia com metricas gerais e ignora texto livre (nao recebido aqui)`() {
        // O motor nunca recebe o relato livre — só respostas (sempre vazio para este objetivo) e
        // o DiagnosticInput medido. Isso já garante, por construção de assinatura, que o texto
        // não influencia status/causa (ver kdoc de avaliarOutroProblema).
        val resultado =
            DiagnosticoGuiadoEngine.avaliar(
                ObjetivoDiagnostico.OUTRO_PROBLEMA,
                respostas = emptyList(),
                input = DiagnosticInput(internet = internet(latencia = 200.0, jitter = 80.0, perda = 5.0)),
            )
        assertTrue(resultado.evidencias.isNotEmpty())
        assertEquals(DiagnosticStatus.critical, resultado.status)
    }

    @Test
    fun `outro problema sem nenhuma metrica fica inconclusivo, sem inventar evidencia`() {
        val resultado = DiagnosticoGuiadoEngine.avaliar(ObjetivoDiagnostico.OUTRO_PROBLEMA, emptyList(), null)
        assertTrue(resultado.dadosInsuficientes)
        assertTrue(resultado.evidencias.isEmpty())
        assertEquals(DiagnosticStatus.inconclusive, resultado.status)
    }

    @Test
    fun `motor continua funcionando com uma unica resposta por objetivo`() {
        // Caracterizacao pos-consolidacao: respostas com 1 elemento (o roteiro
        // reduzido) produzem o mesmo resultado que o motor ja produzia lendo so
        // o indice 0 antes da consolidacao.
        val jogos = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.JOGOS_COM_LAG,
            listOf(1), // "Cabo de rede" — unica resposta do roteiro reduzido
            DiagnosticInput(
                internet = internet(latencia = 30.0, jitter = 5.0, perda = 0.0),
                wifi = WifiDiagnosticInput(rssiDbm = -85, linkSpeedMbps = 20, frequenciaMhz = 2400),
            ),
        )
        assertTrue(jogos.evidencias.none { it.label == "Força do sinal Wi-Fi" })

        val wifiVsOperadora = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.WIFI_VS_OPERADORA,
            listOf(2), // "Não muda nada" — unica resposta do roteiro reduzido
            DiagnosticInput(
                connectionType = ConnectionType.wifi,
                internet = internet(),
                wifi = WifiDiagnosticInput(rssiDbm = -50, linkSpeedMbps = 300, frequenciaMhz = 5200),
            ),
        )
        assertEquals(DiagnosticStatus.ok, wifiVsOperadora.status)
    }

    @Test
    fun `roteiro de velocidade nao chega em rede movel nao cita roteador ou wifi`() {
        val perguntas = PerguntasDiagnosticoGuiado.perguntas(ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA, ConnectionType.mobile)
        perguntas.forEach { pergunta ->
            assertTrue(
                "pergunta '${pergunta.texto}' cita Wi-Fi/roteador mesmo com teste em rede móvel",
                pergunta.opcoes.none { it.contains("Wi-Fi", ignoreCase = true) || it.contains("roteador", ignoreCase = true) },
            )
        }
    }

    @Test
    fun `roteiro de wifi vs operadora em rede movel pergunta sobre trocar para o wifi`() {
        val perguntas = PerguntasDiagnosticoGuiado.perguntas(ObjetivoDiagnostico.WIFI_VS_OPERADORA, ConnectionType.mobile)
        assertTrue(perguntas.first().texto.contains("Wi-Fi"))
        assertTrue(!perguntas.first().texto.contains("desliga o Wi-Fi"))
    }
}
