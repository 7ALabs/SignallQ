package io.signallq.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkSpacing

/**
 * GH#409: dois botoes lado a lado quebram (texto cortado/sobreposto) quando o
 * usuario aumenta a fonte do sistema. Empilha em coluna quando fontScale >= 1.3f
 * ou quando a largura disponivel e insuficiente para os dois botoes lado a lado.
 *
 * [secondary] e [primary] recebem o [Modifier] de layout ja resolvido (weight em
 * linha, fillMaxWidth em coluna) — aplique-o ao botao real (Button/TextButton/etc).
 */
@Composable
fun ResponsiveActionsRow(
    modifier: Modifier = Modifier,
    secondary: @Composable (Modifier) -> Unit,
    primary: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val useColumn = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.3f

        if (useColumn) {
            Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
                primary(Modifier.fillMaxWidth())
                secondary(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
            ) {
                secondary(Modifier.weight(1f))
                primary(Modifier.weight(1f))
            }
        }
    }
}
