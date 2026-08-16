package io.signallq.app

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * GH#1690 -- crash ao trocar o tema do sistema com a pergunta contextual do Assist aberta.
 *
 * Causa raiz: sem declarar `uiMode` (e os demais eixos abaixo) em `android:configChanges`, o SO
 * destroi e recria a [MainActivity] (`handleRelaunchActivityInner`) a cada troca de tema
 * claro/escuro, idioma, tamanho de fonte ou densidade -- e essa recriacao expoe uma race de
 * plataforma em `androidx.activity.compose.setContent` (NullPointerException em
 * `ComponentActivity.kt:55`, `window.decorView.findViewById(android.R.id.content)` retornando
 * null durante o relaunch) que ora crasha o app, ora deixa a tela preta e travada sem crashar.
 * Reproduzido tanto no shell legado quanto no shell 2.0/Assist com `adb shell cmd uimode night
 * yes` em emulador real (AVD signallq_validacao, API 36) -- nao e especifico de nenhuma tela.
 *
 * Este teste nao reproduz a race em si (ela e uma condicao de corrida do framework, nao
 * determinística via `ActivityScenario.recreate()` -- esse metodo forca destroy+recreate por um
 * caminho diferente do relaunch real de config change e nao reproduziu o bug nem antes da
 * correcao). A regressao real que precisamos travar e a declaracao do manifest: se alguem
 * remover `uiMode`/`locale`/`fontScale`/`density` de `configChanges` no futuro, a Activity volta
 * a ser destruida e recriada nesses eixos, e a race volta a ficar exposta. Esta asserção sobre o
 * bitmask de `ActivityInfo.configChanges` falha imediatamente nesse cenario, sem depender de
 * reproduzir a race em CI.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityConfigChangesTest {
    @Test
    fun `MainActivity declara os eixos de config que evitam relaunch destrutivo`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val componentName = ComponentName(context, MainActivity::class.java)
        val activityInfo =
            context.packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)

        val eixosObrigatorios =
            mapOf(
                "uiMode (GH#1690 -- troca de tema claro/escuro)" to ActivityInfo.CONFIG_UI_MODE,
                "locale (GH#1690 -- troca de idioma)" to ActivityInfo.CONFIG_LOCALE,
                "fontScale (GH#1690 -- tamanho de fonte do sistema)" to ActivityInfo.CONFIG_FONT_SCALE,
                "density (GH#1690 -- densidade de tela)" to ActivityInfo.CONFIG_DENSITY,
                // Ja declarados antes do GH#1690 -- preservados para nao reintroduzir
                // recriacao destrutiva em rotacao/redimensionamento/teclado.
                "orientation" to ActivityInfo.CONFIG_ORIENTATION,
                "screenSize" to ActivityInfo.CONFIG_SCREEN_SIZE,
                "screenLayout" to ActivityInfo.CONFIG_SCREEN_LAYOUT,
                "keyboardHidden" to ActivityInfo.CONFIG_KEYBOARD_HIDDEN,
            )

        eixosObrigatorios.forEach { (rotulo, flag) ->
            assertTrue(
                "MainActivity deveria declarar '$rotulo' em android:configChanges " +
                    "(AndroidManifest.xml) para nao ser destruida/recriada nesse eixo -- " +
                    "ver GH#1690.",
                activityInfo.configChanges and flag != 0,
            )
        }
    }

    @Test
    fun `MainActivity permanece travada em portrait`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val componentName = ComponentName(context, MainActivity::class.java)
        val activityInfo =
            context.packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)

        // screenOrientation="portrait" ja impede orientation/screenSize de dispararem por
        // rotacao fisica -- GH#1690 confirmou isso em emulador (nenhum relaunch ao rotacionar).
        // Mantido explicito aqui para documentar por que rotacao nao faz parte do escopo do bug.
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activityInfo.screenOrientation)
    }
}
