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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.signallq.app.BuildConfig
import io.signallq.app.core.diagnostico.CatalogoJogosModoGamer
import io.signallq.app.core.diagnostico.CategoriaJogoModoGamer
import io.signallq.app.core.diagnostico.DeviceJogo
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.JogoCatalogoModoGamer
import io.signallq.app.modogamer.ModoGamerEtapa
import io.signallq.app.modogamer.ModoGamerViewModel
import io.signallq.app.modogamer.SelecaoJogoModoGamer
import io.signallq.app.modogamer.icone
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import kotlinx.coroutines.launch

/**
 * Modo gamer — Feature #550, issue #1476, fundido com o fluxo legado "Jogos" (GH#935) pela
 * issue #1487 (único fluxo "modo gamer" do app, `Overlay.Jogos` removida). Fluxo de 3 etapas
 * (jogo → device → salvar como padrão/usar uma vez) + resultado, protótipo #1474
 * (`ModoGamerJogo`/`ModoGamerDevice`/`ModoGamerConfig`/`ModoGamerResultado`, issue #1483). O
 * resultado reaproveita o motor ([io.signallq.app.core.diagnostico.ModoGamerEngine]) e o
 * container visual "Medido pelo motor SignallQ"/"Explicado por IA" já usados em
 * [DiagnosticoGuiadoScreen] — ver [ModoGamerResultadoConteudo].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModoGamerScreen(
    input: DiagnosticInput?,
    padraoInicial: Pair<SelecaoJogoModoGamer, DeviceJogo>?,
    analisadorState: AnalisadorState,
    onAnalisarProblema: (String?) -> Unit,
    onResetarAnalisador: () -> Unit,
    onSalvarPadrao: suspend (jogoId: String?, categoriaFallback: String?, deviceId: String) -> Unit,
    onVoltar: () -> Unit,
    onIrParaHome: () -> Unit,
    /** Toggle remoto (Firebase Remote Config) + gate de consentimento UMP -- issue #555,
     *  reconectado do fluxo legado "Jogos" (GH#935) pela issue #1489. Default `false`: nunca
     *  mostra anuncio sem sinal explicito de que pode. */
    adsEnabled: Boolean = false,
) {
    val c = LocalLkTokens.current
    val scope = rememberCoroutineScope()
    val inputAtualizado = rememberUpdatedState(input)

    val viewModel =
        remember {
            ModoGamerViewModel(
                padraoInicial = padraoInicial,
                inputAtual = { inputAtualizado.value },
                onSalvarPadrao = onSalvarPadrao,
            )
        }
    val etapa by viewModel.etapa.collectAsState()

    fun voltarUmPasso() {
        when (val atual = etapa) {
            is ModoGamerEtapa.SelecaoJogo -> onVoltar()
            is ModoGamerEtapa.SelecaoDevice -> viewModel.voltarParaSelecaoJogo()
            is ModoGamerEtapa.Medindo -> viewModel.voltarParaSelecaoDevice()
            is ModoGamerEtapa.Resultado -> if (atual.salvoComoPadrao) onVoltar() else viewModel.voltarParaSelecaoDevice()
        }
    }

    BackHandler(onBack = ::voltarUmPasso)

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (etapa is ModoGamerEtapa.Resultado) "Resultado para o jogo" else "Analisar jogos online",
                        style = MaterialTheme.typography.titleLarge,
                        color = c.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::voltarUmPasso) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = c.textPrimary)
                    }
                },
            )
        },
    ) { padding ->
        when (val etapaAtual = etapa) {
            is ModoGamerEtapa.SelecaoJogo ->
                ModoGamerSelecaoJogoConteudo(
                    modifier = Modifier.padding(padding),
                    busca = etapaAtual.busca,
                    onBuscar = viewModel::buscar,
                    onSelecionarJogo = viewModel::selecionarJogo,
                    onSelecionarCategoriaFallback = viewModel::selecionarCategoriaFallback,
                )
            is ModoGamerEtapa.SelecaoDevice ->
                ModoGamerSelecaoDeviceConteudo(
                    modifier = Modifier.padding(padding),
                    nomeJogo = etapaAtual.selecaoJogo.nomeExibido,
                    onSelecionarDevice = viewModel::selecionarDevice,
                )
            is ModoGamerEtapa.Medindo -> {
                ModoGamerMedindoConteudo(
                    modifier = Modifier.padding(padding),
                    selecaoJogo = etapaAtual.selecaoJogo,
                    device = etapaAtual.device,
                    probeUrl = etapaAtual.selecaoJogo.specificProbeHost ?: BuildConfig.GAME_LATENCY_PROBE_URL,
                    onConcluido = { medicao ->
                        scope.launch { viewModel.confirmarMedicao(medicao?.latenciaMs, medicao?.jitterMs, medicao?.perdaPercentual, medicao?.natUdp) }
                    },
                )
            }
            is ModoGamerEtapa.Resultado -> {
                LaunchedEffect(etapaAtual.selecaoJogo, etapaAtual.device) {
                    onResetarAnalisador()
                    onAnalisarProblema("Modo gamer: ${etapaAtual.selecaoJogo.nomeExibido} em ${etapaAtual.device.label}")
                }
                ModoGamerResultadoConteudo(
                    modifier = Modifier.padding(padding),
                    etapa = etapaAtual,
                    analisadorState = analisadorState,
                    onAlternarSalvarPadrao = { salvar ->
                        scope.launch { viewModel.alternarSalvarPadrao(salvar) }
                    },
                    onTrocarJogoOuDevice = viewModel::trocarJogoOuDevice,
                    onIrParaHome = onIrParaHome,
                    adsEnabled = adsEnabled,
                )
            }
        }
    }
}

