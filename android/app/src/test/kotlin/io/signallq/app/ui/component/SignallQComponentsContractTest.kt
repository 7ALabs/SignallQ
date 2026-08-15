package io.signallq.app.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SignallQComponentsContractTest {
    @Test
    fun `feedback exposes textual meaning in addition to color`() {
        assertEquals("Informação", SignallQFeedbackTone.Neutral.accessibleLabel())
        assertEquals("Sucesso", SignallQFeedbackTone.Success.accessibleLabel())
        assertEquals("Atenção", SignallQFeedbackTone.Warning.accessibleLabel())
        assertEquals("Erro", SignallQFeedbackTone.Error.accessibleLabel())
    }

    @Test
    fun `adoption is opt-in and limited to one pilot`() {
        val sourceRoot = findSourceRoot()
        val screens = File(sourceRoot, "screen")
        val allScreens = screens.walkTopDown().filter { it.extension == "kt" }.toList()
        val pilot = File(screens, "DispositivosScreen.kt").readText()

        assertTrue("SignallQOfflineBanner()" in pilot)
        assertFalse(Regex("(?m)^\\s*OfflineBanner\\(\\)").containsMatchIn(pilot))
        val pilotCalls = Regex("SignallQOfflineBanner\\(\\)")
        assertEquals(1, allScreens.sumOf { pilotCalls.findAll(it.readText()).count() })
    }

    @Test
    fun `legacy component contracts remain available`() {
        val components = File(findSourceRoot(), "component")
        assertTrue(File(components, "OfflineBanner.kt").readText().contains("fun OfflineBanner("))
        assertTrue(File(components, "ConfirmacaoDialog.kt").readText().contains("fun ConfirmacaoDialog("))
        assertTrue(File(components, "StatefulScreen.kt").readText().contains("fun <T> StatefulScreen("))
    }

    private fun findSourceRoot(): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        repeat(6) {
            val candidate = File(current, "app/src/main/kotlin/io/signallq/app/ui")
            if (candidate.isDirectory) return candidate
            current = current.parentFile ?: return@repeat
        }
        error("Android source root not found")
    }
}
