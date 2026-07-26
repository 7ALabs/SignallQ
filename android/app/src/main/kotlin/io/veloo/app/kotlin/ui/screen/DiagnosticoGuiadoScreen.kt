package io.signallq.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.DiagnosticoGuiadoEngine
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.core.diagnostico.PerguntaFechada
import io.signallq.app.core.diagnostico.PerguntasDiagnosticoGuiado
import io.signallq.app.core.diagnostico.ResultadoDiagnosticoGuiado
import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.core.recommendation.RecommendationFeedbackType
import io.signallq.app.core.recommendation.RecommendationType
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.ResolvedOperadoraContact
import io.signallq.app.ui.ResolvedOperadoraIdentity
import io.signallq.app.ui.component.LkSectionOverline
import io.signallq.app.ui.component.OperadoraBadge
import io.signallq.app.ui.component.OperadoraBottomSheet
import io.signallq.app.ui.component.corSemantica
import io.signallq.app.ui.component.rememberResolvedOperadoraContact
import io.signallq.app.ui.component.rememberResolvedOperadoraIdentity

/**
 * Diagnóstico guiado por objetivo — Feature #550, issue #1475. 7 objetivos fechados,
 * cada um com um roteiro próprio de perguntas fechadas (nunca chat livre) e um
 * resultado que separa visualmente o que o motor local mede/decide
 * ([DiagnosticoGuiadoEngine], 100% determinístico) do que a IA só explica em prosa
 * ([AnalisadorState], mesmo mecanismo já usado no resto do app — nunca decide status,
 * nunca inventa evidência, nunca sugere compra sem recorrência).
 *
 * Substitui a antiga sheet automática "Análise detalhada"
 * (`DiagnosticoDetalhadoSheet`, retirada nesta issue): o banner de veredito e a
 * recomendação da IA deixam de abrir sozinhos ao entrar no resultado do teste — só
 * aparecem depois que o usuário escolhe um objetivo aqui (decisão do Luiz, comentário
 * de #1474 em 2026-07-26). Protótipo: `diagnostico-guiado.jsx` (#1483).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticoGuiadoScreen(
    input: DiagnosticInput?,
    resultadoValidoParaConclusao: Boolean,
    analisadorState: AnalisadorState,
    onAnalisarProblema: (String?) -> Unit,
    onResetarAnalisador: () -> Unit,
    onVoltar: () -> Unit,
    onIrParaHome: () -> Unit,
    categoria: String?,
    /** Só o nome bruto do ISP é usado aqui (resolução de identidade/contato de
     *  operadora) — não o [io.signallq.app.ui.IspInfo] inteiro. */
    ispNome: String?,
    connectionType: String?,
    operadoraMovel: String?,
    recommendationDecision: RecommendationDecision?,
    recommendationFeedback: RecommendationFeedbackType?,
    onRecommendationShown: () -> Unit,
    onRecommendationClicked: () -> Unit,
    onRecommendationFeedback: (RecommendationFeedbackType) -> Unit,
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity?,
    resolveOperadoraContatoLocal: (String?, Boolean) -> ResolvedOperadoraContact?,
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity,
    resolveOperadoraContatoRemoto: suspend (String?, Boolean) -> ResolvedOperadoraContact,
    /** Só #1476 (Modo gamer) preenche este callback — enquanto `null`, o botão "Ver
     *  diagnóstico por jogo" (mostrado só para [ObjetivoDiagnostico.JOGOS_COM_LAG])
     *  não aparece. Mantém o contrato pronto sem implementar o fluxo de jogo aqui. */
    onIniciarModoGamer: (() -> Unit)? = null,
) {
    val c = LocalLkTokens.current
    var objetivo by remember { mutableStateOf<ObjetivoDiagnostico?>(null) }
    var passo by remember { mutableIntStateOf(0) }
    var respostas by remember { mutableStateOf<List<Int?>>(emptyList()) }
    var mostrarResultado by remember { mutableStateOf(false) }

    fun voltarUmPasso() {
        when {
            mostrarResultado -> {
                mostrarResultado = false
                onResetarAnalisador()
            }
            objetivo != null && passo > 0 -> passo -= 1
            objetivo != null -> {
                objetivo = null
                passo = 0
                respostas = emptyList()
            }
            else -> onVoltar()
        }
    }

    BackHandler(onBack = ::voltarUmPasso)

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text =
                            when {
                                mostrarResultado -> "Resultado do diagnóstico"
                                objetivo != null -> objetivo!!.titulo
                                else -> "Diagnóstico guiado"
                            },
                        style = MaterialTheme.typography.titleLarge,
                        color = c.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::voltarUmPasso) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = c.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        val objetivoAtual = objetivo
        when {
            !resultadoValidoParaConclusao ->
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(LkSpacing.xl)) {
                    ResultadoInvalidoBannerGuiado(c = c)
                }
            objetivoAtual == null ->
                ListaObjetivos(
                    modifier = Modifier.padding(padding),
                    onSelect = { objetivo = it },
                    c = c,
                )
            mostrarResultado -> {
                val respostasIndices = respostas.filterNotNull()
                val resultado =
                    remember(objetivoAtual, respostasIndices, input) {
                        DiagnosticoGuiadoEngine.avaliar(objetivoAtual, respostasIndices, input)
                    }
                LaunchedEffect(objetivoAtual) {
                    onResetarAnalisador()
                    onAnalisarProblema(objetivoAtual.titulo)
                }
                ResultadoDiagnosticoGuiadoConteudo(
                    modifier = Modifier.padding(padding),
                    resultado = resultado,
                    analisadorState = analisadorState,
                    onEscolherOutraSituacao = {
                        mostrarResultado = false
                        objetivo = null
                        passo = 0
                        respostas = emptyList()
                        onResetarAnalisador()
                    },
                    onIrParaHome = onIrParaHome,
                    categoria = categoria,
                    ispNome = ispNome,
                    connectionType = connectionType,
                    operadoraMovel = operadoraMovel,
                    recommendationDecision = recommendationDecision,
                    recommendationFeedback = recommendationFeedback,
                    onRecommendationShown = onRecommendationShown,
                    onRecommendationClicked = onRecommendationClicked,
                    onRecommendationFeedback = onRecommendationFeedback,
                    resolveOperadoraIdentidadeLocal = resolveOperadoraIdentidadeLocal,
                    resolveOperadoraContatoLocal = resolveOperadoraContatoLocal,
                    resolveOperadoraIdentidadeRemota = resolveOperadoraIdentidadeRemota,
                    resolveOperadoraContatoRemoto = resolveOperadoraContatoRemoto,
                    onIniciarModoGamer = onIniciarModoGamer,
                    c = c,
                )
            }
            else -> {
                val perguntas = remember(objetivoAtual) { PerguntasDiagnosticoGuiado.perguntas(objetivoAtual) }
                val pergunta = perguntas[passo]
                PerguntaFechadaConteudo(
                    modifier = Modifier.padding(padding),
                    pergunta = pergunta,
                    passo = passo,
                    total = perguntas.size,
                    respostaSelecionada = respostas.getOrNull(passo),
                    onEscolher = { opcaoIndex ->
                        respostas =
                            respostas.toMutableList().apply {
                                while (size <= passo) add(null)
                                this[passo] = opcaoIndex
                            }
                    },
                    onAvancar = {
                        if (passo < perguntas.size - 1) passo += 1 else mostrarResultado = true
                    },
                    c = c,
                )
            }
        }
    }
}