@Composable
private fun ModoGamerSelecaoJogoConteudo(
    modifier: Modifier = Modifier,
    busca: String,
    onBuscar: (String) -> Unit,
    onSelecionarJogo: (JogoCatalogoModoGamer) -> Unit,
    onSelecionarCategoriaFallback: (CategoriaJogoModoGamer) -> Unit,
) {
    val c = LocalLkTokens.current
    var mostrarCategorias by remember { mutableStateOf(false) }

    if (mostrarCategorias) {
        ModoGamerSelecaoCategoriaFallback(
            modifier = modifier,
            onSelecionar = onSelecionarCategoriaFallback,
            onVoltar = { mostrarCategorias = false },
        )
        return
    }

    val jogosFiltrados = remember(busca) { CatalogoJogosModoGamer.jogos.filter { it.nome.contains(busca, ignoreCase = true) } }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg)) {
            Text(
                text = "Como o jogo deve responder?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                text = "Escolha um jogo para testar a rota mais relevante.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(LkSpacing.md))
            OutlinedTextField(
                value = busca,
                onValueChange = onBuscar,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar um jogo…") },
                leadingIcon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = c.textTertiary) },
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
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = LkSpacing.xl, end = LkSpacing.xl, bottom = LkSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            items(jogosFiltrados, key = { it.gameId }) { jogo ->
                ModoGamerListItem(titulo = jogo.nome, subtitulo = jogo.categoria.label, icone = jogo.categoria.icone(), onClick = { onSelecionarJogo(jogo) })
            }
        }
        Column(modifier = Modifier.padding(horizontal = LkSpacing.xl, vertical = LkSpacing.md)) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.card))
                        .background(c.bgSecondary)
                        .clickable { mostrarCategorias = true }
                        .padding(LkSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Meu jogo não está na lista", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = c.textPrimary)
                    Text(text = "Escolha o tipo de jogo mais parecido", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                }
                Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = c.textTertiary)
            }
        }
    }
}

@Composable
private fun ModoGamerSelecaoCategoriaFallback(
    modifier: Modifier = Modifier,
    onSelecionar: (CategoriaJogoModoGamer) -> Unit,
    onVoltar: () -> Unit,
) {
    val c = LocalLkTokens.current
    BackHandler(onBack = onVoltar)
    Column(modifier = modifier.fillMaxSize().padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg)) {
        Text(
            text = "Seu jogo não está na lista? Escolha a categoria mais parecida. O resultado usa o perfil de referência dela.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(LkSpacing.lg))
        CategoriaJogoModoGamer.entries.forEach { categoria ->
            ModoGamerListItem(titulo = categoria.label, subtitulo = null, icone = categoria.icone(), onClick = { onSelecionar(categoria) })
            Spacer(Modifier.height(LkSpacing.sm))
        }
    }
}

@Composable
private fun ModoGamerSelecaoDeviceConteudo(
    modifier: Modifier = Modifier,
    nomeJogo: String,
    onSelecionarDevice: (DeviceJogo) -> Unit,
) {
    val c = LocalLkTokens.current
    Column(modifier = modifier.fillMaxSize().padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg)) {
        Text(
            text = "Em qual aparelho você joga $nomeJogo?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.W600,
            color = c.textPrimary,
        )
        Spacer(Modifier.height(LkSpacing.lg))
        DeviceJogo.entries.forEach { device ->
            ModoGamerListItem(titulo = device.label, subtitulo = null, icone = device.icone(), onClick = { onSelecionarDevice(device) })
            Spacer(Modifier.height(LkSpacing.sm))
        }
    }
}

@Composable
internal fun ModoGamerListItem(
    titulo: String,
    subtitulo: String?,
    icone: ImageVector,
    onClick: () -> Unit,
) {
    val c = LocalLkTokens.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.surfaceContainer)
                .clickable(onClick = onClick)
                .padding(LkSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(c.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icone, contentDescription = null, tint = c.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(LkSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = c.textPrimary)
            if (subtitulo != null) {
                Text(text = subtitulo, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        }
        Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = c.textTertiary)
    }
}
