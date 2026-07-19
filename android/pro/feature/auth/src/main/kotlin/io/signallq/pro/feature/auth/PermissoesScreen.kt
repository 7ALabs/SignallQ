package io.signallq.pro.feature.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.signallq.pro.core.designsystem.ListRow

/**
 * Tela 1.7 -- ListRow (icone+texto+toggle) por permissao, nunca 1 card por item (handoff
 * Fase 2, #1161). Toggle chama a API real de permissao do Android.
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
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.atualizarEstados()
        }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Permissoes") }) },
    ) { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "O SignallQ Pro precisa dessas permissoes para diagnosticar a rede do cliente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            uiState.itens.forEach { item ->
                ListRow(
                    titulo = item.titulo,
                    subtitulo = item.subtitulo,
                    trailing = {
                        Switch(
                            checked = item.concedida,
                            onCheckedChange = { ligar ->
                                if (ligar && !item.concedida) {
                                    val jaNegadaPermanente =
                                        activity != null &&
                                            !activity.shouldShowRequestPermissionRationale(item.manifestPermission) &&
                                            !item.concedida
                                    if (jaNegadaPermanente) {
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
