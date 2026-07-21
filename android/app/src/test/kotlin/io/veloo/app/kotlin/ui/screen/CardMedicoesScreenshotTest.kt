package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test

/**
 * GH#1245 — gate de QA visual do `CardMedicoes` (Home) via screenshot testing headless
 * (Paparazzi/Robolectric), sem depender de device/emulador real. Cobre os 3 estados citados no
 * critério de aceite da issue: resultado atual, "Resultado anterior ·" (histórico) e sem-dados
 * (CTA único, GH#1223 item 9/RF-10).
 *
 * Timestamp fixo em ~150 min atrás (não "agora") pra manter `formatRelativeTimestamp` estável
 * entre execuções — cai sempre no bucket "há 2 h" (120-179 min), nunca no de minutos.
 */
class CardMedicoesScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    private val timestampEstavel = System.currentTimeMillis() - (150L * 60 * 1000)

    @Test
    fun `estado com resultado atual`() {
        paparazzi.snapshot {
            HarnessCardMedicoes {
                CardMedicoes(
                    effectiveTs = timestampEstavel,
                    effectiveDl = 245.3,
                    effectiveUl = 118.7,
                    hasEffectiveResult = true,
                    resultadoEhAnterior = false,
                    onAbrirHistorico = {},
                    onIniciarTeste = {},
                    c = LocalLkTokens.current,
                )
            }
        }
    }

    @Test
    fun `estado com resultado anterior (prefixo distintivo)`() {
        paparazzi.snapshot {
            HarnessCardMedicoes {
                CardMedicoes(
                    effectiveTs = timestampEstavel,
                    effectiveDl = 245.3,
                    effectiveUl = 118.7,
                    hasEffectiveResult = true,
                    resultadoEhAnterior = true,
                    onAbrirHistorico = {},
                    onIniciarTeste = {},
                    c = LocalLkTokens.current,
                )
            }
        }
    }

    @Test
    fun `estado sem dados (CTA unico)`() {
        paparazzi.snapshot {
            HarnessCardMedicoes {
                CardMedicoes(
                    effectiveTs = null,
                    effectiveDl = null,
                    effectiveUl = null,
                    hasEffectiveResult = false,
                    resultadoEhAnterior = false,
                    onAbrirHistorico = {},
                    onIniciarTeste = {},
                    c = LocalLkTokens.current,
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun HarnessCardMedicoes(content: @androidx.compose.runtime.Composable () -> Unit) {
    SignallQTheme(darkTheme = false) {
        Box(
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
        ) {
            content()
        }
    }
}
