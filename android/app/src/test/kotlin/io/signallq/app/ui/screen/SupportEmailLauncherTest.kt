package io.signallq.app.ui.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SupportEmailLauncherTest {
    private val context = mockk<Context>(relaxed = true)

    @Test
    fun `support email uses mailto without exposing data`() {
        var launched: Intent? = null

        assertTrue(abrirEmailSuporte(context) { launched = it })

        assertEquals(Intent.ACTION_SENDTO, launched?.action)
        assertEquals("mailto:suporte@signallq.com", launched?.dataString)
    }

    @Test
    fun `missing external email handler returns local fallback signal`() {
        assertFalse(
            abrirEmailSuporte(context) {
                throw ActivityNotFoundException()
            },
        )
    }
}
