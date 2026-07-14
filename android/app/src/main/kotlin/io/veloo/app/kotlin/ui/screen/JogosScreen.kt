package io.signallq.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.signallq.app.BuildConfig
import io.signallq.app.ads.AdSlot
import io.signallq.app.ads.AdUnitIds
import io.signallq.app.ads.NativeAdContentSignals
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.WifiLinkSnapshot
import io.signallq.app.jogos.CatalogoJogos
import io.signallq.app.jogos.GameIconCatalog
import io.signallq.app.jogos.JogoCatalogo
import io.signallq.app.jogos.JogosEtapa
import io.signallq.app.jogos.JogosViewModel
import io.signallq.app.jogos.MENSAGENS_PROGRESSO_JOGOS
import io.signallq.app.jogos.NivelMetrica
import io.signallq.app.jogos.NivelResultado
import io.signallq.app.jogos.Plataforma
import io.signallq.app.jogos.ResultadoTesteJogo
import io.signallq.app.jogos.label
import io.signallq.app.jogos.thresholds
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.ads.rememberNativeAd
import io.signallq.app.ui.component.ads.NativeAdCard
import io.signallq.app.ui.component.ads.NativeAdSource
import kotlinx.coroutines.launch

/**
 * Tela "Jogos" (GH#935, Fase 6 MD3) — fluxo de 5 etapas do
 * `docs_ai/functional/JOGOS_TESTE_CONEXAO_SPEC.md`: plataforma → jogo → confirmação →
 * progresso → resultado. Substitui o stub criado na Fase 1 (#930).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JogosScreen(
    tipoConexaoAtual: EstadoConexao,
    wifiLinkSnapshot: WifiLinkSnapshot?,
    onVoltar: () -> Unit,
    adsEnabled: Boolean = false,
) {
    val c = LocalLkTokens.current
    val scope = rememberCoroutineScope()

    val tipoConexaoAtualizado = rememberUpdatedState(tipoConexaoAtual)
    val wifiSnapshotAtualizado = rememberUpdatedState(wifiLinkSnapshot)
    val viewModel =
        remember {
            JogosViewModel(
                probeUrl = BuildConfig.GAME_LATENCY_PROBE_URL,
                tipoConexaoAtual = { tipoConexaoAtualizado.value },
                wifiLinkSnapshot = { wifiSnapshotAtualizado.value },
            )
        }
    val etapa by viewModel.etapa.collectAsState()

    BackHandler(enabled = etapa !is JogosEtapa.SelecaoPlataforma) {
        when (etapa) {
            is JogosEtapa.SelecaoJogo -> viewModel.voltarParaSelecaoPlataforma()
            is JogosEtapa.Confirmacao -> viewModel.voltarParaSelecaoJogo()
            is JogosEtapa.Progresso -> Unit // teste em andamento — sem cancelamento manual no MVP.
            is JogosEtapa.Resultado -> viewModel.escolherOutroJogo()
            is JogosEtapa.Erro -> viewModel.voltarParaSelecaoJogo()
            is JogosEtapa.SelecaoPlataforma -> Unit
        }
    }

    val onVoltarTopo: () -> Unit = {
        when (etapa) {
            is JogosEtapa.SelecaoPlataforma -> onVoltar()
            is JogosEtapa.SelecaoJogo -> viewModel.voltarParaSelecaoPlataforma()
            is JogosEtapa.Confirmacao -> viewModel.voltarParaSelecaoJogo()
            is JogosEtapa.Progresso -> Unit
            is JogosEtapa.Resultado -> onVoltar()
            is JogosEtapa.Erro -> viewModel.voltarParaSelecaoJogo()
        }
    }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Jogos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltarTopo) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = c.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            when (val estadoAtual = etapa) {
                is JogosEtapa.SelecaoPlataforma ->
                    EtapaSelecaoPlataforma(
                        onSelecionar = viewModel::selecionarPlataforma,
                    )

                is JogosEtapa.SelecaoJogo ->
                    EtapaSelecaoJogo(
                        etapaAtual = estadoAtual,
                        onBuscar = viewModel::buscar,
                        onSelecionarJogo = viewModel::selecionarJogo,
                    )

                is JogosEtapa.Confirmacao ->
                    EtapaConfirmacao(
                        plataforma = estadoAtual.plataforma,
                        jogo = estadoAtual.jogo,
                        onTestar = { scope.launch { viewModel.iniciarTeste() } },
                    )

                is JogosEtapa.Progresso ->
                    EtapaProgresso(
                        jogo = estadoAtual.jogo,
                        mensagemIndex = estadoAtual.mensagemIndex,
                    )

                is JogosEtapa.Resultado ->
                    EtapaResultado(
                        resultado = estadoAtual.resultado,
                        adsEnabled = adsEnabled,
                        onTestarNovamente = {
                            scope.launch {
                                viewModel.testarNovamente()
                                viewModel.iniciarTeste()
                            }
                        },
                        onEscolherOutroJogo = viewModel::escolherOutroJogo,
                    )

                is JogosEtapa.Erro ->
                    EtapaErro(
                        mensagem = estadoAtual.mensagem,
                        onTentarNovamente = {
                            scope.launch {
                                viewModel.testarNovamente()
                                viewModel.iniciarTeste()
                            }
                        },
                        onEscolherOutroJogo = viewModel::voltarParaSelecaoJogo,
                    )
            }
        }
    }
}

// ── Etapa 1 — Plataforma ─────────────────────────────────────────────────

@Composable
private fun EtapaSelecaoPlataforma(onSelecionar: (Plataforma) -> Unit) {
    val c = LocalLkTokens.current
    // Indicador de seleção puramente visual (spec 5g, passo 1): a navegação para o
    // passo 2 é imediata e síncrona (ver JogosViewModel.selecionarPlataforma), então
    // este estado só existe para acender o check_circle no instante do toque, antes
    // da troca de tela — nunca persiste depois de voltar (a tela é recriada do zero).
    var selecionada by remember { mutableStateOf<Plataforma?>(null) }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(LkSpacing.xl),
    ) {
        Text(
            text = "Como está sua conexão para jogar?",
            style = MaterialTheme.typography.headlineSmall,
            color = c.textPrimary,
        )
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            text = "A lista de jogos muda de acordo com a plataforma selecionada.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(LkSpacing.xl))
        Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
            Plataforma.entries.forEach { plataforma ->
                PlataformaButton(
                    plataforma = plataforma,
                    ativo = plataforma == selecionada,
                    c = c,
                    onClick = {
                        selecionada = plataforma
                        onSelecionar(plataforma)
                    },
                )
            }
        }
    }
}

@Composable
private fun PlataformaButton(
    plataforma: Plataforma,
    ativo: Boolean,
    c: LkTokens,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(if (ativo) LkColors.accent.copy(alpha = 0.10f) else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (ativo) LkColors.accent else c.outlineVariant,
                    shape = RoundedCornerShape(LkRadius.card),
                ).clickable(onClick = onClick)
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = plataforma.icone(),
            contentDescription = null,
            tint = if (ativo) LkColors.accent else c.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(LkSpacing.md))
        Text(
            text = plataforma.label,
            style = MaterialTheme.typography.titleSmall,
            color = c.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (ativo) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = LkColors.accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun Plataforma.icone(): ImageVector =
    when (this) {
        Plataforma.PC -> Icons.Outlined.Computer
        Plataforma.PS5 -> Icons.Outlined.SportsEsports
        Plataforma.XBOX -> Icons.Outlined.VideogameAsset
    }

// ── Etapa 2 — Jogo ────────────────────────────────────────────────────────

@Composable
private fun EtapaSelecaoJogo(
    etapaAtual: JogosEtapa.SelecaoJogo,
    onBuscar: (String) -> Unit,
    onSelecionarJogo: (JogoCatalogo) -> Unit,
) {
    val c = LocalLkTokens.current
    val jogosFiltrados =
        remember(etapaAtual.plataforma, etapaAtual.busca) {
            CatalogoJogos
                .porPlataforma(etapaAtual.plataforma)
                .filter { it.nome.contains(etapaAtual.busca, ignoreCase = true) }
        }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = LkSpacing.xl)) {
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                text = "${etapaAtual.plataforma.label} · escolha o jogo",
                style = MaterialTheme.typography.titleMedium,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(LkSpacing.md))
            OutlinedTextField(
                value = etapaAtual.busca,
                onValueChange = onBuscar,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar jogo") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = c.textTertiary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(LkRadius.input),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LkColors.accent,
                        unfocusedBorderColor = c.border,
                    ),
            )
            Spacer(Modifier.height(LkSpacing.sm))
        }

        if (jogosFiltrados.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nenhum jogo encontrado para \"${etapaAtual.busca}\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = LkSpacing.xl, vertical = LkSpacing.sm)) {
                items(jogosFiltrados, key = { it.gameId }) { jogo ->
                    JogoListItem(jogo = jogo, onClick = { onSelecionarJogo(jogo) })
                    HorizontalDivider(color = c.border)
                }
            }
        }
    }
}

@Composable
private fun JogoListItem(
    jogo: JogoCatalogo,
    onClick: () -> Unit,
) {
    val c = LocalLkTokens.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = LkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LkColors.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            // GH#935 Fase 6 To-Be — imagem oficial real do catálogo quando disponível
            // (os 16 jogos hoje têm); sigla monoespaçada é só fallback pra jogo futuro
            // sem asset ainda, spec original.
            val iconRes = GameIconCatalog.iconePara(jogo.gameId)
            if (iconRes != null) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = jogo.nome.take(2).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.W600,
                    color = LkColors.accent,
                )
            }
        }
        Spacer(Modifier.width(LkSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = jogo.nome, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
            Text(
                text = jogo.perfil.label(),
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = c.textTertiary,
        )
    }
}

// ── Etapa 3 — Confirmação ────────────────────────────────────────────────

@Composable
private fun EtapaConfirmacao(
    plataforma: Plataforma,
    jogo: JogoCatalogo,
    onTestar: () -> Unit,
) {
    val c = LocalLkTokens.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(LkSpacing.xl),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(c.bgCard)
                    .padding(LkSpacing.cardContent),
        ) {
            Icon(imageVector = Icons.Outlined.SportsEsports, contentDescription = null, tint = LkColors.accent)
            Spacer(Modifier.height(LkSpacing.sm))
            Text(text = jogo.nome, style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
            Spacer(Modifier.height(LkSpacing.xs))
            Text(
                text = "${plataforma.label} · ${jogo.perfil.label()}",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(LkSpacing.xs))
            Text(
                text = "Teste estimado para servidores na América do Sul",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary,
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onTestar,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
            colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
        ) {
            Text("Testar conexão para ${jogo.nome}")
        }
    }
}

// ── Etapa 4 — Progresso ──────────────────────────────────────────────────

@Composable
private fun EtapaProgresso(
    jogo: JogoCatalogo,
    mensagemIndex: Int,
) {
    val c = LocalLkTokens.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(LkSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(LkColors.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.NetworkCheck,
                contentDescription = null,
                tint = LkColors.accent,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(LkSpacing.lg))
        Text(
            text = "Testando conexão para ${jogo.nome}",
            style = MaterialTheme.typography.headlineSmall,
            color = c.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(LkSpacing.xl))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            MENSAGENS_PROGRESSO_JOGOS.forEachIndexed { indice, mensagem ->
                val estado =
                    when {
                        indice < mensagemIndex -> EtapaProgressoEstado.CONCLUIDA
                        indice == mensagemIndex -> EtapaProgressoEstado.ATIVA
                        else -> EtapaProgressoEstado.PENDENTE
                    }
                EtapaProgressoLinha(texto = mensagem, estado = estado, c = c)
            }
        }
    }
}

private enum class EtapaProgressoEstado { CONCLUIDA, ATIVA, PENDENTE }

/** Linha de etapa do passo 4 (spec 5g) — check_circle na concluída, pulso de opacidade
 *  (~800ms, mesmo padrão de [io.signallq.app.ui.component.SilentSpeedtestIndicator])
 *  na ativa, opacidade reduzida na pendente. Puramente visual: quem decide quando cada
 *  etapa avança é o [mensagemIndex] real vindo da medição de rede em JogosViewModel. */