private fun ObjetivoDiagnostico.icone(): ImageVector =
    when (this) {
        ObjetivoDiagnostico.INTERNET_CAI_OSCILA -> Icons.Outlined.WifiOff
        ObjetivoDiagnostico.VIDEOS_TRAVAM -> Icons.Outlined.Tv
        ObjetivoDiagnostico.JOGOS_COM_LAG -> Icons.Outlined.SportsEsports
        ObjetivoDiagnostico.CHAMADAS_CONGELAM -> Icons.Outlined.Videocam
        ObjetivoDiagnostico.SITES_DEMORAM -> Icons.Outlined.Language
        ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA -> Icons.Outlined.Speed
        ObjetivoDiagnostico.WIFI_VS_OPERADORA -> Icons.Outlined.CompareArrows
    }

@Composable
private fun ListaObjetivos(
    modifier: Modifier = Modifier,
    onSelect: (ObjetivoDiagnostico) -> Unit,
    c: LkTokens,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(c.bgPrimary)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
    ) {
        Text(
            text = "Qual dessas situações mais parece com o que você está vivendo?",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(LkSpacing.lg))
        ObjetivoDiagnostico.entries.forEach { objetivo ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.card))
                        .background(c.bgPrimary)
                        .clickable { onSelect(objetivo) }
                        .padding(LkSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.primary.copy(alpha = 0.1f)),
                ) {
                    Icon(
                        imageVector = objetivo.icone(),
                        contentDescription = null,
                        tint = c.primary,
                        modifier = Modifier.padding(9.dp),
                    )
                }
                Spacer(Modifier.width(LkSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = objetivo.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = objetivo.subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = c.textTertiary,
                )
            }
            Spacer(Modifier.height(LkSpacing.sm))
        }
    }
}

