package io.signallq.app.core.diagnostico

/**
 * Ponto unico de classificacao de metrica do motor de diagnostico.
 *
 * Ate a issue SIG-285, a classificacao de RSSI/latencia/jitter/RSRP/RSRQ/SINR/etc
 * estava espalhada entre [WifiSignalQualityEngine], [InternetDiagnosticEngine],
 * [MobileSignalDiagnosticEngine] e SinalScreen.kt (app module) — cada um com sua
 * propria regua numerica, gerando thresholds divergentes para o mesmo dado.
 *
 * Este objeto centraliza TODAS as reguas em um unico lugar, com uma unica fonte de
 * verdade: a skill `/regras-diagnostico-rede`. [WifiSignalQualityEngine] (RSSI Wi-Fi) e
 * [MobileSignalDiagnosticEngine] (RSRP/RSRQ/SINR) ja consomem este classifier (issue
 * #998). [InternetDiagnosticEngine] e SinalScreen.kt (app module) ainda NAO foram
 * migrados — fora do escopo da #998, pendente de issue propria se necessario.
 *
 * ## Vocabulario canonico (MetricStatus)
 * [MetricStatus] usa os mesmos 6 valores que a IA de diagnostico ja usa hoje
 * (decisao de arquitetura ja fechada): excelente, bom, regular, ruim, critico,
 * inconclusivo. NAO confundir com o vocabulario de "perfil de uso" (aba 9 —
 * SpeedtestQualityInput.vereditoStreaming/vereditoGamer/vereditoVideochamada e o
 * campo "impacto" do payload da IA), que usa um vocabulario PROPRIO e SEPARADO:
 * OK, Instavel, Comprometido. Os dois vocabularios nao sao intercambiaveis.
 *
 * Mapeamento de referencia MetricStatus -> perfil de uso (aba 9), quando aplicavel
 * em texto ao usuario (nunca automatize essa conversao sem contexto de perfil):
 *   excelente/bom  -> OK
 *   regular        -> Instavel
 *   ruim/critico   -> Comprometido
 *   inconclusivo   -> (sem equivalente — perfil de uso nao tem "sem dados")
 *
 * [MetricStatus] tambem e diferente de [DiagnosticStatus] (vocabulario tecnico interno
 * dos engines: ok/info/attention/critical/inconclusive, sem granularidade entre
 * "excelente" e "bom", nem entre "ruim" e "critico"). Os engines que hoje devolvem
 * [DiagnosticResult] continuam usando [DiagnosticStatus] — a migracao para basear esses
 * engines no [MetricClassifier] e o escopo das proximas issues.
 */
object MetricClassifier {

    // ── Wi-Fi RSSI ────────────────────────────────────────────────────────────
    // Fonte: skill /regras-diagnostico-rede. Thresholds distintos por banda.

    /** Banda Wi-Fi para fins de classificacao de RSSI. 6GHz reaproveita a regua de 5GHz
     *  (nao ha tabela propria documentada na skill para Wi-Fi 6E/6GHz). */
    enum class WifiBand { GHZ_2_4, GHZ_5, GHZ_6 }

    fun classificarRssiWifi(rssiDbm: Int, band: WifiBand): MetricStatus = when (band) {
        WifiBand.GHZ_2_4 -> when {
            rssiDbm > -50 -> MetricStatus.excelente
            rssiDbm > -60 -> MetricStatus.bom
            rssiDbm > -70 -> MetricStatus.regular
            rssiDbm > -80 -> MetricStatus.ruim
            else -> MetricStatus.critico
        }
        WifiBand.GHZ_5, WifiBand.GHZ_6 -> when {
            rssiDbm > -55 -> MetricStatus.excelente
            rssiDbm > -65 -> MetricStatus.bom
            rssiDbm > -75 -> MetricStatus.regular
            rssiDbm > -82 -> MetricStatus.ruim
            else -> MetricStatus.critico
        }
    }

