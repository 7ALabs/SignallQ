package io.signallq.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NetworkWifi
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import io.signallq.app.core.diagnostico.DiagnosticoGuiadoEngine
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.core.diagnostico.PerguntaFechada
import io.signallq.app.core.diagnostico.PerguntasDiagnosticoGuiado
import io.signallq.app.core.diagnostico.ResultadoDiagnosticoGuiado
import io.signallq.app.core.network.DiagnosticoPlanoIniciado
import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.core.recommendation.RecommendationFeedbackType
import io.signallq.app.core.recommendation.RecommendationType
import io.signallq.app.feature.speedtest.MeasurementStatus
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.ResolvedOperadoraContact
import io.signallq.app.ui.ResolvedOperadoraIdentity
import io.signallq.app.ui.component.AcoesRecomendadasCard
import io.signallq.app.ui.component.AiVsMotorExplainer
import io.signallq.app.ui.component.DiagnosticoStatusBanner
import io.signallq.app.ui.component.LkSectionOverline
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.OperadoraBadge
import io.signallq.app.ui.component.OperadoraBottomSheet
import io.signallq.app.ui.component.rememberResolvedOperadoraContact
import io.signallq.app.ui.component.rememberResolvedOperadoraIdentity
import java.util.UUID

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
    /** GH#1706 — o que o app sabe ao montar o plano (spec §7). Sem isto o fluxo guiado não tinha
     *  como adaptar o plano nem declarar o limite que §8.4 exige. */
    contextoDoPlano: ContextoDoPlano,
    /** GH#1706 — funil, passo 3 da spec §12. */
    onPlanoIniciado: (DiagnosticoPlanoIniciado) -> Unit,
    /** Status real da medição — GH#1705. Era `resultadoValidoParaConclusao: Boolean`, e os 5
     *  valores de `MeasurementStatus` viravam um bit exatamente aqui. `null` = ainda não há
     *  medição. Ver [continuidadeDaMedicao]. */
    statusMedicao: MeasurementStatus?,
    /** GH#1705 / bloqueios B3 e B7 — `false` quando o download foi derrubado pelo nosso rate limit
     *  (429) ou o upload não foi detectado. Nos dois casos o número medido não pode alimentar
     *  conclusão. Ver [continuidadeDaMedicao]. */
    medidasConfiaveis: Boolean,
    /** Rota `Analise` (spec 2.0 §8.5) — GH#1704 parte 4/4. Sem default de propósito: um default
     *  deixaria um caller esquecer de ligar a medição e o fluxo voltaria a depender de um teste
     *  anterior sem que nada quebrasse na compilação. Ver [AnaliseGuiadaContrato]. */
    analise: AnaliseGuiadaContrato,
    /** Pré-seleção vinda do SignallQ Assist (issue #1656) — objetivo já escolhido pelo
     *  usuário antes do teste de velocidade, na tela "O que está acontecendo?". Quando
     *  não nulo, esta tela abre direto no roteiro de perguntas em vez de pedir o
     *  objetivo de novo. Nulo preserva o comportamento anterior (objetivo escolhido
     *  aqui mesmo, fluxo acessado sem passar pelo Assist). */
    objetivoPreSelecionado: ObjetivoDiagnostico? = null,
    /** Resposta da primeira pergunta do roteiro já coletada pelo Assist (índice da
     *  opção) — só relevante quando [objetivoPreSelecionado] tem uma pergunta
     *  contextual (`contextoQueAlteraDiagnostico()` no AssistScreen). Nulo = usuário
     *  responde a primeira pergunta normalmente aqui. */
    respostaPreSelecionadaPasso0: Int? = null,
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
    /** Camada A (issue #1503) — card "próximo passo sugerido" no resultado. Chamado com a
     *  ferramenta mapeada por [ObjetivoDiagnostico.ferramentaSugerida]; quem trata a
     *  navegação de fato (empilhar `Overlay.Ferramentas`) é o AppShell. */
    onAbrirFerramentaSugerida: (TipoFerramenta) -> Unit = {},
) {
    val c = LocalLkTokens.current
    // GH#1704 — a persistência destes quatro estados foi RETIRADA desta fatia, não esquecida.
    // Ver a issue #1704: quatro slots `rememberSaveable` independentes conseguem restaurar
    // incoerentes entre si (objetivo nulo com `passo > 0` estoura em `perguntas[passo]`;
    // `mostrarResultado` verdadeiro com objetivo novo salta o roteiro e avalia com respostas de
    // outra jornada). O invariante "objetivo == null ⇒ passo 0, respostas vazias, sem resultado"
    // hoje vale por construção, porque todo caminho que zera `objetivo` zera os outros três junto.
    //
    // A persistência correta é um saver sobre UM objeto de estado, não sobre quatro campos — o
    // que é exatamente o ViewModel da segunda metade da #1704. Fazer aqui seria criar um remendo
    // que aquela fatia jogaria fora, com risco de diagnóstico errado no intervalo.
    var objetivo by remember { mutableStateOf(objetivoPreSelecionado) }
    var passo by remember { mutableIntStateOf(0) }
    var respostas by
        remember {
            mutableStateOf<List<Int?>>(
                if (objetivoPreSelecionado != null && respostaPreSelecionadaPasso0 != null) {
                    listOf(respostaPreSelecionadaPasso0)
                } else {
                    emptyList()
                },
            )
        }
    var mostrarResultado by remember { mutableStateOf(false) }

    // GH#1704 parte 4/4 — rota `Analise`. Booleano próprio, e não um terceiro valor derivado de
    // `analise.estado`, porque o estado do executor é global: ele fica `Concluida` para o app
    // inteiro depois de qualquer medição. Só este flag diz "a medição em curso pertence a ESTE
    // fluxo", que é o que decide se a conclusão da medição deve avançar a tela.
    var emAnalise by remember { mutableStateOf(false) }

    /**
     * A conclusão (§8.6) só é alcançável com um resultado que o motor aceite. Sem isso o fluxo
     * mede de novo em vez de mostrar o banner de resultado inválido — antes desta fatia o banner
     * substituía a tela inteira já na entrada, e a pessoa não tinha ação nenhuma disponível.
     */
    val continuidade = statusMedicao?.let { continuidadeDaMedicao(it, medidasConfiaveis) }

    // GH#1706 — o plano só existe depois de haver objetivo; antes disso não há o que verificar.
    val plano = objetivo?.let { montarPlano(it, contextoDoPlano, respostas) }

    // Correlaciona os eventos desta jornada. `rememberSaveable` para o id sobreviver à recriação
    // da tela — trocar de id no meio quebraria a correlação, que é a única coisa que ele faz.
    //
    // Ressalva honesta: os eventos do Assist (`diagnostico_objetivo_selecionado`) usam outra
    // origem de id, então a correlação ponta a ponta do funil AINDA não fecha. Fica declarado.
    val analiseId = rememberSaveable { UUID.randomUUID().toString() }

    // Passo 3 do funil: dispara quando o plano é exibido, uma vez por (objetivo, plano). A chave
    // inclui o plano porque uma mudança de permissão no meio da jornada muda o que foi exibido —
    // e não dispara em recomposição sem mudança, que a spec §12 também exclui.
    LaunchedEffect(emAnalise, plano) {
        val planoAtual = plano
        val objetivoAtualParaFunil = objetivo
        if (emAnalise && planoAtual != null && objetivoAtualParaFunil != null) {
            onPlanoIniciado(
                DiagnosticoPlanoIniciado(
                    analiseId = analiseId,
                    objetivoId = objetivoAtualParaFunil.name,
                    capacidades = planoAtual.idsParaTelemetria,
                    qtdCapacidades = planoAtual.capacidades.size.toLong(),
                    planoAdaptado = planoAtual.adaptado,
                ),
            )
        }
    }

    // Só medição COMPLETA dispensa medir de novo — bloqueio B2 de Caio na PR #1723.
    //
    // A primeira versão desta fatia afrouxou para "basta existir medição", argumentando que senão o
    // app remediria em silêncio. O argumento estava errado: quem leva à conclusão depois de
    // `remedirPelaAnalise()` é o `LaunchedEffect` abaixo, que já ignora validade de propósito.
    // `podeConcluirSemMedir` governa só a PRIMEIRA chegada ao fim do roteiro — e ali o resultado
    // pode ser de outra sessão, de dias atrás. Caio mediu: com um `CONTAMINATED` guardado, a pessoa
    // respondia o roteiro inteiro e recebia "sua rede mudou durante a medição" sobre uma medição que
    // ela não fez, sem nenhuma medição nova. Agora mede uma vez; se o resultado NOVO ainda não for
    // completo, aí sim a continuidade aparece com o botão, sobre dado fresco.
    //
    // Pela propriedade canônica (`liberaConclusaoCompleta`), não por comparação solta com COMPLETE:
    // ela é a barreira declarada de "só isto libera diagnóstico conclusivo, IA, Recommendation
    // Engine e contato com operadora", e a versão anterior desta fatia a tinha deixado sem nenhum
    // consumidor de produção (ressalva RS5).
    val podeConcluirSemMedir =
        analise.estado is EstadoAnaliseGuiada.Concluida &&
            statusMedicao?.liberaConclusaoCompleta == true

    // "Já vi esta análise sair do estado concluído" — sem isso há uma corrida real, encontrada
    // pelo teste `medicao concluida porem invalida remede em vez de concluir`.
    //
    // Entrar na rota com `Concluida` é o caso de um resultado anterior que o motor recusou: o
    // fluxo pede uma medição nova, mas o `ExecutorSpeedtest` só publica `executando` alguns frames
    // depois. Nessa janela o snapshot ainda é o `Concluida` velho, e a versão anterior deste efeito
    // concluía imediatamente — a pessoa clicava "ver o que identifiquei", nada era medido, e ela
    // recebia o banner do mesmo resultado inválido que motivou a remedição.
    var medicaoObservadaEmCurso by remember { mutableStateOf(false) }

    // A medição terminou enquanto ESTE fluxo a esperava: avança para a conclusão. A validade do
    // resultado não entra aqui de propósito — medição concluída com resultado insuficiente leva ao
    // banner de §8.6, que explica o limite. Fingir que ainda está medindo seria pior.
    LaunchedEffect(emAnalise, analise.estado) {
        when {
            !emAnalise -> medicaoObservadaEmCurso = false
            analise.estado !is EstadoAnaliseGuiada.Concluida -> medicaoObservadaEmCurso = true
            medicaoObservadaEmCurso -> {
                emAnalise = false
                mostrarResultado = true
            }
        }
    }

    // GH#1705 — o CTA de toda continuidade cai aqui: volta para a rota `Analise` (§8.5) e mede de
    // novo dentro do próprio fluxo. Não manda a pessoa para outra tela, que era o que o
    // `onMedirNovamente` do estado vazio fazia antes da #1704.
    fun remedirPelaAnalise() {
        mostrarResultado = false
        emAnalise = true
        analise.onIniciar()
    }

    fun voltarUmPasso() {
        when {
            mostrarResultado -> {
                mostrarResultado = false
                onResetarAnalisador()
            }
            emAnalise -> {
                emAnalise = false
                analise.onCancelar()
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
                                mostrarResultado -> "O que identifiquei"
                                objetivo != null -> objetivo!!.titulo
                                else -> "Vamos descobrir o que está acontecendo"
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
            // GH#1704 — o banner de resultado inválido saiu daqui (era a PRIMEIRA cláusula, e por
            // isso substituía a tela inteira desde a entrada, inclusive a escolha do objetivo).
            // Agora ele é o que a conclusão mostra quando a medição fecha sem resultado que o
            // motor aceite — o único momento em que a informação é acionável.
            objetivoAtual == null ->
                ListaObjetivos(
                    modifier = Modifier.padding(padding),
                    onSelect = { objetivo = it },
                    c = c,
                )
            // GH#1705 — sem conclusão possível, a tela é a continuidade COM ação, não um banner
            // mudo. `permiteVerConclusaoParcial` decide entre substituir e acompanhar o resultado.
            mostrarResultado && continuidade != null && !continuidade.permiteVerConclusaoParcial ->
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(LkSpacing.xl)) {
                    ContinuidadeMedicaoSection(
                        continuidade = continuidade,
                        onAgir = ::remedirPelaAnalise,
                        c = c,
                    )
                }
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
                    // Conclusão parcial: mostra o que deu para apurar E o que falta, na ordem que a
                    // spec §9 pede. Como cabeçalho do conteúdo que já rola — ver o KDoc do parâmetro.
                    cabecalho =
                        continuidade?.let {
                            {
                                ContinuidadeMedicaoSection(
                                    continuidade = it,
                                    onAgir = ::remedirPelaAnalise,
                                    c = c,
                                )
                            }
                        },
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
                    onAbrirFerramentaSugerida = onAbrirFerramentaSugerida,
                    c = c,
                )
            }
            emAnalise ->
                DiagnosticoGuiadoAnaliseSection(
                    modifier = Modifier.padding(padding),
                    estado = analise.estado,
                    onCancelar = ::voltarUmPasso,
                    onTentarNovamente = analise.onIniciar,
                    plano = plano,
                )
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
                        when {
                            passo < perguntas.size - 1 -> passo += 1
                            // GH#1704 — o fim do roteiro leva à análise (§8.5), não direto à
                            // conclusão. Só pula a medição quando já existe resultado utilizável,
                            // que é o caminho de quem entrou aqui vindo da tela de Resultado.
                            podeConcluirSemMedir -> mostrarResultado = true
                            else -> {
                                emAnalise = true
                                analise.onIniciar()
                            }
                        }
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
            text = "O que está acontecendo com sua internet?",
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
                            .background(if (selecionada) c.primary.copy(alpha = 0.08f) else c.bgCard)
                            .clickable { onEscolher(index) }
                            .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .then(
                                    if (selecionada) {
                                        Modifier
                                    } else {
                                        Modifier.border(1.5.dp, c.border, CircleShape)
                                    },
                                ).background(if (selecionada) c.primary else Color.Transparent),
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
                    text = if (passo < total - 1) "Continuar" else "Ver o que identifiquei",
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
    onAbrirFerramentaSugerida: (TipoFerramenta) -> Unit,
    c: LkTokens,
    /** GH#1705 — slot para a continuidade da medição. Precisa entrar AQUI, dentro do `Column` que
     *  já rola, e não como segundo filho do slot do `Scaffold`: o `ScaffoldLayout` do Material3
     *  posiciona todos os placeables do corpo em (0,0), então dois filhos-raiz se **sobrepõem** em
     *  vez de empilhar. A primeira versão desta fatia fazia isso e escondia o veredito atrás da
     *  top bar e as duas primeiras métricas embaixo do banner opaco — bloqueio B1 de Caio na PR
     *  #1723, medido por bounds na árvore semântica. */
    cabecalho: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(c.bgPrimary)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
    ) {
        if (cabecalho != null) {
            cabecalho()
            Spacer(Modifier.height(LkSpacing.lg))
        }
        DiagnosticoStatusBanner(status = resultado.status, mensagem = resultado.mensagemMotor, c = c)

        Spacer(Modifier.height(LkSpacing.lg))
        AiVsMotorExplainer(evidencias = resultado.evidencias, analisadorState = analisadorState, c = c)

        if (resultado.acoes.isNotEmpty()) {
            Spacer(Modifier.height(LkSpacing.lg))
            LkSectionOverline(text = "Ações recomendadas")
            Spacer(Modifier.height(LkSpacing.sm))
            AcoesRecomendadasCard(acoes = resultado.acoes, c = c)
        }

        // Camada A (issue #1503) — card "próximo passo sugerido", mapeado por objetivo
        // (ver ferramentaSugerida() em TipoFerramenta.kt). Nunca aponta pras 8 ferramentas
        // de uma vez, só a mais relevante pro objetivo escolhido — e alguns objetivos
        // (vídeos travam, jogos com lag, chamadas congelam) não têm mapeamento forte o
        // bastante e ficam sem card, de propósito.
        val ferramentaSugerida = remember(resultado.objetivo) { resultado.objetivo.ferramentaSugerida() }
        val conteudoProximoPasso = remember(ferramentaSugerida) { ferramentaSugerida?.conteudoProximoPasso() }
        if (ferramentaSugerida != null && conteudoProximoPasso != null) {
            Spacer(Modifier.height(LkSpacing.lg))
            LkSectionOverline(text = "Próximo passo")
            Spacer(Modifier.height(LkSpacing.sm))
            ProximoPassoSugeridoCard(
                conteudo = conteudoProximoPasso,
                onClick = { onAbrirFerramentaSugerida(ferramentaSugerida) },
                c = c,
            )
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
                Text(text = "Entrar em contato com a operadora", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W600)
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
            LkSectionOverline(text = "Sugestão")
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
                    text = "Analisar um jogo específico",
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
            Text(text = "Analisar outro problema", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        }
        Spacer(Modifier.height(LkSpacing.sm))
        TextButton(onClick = onIrParaHome, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Voltar ao início", style = MaterialTheme.typography.bodyMedium, color = c.primary)
        }
        Spacer(Modifier.height(LkSpacing.xl))
    }
}

