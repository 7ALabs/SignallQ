package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.core.diagnostico.DeviceJogo
import io.signallq.app.modogamer.ModoGamerEtapa
import io.signallq.app.modogamer.SelecaoJogoModoGamer
import io.signallq.app.modogamer.icone
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.AcoesRecomendadasCard
import io.signallq.app.ui.component.AiVsMotorExplainer
import io.signallq.app.ui.component.DiagnosticoStatusBanner
import io.signallq.app.ui.component.LkSectionOverline

/** Etapa 3/3 — "Como quer usar essa combinação?" (protótipo #1474 `ModoGamerConfig`). */
@Composable
internal fun ModoGamerConfigConteudo(
    modifier: Modifier = Modifier,
    selecaoJogo: SelecaoJogoModoGamer,
    device: DeviceJogo,
    onConfirmar: (salvarComoPadrao: Boolean) -> Unit,
) {
    val c = LocalLkTokens.current
    var salvarComoPadrao by remember { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg)) {
        ModoGamerListItem(titulo = selecaoJogo.nomeExibido, subtitulo = device.label, icone = selecaoJogo.categoria.icone(), onClick = {})
        Spacer(Modifier.height(LkSpacing.lg))
        LkSectionOverline(text = "Como quer usar essa combinação?")
        Spacer(Modifier.height(LkSpacing.sm))
        ModoGamerOpcaoConfig(
            titulo = "Salvar como padrão",
            descricao = "Próxima vez, o Modo gamer já abre direto com ${selecaoJogo.nomeExibido} + ${device.label}",
            selecionada = salvarComoPadrao,
            onClick = { salvarComoPadrao = true },
        )
        Spacer(Modifier.height(LkSpacing.sm))
        ModoGamerOpcaoConfig(
            titulo = "Usar só desta vez",
            descricao = "Não muda o padrão do Modo gamer",
            selecionada = !salvarComoPadrao,
            onClick = { salvarComoPadrao = false },
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { onConfirmar(salvarComoPadrao) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
            colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
        ) {
            Text(text = "Ver diagnóstico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ModoGamerOpcaoConfig(
    titulo: String,
    descricao: String,
    selecionada: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalLkTokens.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.input))
                .background(if (selecionada) c.primary.copy(alpha = 0.08f) else c.bgPrimary)
                .clickable(onClick = onClick)
                .padding(LkSpacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = if (selecionada) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selecionada) c.primary else c.textTertiary,
        )
        Spacer(Modifier.width(LkSpacing.sm))
        Column {
            Text(text = titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = c.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(text = descricao, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
    }
}

/**
 * Resultado do Modo gamer — reaproveita [DiagnosticoStatusBanner]/[AiVsMotorExplainer]/
 * [AcoesRecomendadasCard] (mesmos componentes de [DiagnosticoGuiadoScreen], issue #1476
 * critério "reaproveita motor de decisão de #1475, sem lógica duplicada").
 */
@Composable
internal fun ModoGamerResultadoConteudo(
    modifier: Modifier = Modifier,
    etapa: ModoGamerEtapa.Resultado,
    analisadorState: AnalisadorState,
    onTrocarJogoOuDevice: () -> Unit,
    onIrParaHome: () -> Unit,
) {
    val c = LocalLkTokens.current
    val resultado = etapa.resultado
    val fallback = etapa.selecaoJogo is SelecaoJogoModoGamer.ForaDoCatalogo

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(c.bgPrimary)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(c.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Outlined.SportsEsports, contentDescription = null, tint = c.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(LkSpacing.sm))
            Column {
                Text(text = etapa.selecaoJogo.nomeExibido, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W600, color = c.textPrimary)
                Text(text = etapa.device.label, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        }

        if (fallback) {
            Spacer(Modifier.height(LkSpacing.sm))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.input))
                        .background(c.warningContainer)
                        .padding(LkSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Outlined.Info, contentDescription = null, tint = c.onWarningContainer, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(LkSpacing.sm))
                Text(
                    text = "Este jogo não está no catálogo — usando o perfil de referência de ${etapa.selecaoJogo.categoria.label.lowercase()}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onWarningContainer,
                )
            }
        }

        Spacer(Modifier.height(LkSpacing.lg))
        DiagnosticoStatusBanner(status = resultado.status, mensagem = resultado.mensagemMotor, c = c)

        Spacer(Modifier.height(LkSpacing.lg))
        AiVsMotorExplainer(evidencias = resultado.evidencias, analisadorState = analisadorState, c = c)

        if (resultado.acoes.isNotEmpty()) {
            Spacer(Modifier.height(LkSpacing.lg))
            LkSectionOverline(text = "Ações recomendadas")
            Spacer(Modifier.height(LkSpacing.sm))
            AcoesRecomendadasCard(acoes = resultado.acoes, c = c)
        }

        Spacer(Modifier.height(LkSpacing.lg))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.input))
                    .background(c.bgSecondary)
                    .padding(LkSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (etapa.salvoComoPadrao) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(LkSpacing.sm))
            Text(
                text =
                    if (etapa.salvoComoPadrao) {
                        "Salvo como padrão do Modo gamer (${etapa.selecaoJogo.nomeExibido} + ${etapa.device.label})"
                    } else {
                        "Usado só desta vez — o padrão do Modo gamer não mudou"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }

        Spacer(Modifier.height(LkSpacing.lg))
        OutlinedButton(
            onClick = onTrocarJogoOuDevice,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
        ) {
            Text(text = "Trocar jogo ou aparelho", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        }
        Spacer(Modifier.height(LkSpacing.sm))
        TextButton(onClick = onIrParaHome, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Ir para o início", style = MaterialTheme.typography.bodyMedium, color = c.primary)
        }
        Spacer(Modifier.height(LkSpacing.xl))
    }
}
