package io.signallq.pro.feature.auth

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.signallq.pro.core.designsystem.StateCard
import io.signallq.pro.core.designsystem.StateCardVariant

/**
 * Tela 1.8 -- StateCard variante erro + botao para Ajustes do sistema. Um StateCard so,
 * sem duplicar com banner/ilustracao (handoff Fase 2, #1161).
 */
@Composable
fun PermissaoBloqueadaScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Scaffold { paddingValues ->
        StateCard(
            variant = StateCardVariant.ERRO,
            titulo = "Permissao bloqueada",
            mensagem =
                "Voce negou uma permissao essencial permanentemente. Abra os ajustes do " +
                    "sistema para conceder manualmente.",
            acaoTexto = "Abrir ajustes",
            onAcaoClick = {
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                context.startActivity(intent)
            },
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
        )
    }
}
