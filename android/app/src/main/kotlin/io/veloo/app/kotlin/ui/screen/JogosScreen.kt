package io.signallq.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.signallq.app.BuildConfig
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.WifiLinkSnapshot
import io.signallq.app.jogos.CatalogoJogos
import io.signallq.app.jogos.JogoCatalogo
import io.signallq.app.jogos.JogosEtapa
import io.signallq.app.jogos.JogosViewModel
import io.signallq.app.jogos.MENSAGENS_PROGRESSO_JOGOS
import io.signallq.app.jogos.NivelResultado
import io.signallq.app.jogos.Plataforma
import io.signallq.app.jogos.ResultadoTesteJogo
import io.signallq.app.jogos.label
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
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

                is JogosEtapa.Progresso -> EtapaProgresso(mensagemIndex = estadoAtual.mensagemIndex)

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
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(LkSpacing.xl),
    ) {
        Text(
            text = "Escolha a plataforma",
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            Plataforma.entries.forEach { plataforma ->
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = false,
                    onClick = { onSelecionar(plataforma) },
                    label = { Text(plataforma.label) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            containerColor = c.bgCard,
                            labelColor = c.textPrimary,
                        ),
                )
            }
        }
    }
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
        Icon(
            imageVector = Icons.Outlined.SportsEsports,
            contentDescription = null,
            tint = LkColors.accent,
        )
        Spacer(Modifier.width(LkSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = jogo.nome, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
            Text(
                text = jogo.perfil.label(),
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
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
private fun EtapaProgresso(mensagemIndex: Int) {
    val c = LocalLkTokens.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = LkColors.accent)
        Spacer(Modifier.height(LkSpacing.lg))
        Text(
            text = MENSAGENS_PROGRESSO_JOGOS.getOrElse(mensagemIndex) { MENSAGENS_PROGRESSO_JOGOS.last() },
            style = MaterialTheme.typography.bodyLarge,
            color = c.textPrimary,
        )
    }
}

// ── Etapa 5 — Resultado ──────────────────────────────────────────────────

@Composable
private fun EtapaResultado(
    resultado: ResultadoTesteJogo,
    onTestarNovamente: () -> Unit,
    onEscolherOutroJogo: () -> Unit,
) {
    val c = LocalLkTokens.current
    val corVeredito =
        when (resultado.nivel) {
            NivelResultado.EXCELENTE, NivelResultado.BOA -> LkColors.success
            NivelResultado.ATENCAO -> LkColors.warning
            NivelResultado.RUIM -> LkColors.error
        }

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
            Icon(
                imageVector = if (resultado.nivel == NivelResultado.RUIM) Icons.Outlined.WarningAmber else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = corVeredito,
            )
            Spacer(Modifier.height(LkSpacing.sm))
            Text(text = resultado.tituloVeredito, style = MaterialTheme.typography.headlineSmall, color = corVeredito)
            Spacer(Modifier.height(LkSpacing.xs))
            Text(text = resultado.textoVeredito, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)

            Spacer(Modifier.height(LkSpacing.lg))
            HorizontalDivider(color = c.border)
            Spacer(Modifier.height(LkSpacing.md))

            MetricaLinha("Latência", "${resultado.latenciaMs.toInt()}ms", c)
            MetricaLinha("Variação (jitter)", "${resultado.jitterMs.toInt()}ms", c)
            MetricaLinha("Perda de pacotes", "${resultado.perdaPercentual.toInt()}%", c)
            MetricaLinha("Região testada", resultado.regiaoTestada, c)
            MetricaLinha("Tipo de conexão", conexaoLabel(resultado.tipoConexaoAtual), c)

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

            if (resultado.avisos.isNotEmpty()) {
                Spacer(Modifier.height(LkSpacing.lg))
                resultado.avisos.forEach {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                    Spacer(Modifier.height(LkSpacing.xs))
                }
            }
        }

        Spacer(Modifier.height(LkSpacing.lg))

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

@Composable
private fun MetricaLinha(
    rotulo: String,
    valor: String,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = LkSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = rotulo, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        Text(text = valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600, color = c.textPrimary)
    }
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
