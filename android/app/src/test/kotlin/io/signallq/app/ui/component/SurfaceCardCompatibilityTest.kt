package io.signallq.app.ui.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SurfaceCardCompatibilityTest {
    @Test
    fun `referencia tipada preserva assinatura legada`() {
        assertNotNull(legacySurfaceCardReference)
    }

    @Test
    fun `foundations 2 fica opt-in em um unico piloto`() {
        val mainSource = localizarMainSource()
        val ocorrencias =
            mainSource
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .sumOf { arquivo -> Regex("\\bSignallQSurfaceCard\\(").findAll(arquivo.readText()).count() }
        val piloto = File(mainSource, "io/signallq/app/ui/component/OperadoraContactCard.kt").readText()

        assertEquals("uma definição e um único call site-piloto", 2, ocorrencias)
        assertTrue(piloto.contains("SignallQSurfaceCard("))
        assertTrue(!piloto.contains("LkSurfaceCard("))
    }
}

private val legacySurfaceCardReference:
    @Composable (Modifier, Boolean, @Composable ColumnScope.() -> Unit) -> Unit = ::LkSurfaceCard

private fun localizarMainSource(): File {
    val inicio = File(requireNotNull(System.getProperty("user.dir")))
    return generateSequence(inicio) { it.parentFile }
        .map { raiz -> File(raiz, "app/src/main/kotlin") }
        .firstOrNull(File::isDirectory)
        ?: error("android/app/src/main/kotlin não encontrado a partir de $inicio")
}