@Composable
private fun PerguntaFechadaConteudo(
    modifier: Modifier = Modifier,
    pergunta: PerguntaFechada,
    passo: Int,
    total: Int,
    respostaSelecionada: Int?,
    onEscolher: (Int) -> Unit,
    onAvancar: () -> Unit,
    c: LkTokens,
) {
    Column(modifier = modifier.fillMaxSize().background(c.bgPrimary)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = LkSpacing.xl, vertical = LkSpacing.sm)) {
            repeat(total) { i ->
                Spacer(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (i <= passo) c.primary else c.bgSecondary),
                )
                if (i < total - 1) Spacer(Modifier.width(6.dp))
            }
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
        ) {
            Text(
                text = pergunta.texto,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(LkSpacing.lg))
            pergunta.opcoes.forEachIndexed { index, opcao ->
                val selecionada = respostaSelecionada == index
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(LkRadius.card))
                            .background(if (selecionada) c.primary.copy(alpha = 0.08f) else c.bgPrimary)
                            .clickable { onEscolher(index) }
                            .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (selecionada) c.primary else Color.Transparent),
                    ) {
                        if (selecionada) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = c.onPrimary,
                                modifier = Modifier.padding(3.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        text = opcao,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selecionada) FontWeight.W600 else FontWeight.Normal,
                        color = c.textPrimary,
                    )
                }
                Spacer(Modifier.height(LkSpacing.xs))
            }
        }
        Column(modifier = Modifier.padding(horizontal = LkSpacing.xl, vertical = LkSpacing.md).navigationBarsPadding()) {
            Button(
                onClick = onAvancar,
                enabled = respostaSelecionada != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
            ) {
                Text(
                    text = if (passo < total - 1) "Avançar" else "Ver resultado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ResultadoDiagnosticoGuiadoConteudo(
    modifier: Modifier = Modifier,
    resultado: ResultadoDiagnosticoGuiado,
    analisadorState: AnalisadorState,
    onEscolherOutraSituacao: () -> Unit,
    onIrParaHome: () -> Unit,
    categoria: String?,
    ispNome: String?,
    connectionType: String?,
    operadoraMovel: String?,
    recommendationDecision: RecommendationDecision?,
    recommendationFeedback: RecommendationFeedbackType?,
    onRecommendationShown: () -> Unit,
    onRecommendationClicked: () -> Unit,
    onRecommendationFeedback: (RecommendationFeedbackType) -> Unit,
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity?,
    resolveOperadoraContatoLocal: (String?, Boolean) -> ResolvedOperadoraContact?,
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity,
    resolveOperadoraContatoRemoto: suspend (String?, Boolean) -> ResolvedOperadoraContact,
    onIniciarModoGamer: (() -> Unit)?,
    c: LkTokens,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(c.bgPrimary)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
    ) {
        DiagnosticoGuiadoStatusBanner(status = resultado.status, mensagem = resultado.mensagemMotor, c = c)

        Spacer(Modifier.height(LkSpacing.lg))
        AiVsMotorExplainer(resultado = resultado, analisadorState = analisadorState, c = c)

        if (resultado.acoes.isNotEmpty()) {
            Spacer(Modifier.height(LkSpacing.lg))
            LkSectionOverline(text = "Ações recomendadas")
            Spacer(Modifier.height(LkSpacing.sm))
            AcoesRecomendadasCard(acoes = resultado.acoes, c = c)
        }

        val mostrarContato = categoria == "isp" || categoria == "fibra"
        var showOperadoraSheet by remember { mutableStateOf(false) }
        if (mostrarContato) {
            Spacer(Modifier.height(LkSpacing.lg))
            val identidade =
                rememberResolvedOperadoraIdentity(
                    ispNomeBruto = ispNome,
                    viaMovel = false,
                    resolveLocal = resolveOperadoraIdentidadeLocal,
                    resolveRemoteOrFallback = resolveOperadoraIdentidadeRemota,
                )
            val contato =
                rememberResolvedOperadoraContact(
                    ispNomeBruto = ispNome,
                    viaMovel = false,
                    resolveLocal = resolveOperadoraContatoLocal,
                    resolveRemoteOrFallback = resolveOperadoraContatoRemoto,
                )
            OperadoraResumoCardGuiado(identidade = identidade, contato = contato, c = c)
            Spacer(Modifier.height(LkSpacing.xs))
            OutlinedButton(
                onClick = { showOperadoraSheet = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Icon(imageVector = Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(LkSpacing.sm))
                Text(text = "Falar com a operadora", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W600)
            }
            if (showOperadoraSheet) {
                OperadoraBottomSheet(
                    connectionType = connectionType,
                    ispNome = ispNome,
                    operadoraMovel = operadoraMovel,
                    onDismiss = { showOperadoraSheet = false },
                    resolveOperadoraIdentidadeLocal = resolveOperadoraIdentidadeLocal,
                    resolveOperadoraContatoLocal = resolveOperadoraContatoLocal,
                    resolveOperadoraIdentidadeRemota = resolveOperadoraIdentidadeRemota,
                    resolveOperadoraContatoRemoto = resolveOperadoraContatoRemoto,
                )
            }
        }

        if (recommendationDecision != null) {
            Spacer(Modifier.height(LkSpacing.lg))
            LkSectionOverline(text = "Configurações")
            Spacer(Modifier.height(LkSpacing.sm))
            RecommendationEngineCardGuiado(
                decision = recommendationDecision,
                feedback = recommendationFeedback,
                onShown = onRecommendationShown,
                onClicked = onRecommendationClicked,
                onFeedback = onRecommendationFeedback,
                c = c,
            )
        }

        if (resultado.objetivo == ObjetivoDiagnostico.JOGOS_COM_LAG && onIniciarModoGamer != null) {
            Spacer(Modifier.height(LkSpacing.lg))
            OutlinedButton(
                onClick = onIniciarModoGamer,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Icon(imageVector = Icons.Outlined.SportsEsports, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(LkSpacing.sm))
                Text(
                    text = "Ver diagnóstico por jogo (Modo gamer)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W600,
                )
            }
        }

        Spacer(Modifier.height(LkSpacing.lg))
        OutlinedButton(
            onClick = onEscolherOutraSituacao,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
        ) {
            Text(text = "Escolher outra situação", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        }
        Spacer(Modifier.height(LkSpacing.sm))
        TextButton(onClick = onIrParaHome, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Ir para o início", style = MaterialTheme.typography.bodyMedium, color = c.primary)
        }
        Spacer(Modifier.height(LkSpacing.xl))
    }
}

@Composable
private fun DiagnosticoGuiadoStatusBanner(
    status: DiagnosticStatus,
    mensagem: String,
    c: LkTokens,
) {
    val positivo = status == DiagnosticStatus.ok || status == DiagnosticStatus.info
    val containerColor = if (positivo) c.successContainer else c.errorContainer
    val contentColor = if (positivo) c.onSuccessContainer else c.onErrorContainer
    val icon = if (positivo) Icons.Outlined.CheckCircle else Icons.Outlined.Error
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(containerColor)
                .padding(LkSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
        Text(text = mensagem, style = MaterialTheme.typography.bodyMedium, color = contentColor)
    }
}

@Composable
private fun ResultadoInvalidoBannerGuiado(c: LkTokens) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.warningContainer)
                .padding(LkSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = c.onWarningContainer, modifier = Modifier.size(22.dp))
        Text(
            text = "Este resultado não é confiável o suficiente para um diagnóstico guiado. Refaça o teste de velocidade mantendo a mesma rede.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.onWarningContainer,
        )
    }
}