    // ── Latencia / jitter / perda de pacotes ────────────────────────────────
    // Fonte: skill /regras-diagnostico-rede, tabela "qualidade de conexao (Brasil)".
    // 4 faixas apenas (excelente/bom/aceitavel-regular/ruim) — sem faixa "critico"
    // dedicada, pois a skill nao documenta uma. "ruim" e o teto superior.

    /** Latencia de internet (ping externo), em ms. */
    fun classificarLatencia(latenciaMs: Double): MetricStatus = when {
        latenciaMs < 100.0 -> MetricStatus.excelente
        latenciaMs <= 150.0 -> MetricStatus.bom
        latenciaMs <= 200.0 -> MetricStatus.regular
        else -> MetricStatus.ruim
    }

    fun classificarJitter(jitterMs: Double): MetricStatus = when {
        jitterMs < 5.0 -> MetricStatus.excelente
        jitterMs <= 10.0 -> MetricStatus.bom
        jitterMs <= 20.0 -> MetricStatus.regular
        else -> MetricStatus.ruim
    }

    fun classificarPerdaPacotes(perdaPercentual: Double): MetricStatus = when {
        perdaPercentual <= 0.0 -> MetricStatus.excelente
        perdaPercentual < 0.5 -> MetricStatus.bom
        perdaPercentual <= 2.0 -> MetricStatus.regular
        else -> MetricStatus.ruim
    }

    // ── RSRP / RSRQ / SINR (4G LTE e 5G NR) ─────────────────────────────────
    // Tabela unica de referencia (fonte: skill /regras-diagnostico-rede). NAO usar
    // os valores divergentes de MovelSnapshot.kt (kdoc desatualizado) nem os da
    // regra 15b do prompt da IA — este e o unico ponto de verdade para o motor local.
    //
    // 4G LTE:
    //   RSRP: excelente >-80 | bom -80..-90  | aceitavel -90..-100  | ruim <-100 (dBm)
    //   RSRQ: excelente >-10 | bom -10..-15  | aceitavel -15..-20   | ruim <-20  (dB)
    //   SINR: excelente >20  | bom 13..20    | aceitavel 0..13      | ruim <0    (dB)
    // 5G NR:
    //   RSRP: excelente >-80 | bom -80..-95  | aceitavel -95..-110  | ruim <-110 (dBm)
    //   RSRQ: mesma faixa do 4G (sem tabela propria documentada para 5G)
    //   SINR: excelente >20  | bom 10..20    | aceitavel 0..10      | ruim <0    (dB)
    //
    // "Aceitavel" na skill mapeia para MetricStatus.regular neste classifier.

    enum class RadioTech { LTE_4G, NR_5G }

    fun classificarRsrp(rsrpDbm: Int, tech: RadioTech): MetricStatus = when (tech) {
        RadioTech.LTE_4G -> when {
            rsrpDbm > -80 -> MetricStatus.excelente
            rsrpDbm > -90 -> MetricStatus.bom
            rsrpDbm > -100 -> MetricStatus.regular
            else -> MetricStatus.ruim
        }
        RadioTech.NR_5G -> when {
            rsrpDbm > -80 -> MetricStatus.excelente
            rsrpDbm > -95 -> MetricStatus.bom
            rsrpDbm > -110 -> MetricStatus.regular
            else -> MetricStatus.ruim
        }
    }

    /** RSRQ 5G usa a mesma faixa do 4G — sem tabela propria documentada na skill. */
    fun classificarRsrq(rsrqDb: Int, tech: RadioTech): MetricStatus = when {
        rsrqDb > -10 -> MetricStatus.excelente
        rsrqDb > -15 -> MetricStatus.bom
        rsrqDb > -20 -> MetricStatus.regular
        else -> MetricStatus.ruim
    }

