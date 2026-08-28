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
        // Regex usa só o abre-parêntese (SignallQOfflineBanner\() em vez da chamada vazia
        // completa (SignallQOfflineBanner\(\)) -- achado da revisão do Caio na PR #1815: a
        // Task 4 (#1811) passa o callback real `onDiagnosticarProblema`, então a chamada deixa
        // de ser SignallQOfflineBanner() e vira SignallQOfflineBanner(onDiagnosticarProblema =
        // ...). A string literal vazia e o regex fechado quebrariam por mudança intencional de
        // adoção, não por regressão real -- o contrato que importa é "a tela chama o composable
        // novo", não "chama sem nenhum argumento".
        val sourceRoot = findSourceRoot()
        val screens = File(sourceRoot, "screen")
        val allScreens = screens.walkTopDown().filter { it.extension == "kt" }.toList()
        val telasMigradas = listOf("DispositivosScreen.kt", "SinalScreen.kt")

        telasMigradas.forEach { nomeArquivo ->
            val texto = File(screens, nomeArquivo).readText()
            assertTrue("$nomeArquivo deveria chamar SignallQOfflineBanner(", "SignallQOfflineBanner(" in texto)
            assertFalse(
                "$nomeArquivo não deveria mais chamar o OfflineBanner() legado",
                Regex("(?m)^\\s*OfflineBanner\\(\\)").containsMatchIn(texto),
            )
        }

        val pilotCalls = Regex("SignallQOfflineBanner\\(")
        assertEquals(telasMigradas.size, allScreens.sumOf { pilotCalls.findAll(it.readText()).count() })
    }

    @Test
    fun `legacy component contracts remain available`() {
        // OfflineBanner.kt e StatefulScreen.kt (pré-2.0) foram removidos na issue #1673
        // (Task 2.0.25, épico #1647): zero consumidor de produção restante -- só
        // sobreviviam porque este teste os travava propositalmente até a remoção.
        // ConfirmacaoDialog segue vigente e sem substituto 2.0, então continua coberto aqui.
        val components = File(findSourceRoot(), "component")
        assertTrue(File(components, "ConfirmacaoDialog.kt").readText().contains("fun ConfirmacaoDialog("))
        assertFalse(File(components, "OfflineBanner.kt").exists())
        assertFalse(File(components, "StatefulScreen.kt").exists())
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