/**
 * Separação visual explícita entre o que o motor local mede/decide
 * ([ResultadoDiagnosticoGuiado.evidencias], 100% determinístico) e o que a IA só
 * explica em prosa ([AnalisadorState]) — regra não-negociável da Feature #550
 * (protótipo #1474, `AiVsMotorExplainer`). A IA nunca decide o status: quando ainda
 * está carregando ou falhou, o container de evidências continua mostrando os dados
 * reais normalmente, só o container de IA muda de estado.
 */
@Composable
private fun AiVsMotorExplainer(
    resultado: ResultadoDiagnosticoGuiado,
    analisadorState: AnalisadorState,
    c: LkTokens,
) {
    Column {
        LkSectionOverline(text = "Como chegamos a esse resultado")
        Spacer(Modifier.height(LkSpacing.sm))

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(c.surfaceContainer)
                    .padding(LkSpacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.Verified, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "MEDIDO PELO MOTOR SIGNALLQ",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textSecondary,
                    letterSpacing = 0.5.sp,
                )
            }
            if (resultado.evidencias.isEmpty()) {
                Spacer(Modifier.height(LkSpacing.sm))
                Text(
                    text = "Sem métricas suficientes deste teste para esta situação.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textTertiary,
                )
            } else {
                resultado.evidencias.forEach { evidencia ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = evidencia.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = evidencia.valorExibido,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.W700,
                            color = evidencia.status.corSemantica(c),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(LkSpacing.sm))
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(c.primary.copy(alpha = 0.06f))
                    .padding(LkSpacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.AutoAwesome, contentDescription = null, tint = c.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "EXPLICADO POR IA",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.primary,
                    letterSpacing = 0.5.sp,
                )
            }
            Spacer(Modifier.height(LkSpacing.xs))
            when (analisadorState) {
                is AnalisadorState.Inativo, is AnalisadorState.Analisando ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = c.primary)
                        Text(text = "Preparando a explicação…", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                    }
                is AnalisadorState.Resultado -> {
                    val texto = analisadorState.resumo.ifBlank { analisadorState.texto }
                    Text(text = texto, style = MaterialTheme.typography.bodySmall, color = c.textPrimary, lineHeight = 18.sp)
                }
                is AnalisadorState.Erro ->
                    Text(
                        text = "Não foi possível carregar a explicação da IA — o resultado acima já reflete o motor local.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
            }
        }

        Spacer(Modifier.height(LkSpacing.sm))
        Row(verticalAlignment = Alignment.Top) {
            Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = "A IA só explica o resultado — quem decide o status é sempre o motor de diagnóstico local, com base nos números medidos acima.",
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun AcoesRecomendadasCard(
    acoes: List<String>,
    c: LkTokens,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgSecondary)
                .padding(horizontal = LkSpacing.lg),
    ) {
        acoes.forEachIndexed { index, acao ->
            if (index > 0) {
                androidx.compose.material3.HorizontalDivider(color = c.outlineVariant, thickness = 1.dp)
            }
            Row(modifier = Modifier.padding(vertical = LkSpacing.md), verticalAlignment = Alignment.Top) {
                Icon(imageVector = Icons.Outlined.TaskAlt, contentDescription = null, tint = c.success, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(LkSpacing.sm))
                Text(text = acao, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun OperadoraResumoCardGuiado(
    identidade: ResolvedOperadoraIdentity?,
    contato: ResolvedOperadoraContact?,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgSecondary)
                .padding(LkSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (identidade != null) {
            OperadoraBadge(identidade = identidade, size = 40.dp)
        } else {
            Icon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(LkSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contato?.displayName ?: "Operadora",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Text(text = "Atendimento oficial disponível", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
    }
}

/**
 * Recomendação escolhida pelo Recommendation Engine (`RecommendationEngine.choose`,
 * `coreRecommendation`, issues #790/#811/#812/#813) — reaproveitada aqui sem
 * duplicar lógica, só relocada: antes aparecia automaticamente ao abrir o resultado
 * do teste, agora só depois que o usuário escolheu um objetivo (issue #1475).
 */
@Composable
private fun RecommendationEngineCardGuiado(
    decision: RecommendationDecision,
    feedback: RecommendationFeedbackType?,
    onShown: () -> Unit,
    onClicked: () -> Unit,
    onFeedback: (RecommendationFeedbackType) -> Unit,
    c: LkTokens,
) {
    LaunchedEffect(decision.trackingId) { onShown() }
    var motivoExpandido by remember(decision.trackingId) { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgSecondary)
                .semantics {
                    role = Role.Button
                    contentDescription = "Recomendação: ${decision.recommendation.title}"
                    stateDescription = if (motivoExpandido) "expandido" else "recolhido"
                }.clickable {
                    if (!motivoExpandido) onClicked()
                    motivoExpandido = !motivoExpandido
                }.padding(LkSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(imageVector = Icons.Outlined.AutoAwesome, contentDescription = null, tint = c.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(LkSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recommendationTypeLabel(decision.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                    letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = decision.recommendation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                    lineHeight = 18.sp,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = c.textTertiary,
                modifier = Modifier.size(20.dp).rotate(if (motivoExpandido) 180f else 0f),
            )
        }

        AnimatedVisibility(visible = motivoExpandido) {
            Text(
                text = decision.reason,
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = LkSpacing.sm),
            )
        }

        Spacer(Modifier.height(LkSpacing.md))
        if (feedback == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
                RecommendationFeedbackButtonGuiado(
                    texto = "Útil",
                    icon = Icons.Outlined.ThumbUp,
                    c = c,
                    onClick = { onFeedback(RecommendationFeedbackType.HELPFUL) },
                )
                RecommendationFeedbackButtonGuiado(
                    texto = "Não útil",
                    icon = Icons.Outlined.ThumbDown,
                    c = c,
                    onClick = { onFeedback(RecommendationFeedbackType.NOT_HELPFUL) },
                )
                RecommendationFeedbackButtonGuiado(
                    texto = "Ocultar",
                    icon = Icons.Outlined.VisibilityOff,
                    c = c,
                    onClick = { onFeedback(RecommendationFeedbackType.HIDE) },
                )
            }
        } else {
            Text(
                text = "Obrigado pelo feedback — isso ajuda a melhorar as próximas recomendações.",
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
            )
        }
    }
}

@Composable
private fun RecommendationFeedbackButtonGuiado(
    texto: String,
    icon: ImageVector,
    c: LkTokens,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(LkRadius.pill),
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        border = androidx.compose.foundation.BorderStroke(1.dp, c.border),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = texto, style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
    }
}

internal fun recommendationTypeLabel(type: RecommendationType): String =
    when (type) {
        RecommendationType.FREE_TIP -> "DICA GRATUITA"
        RecommendationType.TUTORIAL -> "TUTORIAL"
        RecommendationType.CONFIGURATION -> "CONFIGURAÇÃO"
        RecommendationType.AFFILIATE_PRODUCT -> "PRODUTO RECOMENDADO"
        RecommendationType.PARTNER_OFFER -> "OFERTA PARCEIRA"
        RecommendationType.OPERATOR_OFFER -> "OFERTA DA OPERADORA"
        RecommendationType.NATIVE_AD_FALLBACK -> "PUBLICIDADE"
    }
