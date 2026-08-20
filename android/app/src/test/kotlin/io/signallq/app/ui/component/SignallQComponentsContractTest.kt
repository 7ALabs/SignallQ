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
    fun `adoption expands to migrated screens tracked by issue 1672`() {
        // Issue #1672 (Task 2.0.24, épico #1647) expande a adoção do banner 2.0 além
        // do piloto único do #1663 -- cada tela que ganha SignallQOfflineBanner() deve
        // parar de importar o OfflineBanner() legado (ver contrato "legacy component
        // contracts remain available" abaixo -- o legado continua existindo para quem
        // ainda não migrou, só não pode coexistir com o novo na mesma tela).
        val sourceRoot = findSourceRoot()
        val screens = File(sourceRoot, "screen")
        val allScreens = screens.walkTopDown().filter { it.extension == "kt" }.toList()
        val telasMigradas = listOf("DispositivosScreen.kt", "SinalScreen.kt")

        telasMigradas.forEach { nomeArquivo ->
            val texto = File(screens, nomeArquivo).readText()
            assertTrue("$nomeArquivo deveria chamar SignallQOfflineBanner()", "SignallQOfflineBanner()" in texto)
            assertFalse(
                "$nomeArquivo não deveria mais chamar o OfflineBanner() legado",
                Regex("(?m)^\\s*OfflineBanner\\(\\)").containsMatchIn(texto),
            )
        }

        val pilotCalls = Regex("SignallQOfflineBanner\\(\\)")
        assertEquals(telasMigradas.size, allScreens.sumOf { pilotCalls.findAll(it.readText()).count() })
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
