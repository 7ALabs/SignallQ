package io.signallq.app.feature.diagnostico

private const val CAT = "decisao"

object DiagnosticDecisionEngine {

    fun decidir(
        internetResultados: List<DiagnosticResult>,
        wifiQuality: WifiQualityResult,
        fibraResultados: List<DiagnosticResult> = emptyList(),
        /** RTT TCP para o gateway local. Usado pelas regras GW-01 e GW-02.
         *  Null = não disponível (emulador, Doze, gateway sem TCP). */
        rttGatewayMs: Int? = null,
        /** Latência de internet (ping externo) para correlação com RTT gateway. */
        latenciaInternetMs: Double? = null,
    ): DiagnosticResult {
        val dnsCritico = internetResultados.any { it.categoria == "dns" && it.status == DiagnosticStatus.critical }
        val dnsAtencao = internetResultados.any { it.categoria == "dns" && it.status == DiagnosticStatus.attention }
        val histCritico = internetResultados.any { it.categoria == "historico" && it.status == DiagnosticStatus.critical }
        val histAtencao = internetResultados.any { it.categoria == "historico" && it.status == DiagnosticStatus.attention }
        val wifiCanalAtencao = internetResultados.any { it.categoria == "wifi-canal" && it.status == DiagnosticStatus.attention }
        val historicoIds = internetResultados.filter { it.categoria == "historico" }.map { it.id }
        val wifiCanalIds = internetResultados.filter { it.categoria == "wifi-canal" }.map { it.id }

        val internetCritico = internetResultados.any { it.status == DiagnosticStatus.critical }
        val internetAtencao = internetResultados.any { it.status == DiagnosticStatus.attention }
        val internetInconclusivo = internetResultados.any { it.status == DiagnosticStatus.inconclusive }
        val internetRuim = internetCritico || internetAtencao
        val wifiRuim = !wifiQuality.confiavelParaTeste
        val wifiCritico = wifiQuality.resultados.any { it.status == DiagnosticStatus.critical }
        val fibraDisponivel = fibraResultados.isNotEmpty() &&
            fibraResultados.any { it.status != DiagnosticStatus.inconclusive }
        val fibraCritica = fibraDisponivel && fibraResultados.any { it.status == DiagnosticStatus.critical }
        val fibraAtencao = fibraDisponivel && fibraResultados.any { it.status == DiagnosticStatus.attention }

        val criticoNaoDns =
            internetResultados.any {
                it.status == DiagnosticStatus.critical &&
                    it.categoria != "dns" &&
                    it.categoria != "historico" &&
                    it.categoria != "wifi-canal"
            }

        // DECISAO-DNS: DNS critico (quando e o unico critico relevante)
        if (dnsCritico && !fibraCritica && !criticoNaoDns) {
            return DiagnosticResult(
                id = "DECISAO-DNS-01",
                titulo = "Problema no DNS",
                status = DiagnosticStatus.critical,
                evidencia = "dnsCritico=true",
                mensagemUsuario = "Foi detectado um problema critico no DNS em uso. Isso pode causar lentidao para abrir sites e falhas em apps.",
                recomendacao = "Troque o DNS para uma opcao mais rapida (ex.: Cloudflare ou Google DNS) e refaca o teste.",
                categoria = CAT,
                podeConcluir = true,
            )
        }

        if (dnsAtencao && !internetCritico && !fibraCritica) {
            return DiagnosticResult(
                id = "DECISAO-DNS-01b",
                titulo = "Atenção ao DNS",
                status = DiagnosticStatus.attention,
                evidencia = "dnsAtencao=true",
                mensagemUsuario = "O DNS atual esta lento e pode impactar a experiencia, mesmo com boa velocidade.",
                recomendacao = "Compare com outros DNS e considere trocar para o melhor no comparativo.",
                categoria = CAT,
                podeConcluir = false,
            )
        }

        // DECISAO-HIST: degradacao historica relevante (nao conclui causa raiz)
        if ((histCritico || histAtencao) && !internetCritico && !fibraCritica) {
            return DiagnosticResult(
                id = "DECISAO-HIST-01",
                titulo = "Degradação Recente Detectada",
                status = if (histCritico) DiagnosticStatus.critical else DiagnosticStatus.attention,
                evidencia = "historico=$historicoIds",
                mensagemUsuario = "O histórico sugere degradação recente da conexão. Vale investigar horários e condições em que piora.",
                recomendacao = "Refaça testes em horarios diferentes (inclusive via cabo). Se confirmar, leve o historico ao provedor.",
                categoria = CAT,
                podeConcluir = false,
            )
        }

        // DECISAO-WIFI-CANAL: canal congestionado (nao conclui internet/ISP)
        if (wifiCanalAtencao && !internetCritico && !fibraCritica) {
            return DiagnosticResult(
                id = "DECISAO-WIFI-CANAL",
                titulo = "Possível Congestionamento de Wi-Fi",
                status = DiagnosticStatus.attention,
                evidencia = "wifiCanal=$wifiCanalIds",
                mensagemUsuario = "O canal Wi-Fi parece congestionado e pode explicar instabilidade ou queda de desempenho no Wi-Fi.",
                recomendacao = "Troque o canal Wi-Fi para um canal menos ocupado e refaça o teste.",
                categoria = CAT,
                podeConcluir = false,
            )
        }

        // DECISAO-00: fibra crítica + internet com problema → problema está na fibra/ONT
        if (fibraCritica && internetRuim) {
            return DiagnosticResult(
                id = "DECISAO-00",
                titulo = "Problema na Fibra",
                status = DiagnosticStatus.critical,
                evidencia = "fibraProblemas=${fibraResultados.filter { it.status == DiagnosticStatus.critical }.map { it.id }}",
                mensagemUsuario = "Foram detectados problemas na fibra óptica que podem estar causando instabilidade na internet.",
                recomendacao = "Verifique o estado da ONT. Se o cabo de fibra ou o laser estiver com problema, contate o provedor.",
                categoria = CAT,
                podeConcluir = true,
            )
        }

        // DECISAO-00b: fibra com atenção mas sem problema crítico na internet
        if (fibraAtencao && !internetCritico) {
            return DiagnosticResult(
                id = "DECISAO-00b",
                titulo = "Atenção na Qualidade da Fibra",
                status = DiagnosticStatus.attention,
                evidencia = "fibraAlertas=${fibraResultados.filter { it.status == DiagnosticStatus.attention }.map { it.id }}",
                mensagemUsuario = "Alguns indicadores da fibra merecem atenção. Monitore a estabilidade da conexão.",
                recomendacao = "Verifique a ventilação da ONT e o estado do cabo de fibra. Informe o provedor se houver quedas recorrentes.",
                categoria = CAT,
            )
        }

        // DECISAO-01: internet com problema OU resultados inconclusivos + WiFi ruim.
        // Cobre dois casos:
        //   a) internetRuim && wifiRuim → problemas reais + WiFi fraco
        //   b) internetInconclusivo && wifiRuim → InternetDiagnosticEngine marcou tudo
        //      como inconclusive por causa do WiFi fraco (IN-NORMAL-08). Sem este
        //      check, o fluxo caía em DECISAO-INC ("sem dados suficientes"), que é
        //      mentira — temos dados, só o WiFi estava ruim durante o teste.
        if ((internetRuim || internetInconclusivo) && wifiRuim) {
            return DiagnosticResult(
                id = "DECISAO-01",
                titulo = "Possível Interferência de Wi-Fi",
                status = DiagnosticStatus.attention,
                evidencia = "wifiConfiavel=false internetProblemas=${internetResultados.filter { it.status != DiagnosticStatus.ok }.map { it.id }}",
                mensagemUsuario = "Foram detectados problemas na conexão, mas o sinal Wi-Fi fraco pode ser a causa. O diagnóstico de internet pode não ser preciso.",
                recomendacao = "Aproxime-se do roteador, reconecte ao Wi-Fi e refaça o teste.",
                categoria = CAT,
                podeConcluir = false,
            )
        }

        // DECISAO-GW-01: gateway rápido + internet lenta → problema na operadora
        // Roteador está ok (RTT < 10ms), mas ping externo está alto (> 200ms).
        // Evidência clara de que o gargalo está fora da rede local.
        if (rttGatewayMs != null && rttGatewayMs < 10 &&
            latenciaInternetMs != null && latenciaInternetMs > 200.0 &&
            !wifiRuim
        ) {
            return DiagnosticResult(
                id = "DECISAO-GW-01",
                titulo = "Problema na Operadora",
                status = DiagnosticStatus.critical,
                evidencia = "rttGatewayMs=$rttGatewayMs latenciaInternetMs=$latenciaInternetMs",
                mensagemUsuario = "Seu roteador está funcionando bem — a lentidão vem da sua operadora.",
                recomendacao = "Entre em contato com sua operadora e informe que a conexão está lenta fora de casa. Se possível, peça abertura de chamado com o resultado deste diagnóstico.",
                categoria = CAT,
                podeConcluir = true,
            )
        }

        // DECISAO-GW-02: gateway lento → roteador ou Wi-Fi é o problema
        // RTT > 50ms para o gateway local indica roteador sobrecarregado ou
        // sinal Wi-Fi fraco (latência de rádio alta).
        if (rttGatewayMs != null && rttGatewayMs > 50) {
            return DiagnosticResult(
                id = "DECISAO-GW-02",
                titulo = "Roteador Respondendo Lentamente",
                status = DiagnosticStatus.attention,
                evidencia = "rttGatewayMs=$rttGatewayMs",
                mensagemUsuario = "Seu roteador está respondendo lentamente — isso pode estar causando a lentidão.",
                recomendacao = "Reinicie o roteador: desligue da tomada, aguarde 30 segundos e ligue novamente. Se a lentidão persistir, tente se aproximar do roteador.",
                categoria = CAT,
                podeConcluir = false,
            )
        }

        // DECISAO-02: internet com problema + WiFi ok → culpa é internet/ISP
        if (internetCritico && !wifiRuim) {
            return DiagnosticResult(
                id = "DECISAO-02",
                titulo = "Problema na Internet",
                status = DiagnosticStatus.critical,
                evidencia = "problemas=${internetResultados.filter { it.status == DiagnosticStatus.critical }.map { it.id }}",
                mensagemUsuario = "O Wi-Fi está bom, mas há problemas na conexão com a internet. O problema pode estar no roteador ou no provedor.",
                recomendacao = "Reinicie o roteador. Se o problema persistir, contate o suporte do seu provedor de internet.",
                categoria = CAT,
                podeConcluir = true,
            )
        }

        if (internetAtencao && !wifiRuim) {
            return DiagnosticResult(
                id = "DECISAO-02b",
                titulo = "Atenção na Qualidade da Internet",
                status = DiagnosticStatus.attention,
                evidencia = "alertas=${internetResultados.filter { it.status == DiagnosticStatus.attention }.map { it.id }}",
                mensagemUsuario = "O Wi-Fi está bom, mas alguns indicadores de internet merecem atenção.",
                recomendacao = "Monitore a conexão. Se o problema persistir, reinicie o roteador ou entre em contato com o provedor.",
                categoria = CAT,
                podeConcluir = false,
            )
        }

        // DECISAO-04: WiFi com atenção mas internet ok
        if (wifiCritico && !internetRuim) {
            return DiagnosticResult(
                id = "DECISAO-04-WIFI",
                titulo = "Atenção ao Sinal Wi-Fi",
                status = DiagnosticStatus.attention,
                evidencia = "wifiStatus=" + wifiQuality.resultados.map { it.id + ":" + it.status },
                mensagemUsuario = "A internet está funcionando bem, mas o sinal Wi-Fi merece atenção para evitar problemas futuros.",
                recomendacao = "Aproxime-se do roteador ou reduza obstáculos entre o dispositivo e o roteador.",
                categoria = CAT,
            )
        }

        // DECISAO-04b-WIFI: Wi-Fi com atenção não-crítica (link speed baixo, muitos dispositivos)
        // Não cobre sinal fraco — esse caso vai para DECISAO-01 via wifiRuim
        val wifiTemAtencao = wifiQuality.resultados.any { it.status == DiagnosticStatus.attention }
        if (wifiTemAtencao && !wifiCritico && !internetRuim && !wifiRuim) {
            val alertas = wifiQuality.resultados.filter { it.status == DiagnosticStatus.attention }.map { it.id }
            return DiagnosticResult(
                id = "DECISAO-04b-WIFI",
                titulo = "Atenção ao Wi-Fi",
                status = DiagnosticStatus.attention,
                evidencia = "wifiAlertas=$alertas",
                mensagemUsuario = "A internet está funcionando, mas há indicadores do Wi-Fi que merecem atenção e podem afetar a experiência.",
                recomendacao = "Verifique os itens sinalizados para melhorar a qualidade da conexão Wi-Fi.",
                categoria = CAT,
            )
        }

        // Inconclusivo (sem dados suficientes)
        if (internetInconclusivo && !internetRuim) {
            return DiagnosticResult(
                id = "DECISAO-INC",
                titulo = "Diagnóstico Inconclusivo",
                status = DiagnosticStatus.inconclusive,
                evidencia = null,
                mensagemUsuario = "Não há dados suficientes para um diagnóstico preciso.",
                recomendacao = "Execute um teste de velocidade completo e refaça o diagnóstico.",
                categoria = CAT,
            )
        }

        // DECISAO-04: tudo ok
        return DiagnosticResult(
            id = "DECISAO-04",
            titulo = "Conexão Sem Problemas",
            status = DiagnosticStatus.ok,
            evidencia = null,
            mensagemUsuario = "Todos os indicadores analisados estão dentro do esperado. Sua internet está funcionando bem.",
            recomendacao = null,
            categoria = CAT,
            podeConcluir = true,
        )
    }
}
