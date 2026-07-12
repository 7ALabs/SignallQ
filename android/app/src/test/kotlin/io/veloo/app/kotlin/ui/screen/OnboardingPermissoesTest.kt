package io.signallq.app.ui.screen

import android.Manifest
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPermissoesTest {
    // API 33+ (POST_NOTIFICATIONS e NEARBY_WIFI_DEVICES existem como permissao em runtime)
    private val sdkComPermissoesNovas = Build.VERSION_CODES.TIRAMISU

    // API < 33 (POST_NOTIFICATIONS e NEARBY_WIFI_DEVICES nao existem como permissao)
    private val sdkAntigo = Build.VERSION_CODES.S

    @Test
    fun `permitir tudo gera as 6 permissoes reais em API 33+`() {
        val marcadas =
            OnboardingPermissoesMarcadas(
                wifiPerto = true,
                dispositivosRede = true,
                sinalChip = true,
                notificacoes = true,
            )

        val permissoes = permissoesAndroidParaSolicitar(marcadas, sdkInt = sdkComPermissoesNovas)

        assertEquals(
            setOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.POST_NOTIFICATIONS,
            ),
            permissoes.toSet(),
        )
    }

    @Test
    fun `toggle desmarcado nao entra na lista de solicitacao`() {
        val marcadas = OnboardingPermissoesMarcadas(wifiPerto = true)

        val permissoes = permissoesAndroidParaSolicitar(marcadas, sdkInt = sdkComPermissoesNovas)

        assertEquals(
            setOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            permissoes.toSet(),
        )
    }

    @Test
    fun `nenhum toggle marcado nao solicita nada`() {
        val permissoes = permissoesAndroidParaSolicitar(OnboardingPermissoesMarcadas(), sdkInt = sdkComPermissoesNovas)
        assertTrue(permissoes.isEmpty())
    }

    @Test
    fun `abaixo da API 33 nao solicita nearby wifi nem notificacao mesmo marcados`() {
        val marcadas = OnboardingPermissoesMarcadas(dispositivosRede = true, notificacoes = true)

        val permissoes = permissoesAndroidParaSolicitar(marcadas, sdkInt = sdkAntigo)

        assertTrue(permissoes.isEmpty())
    }

    @Test
    fun `abaixo da API 33 dispositivos e notificacoes contam como concedidas por definicao`() {
        val estado = estadoInicialPermissoesOnboarding(possuiPermissao = { false }, sdkInt = sdkAntigo)

        assertTrue(estado.dispositivosRede)
        assertTrue(estado.notificacoes)
        assertFalse(estado.wifiPerto)
        assertFalse(estado.sinalChip)
    }

    @Test
    fun `estado inicial reflete o que o sistema ja concedeu`() {
        val concedidas = setOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE)

        val estado =
            estadoInicialPermissoesOnboarding(
                possuiPermissao = { it in concedidas },
                sdkInt = sdkComPermissoesNovas,
            )

        assertTrue(estado.wifiPerto)
        assertTrue(estado.sinalChip)
        assertFalse(estado.dispositivosRede)
        assertFalse(estado.notificacoes)
    }

    @Test
    fun `resultado do sistema atualiza apenas as permissoes solicitadas nesta rodada`() {
        val atual = OnboardingPermissoesConcedidas(wifiPerto = true)
        val resultado = mapOf(Manifest.permission.READ_PHONE_STATE to true)

        val novo = aplicarResultadoPermissoesOnboarding(atual, resultado, sdkInt = sdkComPermissoesNovas)

        assertTrue(novo.wifiPerto) // preservado, nao fazia parte deste resultado
        assertTrue(novo.sinalChip)
        assertFalse(novo.dispositivosRede)
    }

    @Test
    fun `negar tudo no dialogo do sistema mantem nenhumaConcedida verdadeiro`() {
        val resultado =
            mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to false,
                Manifest.permission.ACCESS_COARSE_LOCATION to false,
                Manifest.permission.READ_PHONE_STATE to false,
            )

        val novo =
            aplicarResultadoPermissoesOnboarding(
                OnboardingPermissoesConcedidas(),
                resultado,
                sdkInt = sdkComPermissoesNovas,
            )

        assertTrue(novo.nenhumaConcedida)
    }

    @Test
    fun `conceder ao menos uma permissao derruba nenhumaConcedida`() {
        val estado = OnboardingPermissoesConcedidas(sinalChip = true)
        assertFalse(estado.nenhumaConcedida)
    }

    @Test
    fun `todasMarcadas reflete os 4 toggles ligados`() {
        val marcadas =
            OnboardingPermissoesMarcadas(
                wifiPerto = true,
                dispositivosRede = true,
                sinalChip = true,
                notificacoes = true,
            )
        assertTrue(marcadas.todasMarcadas)

        assertFalse(marcadas.copy(notificacoes = false).todasMarcadas)
    }
}
