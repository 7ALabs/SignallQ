package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test

/**
 * GH#1245 — gate de QA visual do picker de tema (`TemaSheet.kt` > `ThemeSelector`). Critério de
 * aceite: "tema resolve pra opção válida e picker mostra uma opção destacada", inclusive com valor
 * salvo não reconhecido (bug corrigido em GH#1227 item 14 — `ThemePreference.parse` nunca cai em
 * "nenhuma opção selecionada", sempre resolve pra SYSTEM por padrão).
 */
class ThemeSelectorScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    @Test
    fun `tema sistema selecionado`() {
        paparazzi.snapshot { Harness { ThemeSelector(selecionado = "sistema", onSelect = {}, c = LocalLkTokens.current) } }
    }

    @Test
    fun `tema claro selecionado`() {
        paparazzi.snapshot { Harness { ThemeSelector(selecionado = "claro", onSelect = {}, c = LocalLkTokens.current) } }
    }

    @Test
    fun `tema escuro selecionado`() {
        paparazzi.snapshot { Harness { ThemeSelector(selecionado = "escuro", onSelect = {}, c = LocalLkTokens.current) } }
    }

    @Test
    fun `valor nao reconhecido resolve para sistema destacado, nunca sem selecao`() {
        // GH#1227 item 14 -- este e o cenario do bug real: um valor persistido invalido/antigo
        // nao pode deixar as 3 opcoes sem destaque nenhum.
        paparazzi.snapshot { Harness { ThemeSelector(selecionado = "valor-invalido-legado", onSelect = {}, c = LocalLkTokens.current) } }
    }
}

@Composable
private fun Harness(content: @Composable () -> Unit) {
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
