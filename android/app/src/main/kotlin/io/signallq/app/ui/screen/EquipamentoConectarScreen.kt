package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.core.network.contracts.gateway.GatewayConnectionService
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkSectionOverline

/**
 * Etapa de conexão do equipamento (GH#1806), aberta a partir do hub Ferramentas quando ainda não
 * há um endereço salvo para o modem ou roteador — antes disso, [FerramentasScreen] pulava direto
 * para [EquipamentoInternetScreen], que só oferecia o formulário de conexão depois de uma
 * tentativa de leitura já falhar.
 *
 * Reaproveita [GatewayConnectionSheet] (formulário completo, com "lembrar senha"/"manter
 * conectado") e [GatewayCompatibleModelsSheetContent] (catálogo de modelos) — mesmos componentes
 * já usados pelo nó do gateway na Home, sem duplicar campo nem lógica de conexão.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipamentoConectarScreen(
    enderecoDetectado: String?,
    conectar: GatewayConnectionService,
    onVoltar: () -> Unit,
    onAbrirMenu: () -> Unit,
    onConectado: (ip: String, usuario: String, senha: String, lembrarSenha: Boolean, manterConectado: Boolean) -> Unit,
    onPular: () -> Unit,
) {
    val c = LocalLkTokens.current
    var mostrarFormularioConexao by remember { mutableStateOf(false) }
    var mostrarModelosCompativeis by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = { EquipamentoConectarTopBar(onVoltar = onVoltar, onAbrirMenu = onAbrirMenu, c = c) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            IntroConexao(c = c)

            EnderecoDetectadoCard(
                enderecoDetectado = enderecoDetectado,
                onConectar = { mostrarFormularioConexao = true },
                c = c,
            )

            LkSectionOverline(text = "Modems e roteadores compatíveis")
            ModelosCompativeisRow(onClick = { mostrarModelosCompativeis = true }, c = c)

            Spacer(Modifier.height(LkSpacing.sm))
            TextButton(onClick = onPular, modifier = Modifier.fillMaxWidth()) {
                Text("Pular por enquanto", color = c.textSecondary)
            }
        }
    }

    if (mostrarFormularioConexao) {
        GatewayConnectionSheet(
            ipInicial = enderecoDetectado,
            onDismissRequest = { mostrarFormularioConexao = false },
            conectar = conectar,
            onConectado = onConectado,
        )
    }

    if (mostrarModelosCompativeis) {
        ModalBottomSheet(
            onDismissRequest = { mostrarModelosCompativeis = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.bgSecondary,
        ) {
            GatewayCompatibleModelsSheetContent(onBack = { mostrarModelosCompativeis = false }, c = c)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EquipamentoConectarTopBar(
    onVoltar: () -> Unit,
    onAbrirMenu: () -> Unit,
    c: LkTokens,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                "Equipamento de internet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
        },
        navigationIcon = {
            IconButton(onClick = onVoltar) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar", tint = c.textPrimary)
            }
        },
        actions = {
            IconButton(onClick = onAbrirMenu) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Abrir ajustes", tint = c.textPrimary)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
    )
}

@Composable
private fun IntroConexao(c: LkTokens) {
    Text(
        "Conecte para ver os dados do seu equipamento",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.W600,
        color = c.textPrimary,
    )
    Text(
        "O SignallQ lê as informações direto na rede local do seu modem ou roteador. Nada sai de casa.",
        style = MaterialTheme.typography.bodyMedium,
        color = c.textSecondary,
    )
}

@Composable
private fun EnderecoDetectadoCard(
    enderecoDetectado: String?,
    onConectar: () -> Unit,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.surfaceContainer)
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
                    imageVector = if (enderecoDetectado.isNullOrBlank()) Icons.AutoMirrored.Outlined.HelpOutline else Icons.Outlined.Router,
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = LkSpacing.md)) {
            Text(
                text = if (enderecoDetectado.isNullOrBlank()) "Nenhum equipamento detectado automaticamente" else "Endereço detectado nesta rede",
                style = MaterialTheme.typography.titleSmall,
                color = c.textPrimary,
            )
            Text(
                text =
                    if (enderecoDetectado.isNullOrBlank()) {
                        "Informe o endereço, usuário e senha do seu modem ou roteador."
                    } else {
                        "$enderecoDetectado · fabricante ainda não identificado"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(LkSpacing.sm))
            Button(
                onClick = onConectar,
                shape = RoundedCornerShape(LkRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = c.primary),
            ) {
                Text(if (enderecoDetectado.isNullOrBlank()) "Configurar conexão" else "Conectar")
            }
        }
    }
}

@Composable
private fun ModelosCompativeisRow(
    onClick: () -> Unit,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.surfaceContainer)
                .clickable(onClick = onClick)
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Outlined.Router, contentDescription = null, tint = c.secondary)
        Column(modifier = Modifier.weight(1f).padding(start = LkSpacing.md)) {
            Text("Veja se o seu aparelho está na lista", style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
            Text("Modelos já testados pelo SignallQ", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
        Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, tint = c.textSecondary)
    }
}
