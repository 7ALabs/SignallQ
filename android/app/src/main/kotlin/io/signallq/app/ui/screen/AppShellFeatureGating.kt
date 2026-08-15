package io.signallq.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkTokens

// Gate de navegacao dos 9 modulos `:feature:*` do Consumer via Firebase Remote Config
// (Epico #1347, F4/#1480) -- extraido de `AppShell.kt` pra nao inflar ainda mais um
// arquivo ja acima do limiar de "divida critica" (regra de higiene, secao 4.3).
//
// So cobre os 9 modulos listados na issue #1480 -- `Overlay.Privacidade`/`Overlay.Termos`
// (obrigacao legal/LGPD) e o hub Ferramentas (nao pertence a um unico modulo) nunca
// passam por aqui, por decisao explicita do Epico ("Flags nao podem... esconder
// obrigacao legal ou privacidade").

/** Identificador curto de cada modulo -- mesmo padrao ja usado nos `registrarFeatureUsada`
 *  existentes em MainActivity (ex.: "wifi", "dns", "fibra"). Tambem o valor enviado em
 *  `feature_blocked_remote` (sem dado pessoal). */
internal object ConsumerFeatureModuleIds {
    const val HOME = "home"
    const val SPEEDTEST = "speedtest"
    const val WIFI = "wifi"
    const val DEVICES = "devices"
    const val DNS = "dns"
    const val FIBRA = "fibra"
    const val DIAGNOSTICO = "diagnostico"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

/** Indice das 4 tabs fixas gateadas por modulo -- tab 4 (Ferramentas) nunca aparece aqui,
 *  e um hub de atalhos, nao pertence a um unico modulo do catalogo. */
internal fun AppShellFeatureFlagsState.tabHabilitada(index: Int): Boolean =
    when (index) {
        0 -> homeEnabled
        1 -> speedtestEnabled
        2 -> wifiEnabled
        3 -> historyEnabled
        else -> true
    }

internal fun tabModuleId(index: Int): String? =
    when (index) {
        0 -> ConsumerFeatureModuleIds.HOME
        1 -> ConsumerFeatureModuleIds.SPEEDTEST
        2 -> ConsumerFeatureModuleIds.WIFI
        3 -> ConsumerFeatureModuleIds.HISTORY
        else -> null
    }

/** Primeira tab habilitada na ordem de prioridade 1 (Velocidade, padrao de cold start),
 *  0 (Inicio), 2 (Sinal), 3 (Historico) -- usada quando a tab atual perde a flag em
 *  runtime e precisa de um destino seguro. `4` (Ferramentas) e sempre o ultimo recurso
 *  por nunca ser gateada. */
internal fun AppShellFeatureFlagsState.primeiraTabHabilitada(): Int =
    listOf(1, 0, 2, 3).firstOrNull { tabHabilitada(it) } ?: 4

/** Fallback respeita as raízes expostas pelo shell ativo. */
internal fun AppShellFeatureFlagsState.primeiraTabHabilitada(mode: AppShellMode): Int =
    mode.roots().firstOrNull { tabHabilitada(it.legacyIndex) }?.legacyIndex ?: AppShellRoot.Tools.legacyIndex

/**
 * Bloqueio de uma rota/overlay: registra `feature_blocked_remote` (sem dado pessoal) e
 * devolve `false` -- call sites usam o retorno pra decidir se abrem o overlay de verdade.
 * Retorna `true` (permitido) sem efeito colateral quando a flag esta ligada.
 */
internal fun AppShellFeatureFlagsState.permitirOuBloquear(
    habilitada: Boolean,
    moduleId: String,
): Boolean {
    if (!habilitada) onFeatureBlocked(moduleId)
    return habilitada
}

/**
 * Conteudo neutro exibido no lugar de uma tab desabilitada (comportamento
 * `HIDE_ENTRY_AND_BLOCK_ROUTE` do catalogo) -- so acontece se o usuario ja estivesse
 * nessa tab no instante em que a flag desligou (o clique na tab bar ja fica desabilitado
 * antes disso, ver `AppBottomNavBar`).
 */
@Composable
internal fun FeatureDisabledContent(
    c: LkTokens,
    mensagem: String = "Recurso temporariamente indisponível.",
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = c.textTertiary,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Text(
            text = mensagem,
            style = MaterialTheme.typography.bodyLarge,
            color = c.textSecondary,
        )
    }
}
