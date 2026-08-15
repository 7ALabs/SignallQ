package io.signallq.app.ui.screen

import androidx.compose.ui.graphics.Color
import io.signallq.app.core.diagnostico.MetricClassifier
import io.signallq.app.core.diagnostico.MetricStatus
import io.signallq.app.core.telephony.MovelSimSnapshot
import io.signallq.app.core.telephony.MovelSnapshot
import io.signallq.app.ui.LkTokens

/**
 * GH#1206 — classificacao canonica de sinal movel (qualidade/tipo de conexao/experiencia),
 * consumida por [SinalScreen] (aba Movel) e por `HomeScreen.kt` (card "Rede movel" e "CHIP
 * MOVEL" — GH#1258). Extraido de SinalScreen.kt em 2026-07-21 (GH#1258) porque passou a ter
 * 2 consumidores fora do arquivo original; nao move Composables, so as funcoes puras de
 * classificacao (regra de higiene, secao 4.8b).
 *
 * Fonte unica: nao criar um terceiro classificador. Qualquer tela que precise de veredito de
 * sinal movel usa [classificarQualidadeSinalMovel] (ou as irmas de tipo de conexao/experiencia)
 * a partir de um [DadosSinalMovel] normalizado.
 */

internal data class MobileInsight(
    val label: String,
    val description: String,
    val color: Color,
)

/**
 * GH#1206 — dados de sinal ja normalizados de UMA fonte (SIM especifico ou snapshot
 * geral), usados pelas funcoes de classificacao abaixo. Existe pra eliminar a duplicacao
 * entre as antigas mobileX()/snapshotX() (item 6 da issue) -- agora ha uma unica
 * implementacao de qualidade/tipo de conexao/experiencia, alimentada por qualquer uma
 * das duas fontes.
 */
internal data class DadosSinalMovel(
    val rsrpDbm: Int?,
    val rsrqDb: Int?,
    val sinrDb: Int?,
    val tecnologia: String?,
    val radioDesligado: Boolean,
)

/**
 * GH#1206 item 1 — so usa [summarySnapshot] como fallback quando ele inequivocamente
 * representa o MESMO SIM ([MovelSimSnapshot.isDefaultData]). O TelephonyManager que
 * produz o snapshot geral nunca e criado com `createForSubscriptionId`, entao ele reflete
 * o SIM padrao de dados -- usa-lo pra completar um SIM secundario misturaria dados de
 * chips diferentes (Chip 2 exibindo tecnologia/RSRP do Chip 1).
 */
internal fun MovelSimSnapshot.paraDadosSinalMovel(summarySnapshot: MovelSnapshot?): DadosSinalMovel {
    val podeUsarFallback = isDefaultData
    return DadosSinalMovel(
        rsrpDbm = rsrpDbm ?: summarySnapshot?.rsrpDbm.takeIf { podeUsarFallback },
        rsrqDb = rsrqDb ?: summarySnapshot?.rsrqDb.takeIf { podeUsarFallback },
        sinrDb = sinrDb ?: summarySnapshot?.sinrDb.takeIf { podeUsarFallback },
        tecnologia = tecnologiaRede ?: summarySnapshot?.tecnologia.takeIf { podeUsarFallback },
        radioDesligado = radioDesligado,
    )
}

internal fun MovelSnapshot.paraDadosSinalMovel(): DadosSinalMovel =
    DadosSinalMovel(
        rsrpDbm = rsrpDbm,
        rsrqDb = rsrqDb,
        sinrDb = sinrDb,
        tecnologia = tecnologia,
        radioDesligado = radioDesligado,
    )

internal fun buildMobileSummary(dados: DadosSinalMovel): String {
    if (dados.radioDesligado) return "Modo avião ativo · rádio celular desligado"
    return buildString {
        if (dados.rsrpDbm != null) append("RSRP ${dados.rsrpDbm} dBm")
        if (dados.rsrpDbm != null) append(" · ")
        append(dados.tecnologia ?: "Rede móvel")
    }
}

/** GH#1206 item 2 — deriva o [MetricClassifier.RadioTech] a partir do texto de
 *  tecnologia, mesmo criterio ja usado por `MobileSignalDiagnosticEngine` (nao inventa
 *  regra nova): tecnologia contendo "5G" vira NR_5G, qualquer outra vira LTE_4G. */
internal fun radioTechDeTecnologia(tecnologia: String?): MetricClassifier.RadioTech =
    if (tecnologia?.contains("5G", ignoreCase = true) == true) {
        MetricClassifier.RadioTech.NR_5G
    } else {
        MetricClassifier.RadioTech.LTE_4G
    }

/** Pior status entre RSRP (obrigatorio) e RSRQ/SINR (quando disponiveis) — mesmo
 *  criterio de "assume a pior metrica" do `MobileSignalDiagnosticEngine` (GH#1206 item 6).
 *  `null` quando RSRP nao esta disponivel (nunca vira classificacao positiva por padrao). */
internal fun piorMetricStatusSinalMovel(dados: DadosSinalMovel): MetricStatus? {
    val rsrp = dados.rsrpDbm ?: return null
    val tech = radioTechDeTecnologia(dados.tecnologia)
    val statuses =
        buildList {
            add(MetricClassifier.classificarRsrp(rsrp, tech))
            dados.rsrqDb?.let { add(MetricClassifier.classificarRsrq(it, tech)) }
            dados.sinrDb?.let { add(MetricClassifier.classificarSinr(it, tech)) }
        }
    return statuses.maxBy { it.ordinal }
}