@Composable
private fun EtapaProgressoLinha(
    texto: String,
    estado: EtapaProgressoEstado,
    c: LkTokens,
) {
    val transition = rememberInfiniteTransition(label = "jogos-progresso-pulso")
    val alphaPulso by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "jogos-progresso-alpha",
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (estado) {
            EtapaProgressoEstado.CONCLUIDA ->
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = LkColors.success,
                    modifier = Modifier.size(20.dp),
                )

            EtapaProgressoEstado.ATIVA ->
                Box(
                    modifier =
                        Modifier
                            .size(20.dp)
                            .alpha(alphaPulso)
                            .clip(CircleShape)
                            .background(LkColors.accent),
                )

            EtapaProgressoEstado.PENDENTE ->
                Box(
                    modifier =
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(c.outlineVariant),
                )
        }
        Spacer(Modifier.width(LkSpacing.md))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            color =
                when (estado) {
                    EtapaProgressoEstado.PENDENTE -> c.textTertiary
                    else -> c.textPrimary
                },
            modifier = if (estado == EtapaProgressoEstado.ATIVA) Modifier.alpha(alphaPulso) else Modifier,
        )
    }
}

// ── Etapa 5 — Resultado ──────────────────────────────────────────────────

@Composable
private fun EtapaResultado(
    resultado: ResultadoTesteJogo,
    adsEnabled: Boolean,
    onTestarNovamente: () -> Unit,
    onEscolherOutroJogo: () -> Unit,
) {
    val c = LocalLkTokens.current
    var nativeAdDismissed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(LkSpacing.xl),
        ) {
            VeredictoJogoBanner(resultado = resultado, c = c)

            Spacer(Modifier.height(LkSpacing.lg))

            MetricasJogoGrid(resultado = resultado, c = c)

            if (resultado.recomendacoes.isNotEmpty()) {
                Spacer(Modifier.height(LkSpacing.lg))
                Text(
                    text = "RECOMENDAÇÕES",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                )
                resultado.recomendacoes.forEach {
                    Spacer(Modifier.height(LkSpacing.xs))
                    Text(text = "• $it", style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
                }
            }

            // Spec 5g: NativeAd é a ÚNICA superfície de anúncio do fluxo de Jogos, e só
            // aqui — logo abaixo das recomendações, antes dos avisos de rodapé/CTAs.
            if (!nativeAdDismissed) {
                Spacer(Modifier.height(LkSpacing.lg))
                val nativeAd by
                    rememberNativeAd(
                        adUnitId = AdUnitIds.para(AdSlot.JOGOS),
                        contentSignal = NativeAdContentSignals.forSlot(AdSlot.JOGOS),
                        eligible = adsEnabled,
                    )
                NativeAdCard(
                    nativeAd = nativeAd,
                    source = NativeAdSource.ADMOB,
                    onDismiss = { nativeAdDismissed = true },
                )
            }

            if (resultado.avisos.isNotEmpty()) {
                Spacer(Modifier.height(LkSpacing.lg))
                resultado.avisos.forEach {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                    Spacer(Modifier.height(LkSpacing.xs))
                }
            }
        }

        HorizontalDivider(color = c.border)

        Column(modifier = Modifier.padding(LkSpacing.xl)) {
            Button(
                onClick = onTestarNovamente,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
            ) {
                Text("Testar novamente")
            }
            Spacer(Modifier.height(LkSpacing.sm))
            OutlinedButton(
                onClick = onEscolherOutroJogo,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Text("Escolher outro jogo")
            }
        }
    }
}

