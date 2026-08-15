package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.signallq.app.core.network.contracts.gateway.DeviceType
import io.signallq.app.core.network.contracts.gateway.PublicCompatibilityCatalog
import io.signallq.app.core.network.contracts.gateway.PublicCompatibilityEntry
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.component.Overline
import io.signallq.app.ui.component.SheetDragHandle

/**
 * Tela 2b-iii do design To-Be — "Modelos compatíveis", acessada a partir do
 * botão "Ver modelos de roteador compatíveis" da [GatewayConnectionSheet] (2b-i).
 *
 * Dado real via [PublicCompatibilityCatalog] (GH#539) — nunca mistura equipamento
 * validado fisicamente ([PublicCompatibilityCatalog.validado]) com equipamento
 * experimental ([PublicCompatibilityCatalog.experimental]) na mesma seção, regra
 * de negócio explícita do catálogo (ver KDoc de [PublicCompatibilityCatalog]).
 */
@Composable
internal fun GatewayCompatibleModelsSheetContent(
    onBack: () -> Unit,
    c: LkTokens,
) {
    val catalogo = PublicCompatibilityCatalog.montar()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.xl)
                .padding(bottom = LkSpacing.xxl)
                .navigationBarsPadding(),
    ) {
        SheetDragHandle()
        Spacer(Modifier.height(LkSpacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Voltar",
                    tint = c.textSecondary,
                )
            }
            Spacer(Modifier.width(LkSpacing.xs))
            Text(
                text = "Modelos compatíveis",
                style = MaterialTheme.typography.headlineSmall,
                color = c.textPrimary,
            )
        }
        Spacer(Modifier.height(LkSpacing.md))
        Text(
            text = "O SignallQ já testou a conexão automática com estes roteadores. Outros modelos também podem funcionar via usuário e senha manuais.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(LkSpacing.xl))

        if (catalogo.validado.isNotEmpty()) {
            Overline(texto = "Testados em laboratório")
            Spacer(Modifier.height(LkSpacing.sm))
            Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
                catalogo.validado.forEach { entry ->
                    CompatibleModelCard(entry = entry, selo = "Compatível", seloColor = c.success, c = c)
                }
            }
        }

        if (catalogo.experimental.isNotEmpty()) {
            if (catalogo.validado.isNotEmpty()) Spacer(Modifier.height(LkSpacing.xl))
            Overline(texto = "Compatibilidade experimental")
            Spacer(Modifier.height(LkSpacing.sm))
            Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
                catalogo.experimental.forEach { entry ->
                    CompatibleModelCard(entry = entry, selo = "Experimental", seloColor = c.warning, c = c)
                }
            }
        }
    }
}

@Composable
private fun CompatibleModelCard(
    entry: PublicCompatibilityEntry,
    selo: String,
    seloColor: Color,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .padding(LkSpacing.base),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(c.primary.copy(alpha = 0.12f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Router,
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.width(LkSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${entry.vendor} ${entry.modelo}",
                style = MaterialTheme.typography.titleSmall,
                color = c.textPrimary,
            )
            Text(
                text = deviceTypeDisplayLabel(entry.deviceType),
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
        Spacer(Modifier.width(LkSpacing.sm))
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(LkRadius.pill))
                    .background(seloColor.copy(alpha = 0.14f))
                    .padding(horizontal = LkSpacing.sm, vertical = 4.dp),
        ) {
            Text(
                text = selo,
                style = MaterialTheme.typography.labelMedium,
                color = seloColor,
            )
        }
    }
}

private fun deviceTypeDisplayLabel(deviceType: DeviceType): String =
    when (deviceType) {
        DeviceType.ONT_GPON -> "ONT / GPON (fibra)"
        DeviceType.ROUTER -> "Roteador Wi-Fi"
        DeviceType.MESH_OR_EXTENDER -> "Rede mesh / repetidor"
        DeviceType.UNKNOWN_SUPPORTED, DeviceType.UNKNOWN_UNSUPPORTED -> "Equipamento compatível"
    }
