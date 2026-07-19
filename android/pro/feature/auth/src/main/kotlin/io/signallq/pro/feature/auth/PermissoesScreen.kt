package io.signallq.pro.feature.auth

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.signallq.pro.core.designsystem.ListRow

/**
 * Tela 1.7 -- ListRow (ícone+texto+toggle) por permissão, nunca 1 card por item (handoff
 * Fase 2, #1161). Toggle chama a API real de permissão do Android.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissoesScreen(
    onContinuar: () -> Unit,
    onPermissaoBloqueada: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PermissoesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? androidx.activity.ComponentActivity

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultados ->
            // Marca "ja solicitada" so apos a resposta do usuario (nao antes do launch) --
            // e o que permite diferenciar "nunca pedida" de "negada permanentemente" na
            // proxima vez que o switch for ligado (#1179).
            resultados.keys.forEach { permissao -> viewModel.marcarPermissaoSolicitada(permissao) }
            viewModel.atualizarEstados()
        }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Permissões") }) },
    ) { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "O SignallQ Pro precisa dessas permissões para diagnosticar a rede do cliente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            uiState.itens.forEach { item ->
                ListRow(
                    titulo = item.titulo,
                    subtitulo = item.subtitulo,
                    icone = iconePermissao(item.manifestPermission),
                    trailing = {
                        Switch(
                            checked = item.concedida,
                            onCheckedChange = { ligar ->
                                if (ligar && !item.concedida) {
                                    // shouldShowRequestPermissionRationale() sozinho nao distingue
                                    // "nunca pedida" (deve abrir o dialogo do SO) de "negada
                                    // permanentemente" (deve ir direto pro bloqueio) -- os dois
                                    // retornam false. So trata como bloqueio quando ja houve uma
                                    // solicitacao anterior registrada E o SO nao mostra mais o
                                    // rationale (#1179).
                                    val jaSolicitadaAntes = viewModel.permissaoJaSolicitada(item.manifestPermission)
                                    val podeMostrarRationale =
                                        activity?.shouldShowRequestPermissionRationale(item.manifestPermission) ?: true
                                    val bloqueadaPermanentemente = jaSolicitadaAntes && !podeMostrarRationale
                                    if (bloqueadaPermanentemente) {
                                        onPermissaoBloqueada()
                                    } else {
                                        launcher.launch(arrayOf(item.manifestPermission))
                                    }
                                }
                            },
                        )
                    },
                )
            }
            Button(
                onClick = onContinuar,
                enabled = uiState.todasConcedidas,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text("Continuar")
            }
        }
    }
}

private fun iconePermissao(manifestPermission: String): ImageVector =
    when (manifestPermission) {
        Manifest.permission.ACCESS_FINE_LOCATION -> Icons.Outlined.LocationOn
        Manifest.permission.CAMERA -> Icons.Outlined.CameraAlt
        else -> Icons.Outlined.Wifi
    }
