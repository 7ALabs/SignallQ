package io.signallq.app.ui.screen

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.signallq.app.core.telephony.MovelSimSnapshot
import io.signallq.app.core.telephony.MovelSnapshot
import io.signallq.app.ui.BancoOperadoras
import io.signallq.app.ui.ContatoOperadora
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
    onAbrirContatoOperadora: (ContatoOperadora?, String?) -> Unit,
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity?,
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity,
) {
    val c = tokens
    // GH#1662 — decisão de produto (Luiz, 2026-08-19): sem READ_PHONE_STATE a aba continua útil
    // de forma reduzida (mostra o que der pra saber e explica o que falta), em vez de bloquear a
    // tela inteira esperando o aceite. Só cai no empty state de permissão quando não há
    // NENHUM dado — nem o snapshot reduzido (sem SIM, emulador, operadora não reportada).
    val temAlgumDado = movelSnapshot != null || simsAtivos.isNotEmpty()
    if (!temAlgumDado) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (temPermissaoTelefonia) {
                EmptyStateMobile(c)
            } else {
                EmptyStatePermissaoTelefonia(
                    onSolicitarPermissao = onSolicitarPermissaoTelefonia,
                    tokens = c,
                )
            }
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(LkSpacing.lg),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        if (!temPermissaoTelefonia) {
            PermissaoReduzidaBanner(
                onSolicitarPermissao = onSolicitarPermissaoTelefonia,
                tokens = c,
            )
        }
        if (simsAtivos.isNotEmpty()) {
            ChipsAtivosSection(
                simsAtivos = simsAtivos,
                movelSnapshot = movelSnapshot,
                tokens = c,
                onAbrirContatoOperadora = onAbrirContatoOperadora,
                resolveOperadoraIdentidadeLocal = resolveOperadoraIdentidadeLocal,
                resolveOperadoraIdentidadeRemota = resolveOperadoraIdentidadeRemota,
            )
        } else if (movelSnapshot != null) {
            MobileSnapshotCard(
                snapshot = movelSnapshot,
                tokens = c,
                onAbrirContatoOperadora = onAbrirContatoOperadora,
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
    onAbrirContatoOperadora: (ContatoOperadora?, String?) -> Unit,
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
                onAbrirContatoOperadora = onAbrirContatoOperadora,
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
    onAbrirContatoOperadora: (ContatoOperadora?, String?) -> Unit,
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity?,
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity,
) {
    // GH#1206 item 1 — summarySnapshot representa o SIM PADRAO de dados (o
    // TelephonyManager que o produz nunca e criado com createForSubscriptionId). So pode
    // complementar dados deste card quando `sim` e de fato o SIM padrao — nunca pra um SIM
    // secundario, senao o Chip 2 pode exibir operadora/tecnologia/RSRP do Chip 1.
    val operadoraIdentificada = sim.operadora ?: summarySnapshot?.operadora?.takeIf { sim.isDefaultData }
    val operadora = operadoraIdentificada ?: "Operadora"
    // Contato (site/SAC/WhatsApp) continua so nivel 1 (catalogo local) — fora do escopo desta
    // troca (ver kdoc de ResolvedOperadoraIdentity: nao carrega contato).
    val operadoraLocal = remember(operadora) { BancoOperadoras.resolverMovel(operadora) }
    // Identidade visual (logo) agora usa a cadeia completa local -> diretorio remoto ->
    // fallback (GH#965/#970), igual a Inicio2Screen/DiagnosticoGuiadoScreen — antes, esta aba
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
    // GH#1662 — cabeçalho não expõe mais RSRP em dBm direto (conclusão precede siglas, spec
    // design 2.0 §4.3/4.4); o valor bruto passa a viver em MobileDetalhesTecnicosCard, depois
    // dos cards de conclusão. buildMobileSummary (com o RSRP) continua existindo — é
    // compartilhado com Inicio2Screen.kt (GH#1258) e não muda aqui.
    val resumoRede = resumoCabecalhoMovel(dadosSinal, capturaReduzida = false)
    val qualidade = classificarQualidadeSinalMovel(dadosSinal, tokens)
    val tipoConexao = classificarTipoConexaoMovel(dadosSinal, tokens)
    val experiencia = classificarExperienciaMovel(dadosSinal, tokens)
    // GH#1662 — decisão de produto (Luiz, 2026-08-19): operadora não identificada não esconde
    // o botão, cai num fallback genérico (busca) em vez de ficar desabilitado.

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            identidadeOperadora?.let {
                OperadoraBadge(identidade = it, size = 48.dp)
            } ?: PlaceholderOperadoraBadge(tokens = tokens)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cardLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.W600,
                    color = tokens.textPrimary,
                )
                Text(
                    text = "$operadora · $resumoRede",
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.textSecondary,
                )
            }
        }

        MobileSignalHero(
            qualidade = qualidade,
            experiencia = experiencia,
            tokens = tokens,
        )

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
        MobileDetalhesTecnicosCard(dadosSinal, tokens)

        OutlinedButton(
            onClick = { onAbrirContatoOperadora(operadoraLocal, operadoraIdentificada) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
        ) {
            Text(
                text = rotuloBotaoContatoOperadora(operadoraIdentificada),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MobileSignalHero(
    qualidade: MobileInsight,
    experiencia: MobileInsight,
    tokens: LkTokens,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
    ) {
        Box(
            modifier =
                Modifier
                    .size(LkSpacing.xxxl + LkSpacing.xxxl)
                    .clip(CircleShape)
                    .background(qualidade.color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.SignalCellularAlt,
                contentDescription = null,
                tint = qualidade.color,
                modifier = Modifier.size(LkSpacing.xl),
            )
        }
        Text(
            text = destaqueSinalMovel(qualidade.label),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.W600,
            color = qualidade.color,
        )
        Text(
            text = experiencia.description,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

internal fun destaqueSinalMovel(qualidade: String): String =
    when (qualidade) {
        "Excelente", "Bom" -> "Forte"
        "Regular" -> "Regular"
        "Ruim" -> "Fraco"
        else -> qualidade
    }

// GH#1258 — MobileInsight/DadosSinalMovel/paraDadosSinalMovel/classificarQualidadeSinalMovel/
// classificarTipoConexaoMovel/classificarExperienciaMovel/piorMetricStatusSinalMovel/
// radioTechDeTecnologia/buildMobileSummary foram extraidos para SinalMovelClassificacao.kt
// (mesmo pacote, sem import necessario) porque a Home passou a consumi-los tambem — ver #1258.

/**
 * GH#1662 — subtítulo do cabeçalho do card (SimCard/MobileSnapshotCard). Antes usava
 * [buildMobileSummary], que mostra "RSRP -85 dBm · 4G" — sigla técnica ANTES da conclusão
 * (cards "Qualidade do sinal"/"Tipo de conexão" abaixo). A spec de design 2.0 (§4.3) diz que
 * RSRP/RSRQ/SINR só devem aparecer "nos detalhes", depois da conclusão — por isso o valor bruto
 * agora só existe em [MobileDetalhesTecnicosCard]. Esta função fica local (não em
 * SinalMovelClassificacao.kt) porque só serve a esta tela — buildMobileSummary continua igual e
 * é o que Inicio2Screen.kt consome (GH#1258), não mudou aqui.
 */
internal fun resumoCabecalhoMovel(
    dados: DadosSinalMovel,
    capturaReduzida: Boolean,
): String =
    when {
        dados.radioDesligado -> "Modo avião ativo · rádio celular desligado"
        capturaReduzida -> "Detalhes completos exigem permissão de telefone"
        else -> dados.tecnologia ?: "Rede móvel"
    }

/**
 * GH#1662 — decisão de produto (Luiz, 2026-08-19): operadora não identificada no catálogo local
 * (ou não reportada pelo Android) não esconde nem desabilita o botão de contato — cai num
 * fallback genérico de busca, sem link específico de nenhuma operadora.
 */
internal fun contatoOperadoraUrl(
    operadoraLocal: ContatoOperadora?,
    operadoraIdentificada: String?,
): String {
    operadoraLocal?.let { return it.site }
    val consulta =
        operadoraIdentificada?.let { "central de atendimento $it" }
            ?: "central de atendimento operadora de celular"
    // java.net.URLEncoder (JVM puro) em vez de android.net.Uri.encode: mesma funcao e
    // testavel em unit test JVM sem Robolectric (Uri.* nao e mockado por padrao neste modulo).
    val consultaCodificada = java.net.URLEncoder.encode(consulta, "UTF-8")
    return "https://www.google.com/search?q=$consultaCodificada"
}

/** GH#1662 — rótulo do botão de contato: nomeia a operadora só quando o Android de fato
 *  identificou uma (mesmo que fora do catálogo local); senão fica genérico. */
internal fun rotuloBotaoContatoOperadora(operadoraIdentificada: String?): String =
    operadoraIdentificada?.let { "Falar com a $it" } ?: "Falar com sua operadora"

/**
 * GH#1662 — "detalhes técnicos" exigidos pela spec de design 2.0 (§4.4: "Detalhes técnicos
 * podem mostrar RSSI, banda, canal e evidências após a conclusão"). Mostra RSRP/RSRQ/SINR em
 * dBm/dB só depois dos três cards de conclusão (Qualidade/Tipo de conexão/Experiência). Não
 * renderiza nada quando não há nenhuma métrica bruta disponível (rádio desligado, captura
 * reduzida sem permissão, ou OEM que não reporta).
 */
@Composable
private fun MobileDetalhesTecnicosCard(
    dados: DadosSinalMovel,
    tokens: LkTokens,
) {
    val linhas =
        buildList {
            dados.tecnologia?.let { add("Tecnologia" to it) }
            dados.rsrpDbm?.let { add("RSRP" to "$it dBm") }
            dados.rsrqDb?.let { add("RSRQ" to "$it dB") }
            dados.sinrDb?.let { add("SINR" to "$it dB") }
        }
    if (linhas.isEmpty()) return
    LkSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.xs)) {
            Text(
                text = "Detalhes técnicos",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = tokens.textPrimary,
            )
            linhas.forEach { (rotulo, valor) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = rotulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary,
                    )
                    Text(
                        text = valor,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.W600,
                        color = tokens.textPrimary,
                    )
                }
            }
        }
    }
}

