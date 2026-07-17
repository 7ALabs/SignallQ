package io.signallq.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.signallq.app.BuildConfig
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.WifiLinkSnapshot
import io.signallq.app.jogos.CatalogoJogos
import io.signallq.app.jogos.EstrategiaTeste
import io.signallq.app.jogos.GameArtworkCatalog
import io.signallq.app.jogos.JogoCatalogo
import io.signallq.app.jogos.JogosEtapa
import io.signallq.app.jogos.JogosViewModel
import io.signallq.app.jogos.MENSAGENS_PROGRESSO_JOGOS
import io.signallq.app.jogos.NivelResultado
import io.signallq.app.jogos.Plataforma
import io.signallq.app.jogos.ResultadoTesteJogo
import io.signallq.app.jogos.label
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.Overline
import io.signallq.app.ui.component.ads.SimulatedOfferCard
import kotlinx.coroutines.launch

/**
 * Tela "Jogos" — alinhada à sheet 5g do design system:
 * plataforma → jogo → confirmação → progresso → resultado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JogosScreen(
    tipoConexaoAtual: EstadoConexao,
    wifiLinkSnapshot: WifiLinkSnapshot?,
    onVoltar: () -> Unit,
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
            is JogosEtapa.Progresso -> Unit
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
        when (val estadoAtual = etapa) {
            is JogosEtapa.SelecaoPlataforma ->
                EtapaSelecaoPlataforma(
                    onSelecionar = viewModel::selecionarPlataforma,
                    modifier = Modifier.padding(padding),
                )

            is JogosEtapa.SelecaoJogo ->
                EtapaSelecaoJogo(
                    etapaAtual = estadoAtual,
                    onBuscar = viewModel::buscar,
                    onSelecionarJogo = viewModel::selecionarJogo,
                    modifier = Modifier.padding(padding),
                )

            is JogosEtapa.Confirmacao ->
                EtapaConfirmacao(
                    plataforma = estadoAtual.plataforma,
                    jogo = estadoAtual.jogo,
                    onTestar = { scope.launch { viewModel.iniciarTeste() } },
                    modifier = Modifier.padding(padding),
                )

            is JogosEtapa.Progresso ->
                EtapaProgresso(
                    jogo = estadoAtual.jogo,
                    mensagemIndex = estadoAtual.mensagemIndex,
                    modifier = Modifier.padding(padding),
                )

            is JogosEtapa.Resultado ->
                EtapaResultado(
                    resultado = estadoAtual.resultado,
                    onTestarNovamente = {
                        scope.launch {
                            viewModel.testarNovamente()
                            viewModel.iniciarTeste()
                        }
                    },
                    onEscolherOutroJogo = viewModel::escolherOutroJogo,
                    modifier = Modifier.padding(padding),
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
                    modifier = Modifier.padding(padding),
                )
        }
    }
}

@Composable
private fun EtapaSelecaoPlataforma(
    onSelecionar: (Plataforma) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.xs)) {
            Text(
                text = "Como está sua conexão para jogar?",
                style = MaterialTheme.typography.headlineSmall,
                color = c.textPrimary,
            )
            Text(
                text = "Escolha sua plataforma e o jogo que deseja testar.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
        }

        Overline(texto = "Plataforma", color = c.textTertiary)

        Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
            Plataforma.entries.forEach { plataforma ->
                PlataformaOptionCard(
                    plataforma = plataforma,
                    onClick = { onSelecionar(plataforma) },
                )
            }
        }
    }
}

@Composable
private fun PlataformaOptionCard(
    plataforma: Plataforma,
    onClick: () -> Unit,
) {
    val c = LocalLkTokens.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.surface)
                .border(1.dp, c.outlineVariant, RoundedCornerShape(LkRadius.card))
                .clickable(onClick = onClick)
                .padding(horizontal = LkSpacing.base, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Icon(
            imageVector = plataformaIcon(plataforma),
            contentDescription = null,
            tint = c.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = plataformaTitulo(plataforma),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = c.textPrimary,
            fontWeight = FontWeight.W500,
        )
    }
}

@Composable
private fun EtapaSelecaoJogo(
    etapaAtual: JogosEtapa.SelecaoJogo,
    onBuscar: (String) -> Unit,
    onSelecionarJogo: (JogoCatalogo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val jogosFiltrados =
        remember(etapaAtual.plataforma, etapaAtual.busca) {
            CatalogoJogos
                .porPlataforma(etapaAtual.plataforma)
                .filter { it.nome.contains(etapaAtual.busca, ignoreCase = true) }
        }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.base),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                Text(
                    text = "Escolha o jogo",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.textPrimary,
                    fontWeight = FontWeight.W600,
                )
                Spacer(Modifier.weight(1f))
                PlatformBadge(plataforma = etapaAtual.plataforma)
            }

            OutlinedTextField(
                value = etapaAtual.busca,
                onValueChange = onBuscar,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar jogo…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = c.textTertiary,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(LkRadius.input),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = c.outline,
                        unfocusedBorderColor = c.outline,
                        focusedContainerColor = c.surface,
                        unfocusedContainerColor = c.surface,
                    ),
            )
        }

        if (jogosFiltrados.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = LkSpacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Nenhum jogo encontrado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = LkSpacing.xl, end = LkSpacing.xl, bottom = LkSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                items(jogosFiltrados, key = { it.gameId }) { jogo ->
                    JogoListCard(jogo = jogo, onClick = { onSelecionarJogo(jogo) })
                }
            }
        }
    }
}

@Composable
private fun JogoListCard(
    jogo: JogoCatalogo,
    onClick: () -> Unit,
) {
    val c = LocalLkTokens.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surfaceContainer)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        GameArtworkBadge(jogo = jogo, size = 36.dp, cornerRadius = 10.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = jogo.nome,
                style = MaterialTheme.typography.titleSmall,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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

@Composable
private fun EtapaConfirmacao(
    plataforma: Plataforma,
    jogo: JogoCatalogo,
    onTestar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.xl),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            GameArtworkBadge(jogo = jogo, size = 52.dp, cornerRadius = 14.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${jogo.nome} selecionado",
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.textPrimary,
                )
                Text(
                    text = "${plataformaTitulo(plataforma)} · ${jogo.perfil.label()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }

        LkSurfaceCard {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = null,
                    tint = c.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = regionLabel(jogo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
            }
        }

        Text(
            text = "Vamos avaliar latência, estabilidade e perda de pacotes para o perfil deste jogo.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )

        Button(
            onClick = onTestar,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
            colors = ButtonDefaults.buttonColors(containerColor = c.primary),
        ) {
            Text(
                text = "Testar conexão para ${jogo.nome}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W600,
            )
        }
    }
}

@Composable
private fun EtapaProgresso(
    jogo: JogoCatalogo,
    mensagemIndex: Int,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(c.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.NetworkCheck,
                contentDescription = null,
                tint = c.primary,
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
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            text = "Isso leva só alguns segundos.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(LkSpacing.xl))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            MENSAGENS_PROGRESSO_JOGOS.forEachIndexed { index, label ->
                val concluida = index < mensagemIndex
                val ativa = index == mensagemIndex
                ProgressoStepRow(
                    label = label,
                    concluida = concluida,
                    ativa = ativa,
                )
            }
        }
    }
}

@Composable
private fun ProgressoStepRow(
    label: String,
    concluida: Boolean,
    ativa: Boolean,
) {
    val c = LocalLkTokens.current
    val pulseTransition = rememberInfiniteTransition(label = "jogos_progresso")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "jogos_progresso_alpha",
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .alpha(if (ativa) pulseAlpha else 1f)
                .clip(RoundedCornerShape(LkRadius.input))
                .background(
                    when {
                        ativa -> c.primary.copy(alpha = 0.10f)
                        else -> Color.Transparent
                    },
                ).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Icon(
            imageVector = if (concluida) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint =
                when {
                    concluida -> c.success
                    ativa -> c.primary
                    else -> c.textTertiary
                },
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (concluida || ativa) c.textPrimary else c.textSecondary,
        )
    }
}

@Composable
private fun EtapaResultado(
    resultado: ResultadoTesteJogo,
    onTestarNovamente: () -> Unit,
    onEscolherOutroJogo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val tone = verdictTone(resultado.nivel, c)
    val metricas =
        listOf(
            MetricUi("Latência", "${resultado.latenciaMs.toInt()} ms"),
            MetricUi("Jitter", "${resultado.jitterMs.toInt()} ms"),
            MetricUi("Perda de pacotes", "${resultado.perdaPercentual.formatPercent()}%"),
            MetricUi("Estabilidade", estabilidadeLabel(resultado.nivel)),
            MetricUi("Região testada", regiaoResultado(resultado)),
            MetricUi("Conexão atual", conexaoResultadoLabel(resultado)),
        )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.lg),
    ) {
        VerdictCard(resultado = resultado, tone = tone)

        Overline(texto = "Métricas", color = c.textTertiary)
        Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
            metricas.chunked(2).forEach { linha ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                ) {
                    linha.forEach { metrica ->
                        MetricCard(
                            label = metrica.label,
                            value = metrica.value,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (linha.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        val cardsRecomendacao = recommendationCards(resultado)
        if (cardsRecomendacao.isNotEmpty()) {
            Overline(texto = "Recomendações", color = c.textTertiary)
            Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
                cardsRecomendacao.forEach { rec ->
                    RecommendationCard(rec)
                }
            }
        }

        // TODO: substituir este card SIMULADO por rememberNativeAd + NativeAdCard
        // quando o slot real do AdMob desta tela estiver configurado.
        SimulatedOfferCard(
            title = "Plano gamer com Wi-Fi 6",
            body = "Oferta simulada para melhorar estabilidade e cobertura perto do console ou PC.",
            cta = "Conhecer oferta",
        )

        Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.base)) {
            resultado.avisos.forEach { aviso ->
                InfoNote(text = aviso)
            }
            InfoNote(text = "A precisão varia conforme a rota até os servidores do jogo e a região estimada para a partida.")
            InfoNote(text = "Para uma leitura mais fiel, meça próximo ao console ou PC conectado à mesma rede.")
        }

        Button(
            onClick = onTestarNovamente,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
            colors = ButtonDefaults.buttonColors(containerColor = c.primary),
        ) {
            Text(
                text = "Testar novamente",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W600,
            )
        }
        TextButton(
            onClick = onEscolherOutroJogo,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Escolher outro jogo")
        }
    }
}

@Composable
private fun VerdictCard(
    resultado: ResultadoTesteJogo,
    tone: VerdictTone,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(tone.container)
                .padding(LkSpacing.base),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            Icon(
                imageVector = tone.icon,
                contentDescription = null,
                tint = tone.onContainer,
                modifier = Modifier.size(24.dp),
            )
            Column {
                Text(
                    text = resultado.tituloVeredito,
                    style = MaterialTheme.typography.titleLarge,
                    color = tone.onContainer,
                )
                Text(
                    text = "${resultado.textoVeredito} ${complementoResultado(resultado)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tone.onContainer.copy(alpha = 0.92f),
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    LkSurfaceCard(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = c.textPrimary,
        )
    }
}

@Composable
private fun RecommendationCard(recommendation: RecommendationUi) {
    val c = LocalLkTokens.current
    LkSurfaceCard {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            Icon(
                imageVector = recommendation.icon,
                contentDescription = null,
                tint = c.primary,
                modifier = Modifier.size(18.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = c.textPrimary,
                )
                Text(
                    text = recommendation.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun InfoNote(text: String) {
    val c = LocalLkTokens.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = c.textTertiary,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
        )
    }
}

@Composable
private fun EtapaErro(
    mensagem: String,
    onTentarNovamente: () -> Unit,
    onEscolherOutroJogo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LkSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            outlined = true,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.WifiOff,
                    contentDescription = null,
                    tint = c.error,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(LkSpacing.lg))
                Text(
                    text = "Não foi possível testar",
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(LkSpacing.xs))
                Text(
                    text = mensagem,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(LkSpacing.xl))
        Button(
            onClick = onTentarNovamente,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
            colors = ButtonDefaults.buttonColors(containerColor = c.primary),
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

@Composable
private fun PlatformBadge(plataforma: Plataforma) {
    val c = LocalLkTokens.current
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(LkRadius.pill))
                .background(c.primary.copy(alpha = 0.10f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = plataformaTitulo(plataforma),
            style = MaterialTheme.typography.labelMedium,
            color = c.primary,
            fontWeight = FontWeight.W600,
        )
    }
}

@Composable
private fun GameArtworkBadge(
    jogo: JogoCatalogo,
    size: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp,
) {
    val c = LocalLkTokens.current
    val artwork = remember(jogo.gameId) { GameArtworkCatalog.forGame(jogo.gameId) }

    if (artwork != null) {
        Image(
            painter = painterResource(id = artwork.drawableRes),
            contentDescription = jogo.nome,
            modifier =
                Modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius)),
            contentScale = ContentScale.Crop,
        )
        return
    }

    Box(
        modifier =
            Modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
                .background(c.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = jogoSigla(jogo.nome),
            style = if (size > 40.dp) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
            color = c.primary,
            fontWeight = FontWeight.W700,
        )
    }
}

private data class MetricUi(
    val label: String,
    val value: String,
)

private data class RecommendationUi(
    val title: String,
    val body: String,
    val icon: ImageVector,
)

private data class VerdictTone(
    val container: Color,
    val onContainer: Color,
    val icon: ImageVector,
)

private fun plataformaTitulo(plataforma: Plataforma): String =
    when (plataforma) {
        Plataforma.PC -> "PC"
        Plataforma.PS5 -> "PlayStation 5"
        Plataforma.XBOX -> "Xbox Series"
    }

private fun plataformaIcon(plataforma: Plataforma): ImageVector =
    when (plataforma) {
        Plataforma.PC -> Icons.Outlined.Devices
        Plataforma.PS5, Plataforma.XBOX -> Icons.Outlined.SportsEsports
    }

private fun jogoSigla(nome: String): String =
    nome
        .split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .take(2)

private fun regionLabel(jogo: JogoCatalogo): String =
    when (jogo.estrategiaDeclarada) {
        EstrategiaTeste.PROVIDER_NETWORK -> "Teste via rede oficial · ${regiaoCatalogo(jogo)}"
        EstrategiaTeste.REGIONAL_ESTIMATE -> "Teste estimado para servidores na ${regiaoCatalogo(jogo)}"
    }

private fun regiaoCatalogo(jogo: JogoCatalogo): String =
    when (jogo.gameId) {
        "valorant", "league_of_legends" -> "rede da Riot no Brasil"
        "counter_strike_2" -> "rede Valve (Steam Datagram Relay)"
        else -> "América do Sul"
    }

private fun regiaoResultado(resultado: ResultadoTesteJogo): String =
    if (resultado.estrategiaUsada == EstrategiaTeste.PROVIDER_NETWORK) {
        regiaoCatalogo(resultado.jogo)
    } else {
        resultado.regiaoTestada
    }

private fun complementoResultado(resultado: ResultadoTesteJogo): String =
    if (resultado.estrategiaUsada == EstrategiaTeste.PROVIDER_NETWORK) {
        "Estimativa para partidas em ${regiaoCatalogo(resultado.jogo)}."
    } else {
        "Estimativa para partidas em servidores na ${resultado.regiaoTestada}."
    }

private fun estabilidadeLabel(nivel: NivelResultado): String =
    when (nivel) {
        NivelResultado.EXCELENTE, NivelResultado.BOA -> "Boa"
        NivelResultado.ATENCAO -> "Instável"
        NivelResultado.RUIM -> "Ruim"
    }

private fun conexaoResultadoLabel(resultado: ResultadoTesteJogo): String {
    val recomendacoes = resultado.recomendacoes.joinToString(" ")
    return when {
        recomendacoes.contains("5GHz", ignoreCase = true) -> "Wi-Fi 2,4 GHz"
        resultado.tipoConexaoAtual == EstadoConexao.wifi -> "Wi-Fi 5 GHz"
        else -> conexaoLabel(resultado.tipoConexaoAtual)
    }
}

private fun recommendationCards(resultado: ResultadoTesteJogo): List<RecommendationUi> {
    val cards = mutableListOf<RecommendationUi>()
    val recomendacoesTexto = resultado.recomendacoes.joinToString(" ")

    if (recomendacoesTexto.contains("dados não chegou", ignoreCase = true)) {
        cards +=
            RecommendationUi(
                title = "Parte dos dados não chegou ao destino",
                body = "Foi detectada perda de pacotes. Isso pode causar teletransporte, desconexões ou comandos que não respondem.",
                icon = Icons.Outlined.WarningAmber,
            )
    }

    if (recomendacoesTexto.contains("está variando", ignoreCase = true)) {
        cards +=
            RecommendationUi(
                title = "Sua conexão está variando",
                body = "O tempo de resposta mudou bastante durante o teste. Isso pode causar comandos irregulares ou pequenos travamentos.",
                icon = Icons.Outlined.Info,
            )
    }

    if (recomendacoesTexto.contains("5GHz", ignoreCase = true)) {
        cards +=
            RecommendationUi(
                title = "Use a rede de 5 GHz",
                body = "Seu dispositivo está conectado à rede de 2,4 GHz. Quando estiver próximo ao roteador, a rede de 5 GHz normalmente oferece menor atraso e menos interferência.",
                icon = Icons.Outlined.Wifi,
            )
    }

    if (recomendacoesTexto.contains("Aproxime-se do roteador", ignoreCase = true)) {
        cards +=
            RecommendationUi(
                title = "Aproxime-se do roteador",
                body = "O sinal atual pode aumentar a variação da conexão durante a partida, principalmente em locais com paredes ou interferência.",
                icon = Icons.Outlined.Wifi,
            )
    }

    return cards
}

private fun verdictTone(
    nivel: NivelResultado,
    c: LkTokens,
): VerdictTone =
    when (nivel) {
        NivelResultado.EXCELENTE, NivelResultado.BOA ->
            VerdictTone(
                container = c.successContainer,
                onContainer = c.onSuccessContainer,
                icon = Icons.Outlined.CheckCircle,
            )

        NivelResultado.ATENCAO ->
            VerdictTone(
                container = c.warningContainer,
                onContainer = c.onWarningContainer,
                icon = Icons.Outlined.WarningAmber,
            )

        NivelResultado.RUIM ->
            VerdictTone(
                container = c.errorContainer,
                onContainer = c.onErrorContainer,
                icon = Icons.Outlined.WarningAmber,
            )
    }

private fun Double.formatPercent(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format("%.1f", this).replace(',', '.')
    }

private fun conexaoLabel(estado: EstadoConexao): String =
    when (estado) {
        EstadoConexao.wifi -> "Wi-Fi"
        EstadoConexao.movel -> "Rede móvel"
        EstadoConexao.ethernet -> "Cabo (Ethernet)"
        EstadoConexao.desconectado -> "Desconectado"
        EstadoConexao.desconhecido -> "Desconhecido"
    }
