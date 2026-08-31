package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.signallq.app.core.diagnostico.PerguntaFechada
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens

@Composable
internal fun DiagnosticoGuiadoPerguntaFechadaSection(
    modifier: Modifier = Modifier,
    pergunta: PerguntaFechada,
    passo: Int,
    total: Int,
    respostaSelecionada: Int?,
    onEscolher: (Int) -> Unit,
    /**
     * Pular esta pergunta sem escolher opção nenhuma — o motor continua funcionando com
     * respostas parciais/nenhuma (mesmo comportamento de antes desta pergunta existir, ver
     * `DiagnosticoGuiadoEngine`: `respostas.getOrNull(0)` já tolerava `null`).
     */
    onPular: () -> Unit,
    c: LkTokens,
) {
    Column(modifier.fillMaxSize().background(c.bgPrimary)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = LkSpacing.xl, vertical = LkSpacing.sm)) {
            repeat(total) { i ->
                Spacer(Modifier.weight(1f).height(4.dp).background(if (i <= passo) c.primary else c.bgSecondary, RoundedCornerShape(2.dp)))
                if (i < total - 1) Spacer(Modifier.width(6.dp))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg)) {
            Text(pergunta.texto, style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
            Spacer(Modifier.height(LkSpacing.lg))
            pergunta.opcoes.forEachIndexed { index, opcao ->
                val selecionada = respostaSelecionada == index
                Row(Modifier.fillMaxWidth().clickable { onEscolher(index) }.padding(vertical = LkSpacing.md)) {
                    Column(
                        Modifier
                            .width(20.dp)
                            .height(20.dp)
                            .border(1.5.dp, if (selecionada) c.primary else c.border, CircleShape)
                            .background(if (selecionada) c.primary else Color.Transparent, CircleShape),
                    ) {
                        if (selecionada) Icon(Icons.Outlined.Check, contentDescription = null, tint = c.onPrimary, modifier = Modifier.padding(3.dp))
                    }
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(opcao, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
                }
            }
            Spacer(Modifier.height(LkSpacing.lg))
            Text(
                "Pular esta pergunta",
                style = MaterialTheme.typography.titleSmall,
                color = c.textSecondary,
                modifier = Modifier.fillMaxWidth().clickable { onPular() }.padding(vertical = LkSpacing.sm),
            )
        }
    }
}