    fun classificarSinr(sinrDb: Int, tech: RadioTech): MetricStatus = when (tech) {
        RadioTech.LTE_4G -> when {
            sinrDb > 20 -> MetricStatus.excelente
            sinrDb > 13 -> MetricStatus.bom
            sinrDb > 0 -> MetricStatus.regular
            else -> MetricStatus.ruim
        }
        RadioTech.NR_5G -> when {
            sinrDb > 20 -> MetricStatus.excelente
            sinrDb > 10 -> MetricStatus.bom
            sinrDb > 0 -> MetricStatus.regular
            else -> MetricStatus.ruim
        }
    }

    // ── Download / Upload (throughput do speedtest) ─────────────────────────
    // Fonte: limiares ja em producao na tela de Resultado do speedtest (ate a
    // issue #1221/#1225), reorganizados nas 6 faixas canonicas (antes eram so
    // 3: Excelente/Regular/Ruim, sem "bom" nem "critico"). Preserva os cortes
    // de negocio existentes (50/25 download, 10/3 upload) como fronteiras
    // bom/regular e regular/ruim -- so adiciona a faixa superior (excelente)
    // e a faixa inferior (critico) que faltavam pra fechar o vocabulario de 6
    // valores. GH#1221 RF-06: unico ponto de verdade, tela de Resultado nao
    // pode mais ter sua propria regua.

    fun classificarDownload(mbps: Double): MetricStatus = when {
        mbps >= 100.0 -> MetricStatus.excelente
        mbps >= 50.0 -> MetricStatus.bom
        mbps >= 25.0 -> MetricStatus.regular
        mbps >= 10.0 -> MetricStatus.ruim
        else -> MetricStatus.critico
    }

    fun classificarUpload(mbps: Double): MetricStatus = when {
        mbps >= 20.0 -> MetricStatus.excelente
        mbps >= 10.0 -> MetricStatus.bom
        mbps >= 3.0 -> MetricStatus.regular
        mbps >= 1.0 -> MetricStatus.ruim
        else -> MetricStatus.critico
    }

    // ── DNS (latencia) ───────────────────────────────────────────────────────
    // Sem tabela dedicada de 4 faixas na skill para DNS — reaproveita os limiares
    // ja em producao em DnsDiagnosticEngine (50/150/300ms), remapeados para o
    // vocabulario canonico de 6 valores.
    fun classificarLatenciaDns(latenciaMs: Int): MetricStatus = when {
        latenciaMs <= 50 -> MetricStatus.excelente
        latenciaMs <= 150 -> MetricStatus.bom
        latenciaMs <= 300 -> MetricStatus.regular
        else -> MetricStatus.ruim
    }

    // ── Bufferbloat ──────────────────────────────────────────────────────────
    // Thresholds DSLReports/waveform, ja em uso em InternetDiagnosticEngine e em
    // SpeedtestQualityClassifier (:featureSpeedtest): nenhum <5ms | leve 5-30ms |
    // moderado 30-100ms | severo >100ms. Este modulo (:featureDiagnostico) nao pode
    // depender de :featureSpeedtest (lei de dependencias :feature* -> :feature*
    // proibido), entao os thresholds sao reimplementados aqui com o mesmo valor.
    fun classificarBufferbloat(deltaMs: Double): MetricStatus = when {
        deltaMs < 5.0 -> MetricStatus.excelente
        deltaMs <= 30.0 -> MetricStatus.bom
        deltaMs <= 100.0 -> MetricStatus.regular
        else -> MetricStatus.ruim
    }
}

/**
 * Vocabulario canonico de status de metrica — os mesmos 6 valores que a IA de
 * diagnostico ja usa hoje (ver worker `ai-diagnosis-worker/src/index.ts`, campo
 * "status": "excelente"|"bom"|"regular"|"ruim"|"critico"|"inconclusivo").
 *
 * NAO e o mesmo vocabulario do perfil de uso (aba 9: OK/Instavel/Comprometido) —
 * ver mapeamento de referencia no kdoc de [MetricClassifier].
 */
enum class MetricStatus {
    excelente,
    bom,
    regular,
    ruim,
    critico,
    inconclusivo,
}
