package io.signallq.pro.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private const val ANGULO_INICIAL = 135f
private const val ANGULO_VARREDURA_MAXIMO = 270f
private const val ESPESSURA_TRACO_DP = 10

/**
 * Gauge circular para a métrica dominante de uma medição (ex.: download em Mbps) --
 * substitui grid de 6-7 cards de métrica por 1 destaque + [ListRow] expansível pras
 * secundárias (handoff Fase 2, #1161).
 */
@Composable
fun QualityGauge(
    valorFormatado: String,
    unidade: String,
    veredito: String,
    progresso: Float,
    modifier: Modifier = Modifier,
) {
    val progressoClamped = progresso.coerceIn(0f, 1f)
    val corAtiva = corVereditoQualidade(veredito)
    val corTrilha = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(160.dp)) {
                val stroke = Stroke(width = ESPESSURA_TRACO_DP.dp.toPx(), cap = StrokeCap.Round)
                drawArc(
                    color = corTrilha,
                    startAngle = ANGULO_INICIAL,
                    sweepAngle = ANGULO_VARREDURA_MAXIMO,
                    useCenter = false,
                    style = stroke,
                    size = Size(size.width - stroke.width, size.height - stroke.width),
                    topLeft = Offset(stroke.width / 2, stroke.width / 2),
                )
                drawArc(
                    color = corAtiva,
                    startAngle = ANGULO_INICIAL,
                    sweepAngle = ANGULO_VARREDURA_MAXIMO * progressoClamped,
                    useCenter = false,
                    style = stroke,
                    size = Size(size.width - stroke.width, size.height - stroke.width),
                    topLeft = Offset(stroke.width / 2, stroke.width / 2),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = valorFormatado, style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = unidade,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(text = veredito, style = MaterialTheme.typography.titleMedium, color = corAtiva)
    }
}

/**
 * Cor de status a partir do texto do veredito (Excelente/Bom/Regular/Fraco/Forte, vocabulario
 * canonico do produto -- ver .claude/CLAUDE.md, secao Design System), nao do valor cru de
 * [progresso]. Corrige #1170 item 6: cada tela calibra sua propria faixa de progresso pro
 * [QualityGauge] (ex.: 100 Mbps = "Excelente" no texto, mas so 0.5 de progresso numa referencia
 * de 200 Mbps) -- decidir a cor por faixa de progresso fazia "Excelente" cair na faixa do meio
 * (roxo/tertiary) em vez do verde de sucesso. Exposta para composables que precisem sincronizar
 * tonalidade com o gauge (ex.: chip).
 */
@Composable
fun corVereditoQualidade(veredito: String): Color =
    when (veredito) {
        "Excelente", "Bom", "Boa", "Forte" -> corStatusSucesso()
        "Regular" -> corStatusAtencao()
        "Fraco", "Fraca", "Ruim" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
