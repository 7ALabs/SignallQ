package io.signallq.app.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.signallq.app.ui.component.SignallQScreenState
import io.signallq.app.ui.component.SignallQStatefulScreen

/** Estado transitório da chamada direta do SignallQ Assist ao NDS. */
@Composable
internal fun DiagnosticoGuiadoProcessandoSection(
    estado: SignallQScreenState<Unit>,
    modifier: Modifier = Modifier,
    onTentarNovamente: () -> Unit,
) {
    SignallQStatefulScreen(
        state = estado,
        modifier = modifier,
        onAction = onTentarNovamente,
        actionLabel = "Tentar novamente",
    ) { }
}
