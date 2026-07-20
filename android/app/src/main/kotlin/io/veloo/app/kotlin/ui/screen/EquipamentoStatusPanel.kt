package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.core.network.contracts.fibra.GponSaudeStatus
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkSurfaceCard

/**
 * Painel de status da tela "Equipamento de internet" — passos 3, 4, 5, 6 e 7
 * da narrativa (bug #6, spec Lia): veredito geral, disponibilidade (Fibra |
 * Wi-Fi), uso (Clientes | Acesso), alerta acionável e aviso de leitura
 * parcial. Extraído de `EquipamentoInternetScreen.kt` (dívida crítica, ver
 * `.claude/rules/higiene-e-padronizacao-repositorio.md` seção 4.6).
 *
 * A causa raiz do redesign (Luiz/Lia, 2026-07-18) não era largura desigual
 * de card — era o grid 2x2 de mini-stats dentro do card de status disputando
 * hierarquia com o veredito, mais a pill solta de saúde óptica fora do
 * sistema de card. Aqui o status volta a ser só título+ícone+descrição, a
 * saúde óptica vira uma linha absorvida dentro do próprio card, e as
 * mini-stats saem para dois pares de cards full-width dedicados.
 */
@Composable
internal fun StatusEquipamentoCard(
    titulo: String,
    descricao: String,
    cor: Color,
    gponSaude: GponSaudeStatus?,
    c: LkTokens,
) {
    LkSurfaceCardComCor(cor = cor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector =
                    when (cor) {
                        c.success -> Icons.Outlined.CheckCircle
                        c.error -> Icons.Outlined.ErrorOutline
                        else -> Icons.Outlined.WarningAmber
                    },
                contentDescription = null,
                tint = cor,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(LkSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
                Text(
                    text = descricao,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
            }
        }
        gponSaude?.let { status ->
            Spacer(Modifier.height(LkSpacing.sm))
            val (label, corSaude) = saudeOpticaTextoECor(status, c)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(corSaude))
                Spacer(Modifier.width(LkSpacing.xs))
                Text(
                    text = "Sinal óptico: $label",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.W600,
                    color = corSaude,
                )
            }
        }
    }
}

/** Card com fundo/borda tintados pela cor do veredito (10%/30% alpha) —
 *  mesmo padrão visual do antigo `StatusEquipamentoCard`, isolado aqui para
 *  não duplicar o `Column` base entre este card e os futuros usos. */
@Composable
private fun LkSurfaceCardComCor(
    cor: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    LkSurfaceCard(
        modifier = Modifier.background(cor.copy(alpha = 0.10f), RoundedCornerShape(LkRadius.card)),
        content = content,
    )
}

private fun saudeOpticaTextoECor(
    status: GponSaudeStatus,
    c: LkTokens,
): Pair<String, Color> =
    when (status) {
        GponSaudeStatus.boa -> "Bom" to c.success
        GponSaudeStatus.regular -> "Regular" to c.warning
        GponSaudeStatus.ruim -> "Ruim" to c.error
    }

/** Card compacto de um único par label/valor — usado pelos pares
 *  Disponibilidade (Fibra | Wi-Fi) e Uso (Clientes | Acesso), sempre em
 *  `Row` com `Modifier.weight(1f)` nos dois lados (2-col simétrico, mesma
 *  origem/estrutura de dado nos dois cards — regra da spec). */
@Composable
private fun EquipamentoParInfoCard(
    label: String,
    valor: String,
    modifier: Modifier = Modifier,
    c: LkTokens,
) {
    LkSurfaceCard(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
        Spacer(Modifier.height(2.dp))
        Text(
            valor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W600,
            color = c.textPrimary,
        )
    }
}

@Composable
internal fun DisponibilidadeCardsRow(
    suportaFibra: Boolean,
    suportaWifi: Boolean,
    c: LkTokens,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.md)) {
        EquipamentoParInfoCard(
            label = "Fibra",
            valor = if (suportaFibra) "Disponível" else "Não se aplica",
            modifier = Modifier.weight(1f),
            c = c,
        )
        EquipamentoParInfoCard(
            label = "Wi-Fi",
            valor = if (suportaWifi) "Disponível" else "Não se aplica",
            modifier = Modifier.weight(1f),
            c = c,
        )
    }
}

@Composable
internal fun UsoCardsRow(
    totalClientes: Int,
    acessoLabel: String,
    c: LkTokens,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.md)) {
        EquipamentoParInfoCard(
            label = "Clientes",
            valor = totalClientes.toString(),
            modifier = Modifier.weight(1f),
            c = c,
        )
        EquipamentoParInfoCard(
            label = "Acesso",
            valor = acessoLabel,
            modifier = Modifier.weight(1f),
            c = c,
        )
    }
}

/** Alerta acionável (fundo warning 10% / borda warning 30% / botão tonal) — ver protótipo
 *  TO-BE `tobe/screens/EquipamentoInternet.jsx`, função `AlertCard` (linhas ~145-154). Botão sem
 *  `onClick` real de propósito: nenhuma das ações candidatas ("Executar diagnóstico") está
 *  ligada a um fluxo de navegação real ainda (ver GH#1031). */
@Composable
internal fun AlertaCard(
    alerta: EquipmentAlertUi,
    onAcionar: () -> Unit,
) {
    val c = LocalLkTokens.current
    val cor = c.warning
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(cor.copy(alpha = 0.10f))
                .padding(LkSpacing.base),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(LkSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(alerta.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = c.textPrimary)
                Spacer(Modifier.height(2.dp))
                Text(alerta.descricao, fontSize = 12.sp, color = c.textSecondary, lineHeight = 17.sp)
            }
        }
        FilledTonalButton(onClick = onAcionar) {
            Text(alerta.botaoLabel)
        }
    }
}

@Composable
internal fun AvisoAcessoCard(
    icone: ImageVector,
    cor: Color,
    texto: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(cor.copy(alpha = 0.10f))
                .padding(LkSpacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(LkSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text("Atenção", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = cor)
            Spacer(Modifier.height(2.dp))
            Text(texto, fontSize = 12.sp, color = LocalLkTokens.current.textSecondary, lineHeight = 17.sp)
        }
    }
}
