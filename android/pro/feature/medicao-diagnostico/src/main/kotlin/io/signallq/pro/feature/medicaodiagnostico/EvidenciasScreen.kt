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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import io.signallq.pro.core.designsystem.ProButton
import io.signallq.pro.core.designsystem.ProButtonVariant
import io.signallq.pro.core.designsystem.ProTextField
import io.signallq.pro.core.designsystem.StateCard
import io.signallq.pro.core.designsystem.StateCardVariant
import io.signallq.pro.core.designsystem.TopBar

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
        topBar = { TopBar(titulo = "Evidências", leading = null) },
    ) { paddingValues ->
        Column(
            modifier = modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProButton(
                    texto = "Tirar foto",
                    onClick = { launcherCamera.launch(null) },
                    variant = ProButtonVariant.SECUNDARIO,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ProTextField(
                    valor = nota,
                    onValorChange = { nota = it },
                    rotulo = "Nota",
                    modifier = Modifier.wrapContentWidth().padding(bottom = 4.dp),
                )
                ProButton(
                    texto = "Adicionar",
                    onClick = {
                        viewModel.salvarNota(nota)
                        nota = ""
                    },
                )
            }

            if (uiState.itens.isEmpty()) {
                StateCard(
                    variant = StateCardVariant.VAZIO,
                    titulo = "Nenhuma evidência ainda",
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

            ProButton(texto = "Concluir visita", onClick = onContinuar, modifier = Modifier.fillMaxWidth())
        }
    }
}