/**
 * GH#1662 — banner exibido no topo da aba Móvel quando não há READ_PHONE_STATE mas ainda assim
 * há algo pra mostrar (snapshot reduzido). Substitui o bloqueio total anterior: explica o que
 * falta e por quê, sem impedir o resto do conteúdo de renderizar (decisão de produto, Luiz
 * 2026-08-19).
 */
@Composable
private fun PermissaoReduzidaBanner(
    onSolicitarPermissao: () -> Unit,
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
                        .background(tokens.warning.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.SimCard,
                    contentDescription = null,
                    tint = tokens.warning,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                Text(
                    text = "Mostrando o que dá para saber sem a permissão de telefone",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = tokens.textPrimary,
                )
                Text(
                    text =
                        "Qualidade do sinal e tecnologia (4G/5G) exigem a permissão de telefone. " +
                            "Sem ela, mostramos só a operadora.",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textSecondary,
                )
                Spacer(Modifier.height(LkSpacing.xs))
                OutlinedButton(onClick = onSolicitarPermissao) {
                    Text(
                        text = "Permitir leitura do chip",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.W600,
                    )
                }
            }
        }
    }
}

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
    onAbrirContatoOperadora: (ContatoOperadora?, String?) -> Unit,
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
    // GH#1662 — este card também passou a exibir o botão de contato da operadora (antes só
    // SimCard tinha): é o caminho usado quando o snapshot vem reduzido (sem permissão) ou
    // quando não há SIM detectado como ativo, e a decisão de produto (Luiz, 2026-08-19) vale
    // pros dois cards, não só pra aba com SIM ativo.
    val operadoraLocal = remember(operadora) { BancoOperadoras.resolverMovel(operadora) }
    val dadosSinal = snapshot.paraDadosSinalMovel()
    val resumoRede = resumoCabecalhoMovel(dadosSinal, capturaReduzida = snapshot.capturaReduzida)
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
        MobileSignalHero(
            qualidade = qualidade,
            experiencia = experiencia,
            tokens = tokens,
        )
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
        MobileDetalhesTecnicosCard(dadosSinal, tokens)

        OutlinedButton(
            onClick = { onAbrirContatoOperadora(operadoraLocal, snapshot.operadora) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
        ) {
            Text(
                text = rotuloBotaoContatoOperadora(snapshot.operadora),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
