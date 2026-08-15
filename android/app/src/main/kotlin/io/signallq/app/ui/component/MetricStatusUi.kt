package io.signallq.app.ui.component

import androidx.compose.ui.graphics.Color
import io.signallq.app.core.diagnostico.MetricStatus
import io.signallq.app.ui.LkTokens

/**
 * Mapeamento UNICO de [MetricStatus] (vocabulario canonico de 6 valores, `core/diagnostico`)
 * para cor semantica e label PT-BR — GH#1221 RF-06 / GH#1225 item C.
 *
 * Antes desta issue, `ResultadoVelocidadeScreen.kt` tinha sua propria regua de 3 valores
 * (Excelente/Regular/Ruim) com limiares numericos proprios para download/upload/latencia/
 * jitter/bufferbloat, divergentes do classificador canonico usado pelo motor de diagnostico
 * e pelo restante do app. Este arquivo e o UNICO ponto de conversao MetricStatus -> UI —
 * qualquer tela que precise mostrar veredito de metrica usa isto, nao reimplementa.
 *
 * `LkTokens` so tem 3 cores semanticas (success/warning/error) — nao existe uma 4a cor
 * "critico" nem uma variante clara para "bom" distinta de "excelente" hoje no design
 * system. Mapeamento conservador: excelente/bom -> success, regular -> warning,
 * ruim/critico -> error, inconclusivo -> textTertiary (neutro, sem alarmar o usuario por
 * falta de dado).
 */
fun MetricStatus.corSemantica(c: LkTokens): Color =
    when (this) {
        MetricStatus.excelente, MetricStatus.bom -> c.success
        MetricStatus.regular -> c.warning
        MetricStatus.ruim, MetricStatus.critico -> c.error
        MetricStatus.inconclusivo -> c.textTertiary
    }

fun MetricStatus.labelPt(): String =
    when (this) {
        MetricStatus.excelente -> "Excelente"
        MetricStatus.bom -> "Bom"
        MetricStatus.regular -> "Regular"
        MetricStatus.ruim -> "Ruim"
        MetricStatus.critico -> "Crítico"
        MetricStatus.inconclusivo -> "Inconclusivo"
    }
