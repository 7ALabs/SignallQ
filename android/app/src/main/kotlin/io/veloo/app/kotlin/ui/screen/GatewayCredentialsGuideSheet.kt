package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Rotate90DegreesCcw
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.SheetDragHandle

/**
 * Passo do guia ilustrado de credenciais (GH#529, epic #525).
 */
private data class PassoCredencial(
    val icone: ImageVector,
    val titulo: String,
    val descricao: String,
)

private val passosCredenciais =
    listOf(
        PassoCredencial(
            icone = Icons.Outlined.Rotate90DegreesCcw,
            titulo = "Vire o roteador",
            descricao = "A etiqueta com os dados de acesso costuma ficar embaixo ou atrás do aparelho.",
        ),
        PassoCredencial(
            icone = Icons.AutoMirrored.Outlined.Label,
            titulo = "Encontre a etiqueta",
            descricao = "Procure os campos \"Usuário\" e \"Senha\" (ou \"Login\" e \"Password\").",
        ),
        PassoCredencial(
            icone = Icons.Outlined.Password,
            titulo = "Usuário e senha padrão estão ali",
            descricao = "Digite exatamente como está escrito, com atenção a maiúsculas e minúsculas.",
        ),
        PassoCredencial(
            icone = Icons.Outlined.Edit,
            titulo = "Se já foi alterado, use o que você configurou",
            descricao = "Se você já trocou a senha antes, use a que definiu — não a da etiqueta.",
        ),
    )

/**
 * Guia ilustrado de como obter usuário e senha do modem/roteador (GH#529, epic #525).
 *
 * Aberto como bottom sheet a partir da [GatewayConnectionSheet], sem sair do
 * fluxo de conexão. Conteúdo 100% embutido no app (sem imagens de rede),
 * para funcionar offline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatewayCredentialsGuideSheet(onDismissRequest: () -> Unit) {
    val c = LocalLkTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = c.bgSecondary,
    ) {
        GatewayCredentialsGuideSheetContent(c = c)
    }
}

@Composable
internal fun GatewayCredentialsGuideSheetContent(c: LkTokens) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.xl)
                .padding(bottom = LkSpacing.xxl)
                .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.xl),
    ) {
        SheetDragHandle()
        Text(
            text = "Como encontrar usuário e senha do roteador",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.W700,
            color = c.textPrimary,
        )
        Text(
            text = "Essas informações costumam vir de fábrica, impressas no próprio aparelho.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )

        Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.xl)) {
            passosCredenciais.forEachIndexed { index, passo ->
                PassoCredencialRow(numero = index + 1, passo = passo, c = c)
            }
        }
    }
}

@Composable
private fun PassoCredencialRow(
    numero: Int,
    passo: PassoCredencial,
    c: LkTokens,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(LkColors.accent.copy(alpha = 0.12f)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = passo.icone,
                    contentDescription = null,
                    tint = LkColors.accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Passo $numero",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W600,
                color = LkColors.accent,
            )
            Text(
                text = passo.titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Text(
                text = passo.descricao,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
        }
    }
}
