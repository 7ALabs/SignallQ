package io.signallq.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes de [encontrarActivity] — issue #1703.
 *
 * A função existe porque `LocalContext.current` **não** é garantidamente uma `Activity`: o Compose
 * entrega o contexto da composição, que em vários caminhos vem embrulhado. Um `as Activity` direto
 * funciona no teste feliz e estoura em produção assim que qualquer camada embrulha — por isso o
 * desembrulho iterativo, e por isso estes casos.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContextActivityTest {
    @Test
    fun `activity direta se encontra a si mesma`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        assertSame(activity, activity.encontrarActivity())
    }

    @Test
    fun `activity embrulhada uma vez e encontrada`() {
        // Caso real: `ContextThemeWrapper` aplicado por tema Material.
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val embrulhado: Context = ContextWrapper(activity)
        assertSame(activity, embrulhado.encontrarActivity())
    }

    @Test
    fun `activity embrulhada varias vezes e encontrada`() {
        // Mutante que este teste mata: trocar a recursão por um único `baseContext as? Activity`.
        // Um nível de desembrulho passa no teste anterior e falha aqui — e produção empilha mais
        // de um wrapper com facilidade (tema + host de ComposeView, por exemplo).
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val embrulhado: Context = ContextWrapper(ContextWrapper(ContextWrapper(activity)))
        assertSame(activity, embrulhado.encontrarActivity())
    }

    @Test
    fun `contexto de aplicacao devolve nulo em vez de estourar`() {
        // É o caso de `@Preview` e de teste de composição sem Activity hospedeira. Devolver null
        // faz a tela apenas não oferecer a entrada da UMP; lançar derrubaria a Privacidade inteira
        // por causa de um item opcional.
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        assertNull(appContext.encontrarActivity())
    }

    @Test
    fun `contexto de aplicacao embrulhado tambem devolve nulo`() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        assertNull(ContextWrapper(ContextWrapper(appContext)).encontrarActivity())
    }

    @Test
    fun `subclasse de Activity e reconhecida`() {
        // A checagem é `is Activity`, não igualdade de tipo — qualquer Activity do app serve.
        val activity: Activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        assertSame(activity, ContextWrapper(activity).encontrarActivity())
    }
}
