package io.signallq.pro.feature.medicaodiagnostico

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.signallq.pro.core.database.evidencia.TipoEvidencia
import io.signallq.pro.core.designsystem.EvidenceChip
import io.signallq.pro.core.designsystem.EvidenceType
import io.signallq.pro.core.designsystem.StateCard
import io.signallq.pro.core.designsystem.StateCardVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenciasScreen(
    onContinuar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EvidenciasViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var nota by remember { mutableStateOf("") }

    val launcherCamera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let(viewModel::salvarFoto)
        }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Evidencias") }) },
    ) { paddingValues ->
        Column(
            modifier = modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { launcherCamera.launch(null) }) {
                    Text("Tirar foto")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Nota") },
                    modifier = Modifier.wrapContentWidth().padding(bottom = 4.dp),
                )
                Button(onClick = {
                    viewModel.salvarNota(nota)
                    nota = ""
                }) {
                    Text("Adicionar")
                }
            }

            if (uiState.itens.isEmpty()) {
                StateCard(
                    variant = StateCardVariant.VAZIO,
                    titulo = "Nenhuma evidencia ainda",
                    mensagem = "Tire uma foto ou registre uma nota sobre este ambiente.",
                )
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                    modifier = Modifier.weight(1f),
                ) {
                    items(uiState.itens, key = { it.id }) { evidencia ->
                        EvidenceChip(
                            tipo = if (evidencia.tipo == TipoEvidencia.FOTO) EvidenceType.FOTO else EvidenceType.NOTA,
                            texto = evidencia.nota ?: "Foto registrada",
                        )
                    }
                }
            }

            Button(onClick = onContinuar, modifier = Modifier.fillMaxWidth()) {
                Text("Concluir visita")
            }
        }
    }
}
