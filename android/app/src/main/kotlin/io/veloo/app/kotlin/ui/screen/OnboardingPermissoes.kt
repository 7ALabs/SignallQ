package io.signallq.app.ui.screen

import android.Manifest
import android.os.Build

/**
 * Estado real (segundo o Android) das 4 categorias de permissao oferecidas no onboarding.
 * NEARBY_WIFI_DEVICES e POST_NOTIFICATIONS so existem como permissao em runtime a partir da
 * API 33 (TIRAMISU) — abaixo disso o recurso ja funciona sem pedir nada, entao contam como
 * concedidas por definicao.
 */
data class OnboardingPermissoesConcedidas(
    val wifiPerto: Boolean = false,
    val dispositivosRede: Boolean = false,
    val sinalChip: Boolean = false,
    val notificacoes: Boolean = false,
) {
    val nenhumaConcedida: Boolean
        get() = !wifiPerto && !dispositivosRede && !sinalChip && !notificacoes
}

/** Quais dos 4 toggles da tela 2 do onboarding estao marcados pelo usuario. */
data class OnboardingPermissoesMarcadas(
    val wifiPerto: Boolean = false,
    val dispositivosRede: Boolean = false,
    val sinalChip: Boolean = false,
    val notificacoes: Boolean = false,
) {
    val todasMarcadas: Boolean
        get() = wifiPerto && dispositivosRede && sinalChip && notificacoes
}

/** Le o estado real de concessao no sistema — usado para preencher a tela ao abrir (reinstall etc). */
fun estadoInicialPermissoesOnboarding(
    possuiPermissao: (String) -> Boolean,
    sdkInt: Int = Build.VERSION.SDK_INT,
): OnboardingPermissoesConcedidas =
    OnboardingPermissoesConcedidas(
        wifiPerto = possuiPermissao(Manifest.permission.ACCESS_FINE_LOCATION),
        dispositivosRede =
            sdkInt < Build.VERSION_CODES.TIRAMISU || possuiPermissao(Manifest.permission.NEARBY_WIFI_DEVICES),
        sinalChip = possuiPermissao(Manifest.permission.READ_PHONE_STATE),
        notificacoes =
            sdkInt < Build.VERSION_CODES.TIRAMISU || possuiPermissao(Manifest.permission.POST_NOTIFICATIONS),
    )

/**
 * Traduz os toggles marcados na tela 2 para a lista real de permissoes Android a solicitar
 * via RequestMultiplePermissions. So permissoes marcadas entram na lista — nenhuma e obrigatoria.
 */
fun permissoesAndroidParaSolicitar(
    marcadas: OnboardingPermissoesMarcadas,
    sdkInt: Int = Build.VERSION.SDK_INT,
): List<String> {
    val permissoes = mutableListOf<String>()
    if (marcadas.wifiPerto) {
        permissoes += Manifest.permission.ACCESS_FINE_LOCATION
        permissoes += Manifest.permission.ACCESS_COARSE_LOCATION
    }
    if (marcadas.dispositivosRede && sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        permissoes += Manifest.permission.NEARBY_WIFI_DEVICES
    }
    if (marcadas.sinalChip) {
        permissoes += Manifest.permission.READ_PHONE_STATE
    }
    if (marcadas.notificacoes && sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        permissoes += Manifest.permission.POST_NOTIFICATIONS
    }
    return permissoes
}

/**
 * Atualiza o estado de concessao com o resultado real do RequestMultiplePermissions.
 * Permissoes ausentes do mapa (nao solicitadas nesta rodada) preservam o valor anterior.
 */
fun aplicarResultadoPermissoesOnboarding(
    atual: OnboardingPermissoesConcedidas,
    resultado: Map<String, Boolean>,
    sdkInt: Int = Build.VERSION.SDK_INT,
): OnboardingPermissoesConcedidas =
    OnboardingPermissoesConcedidas(
        wifiPerto = resultado[Manifest.permission.ACCESS_FINE_LOCATION] ?: atual.wifiPerto,
        dispositivosRede =
            when {
                sdkInt < Build.VERSION_CODES.TIRAMISU -> true
                else -> resultado[Manifest.permission.NEARBY_WIFI_DEVICES] ?: atual.dispositivosRede
            },
        sinalChip = resultado[Manifest.permission.READ_PHONE_STATE] ?: atual.sinalChip,
        notificacoes =
            when {
                sdkInt < Build.VERSION_CODES.TIRAMISU -> true
                else -> resultado[Manifest.permission.POST_NOTIFICATIONS] ?: atual.notificacoes
            },
    )
