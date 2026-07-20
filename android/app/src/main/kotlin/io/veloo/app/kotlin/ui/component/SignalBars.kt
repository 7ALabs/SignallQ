package io.signallq.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.signallq.app.core.diagnostico.BandaWifi
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens

/**
 * Indicador de força de sinal Wi-Fi em 4 barras, por faixa de dBm (RSSI). Extraído de
 * `SinalScreen.kt` (GH#1201) para reuso em `SinalWifiScreen.kt` — comportamento e assinatura
 * preservados 1:1, nenhuma mudança visual ou de cálculo.
 */
@Composable
fun SignalBars(
    rssiDbm: Int,
    banda: BandaWifi = BandaWifi.desconhecida,
    overrideColor: Color? = null,
) {
    val bars =
        when {
            rssiDbm >= -50 -> 4
            rssiDbm >= -60 -> 3
            rssiDbm >= -70 -> 2
            else -> 1
        }
    val c = LocalLkTokens.current
    val color = overrideColor ?: signalColor(rssiDbm, banda, c)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        for (i in 1..4) {
            Box(
                Modifier
                    .width(4.dp)
                    .height((4 + i * 3).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (i <= bars) color else c.border),
            )
        }
    }
}

/**
 * Cor semântica do sinal (sucesso/atenção/erro) por faixa de dBm — mais permissiva em 5GHz
 * (RSSI naturalmente mais baixo por atenuação de frequência). Extraído junto com [SignalBars]
 * porque é a única consumidora fora de `SinalScreen.kt`; os 3 usos que restaram em
 * `SinalScreen.kt` (cores de item de lista de rede) importam esta função pública.
 */
fun signalColor(
    rssiDbm: Int,
    banda: BandaWifi = BandaWifi.desconhecida,
    c: LkTokens,
): Color =
    when (banda) {
        BandaWifi.ghz5 ->
            when {
                rssiDbm >= -65 -> c.success
                rssiDbm >= -75 -> c.warning
                else -> c.error
            }
        else ->
            when {
                rssiDbm >= -60 -> c.success
                rssiDbm >= -70 -> c.warning
                else -> c.error
            }
    }
