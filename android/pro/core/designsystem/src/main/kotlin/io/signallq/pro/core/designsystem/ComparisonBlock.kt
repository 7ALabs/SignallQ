package io.signallq.pro.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Bloco antes/depois do Pro (#1170 item 1) -- comparativo de metrica pre/pos intervencao
 * (ex.: latencia antes/depois de trocar canal Wi-Fi). Sem card wrapper (o JSX de origem nao
 * usa `.sqp-card`, e um bloco de conteudo simples dentro de outro container).
 *
 * `valor antes/depois` (22sp/600) e a seta (20sp/600) usam `titleLarge.copy(fontSize = ...)`
 * pelo mesmo motivo do [TopBar]: a escala do Pro nao tem slot nativo pra 22sp, e `.copy()`
 * preserva a `fontFamily` do tema (o peso 600 ja bate com `titleLarge`, so o tamanho muda).
 */
@Composable
fun ComparisonBlock(
    rotulo: String,
    antes: Float,
    depois: Float,
    unidade: String,
    modifier: Modifier = Modifier,
    maiorEhMelhor: Boolean = true,
    conclusao: String? = null,
) {
    val delta = depois - antes
    val percentual = if (antes != 0f) (delta / abs(antes)) * 100f else null
    val cor = corComparacao(delta, maiorEhMelhor)
    val seta =
        when {
            delta > 0f -> "↑"
            delta < 0f -> "↓"
            else -> "→"
        }

    Column(
        modifier = modifier.widthIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = rotulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            ValorAntesDepois(rotulo = "ANTES", valor = formatarValor(antes, unidade))
            Text(
                text = seta,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 24.sp),
                color = cor,
            )
            ValorAntesDepois(rotulo = "DEPOIS", valor = formatarValor(depois, unidade))
        }
        Text(
            text = formatarLinhaDelta(delta, percentual, unidade),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = cor,
        )
        if (conclusao != null) {
            Text(text = conclusao, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ValorAntesDepois(
    rotulo: String,
    valor: String,
) {
    Column {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun corComparacao(
    delta: Float,
    maiorEhMelhor: Boolean,
): Color =
    when {
        delta == 0f -> MaterialTheme.colorScheme.onSurfaceVariant
        (delta > 0f) == maiorEhMelhor -> corStatusSucesso()
        else -> MaterialTheme.colorScheme.error
    }

private fun formatarValor(
    valor: Float,
    unidade: String,
): String {
    val numero = if (valor == valor.roundToInt().toFloat()) valor.roundToInt().toString() else "%.1f".format(valor)
    return "$numero$unidade"
}

private fun formatarLinhaDelta(
    delta: Float,
    percentual: Float?,
    unidade: String,
): String {
    val sinalDelta = if (delta >= 0f) "+" else ""
    val deltaFormatado = "$sinalDelta${delta.roundToInt()}$unidade"
    if (percentual == null) return deltaFormatado
    val sinalPercentual = if (percentual >= 0f) "+" else ""
    return "$deltaFormatado ($sinalPercentual${percentual.roundToInt()}%)"
}
