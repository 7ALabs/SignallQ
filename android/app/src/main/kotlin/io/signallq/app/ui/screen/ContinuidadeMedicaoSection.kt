package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.component.corContainer
import io.signallq.app.ui.component.corConteudo
import io.signallq.app.ui.component.icone

/**
 * Cores da continuidade, resolvidas pelo mapeamento canônico.
 *
 * Existe como função separada por causa do bloqueio B4 de Caio na PR #1723: fixar `corContainer`
 * em `warningContainer` para todos os status sobrevivia à suíte inteira — e esse mutante É o
 * defeito que a #1705 dizia estar removendo. Remover um defeito sem deixar nada impedindo alguém
 * de recolocá-lo não é remover.
 */
@Stable
internal data class CoresDaContinuidade(
    val container: Color,
    val conteudo: Color,
)

internal fun coresDaContinuidade(
    continuidade: ContinuidadeMedicao,
    c: LkTokens,
): CoresDaContinuidade =
    CoresDaContinuidade(
        container = continuidade.statusVisual.corContainer(c),
        conteudo = continuidade.statusVisual.corConteudo(c),
    )

/**
 * A continuidade desenhada — issue #1705 (2.0.09c). Substitui `ResultadoInvalidoBannerGuiado`.
 *
 * O que muda em relação ao banner que existia: ele tratava os quatro status não-completos com um
 * texto único ("Este resultado não é confiável o suficiente...") e **nenhum botão**. Aqui cada
 * caso tem título próprio, explicação própria e uma ação concreta — que é o que a spec §9 pede e o
 * critério §13 cobra.
 *
 * Cor e ícone vêm de `DiagnosticStatusUi.kt`, o mapeamento canônico único (GH#1228 P0-8). Nenhuma
 * cor local: o banner anterior fixava `warningContainer` para tudo, inclusive para inconclusivo —
 * que é exatamente a confusão entre "problema detectado" e "não medi" que aquele arquivo resolveu.
 */
@Composable
internal fun ContinuidadeMedicaoSection(
    continuidade: ContinuidadeMedicao,
    onAgir: () -> Unit,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    val cores = coresDaContinuidade(continuidade, c)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(cores.container)
                .padding(LkSpacing.lg)
                .testTag(TAG_CONTINUIDADE_MEDICAO),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = continuidade.statusVisual.icone(),
                contentDescription = null,
                tint = cores.conteudo,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = continuidade.titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = cores.conteudo,
            )
        }
        Text(
            text = continuidade.explicacao,
            style = MaterialTheme.typography.bodyMedium,
            color = cores.conteudo,
        )
        Button(
            onClick = onAgir,
            shape = RoundedCornerShape(LkRadius.button),
            colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
            modifier = Modifier.padding(top = LkSpacing.sm).testTag(TAG_CONTINUIDADE_MEDICAO_ACAO),
        ) {
            Text(continuidade.rotuloAcao)
        }
    }
}

internal const val TAG_CONTINUIDADE_MEDICAO = "diagnostico_guiado_continuidade_medicao"
internal const val TAG_CONTINUIDADE_MEDICAO_ACAO = "diagnostico_guiado_continuidade_medicao_acao"
