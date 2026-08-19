package io.signallq.app.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.SignalCellularOff
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.signallq.app.core.telephony.MovelSimSnapshot
import io.signallq.app.core.telephony.MovelSnapshot
import io.signallq.app.ui.BancoOperadoras
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.ResolvedOperadoraIdentity
import io.signallq.app.ui.component.LkPillBadge
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.OperadoraBadge
import io.signallq.app.ui.component.rememberResolvedOperadoraIdentity

// ─── Tab Móvel ────────────────────────────────────────────────────────────────

@Composable
internal fun MovelTab(
    movelSnapshot: MovelSnapshot?,
    simsAtivos: List<MovelSimSnapshot>,
    temPermissaoTelefonia: Boolean,
    onSolicitarPermissaoTelefonia: () -> Unit,
    tokens: LkTokens,
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity?,
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity,
) {
    val c = tokens
    if (!temPermissaoTelefonia) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStatePermissaoTelefonia(
                onSolicitarPermissao = onSolicitarPermissaoTelefonia,
                tokens = c,
            )
        }
        return
    }
    if (movelSnapshot == null && simsAtivos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateMobile(c)
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(LkSpacing.lg),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        if (simsAtivos.isNotEmpty()) {
            ChipsAtivosSection(
                simsAtivos = simsAtivos,
                movelSnapshot = movelSnapshot,
                tokens = c,
                resolveOperadoraIdentidadeLocal = resolveOperadoraIdentidadeLocal,
                resolveOperadoraIdentidadeRemota = resolveOperadoraIdentidadeRemota,
            )
        } else if (movelSnapshot != null) {
            MobileSnapshotCard(
                snapshot = movelSnapshot,
                tokens = c,
                resolveOperadoraIdentidadeLocal = resolveOperadoraIdentidadeLocal,
                resolveOperadoraIdentidadeRemota = resolveOperadoraIdentidadeRemota,
            )
        }
    }
}

@Composable
private fun ChipsAtivosSection(
    simsAtivos: List<MovelSimSnapshot>,
    movelSnapshot: MovelSnapshot?,
    tokens: LkTokens,
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity?,
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.lg),
    ) {
        simsAtivos.forEachIndexed { index, sim ->
            SimCard(
                sim = sim,
                summarySnapshot = movelSnapshot,
                cardLabel = "Chip ${index + 1}",
                tokens = tokens,
                resolveOperadoraIdentidadeLocal = resolveOperadoraIdentidadeLocal,
                resolveOperadoraIdentidadeRemota = resolveOperadoraIdentidadeRemota,
            )
        }
    }
}

@Composable
private fun SimCard(
    sim: MovelSimSnapshot,
    summarySnapshot: MovelSnapshot?,
    cardLabel: String,
    tokens: LkTokens,
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity?,
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity,
) {
    // GH#1206 item 1 — summarySnapshot representa o SIM PADRAO de dados (o
    // TelephonyManager que o produz nunca e criado com createForSubscriptionId). So pode
    // complementar dados deste card quando `sim` e de fato o SIM padrao — nunca pra um SIM
    // secundario, senao o Chip 2 pode exibir operadora/tecnologia/RSRP do Chip 1.
    val operadora = sim.operadora ?: summarySnapshot?.operadora?.takeIf { sim.isDefaultData } ?: "Operadora"
    // Contato (site/SAC/WhatsApp) continua so nivel 1 (catalogo local) — fora do escopo desta
    // troca (ver kdoc de ResolvedOperadoraIdentity: nao carrega contato).
    val operadoraLocal = remember(operadora) { BancoOperadoras.resolverMovel(operadora) }
    // Identidade visual (logo) agora usa a cadeia completa local -> diretorio remoto ->
    // fallback (GH#965/#970), igual a HomeScreen/DiagnosticoGuiadoScreen — antes, esta aba
    // usava so o catalogo local e caia direto no placeholder generico pra qualquer operadora
    // fora dele, mesmo quando o diretorio remoto teria o logo.
    val identidadeOperadora =
        rememberResolvedOperadoraIdentity(
            ispNomeBruto = operadora,
            viaMovel = true,
            resolveLocal = resolveOperadoraIdentidadeLocal,
            resolveRemoteOrFallback = resolveOperadoraIdentidadeRemota,
        )
    val dadosSinal = sim.paraDadosSinalMovel(summarySnapshot)
    val resumoRede = buildMobileSummary(dadosSinal)
    val qualidade = classificarQualidadeSinalMovel(dadosSinal, tokens)
    val tipoConexao = classificarTipoConexaoMovel(dadosSinal, tokens)
    val experiencia = classificarExperienciaMovel(dadosSinal, tokens)
    val suporteUrl = operadoraLocal?.site
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        LkSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
                    ) {
                        Text(
                            text = cardLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.W600,
                            color = tokens.textSecondary,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
                    ) {
                        Text(
                            text = operadora,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.W600,
                            color = tokens.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (sim.isDefaultData) {
                            MobileStatusBadge(
                                label = "EM USO",
                                color = tokens.success,
                            )
                        }
                    }
                    Text(
                        text = resumoRede,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary,
                    )
                }
                identidadeOperadora?.let {
                    OperadoraBadge(
                        identidade = it,
                        size = 48.dp,
                    )
                } ?: PlaceholderOperadoraBadge(tokens = tokens)
            }
        }

        MobileDetailCard(
            icon = Icons.Outlined.SignalCellularAlt,
            title = "Qualidade do sinal",
            body = qualidade.description,
            badge = qualidade.label,
            accent = qualidade.color,
            tokens = tokens,
        )
        MobileDetailCard(
            icon = Icons.Outlined.CellTower,
            title = "Tipo de conexão",
            body = tipoConexao.description,
            badge = tipoConexao.label,
            accent = tipoConexao.color,
            tokens = tokens,
        )
        MobileDetailCard(
            icon = Icons.Outlined.CheckCircle,
            title = "Experiência esperada",
            body = experiencia.description,
            badge = experiencia.label,
            accent = experiencia.color,
            tokens = tokens,
        )

        OutlinedButton(
            onClick = {
                suporteUrl?.let { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = suporteUrl != null,
            shape = RoundedCornerShape(LkRadius.button),
        ) {
            Text(
                text = "Falar com a $operadora",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// GH#1258 — MobileInsight/DadosSinalMovel/paraDadosSinalMovel/classificarQualidadeSinalMovel/
// classificarTipoConexaoMovel/classificarExperienciaMovel/piorMetricStatusSinalMovel/
// radioTechDeTecnologia/buildMobileSummary foram extraidos para SinalMovelClassificacao.kt
// (mesmo pacote, sem import necessario) porque a Home passou a consumi-los tambem — ver #1258.

@Composable
private fun MobileDetailCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    badge: String,
    accent: Color,
    tokens: LkTokens,
) {
    LkSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textSecondary,
                )
            }
            MobileStatusBadge(
                label = badge,
                color = accent,
            )
        }
    }
}

