package io.signallq.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.R
import io.signallq.app.ui.BancoOperadoras
import io.signallq.app.ui.ConnectionType
import io.signallq.app.ui.ContatoOperadora
import io.signallq.app.ui.ExternalActionLauncher
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.OperadoraSource
import io.signallq.app.ui.OperadoraUiState
import io.signallq.app.ui.OperatorScope
import io.signallq.app.ui.ResolvedOperadoraContact
import io.signallq.app.ui.ResolvedOperadoraIdentity
import io.signallq.app.ui.normalizarWhatsappLocal
import io.signallq.app.ui.resolverOperadoraUiState
import io.signallq.app.ui.whatsappUrl

// Verde oficial da marca WhatsApp — intencionalmente fora da paleta semantica SignallQ,
// mesmo criterio de "cor de marca de terceiro" usado nos badges de operadora.
private val whatsappGreen = Color(0xFF25D366)

/**
 * Bottom sheet "Falar com a operadora" — GH#970. A secao "Sua operadora" agora usa a
 * cadeia local -> diretorio remoto (worker `signallq-diagnostic`) -> fallback generico
 * ([io.signallq.app.ui.OperadoraDirectoryResolver]), via [resolveOperadoraIdentidadeLocal]/
 * [resolveOperadoraIdentidadeRemota] (identidade) e [resolveOperadoraContatoLocal]/
 * [resolveOperadoraContatoRemoto] (contato). A lista "Outras operadoras" continua 100%
 * local (as ~12 principais, [BancoOperadoras.lista]) — sem mudanca de comportamento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperadoraBottomSheet(
    connectionType: String?,
    ispNome: String?,
    operadoraMovel: String?,
    onDismiss: () -> Unit,
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity? =
        { _, _ -> null },
    resolveOperadoraContatoLocal: (String?, Boolean) -> ResolvedOperadoraContact? =
        { _, _ -> null },
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity =
        { nome, _ ->
            ResolvedOperadoraIdentity(
                displayName = nome ?: "Operadora",
                monograma = nome?.firstOrNull()?.uppercase() ?: "?",
                corMarca = null,
                logoRes = null,
                logoUrl = null,
                source = OperadoraSource.FALLBACK,
            )
        },
    resolveOperadoraContatoRemoto: suspend (String?, Boolean) -> ResolvedOperadoraContact =
        { nome, _ ->
            ResolvedOperadoraContact(
                displayName = nome ?: "Operadora",
                sacPhone = null,
                whatsapp = null,
                site = null,
                source = OperadoraSource.FALLBACK,
            )
        },
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // GH#1226 item 10/J — ConnectionType tipado em vez de comparar string livre direto.
    val viaMovel = ConnectionType.parse(connectionType) == ConnectionType.MOBILE
    val nomeParaResolver = if (viaMovel) operadoraMovel else ispNome

    // So pra excluir da lista "Outras operadoras" (sempre local) quando o match tambem
    // for local — operadora resolvida via diretorio remoto nunca esta em BancoOperadoras.lista.
    val operadoraDetectadaLocal = BancoOperadoras.resolver(nomeParaResolver)

    val identidadeDetectada =
        rememberResolvedOperadoraIdentity(
            ispNomeBruto = nomeParaResolver,
            viaMovel = viaMovel,
            resolveLocal = resolveOperadoraIdentidadeLocal,
            resolveRemoteOrFallback = resolveOperadoraIdentidadeRemota,
        )
    val contatoDetectado =
        rememberResolvedOperadoraContact(
            ispNomeBruto = nomeParaResolver,
            viaMovel = viaMovel,
            resolveLocal = resolveOperadoraContatoLocal,
            resolveRemoteOrFallback = resolveOperadoraContatoRemoto,
        )
    // GH#1226 item 6/RF-A — antes "operadora detectada" só existia quando havia CONTATO
    // acionável, então uma identidade conhecida sem nenhum canal (ex.: diretório remoto só
    // devolveu o site) caía como "não identificada" — uma mentira. Estado tipado separa os
    // dois conceitos. "Carregando" só quando há nome pra resolver e a identidade ainda não
    // chegou (nível 1 síncrono já teria resolvido; null aqui significa corrotina em voo).
    val carregando = !nomeParaResolver.isNullOrBlank() && identidadeDetectada == null
    val operadoraUiState = resolverOperadoraUiState(carregando, identidadeDetectada, contatoDetectado)
    val operadoraDetectada = (operadoraUiState as? OperadoraUiState.IdentifiedWithContacts)?.contact

    val subtituloConexao =
        when {
            operadoraUiState is OperadoraUiState.IdentifiedWithContacts && viaMovel ->
                "Detectamos sua operadora pela rede móvel. Atendimento oficial."
            operadoraUiState is OperadoraUiState.IdentifiedWithContacts ->
                "Detectamos sua operadora pela rede fixa. Atendimento oficial."
            operadoraUiState is OperadoraUiState.IdentifiedWithoutContacts ->
                "Identificamos sua operadora, mas nenhum canal de atendimento está disponível no momento."
            else ->
                "Não foi possível identificar sua operadora automaticamente. Escolha abaixo para ver os canais de atendimento."
        }

    val legendaDetectada = if (viaMovel) "SIM ativo · plano móvel" else "rede fixa"

    val outrasOperadoras =
        BancoOperadoras.lista.filter { it.id != operadoraDetectadaLocal?.id }

    // GH#1226 item 9/RF-I — divisão nacional/regional vem do campo declarado no catálogo
    // (ContatoOperadora.scope), não mais de uma lista hardcoded de IDs na UI.
    val outrasNacionais = outrasOperadoras.filter { it.scope == OperatorScope.NATIONAL }
    val outrasRegionais = outrasOperadoras.filter { it.scope != OperatorScope.NATIONAL }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surfaceContainerLow,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
        ) {
            Text(
                text = "Falar com a operadora",
                style = MaterialTheme.typography.headlineSmall,
                color = c.textPrimary,
            )

            Spacer(Modifier.height(LkSpacing.sm))

            Text(
                text = subtituloConexao,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )

            Spacer(Modifier.height(LkSpacing.xl))

            // GH#1226 item 6 — a seção "Sua operadora" aparece pra IdentifiedWithContacts E
            // IdentifiedWithoutContacts (identidade sabida, sem canal — mostra isso
            // explicitamente, não vira "não identificada").
            val identidadeParaExibir =
                when (operadoraUiState) {
                    is OperadoraUiState.IdentifiedWithContacts -> operadoraUiState.identity
                    is OperadoraUiState.IdentifiedWithoutContacts -> operadoraUiState.identity
                    else -> null
                }
            if (identidadeParaExibir != null) {
                // Seção: operadora detectada (local ou via diretorio remoto)
                LkSheetDivider()
                Spacer(Modifier.height(LkSpacing.lg))
                Overline(texto = "Sua operadora", color = c.textTertiary)
                Spacer(Modifier.height(LkSpacing.md))
                OperadoraDetectadaSection(
                    identidade = identidadeParaExibir,
                    contato = operadoraDetectada,
                    legenda = legendaDetectada,
                    onDismiss = onDismiss,
                )
                Spacer(Modifier.height(LkSpacing.lg))

                // Seção: outras operadoras (só quando há detectada)
                if (outrasOperadoras.isNotEmpty()) {
                    LkSheetDivider()
                    Spacer(Modifier.height(LkSpacing.md))
                    Overline(texto = "Não é a sua? Outras operadoras", color = c.textTertiary)
                    Spacer(Modifier.height(LkSpacing.md))
                    if (outrasNacionais.isNotEmpty()) {
                        outrasNacionais.forEach { op ->
                            OutraOperadoraRow(operadora = op)
                            Spacer(Modifier.height(LkSpacing.sm))
                        }
                    }
                    if (outrasRegionais.isNotEmpty()) {
                        if (outrasNacionais.isNotEmpty()) {
                            Spacer(Modifier.height(LkSpacing.xs))
                            Overline(texto = "Regionais", color = c.textTertiary)
                            Spacer(Modifier.height(LkSpacing.sm))
                        }
                        outrasRegionais.forEach { op ->
                            OutraOperadoraRow(operadora = op)
                            Spacer(Modifier.height(LkSpacing.sm))
                        }
                    }
                    Spacer(Modifier.height(LkSpacing.md))
                }
            } else {
                // Seção: nenhuma detectada — mostrar todas com divisão nacional/regional
                Overline(texto = "Operadoras disponíveis", color = c.textTertiary)
                Spacer(Modifier.height(LkSpacing.md))
                val nacionais = BancoOperadoras.lista.filter { it.scope == OperatorScope.NATIONAL }
                val regionais = BancoOperadoras.lista.filter { it.scope != OperatorScope.NATIONAL }
                nacionais.forEach { op ->
                    OutraOperadoraRow(operadora = op)
                    Spacer(Modifier.height(LkSpacing.sm))
                }
                if (regionais.isNotEmpty()) {
                    Spacer(Modifier.height(LkSpacing.xs))
                    Overline(texto = "Regionais", color = c.textTertiary)
                    Spacer(Modifier.height(LkSpacing.sm))
                    regionais.forEach { op ->
                        OutraOperadoraRow(operadora = op)
                        Spacer(Modifier.height(LkSpacing.sm))
                    }
                }
                Spacer(Modifier.height(LkSpacing.md))
            }

            Spacer(Modifier.height(LkSpacing.md))
            LkSheetDivider()
            Spacer(Modifier.height(LkSpacing.base))

            Text(
                text = "O SignallQ não tem vínculo com as operadoras. Você será levado ao canal oficial de cada uma.",
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant,
            )

            Spacer(Modifier.height(LkSpacing.lg))
        }
    }
}

@Composable
private fun OperadoraDetectadaSection(
    identidade: ResolvedOperadoraIdentity,
    // GH#1226 item 6 — nulo quando a operadora foi identificada mas nenhum canal de contato
    // está disponível (OperadoraUiState.IdentifiedWithoutContacts). A seção ainda mostra a
    // identidade, só sem nenhum botão de ação — nunca finge ter um canal que não existe.
    contato: ResolvedOperadoraContact?,
    legenda: String,
    onDismiss: () -> Unit,
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // Identificacao
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(c.surfaceContainer, RoundedCornerShape(LkRadius.card))
                    .padding(LkSpacing.base),
        ) {
            OperadoraBadge(identidade = identidade, size = 40.dp)
            Spacer(Modifier.width(LkSpacing.md))
            Column {
                Text(
                    text = contato?.displayName ?: identidade.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.textPrimary,
                )
                Text(
                    text = legenda,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(LkSpacing.md))

        if (contato == null) {
            Text(
                text = "Nenhum canal de atendimento disponível no momento.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
            return
        }

        // WhatsApp primario
        val waUrl = contato.whatsappUrl()
        if (waUrl != null) {
            Button(
                onClick = {
                    if (ExternalActionLauncher.abrirView(context, waUrl)) onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = whatsappGreen),
            ) {
                Text(
                    text = "Falar no WhatsApp",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Spacer(Modifier.height(LkSpacing.sm))
        }

        // Ligar + App (App so quando ha grupo conhecido — hoje so pra OperadoraSource.LOCAL,
        // o diretorio remoto GH#965 nao tem esse dado, nunca inventamos)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            if (contato.sacPhone != null) {
                OutlinedButton(
                    onClick = {
                        // GH#1226 item 3 — texto exibido e numero discado sao exatamente o
                        // mesmo, sem "*" inventado so pela UI (o "*" nunca fez parte do
                        // numero real, so era mostrado -- discava sem ele).
                        if (ExternalActionLauncher.abrirDiscador(context, contato.sacPhone)) onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(LkRadius.button),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = c.textPrimary,
                    )
                    Spacer(Modifier.width(LkSpacing.xs))
                    Text(
                        text = "Ligar ${contato.sacPhone}",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (contato.grupo != null) {
                OutlinedButton(
                    onClick = {
                        if (ExternalActionLauncher.abrirView(context, "market://search?q=${contato.grupo}")) onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(LkRadius.button),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = c.textPrimary,
                    )
                    Spacer(Modifier.width(LkSpacing.xs))
                    Text(
                        text = "App ${contato.grupo}",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // GH#1226 item 1/B — quando o unico canal disponivel e o site (ex.: diretorio
        // remoto so devolveu isso), a secao nao pode ficar sem nenhuma acao. Antes, uma
        // operadora so-com-site aparecia como "detectada" mas sem nenhum botao.
        if (contato.whatsapp == null && contato.sacPhone == null && contato.grupo == null && contato.site != null) {
            Spacer(Modifier.height(LkSpacing.sm))
            OutlinedButton(
                onClick = {
                    if (ExternalActionLauncher.abrirView(context, contato.site)) onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = c.textPrimary,
                )
                Spacer(Modifier.width(LkSpacing.xs))
                Text(
                    text = "Acessar site",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.textPrimary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun OutraOperadoraRow(operadora: ContatoOperadora) {
    val c = LocalLkTokens.current
    val context = LocalContext.current
    val temSac = operadora.sac != null

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = LkSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OperadoraBadge(operadora = operadora, size = 36.dp)

        Spacer(Modifier.width(LkSpacing.md))

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .let { base ->
                        // GH#1226 item 2/C — telefone da operadora secundária precisa ser
                        // acionável mesmo sem WhatsApp: toque na linha já disca. Antes só o
                        // ícone de WhatsApp era clicável; sem WhatsApp, a linha inteira ficava
                        // sem nenhuma ação.
                        if (temSac) {
                            base.clickable { ExternalActionLauncher.abrirDiscador(context, operadora.sac) }
                        } else {
                            base
                        }
                    },
        ) {
            Text(
                text = operadora.nome,
                style = MaterialTheme.typography.titleSmall,
                color = c.textPrimary,
            )
            // GH#1226 item 3 — texto exibido é exatamente o número discado, sem "*" inventado.
            // GH#1226 (bug latente) — operadora sem SAC cadastrado (ex.: Coopertec SPEED)
            // não mostra mais "ligar *null".
            val textoContato =
                when {
                    operadora.whatsapp != null && temSac -> "WhatsApp · ligar ${operadora.sac}"
                    operadora.whatsapp != null -> "WhatsApp"
                    temSac -> "ligar ${operadora.sac}"
                    else -> null
                }
            if (textoContato != null) {
                Text(
                    text = textoContato,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }

        if (operadora.whatsapp != null) {
            IconButton(
                onClick = { ExternalActionLauncher.abrirView(context, normalizarWhatsappLocal(operadora.whatsapp)) },
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .background(whatsappGreen, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Chat,
                        contentDescription = stringResource(R.string.cd_abrir_whatsapp),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
