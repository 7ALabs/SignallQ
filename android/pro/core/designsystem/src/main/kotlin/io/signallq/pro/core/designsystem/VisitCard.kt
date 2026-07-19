package io.signallq.pro.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Card de visita do Pro (#1170 item 1) -- lista de visitas do Painel/Atendimento.
 *
 * O JSX de origem especula 5 status (`rascunho`/`em_andamento`/`aguardando_validacao`/
 * `concluida`/`cancelada`) que NAO existem no dominio real -- `StatusVisita`
 * (`:pro:core:database`) so tem `EM_ANDAMENTO`/`CONCLUIDA`/`INTERROMPIDA`, e
 * `:pro:core:designsystem` nao depende de `:pro:core:database` (cada `:pro:feature:*`
 * depende dos dois separadamente, ver `build.gradle.kts`). Por isso este componente recebe
 * [statusLabel] + [statusTone] ja prontos -- quem chama (feature com acesso ao banco) faz o
 * mapeamento de `StatusVisita` real para rotulo/tom, este componente so exibe.
 */
@Composable
fun VisitCard(
    nomeCliente: String,
    nomeLocal: String,
    objetivo: String,
    statusLabel: String,
    statusTone: StatusChipTone,
    horario: String,
    onContinuar: () -> Unit,
    modifier: Modifier = Modifier,
    labelContinuar: String = "Continuar",
) {
    Surface(
        modifier = modifier.widthIn(max = 380.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nomeCliente,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = nomeLocal,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(texto = statusLabel, tone = statusTone)
            }
            Text(
                text = objetivo,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = horario,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ProButton(texto = labelContinuar, onClick = onContinuar, variant = ProButtonVariant.TEXTO)
            }
        }
    }
}