/** Banner de veredito do passo 5 (spec 5g) — successContainer (excelente/boa),
 *  warningContainer (atenção) ou errorContainer (ruim), com a região testada no corpo. */
@Composable
private fun VeredictoJogoBanner(
    resultado: ResultadoTesteJogo,
    c: LkTokens,
) {
    val (bg, fg, icone) =
        when (resultado.nivel) {
            NivelResultado.EXCELENTE, NivelResultado.BOA ->
                Triple(c.successContainer, c.onSuccessContainer, Icons.Outlined.CheckCircle)
            NivelResultado.ATENCAO ->
                Triple(c.warningContainer, c.onWarningContainer, Icons.Outlined.WarningAmber)
            NivelResultado.RUIM ->
                Triple(c.errorContainer, c.onErrorContainer, Icons.Outlined.ErrorOutline)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(bg)
                .padding(LkSpacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(imageVector = icone, contentDescription = null, tint = fg, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(LkSpacing.sm))
        Column {
            Text(
                text = resultado.tituloVeredito,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W600,
                color = fg,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${resultado.textoVeredito} Teste feito para servidores em ${resultado.regiaoTestada}.",
                style = MaterialTheme.typography.bodyMedium,
                color = fg,
            )
        }
    }
}

/** Grade de métricas do passo 5 (spec 5g) — 3 linhas x 2 colunas, fundo surfaceContainer.
 *  "Estabilidade" é derivada da mesma classificação de jitter que o motor já usa como
 *  proxy de estabilidade (ver comentário em JogoConexaoEngine) — não é uma métrica nova,
 *  só a superfície de exibição que faltava para um valor que o motor já calcula. */
@Composable
private fun MetricasJogoGrid(
    resultado: ResultadoTesteJogo,
    c: LkTokens,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
            MetricaGridCell("Latência", "${resultado.latenciaMs.toInt()}ms", c, Modifier.weight(1f))
            MetricaGridCell("Jitter", "${resultado.jitterMs.toInt()}ms", c, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
            MetricaGridCell("Perda de pacotes", "${resultado.perdaPercentual.toInt()}%", c, Modifier.weight(1f))
            MetricaGridCell("Estabilidade", estabilidadeLabel(resultado), c, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
            MetricaGridCell("Região testada", resultado.regiaoTestada, c, Modifier.weight(1f))
            MetricaGridCell("Conexão atual", conexaoLabel(resultado.tipoConexaoAtual), c, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricaGridCell(
    rotulo: String,
    valor: String,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(LkSpacing.md),
    ) {
        Text(text = rotulo, style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
        Spacer(Modifier.height(2.dp))
        Text(text = valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W600, color = c.textPrimary)
    }
}

private fun estabilidadeLabel(resultado: ResultadoTesteJogo): String =
    when (resultado.jogo.perfil.thresholds().jitterMs.classificar(resultado.jitterMs)) {
        NivelMetrica.EXCELENTE -> "Excelente"
        NivelMetrica.BOA -> "Boa"
        NivelMetrica.ATENCAO -> "Instável"
        NivelMetrica.RUIM -> "Muito instável"
    }

private fun conexaoLabel(estado: EstadoConexao): String =
    when (estado) {
        EstadoConexao.wifi -> "Wi-Fi"
        EstadoConexao.movel -> "Rede móvel"
        EstadoConexao.ethernet -> "Cabo (Ethernet)"
        EstadoConexao.desconectado -> "Desconectado"
        EstadoConexao.desconhecido -> "Desconhecido"
    }

// ── Erro ──────────────────────────────────────────────────────────────────

@Composable
private fun EtapaErro(
    mensagem: String,
    onTentarNovamente: () -> Unit,
    onEscolherOutroJogo: () -> Unit,
) {
    val c = LocalLkTokens.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(LkSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.WifiOff,
            contentDescription = null,
            tint = LkColors.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(LkSpacing.lg))
        Text(
            text = "Não foi possível testar",
            style = MaterialTheme.typography.headlineSmall,
            color = c.textPrimary,
        )
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            text = mensagem,
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(LkSpacing.xl))
        Button(
            onClick = onTentarNovamente,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
            colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
        ) {
            Text("Tentar novamente")
        }
        Spacer(Modifier.height(LkSpacing.sm))
        OutlinedButton(
            onClick = onEscolherOutroJogo,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
        ) {
            Text("Escolher outro jogo")
        }
    }
}