/** Conteúdo do card "próximo passo sugerido" (Camada A, issue #1503) — icone/título/
 *  descrição/rótulo do botão só existem pras 3 ferramentas alcançáveis por
 *  [ferramentaSugerida] ([TipoFerramenta.DNS], [TipoFerramenta.MONITORAMENTO],
 *  [TipoFerramenta.SINAL_WIFI]); as demais nunca chegam aqui. */
private data class ConteudoProximoPasso(
    val icon: ImageVector,
    val titulo: String,
    val descricao: String,
    val textoBotao: String,
)

private fun TipoFerramenta.conteudoProximoPasso(): ConteudoProximoPasso? =
    when (this) {
        TipoFerramenta.DNS ->
            ConteudoProximoPasso(
                icon = Icons.Outlined.Dns,
                titulo = "Verificar a abertura de sites",
                descricao = "Vamos verificar se o serviço que localiza os sites está deixando a navegação lenta.",
                textoBotao = "Verificar abertura de sites",
            )
        TipoFerramenta.MONITORAMENTO ->
            ConteudoProximoPasso(
                icon = Icons.Outlined.MonitorHeart,
                titulo = "Acompanhar a conexão",
                descricao = "O SignallQ pode acompanhar quedas e oscilações automaticamente.",
                textoBotao = "Acompanhar a conexão",
            )
        TipoFerramenta.SINAL_WIFI ->
            ConteudoProximoPasso(
                icon = Icons.Outlined.NetworkWifi,
                titulo = "Ver a força do Wi-Fi",
                descricao = "Veja a força do Wi-Fi para entender se o problema está dentro de casa ou na operadora.",
                textoBotao = "Ver a força do Wi-Fi",
            )
        TipoFerramenta.DISPOSITIVOS,
        TipoFerramenta.SINAL_CANAIS_MOVEL,
        TipoFerramenta.EQUIPAMENTO_INTERNET,
        TipoFerramenta.PING,
        TipoFerramenta.LAUDO,
        TipoFerramenta.MODO_JOGOS,
        -> null
    }