@Composable
private fun MobileStatusBadge(
    label: String,
    color: Color,
) {
    LkPillBadge(
        text = label,
        containerColor = color.copy(alpha = 0.12f),
        contentColor = color,
    )
}

@Composable
private fun PlaceholderOperadoraBadge(tokens: LkTokens) {
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(LkRadius.input))
                .background(tokens.surfaceContainerHigh)
                .border(1.dp, tokens.outlineVariant, RoundedCornerShape(LkRadius.input)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "logo",
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textTertiary,
        )
    }
}

@Composable
private fun MobileSnapshotCard(
    snapshot: MovelSnapshot,
    tokens: LkTokens,
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity?,
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity,
) {
    val operadora = snapshot.operadora ?: "Operadora"
    // Mesma cadeia local -> diretorio remoto -> fallback (GH#965/#970) usada em SimCard —
    // antes desta correcao, este card (usado quando nao ha SIM ativo detectado, so
    // movelSnapshot) nunca tentava nem o catalogo local, sempre caindo no placeholder
    // estatico incondicionalmente (achado na revisao do Caio).
    val identidadeOperadora =
        rememberResolvedOperadoraIdentity(
            ispNomeBruto = snapshot.operadora,
            viaMovel = true,
            resolveLocal = resolveOperadoraIdentidadeLocal,
            resolveRemoteOrFallback = resolveOperadoraIdentidadeRemota,
        )
    val dadosSinal = snapshot.paraDadosSinalMovel()
    val qualidade = classificarQualidadeSinalMovel(dadosSinal, tokens)
    val tipoConexao = classificarTipoConexaoMovel(dadosSinal, tokens)
    val experiencia = classificarExperienciaMovel(dadosSinal, tokens)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        LkSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
                ) {
                    Text(
                        text = "Chip 1",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.W600,
                        color = tokens.textSecondary,
                    )
                    Text(
                        text = operadora,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.W600,
                        color = tokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildMobileSummary(dadosSinal),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary,
                    )
                }
                identidadeOperadora?.let {
                    OperadoraBadge(
                        identidade = it,
                        size = 48.dp,
                    )
                } ?: PlaceholderOperadoraBadge(tokens = tokens)
            }
        }
        MobileDetailCard(
            icon = Icons.Outlined.SignalCellularAlt,
            title = "Qualidade do sinal",
            body = qualidade.description,
            badge = qualidade.label,
            accent = qualidade.color,
            tokens = tokens,
        )
        MobileDetailCard(
            icon = Icons.Outlined.CellTower,
            title = "Tipo de conexão",
            body = tipoConexao.description,
            badge = tipoConexao.label,
            accent = tipoConexao.color,
            tokens = tokens,
        )
        MobileDetailCard(
            icon = Icons.Outlined.CheckCircle,
            title = "Experiência esperada",
            body = experiencia.description,
            badge = experiencia.label,
            accent = experiencia.color,
            tokens = tokens,
        )
    }
}

@Composable
private fun EmptyStatePermissaoTelefonia(
    onSolicitarPermissao: () -> Unit,
    tokens: LkTokens,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(tokens.warning.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.SignalCellularAlt,
                contentDescription = null,
                tint = tokens.warning,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            "Permissão necessária",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.W600,
            color = tokens.textPrimary,
        )
        Text(
            "Seu aparelho está sem permissão para ler\nas informações de rede móvel.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(LkSpacing.xs))
        OutlinedButton(onClick = onSolicitarPermissao) {
            Text(
                "Permitir leitura do chip",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
            )
        }
    }
}

@Composable
private fun EmptyStateMobile(tokens: LkTokens) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(tokens.warning.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.SignalCellularOff,
                contentDescription = null,
                tint = tokens.warning,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            "Sem chip detectado",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.W600,
            color = tokens.textPrimary,
        )
        Text(
            "Seu aparelho está sem chip de celular ou sem\npermissão para ler as informações de rede móvel.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
