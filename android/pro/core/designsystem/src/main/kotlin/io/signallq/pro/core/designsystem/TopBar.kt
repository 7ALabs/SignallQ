package io.signallq.pro.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Estado de leading action do [TopBar] -- volta (pilha de navegacao) ou fecha (modal/sheet). */
enum class TopBarLeading { VOLTAR, FECHAR }

/** Estado de sincronizacao mostrado pelo save-dot ao lado do subtitulo do [TopBar]. */
enum class TopBarSaveState { SALVO, SALVANDO, OFFLINE }

/**
 * Barra superior do Pro -- titulo + subtitulo opcional com indicador de estado de
 * salvamento, leading (voltar/fechar) e acao de texto a direita (#1170 item 1).
 *
 * O titulo usa `titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = Normal)`
 * em vez de `titleLarge` puro -- a escala tipografica do Pro (`SignallQProTypography.kt`)
 * remapeou `titleLarge` para 18sp/600 (uso de secao de tela), diferente do valor 22sp/28sp/400
 * que o CSS real do TopBar pede. `.copy()` preserva a `fontFamily` (Google Sans Flex) herdada
 * do tema em vez de fixar um `TextStyle` solto sem fonte -- `:core:designsystem` nao pode
 * acessar a `FontFamily` privada de `:pro:app` diretamente.
 */
@Composable
fun TopBar(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    leading: TopBarLeading? = TopBarLeading.VOLTAR,
    onLeadingClick: () -> Unit = {},
    saveState: TopBarSaveState? = null,
    acao: String? = null,
    onAcao: () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                IconButton(onClick = onLeadingClick) {
                    Icon(
                        imageVector = if (leading == TopBarLeading.VOLTAR) Icons.AutoMirrored.Outlined.ArrowBack else Icons.Outlined.Close,
                        contentDescription = if (leading == TopBarLeading.VOLTAR) "Voltar" else "Fechar",
                    )
                }
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
            ) {
                Text(
                    text = titulo,
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitulo != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (saveState != null) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(6.dp)
                                        .background(corSaveState(saveState), CircleShape),
                            )
                        }
                        Text(
                            text = subtitulo,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (acao != null) {
                ProButton(texto = acao, onClick = onAcao, variant = ProButtonVariant.TEXTO)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
    }
}

@Composable
private fun corSaveState(state: TopBarSaveState): Color =
    when (state) {
        TopBarSaveState.SALVO -> corStatusSucesso()
        TopBarSaveState.SALVANDO -> MaterialTheme.colorScheme.onSurfaceVariant
        TopBarSaveState.OFFLINE -> corStatusAtencao()
    }