@Composable
private fun ProximoPassoSugeridoCard(
    conteudo: ConteudoProximoPasso,
    onClick: () -> Unit,
    c: LkTokens,
) {
    LkSurfaceCard(modifier = Modifier.fillMaxWidth(), outlined = false) {
        Column(modifier = Modifier.fillMaxWidth().padding(LkSpacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(LkRadius.input))
                            .background(c.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = conteudo.icon,
                        contentDescription = null,
                        tint = c.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(LkSpacing.md))
                Text(
                    text = conteudo.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(LkSpacing.sm))
            Text(text = conteudo.descricao, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            Spacer(Modifier.height(LkSpacing.md))
            // Mesmo padrão visual do CTA primário do resumo pós-teste (issue #1475,
            // ResultadoVelocidadeScreen) — Button cheio, cor primária, mesmo shape.
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
            ) {
                Text(
                    text = conteudo.textoBotao,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
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
            Text(text = "Canais oficiais disponíveis", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
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
                text = "Obrigado pelo feedback! Isso ajuda a melhorar as próximas recomendações.",
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
        RecommendationType.FREE_TIP -> "DICA"
        RecommendationType.TUTORIAL -> "TUTORIAL"
        RecommendationType.CONFIGURATION -> "AJUSTE RECOMENDADO"
        RecommendationType.AFFILIATE_PRODUCT -> "PRODUTO SUGERIDO"
        RecommendationType.PARTNER_OFFER -> "OFERTA DE PARCEIRO"
        RecommendationType.OPERATOR_OFFER -> "OFERTA DA OPERADORA"
        RecommendationType.NATIVE_AD_FALLBACK -> "PUBLICIDADE"
    }