/** GH#1206 item 2/3 — substitui `mobileSignalQuality`/`snapshotSignalQuality` (limiares
 *  proprios -90/-105 dBm, iguais pra 4G e 5G) por `MetricClassifier.classificarRsrp`
 *  (limiares corretos por tecnologia) + RSRQ/SINR quando disponiveis. RSRP ausente NUNCA
 *  vira classificacao positiva -- fica "Indisponível". */
internal fun classificarQualidadeSinalMovel(
    dados: DadosSinalMovel,
    c: LkTokens,
): MobileInsight {
    if (dados.radioDesligado) {
        return MobileInsight(
            label = "Sem sinal",
            description = "Rádio celular desligado. Ative a rede móvel para medir chamadas e dados.",
            color = c.warning,
        )
    }
    val pior =
        piorMetricStatusSinalMovel(dados)
            ?: return MobileInsight(
                label = "Indisponível",
                description = "O Android não expôs a intensidade deste chip agora.",
                color = c.secondary,
            )
    return when (pior) {
        MetricStatus.excelente ->
            MobileInsight("Excelente", "Excelente — chamadas e vídeos tendem a ficar estáveis mesmo sob uso pesado.", c.success)
        MetricStatus.bom ->
            MobileInsight("Bom", "Bom — chamadas e vídeos tendem a ocorrer sem cortes.", c.success)
        MetricStatus.regular ->
            MobileInsight("Regular", "Regular — pode oscilar em chamadas e vídeo em movimento.", c.warning)
        MetricStatus.ruim, MetricStatus.critico ->
            MobileInsight("Ruim", "Ruim — maior chance de falhas em chamadas, uploads e streaming.", c.error)
        MetricStatus.inconclusivo ->
            MobileInsight("Inconclusivo", "Amostra insuficiente para classificar a qualidade do sinal.", c.secondary)
    }
}

internal fun classificarTipoConexaoMovel(
    dados: DadosSinalMovel,
    c: LkTokens,
): MobileInsight {
    val tecnologia = dados.tecnologia
    return when {
        dados.radioDesligado ->
            MobileInsight("Off", "Rádio desligado. Não há tecnologia ativa neste momento.", c.warning)
        tecnologia?.contains("5G", ignoreCase = true) == true ->
            MobileInsight("5G", "5G NR — tecnologia mais rápida disponível para este chip.", c.primary)
        tecnologia?.contains("4G", ignoreCase = true) == true ->
            MobileInsight("4G", "4G LTE — bom equilíbrio entre cobertura e velocidade.", c.warning)
        tecnologia?.contains("3G", ignoreCase = true) == true ->
            MobileInsight("3G", "3G — tecnologia legada com menor capacidade para vídeo e uploads.", c.warning)
        tecnologia?.contains("2G", ignoreCase = true) == true ->
            MobileInsight("2G", "2G — suficiente para voz e mensagens básicas, mas limitada para internet.", c.error)
        else ->
            MobileInsight("Móvel", "Tecnologia não identificada pelo Android neste momento.", c.secondary)
    }
}

/** GH#1206 item 4/7 — RSRP ausente deixa de virar "Ótima"/"Boa" (o app afirmava
 *  experiencia excelente sem medicao alguma). Copy deixa explicito que e ESTIMATIVA
 *  baseada so no sinal (RSRP/RSRQ/SINR nao medem velocidade real, latencia, jitter ou
 *  perda), sem prometer estabilidade que os dados nao sustentam. */
internal fun classificarExperienciaMovel(
    dados: DadosSinalMovel,
    c: LkTokens,
): MobileInsight {
    if (dados.radioDesligado) {
        return MobileInsight("Indisponível", "Sem rede ativa para estimar experiência de uso.", c.warning)
    }
    val pior =
        piorMetricStatusSinalMovel(dados)
            ?: return MobileInsight(
                label = "Inconclusivo",
                description = "Sinal não medido pelo Android neste momento — não é possível estimar a experiência.",
                color = c.secondary,
            )
    return when (pior) {
        MetricStatus.excelente, MetricStatus.bom ->
            MobileInsight(
                "Boa (estimativa)",
                "Sinal indica boa experiência estimada para navegação, vídeo e chamadas — estimativa baseada só " +
                    "no sinal, não mede velocidade real, latência ou perda.",
                c.success,
            )
        MetricStatus.regular ->
            MobileInsight(
                "Regular (estimativa)",
                "Uso cotidiano possível, com chance de oscilação em horários de pico — estimativa baseada só no sinal.",
                c.warning,
            )
        MetricStatus.ruim, MetricStatus.critico ->
            MobileInsight(
                "Limitada (estimativa)",
                "Sinal fraco — chamadas e vídeo tendem a oscilar. Estimativa baseada só no sinal.",
                c.warning,
            )
        MetricStatus.inconclusivo ->
            MobileInsight("Inconclusivo", "Dados insuficientes para estimar a experiência de uso.", c.secondary)
    }
}
