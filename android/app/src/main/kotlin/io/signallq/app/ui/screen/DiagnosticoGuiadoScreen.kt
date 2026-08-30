package io.signallq.app.ui.screen

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NetworkWifi
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.core.diagnostico.DiagnosticContext
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.diagnostico.DiagnosticoGuiadoEngine
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.core.diagnostico.PerguntasDiagnosticoGuiado
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
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.OperadoraBadge
import io.signallq.app.ui.component.SignallQScreenState
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
    diagnosticReport: DiagnosticReport? = null,
    /** GH#1706 — o que o app sabe ao montar o plano (spec §7). Sem isto o fluxo guiado não tinha
     *  como adaptar o plano nem declarar o limite que §8.4 exige. */
    contextoDoPlano: ContextoDoPlano,
    /** GH#1706 — funil, passo 3 da spec §12. */
    onPlanoIniciado: (DiagnosticoPlanoIniciado) -> Unit,
    /** Status real da medição — GH#1705. Era `resultadoValidoParaConclusao: Boolean`, e os 5
     *  valores de `MeasurementStatus` viravam um bit exatamente aqui. `null` = ainda não há
     *  medição. Ver [continuidadeDaMedicao]. */
    statusMedicao: MeasurementStatus?,
    /** Origem que define se a medição pode ser reaproveitada nesta jornada. */
    entradaAssist: EntradaAssist = EntradaAssist.Padrao,
    tipoMidiaAssist: TipoMidiaAssist? = null,
    onAvaliarAssist: (suspend (DiagnosticInput) -> DiagnosticReport)? = null,
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
     *  contextual da jornada consolidada. Nulo = usuário
     *  responde a primeira pergunta normalmente aqui. */
    respostaPreSelecionadaPasso0: Int? = null,
    analisadorState: AnalisadorState,
    onAnalisarProblema: (String?) -> Unit,
    onResetarAnalisador: () -> Unit,
    onVoltar: () -> Unit,
    onAbrirPerfil: () -> Unit = {},
    onAlternarTema: () -> Unit = {},
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
    /** GH#1707 (Task 2.0.09e, parte 2/2) — CTA "Testar novamente" vinculado à análise original
     *  (spec §8.8). Recebe `analiseId` (correlação do funil, gerado logo abaixo) e o id da ação
     *  anterior executada pelo usuário — vazio quando ele retestou sem agir. Default no-op só
     *  para não quebrar callers/testes que não passam esta CTA. */
    onTestarNovamenteVinculado: (analiseId: String, acaoAnteriorId: String) -> Unit = { _, _ -> },
    /** GH#1707 — estado do reteste em curso (spec §14.6): ausente/em andamento/concluído com o
     *  veredito já em texto pronto pra exibição. */
    comparacaoRetesteState: ComparacaoRetesteUiState = ComparacaoRetesteUiState.Ausente,
    /**
     * Ponte para `RegistrarBackDoOverlay` — issue #1720. Chamado a cada composição bem-sucedida
     * com o comportamento de saída desta tela. O Assist não usa o voltar para navegar entre
     * perguntas: ele devolve `false` para o navigator fechar o overlay imediatamente. Quem registra de fato é o overlay que hospeda esta
     * tela ([AppShellDiagnosticoGuiadoOverlay]) — fora do conteúdo do `AnimatedVisibility`, para o
     * desregistro não ficar preso à animação de saída (ver KDoc de `RegistrarBackDoOverlay`).
     *
     * Esta tela não chama `BackHandler` diretamente: um `BackHandler` local aqui seria registrado
     * DEPOIS do `BackHandler` de `AppShellBackHandlers` (LIFO do
     * `OnBackPressedDispatcher`) e sequestraria o back antes de `consumirBackDoOverlayTopo`
     * rodar — era exatamente esse o defeito que a #1720 corrigiu. Default no-op preserva quem
     * usa esta tela sem overlay (testes, por exemplo).
     */
    onBackHandlerReady: (onBack: () -> Boolean) -> Unit = {},
) {
    val c = LocalLkTokens.current
    // GH#1704 (persistência) / GH#1720 (ligação) — estado indivisível restaurável entre mortes de
    // processo. Um saver sobre QUATRO slots `rememberSaveable` independentes conseguia restaurar
    // incoerente entre si (objetivo nulo com `passo > 0` estoura em `perguntas[passo]`;
    // `mostrarResultado` verdadeiro com objetivo novo salta o roteiro e avalia com respostas de
    // outra jornada) — motivo de existir [DiagnosticoGuiadoEstado] como objeto único. Ver
    // `DiagnosticoGuiadoEstado.kt` para o racional completo do saver e do invariante `coerente`.
    var estado by
        rememberSaveable(stateSaver = DiagnosticoGuiadoEstado.Saver) {
            mutableStateOf(
                DiagnosticoGuiadoEstado(
                    entrada = entradaAssist,
                    tipoMidia = tipoMidiaAssist,
                    objetivo = objetivoPreSelecionado,
                    respostas =
                        if (objetivoPreSelecionado != null && respostaPreSelecionadaPasso0 != null) {
                            listOf(respostaPreSelecionadaPasso0)
                        } else {
                            emptyList()
                        },
                ),
            )
        }
    val objetivo = estado.objetivo
    val passo = estado.passo
    val respostas = estado.respostas
    val relatoLivre = estado.relatoLivre
    val mostrarResultado = estado.rotaAtual == DiagnosticoGuiadoRota.Resultado

    // GH#1704 parte 4/4 — rota `Analise`. Derivado do topo da pilha, e não um terceiro valor
    // independente calculado de `analise.estado`, porque o estado do executor é global: ele fica
    // `Concluida` para o app inteiro depois de qualquer medição. Só este flag diz "a medição em
    // curso pertence a ESTE fluxo", que é o que decide se a conclusão da medição deve avançar a
    // tela.
    val emAnalise = estado.rotaAtual == DiagnosticoGuiadoRota.Analise

    /**
     * A conclusão (§8.6) só é alcançável com um resultado que o motor aceite. Sem isso o fluxo
     * mede de novo em vez de mostrar o banner de resultado inválido — antes desta fatia o banner
     * substituía a tela inteira já na entrada, e a pessoa não tinha ação nenhuma disponível.
     */
    val continuidade = statusMedicao?.let { continuidadeDaMedicao(it, medidasConfiaveis) }

    // GH#1706 — o plano só existe depois de haver objetivo; antes disso não há o que verificar.
    val plano = objetivo?.let { montarPlano(it, contextoDoPlano, respostas) }
    val contextoNds =
        remember(objetivo, respostas, relatoLivre) {
            objetivo?.let { objetivoAtual ->
                DiagnosticContext(
                    // O relato livre é contexto de explicação, nunca evidência nem causa. O NDS
                    // limita este campo a 200 caracteres no contrato v2; a tela já aplica o
                    // mesmo teto antes de ele chegar aqui.
                    reportedProblem = relatoLivre,
                    objective = objetivoAtual.name,
                    answers =
                        respostas
                            .mapIndexedNotNull { index, answer ->
                                answer?.let { "pergunta_$index" to "resposta_$it" }
                            }.toMap(),
                )
            }
        }

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
        (onAvaliarAssist == null || estado.entrada == EntradaAssist.ComDadosRecentes) &&
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
    var estadoChamadaNds by remember { mutableStateOf<EstadoChamadaNds>(EstadoChamadaNds.EmCurso) }
    val relatorioAssist = (estadoChamadaNds as? EstadoChamadaNds.Sucesso)?.relatorio ?: diagnosticReport

    // A medição terminou enquanto ESTE fluxo a esperava: avança para a conclusão. A validade do
    // resultado não entra aqui de propósito — medição concluída com resultado insuficiente leva ao
    // banner de §8.6, que explica o limite. Fingir que ainda está medindo seria pior.
    LaunchedEffect(emAnalise, analise.estado) {
        when {
            !emAnalise -> medicaoObservadaEmCurso = false
            analise.estado !is EstadoAnaliseGuiada.Concluida -> medicaoObservadaEmCurso = true
            medicaoObservadaEmCurso -> {
                if (onAvaliarAssist == null) {
                    estado = estado.irPara(DiagnosticoGuiadoRota.Resultado)
                } else {
                    estado = estado.irPara(DiagnosticoGuiadoRota.Processando)
                    estadoChamadaNds = EstadoChamadaNds.EmCurso
                }
            }
        }
    }

    LaunchedEffect(estado.rotaAtual, input, estadoChamadaNds) {
        if (estado.rotaAtual != DiagnosticoGuiadoRota.Processando || estadoChamadaNds != EstadoChamadaNds.EmCurso) return@LaunchedEffect
        val inputAtual = input?.copy(context = contextoNds) ?: return@LaunchedEffect
        val relatorio = runCatching { onAvaliarAssist?.invoke(inputAtual) }.getOrNull()
        if (relatorio?.evaluationSource == DiagnosticEvaluationSource.REMOTE) {
            estadoChamadaNds = EstadoChamadaNds.Sucesso(relatorio)
            estado = estado.irPara(DiagnosticoGuiadoRota.Resultado)
        } else {
            estadoChamadaNds = EstadoChamadaNds.Falhou
        }
    }

    // GH#1705 — o CTA de toda continuidade cai aqui: volta para a rota `Analise` (§8.5) e mede de
    // novo dentro do próprio fluxo. Não manda a pessoa para outra tela, que era o que o
    // `onMedirNovamente` do estado vazio fazia antes da #1704.
    fun remedirPelaAnalise() {
        estado = estado.irPara(DiagnosticoGuiadoRota.Analise)
        analise.onIniciar()
    }

    // GH#1720 — `estado.recuar()` é a MESMA regra que vivia aqui espalhada em cinco cláusulas
    // `when`, só que centralizada em `DiagnosticoGuiadoEstado` e testada isoladamente
    // (`DiagnosticoGuiadoEstadoTest`). `false` é o sinal "não há mais o que recuar dentro do
    // fluxo" — é exatamente o contrato que `RegistrarBackDoOverlay` espera de `onBack`.
    fun tentarRecuar(): Boolean {
        val rotaAnterior = estado.rotaAtual
        val proximo = estado.recuar() ?: return false
        estado = proximo
        when (rotaAnterior) {
            DiagnosticoGuiadoRota.Resultado -> onResetarAnalisador()
            DiagnosticoGuiadoRota.Analise -> analise.onCancelar()
            else -> Unit
        }
        return true
    }

    // Wrapper Unit para os call sites de UI (ícone de voltar da AppBar, cancelar da análise): eles
    // não têm "deixar o overlay inteiro fechar" como opção própria, então caem em [onVoltar]
    // quando não há mais o que recuar.
    fun voltarUmPasso() {
        if (!tentarRecuar()) onVoltar()
    }

    /**
     * Avança um passo do roteiro, ou conclui quando não há mais pergunta — compartilhado entre
     * "respondeu a pergunta", "pulou a pergunta" e "pulou o objetivo inteiro" (issue de melhoria
     * do Assist, 2026-08: garantir que dá pra pular em cada etapa). `perguntasTotal == 0` (caso
     * de [io.signallq.app.core.diagnostico.ObjetivoDiagnostico.OUTRO_PROBLEMA], sem pergunta
     * fechada, e de "pular objetivo", que trata o roteiro inteiro como já esgotado) cai direto
     * no mesmo ramo de conclusão que a última pergunta de um roteiro normal — o motor já tolera
     * respostas parciais ou vazias, ver `DiagnosticoGuiadoEngine`.
     */
    fun avançarOuConcluir(
        perguntasTotal: Int,
        passoAtual: Int,
        respostasAtualizadas: List<Int?>,
    ) {
        estado =
            when {
                passoAtual < perguntasTotal - 1 ->
                    estado.copy(passo = passoAtual + 1, respostas = respostasAtualizadas)
                podeConcluirSemMedir -> {
                    if (onAvaliarAssist == null) {
                        estado.copy(respostas = respostasAtualizadas).irPara(DiagnosticoGuiadoRota.Resultado)
                    } else {
                        estado.copy(respostas = respostasAtualizadas).irPara(DiagnosticoGuiadoRota.Processando)
                    }
                }
                else -> {
                    analise.onIniciar()
                    estado.copy(respostas = respostasAtualizadas).irPara(DiagnosticoGuiadoRota.Analise)
                }
            }
        if (passoAtual == perguntasTotal - 1 && podeConcluirSemMedir && onAvaliarAssist != null) {
            estadoChamadaNds = EstadoChamadaNds.EmCurso
        }
    }

    /** Pula a escolha de objetivo inteira (Camada A do Assist) — vai direto para o diagnóstico
     *  sem escolher motivo nem responder pergunta nenhuma. [ObjetivoDiagnostico.WIFI_VS_OPERADORA]
     *  é o objetivo de baixo nível usado aqui porque seu título já é "Não sei onde está o
     *  problema" — a semântica exata de quem pulou a escolha. */
    fun pularEscolhaDeObjetivo() {
        estado = estado.copy(objetivo = ObjetivoDiagnostico.WIFI_VS_OPERADORA, respostas = emptyList())
        avançarOuConcluir(perguntasTotal = 0, passoAtual = 0, respostasAtualizadas = emptyList())
    }

    // GH#1720 — o voltar do Assist fecha a jornada inteira. As perguntas são respostas da mesma
    // jornada, não uma pilha de navegação para o usuário percorrer com o botão voltar.
    // `false` chega intacto ao navigator para ele remover o overlay do topo.
    SideEffect { onBackHandlerReady { false } }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text =
                            when {
                                mostrarResultado -> "Resultado"
                                objetivo != null -> objetivo!!.titulo
                                else -> "Vamos descobrir o que está acontecendo"
                            },
                        style = MaterialTheme.typography.titleLarge,
                        color = c.textPrimary,
                    )
                },
                actions = {
                    IconButton(onClick = onAbrirPerfil) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Abrir ajustes", tint = c.textPrimary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = c.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        val objetivoAtual = objetivo
        when {
            estado.entrada == EntradaAssist.VideoOuChamada && estado.tipoMidia == null && objetivoAtual == null ->
                DiagnosticoGuiadoPerguntaBinariaSection(
                    modifier = Modifier.padding(padding),
                    onSelecionarVideo = {
                        estado = estado.copy(tipoMidia = TipoMidiaAssist.VIDEO, objetivo = ObjetivoDiagnostico.VIDEOS_TRAVAM)
                    },
                    onSelecionarChamada = {
                        estado = estado.copy(tipoMidia = TipoMidiaAssist.CHAMADA, objetivo = ObjetivoDiagnostico.CHAMADAS_CONGELAM)
                    },
                    c = c,
                )
            // GH#1704 — o banner de resultado inválido saiu daqui (era a PRIMEIRA cláusula, e por
            // isso substituía a tela inteira desde a entrada, inclusive a escolha do objetivo).
            // Agora ele é o que a conclusão mostra quando a medição fecha sem resultado que o
            // motor aceite — o único momento em que a informação é acionável.
            objetivoAtual == null ->
                DiagnosticoGuiadoListaObjetivosSection(
                    modifier = Modifier.padding(padding),
                    onSelect = { estado = estado.copy(objetivo = it) },
                    onPular = ::pularEscolhaDeObjetivo,
                    c = c,
                )
            estado.rotaAtual == DiagnosticoGuiadoRota.Processando ->
                DiagnosticoGuiadoProcessandoSection(
                    modifier = Modifier.padding(padding),
                    estado =
                        when (estadoChamadaNds) {
                            EstadoChamadaNds.EmCurso -> SignallQScreenState.Loading
                            is EstadoChamadaNds.Sucesso -> SignallQScreenState.Content(Unit)
                            EstadoChamadaNds.Falhou ->
                                SignallQScreenState.RecoverableError(
                                    title = "Não foi possível acessar o Assist no momento",
                                    message = "Tente novamente para buscar uma análise atualizada.",
                                )
                        },
                    onTentarNovamente = { estadoChamadaNds = EstadoChamadaNds.EmCurso },
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
                DiagnosticoGuiadoResultadoSection(
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
                    diagnosticReport = relatorioAssist,
                    input = input,
                    analisadorState = analisadorState,
                    onEscolherOutraSituacao = {
                        estado = DiagnosticoGuiadoEstado()
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
                    onTestarNovamenteVinculado = {
                        onTestarNovamenteVinculado(analiseId, relatorioAssist?.decisao?.recomendacaoId.orEmpty())
                    },
                    comparacaoRetesteState = comparacaoRetesteState,
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
            // ObjetivoDiagnostico.OUTRO_PROBLEMA não tem pergunta fechada — troca a lista de
            // opções por um campo de texto livre (ver kdoc do enum e de
            // DiagnosticoGuiadoRelatoLivreSection). `perguntas` fica vazio para ele, e
            // avançarOuConcluir já trata `perguntasTotal == 0` como "roteiro esgotado".
            objetivoAtual == ObjetivoDiagnostico.OUTRO_PROBLEMA ->
                DiagnosticoGuiadoRelatoLivreSection(
                    modifier = Modifier.padding(padding),
                    texto = estado.relatoLivre.orEmpty(),
                    onTextoAlterado = { novo -> estado = estado.copy(relatoLivre = novo.takeIf { it.isNotEmpty() }) },
                    onContinuar = { avançarOuConcluir(perguntasTotal = 0, passoAtual = 0, respostasAtualizadas = emptyList()) },
                    onPular = {
                        estado = estado.copy(relatoLivre = null)
                        avançarOuConcluir(perguntasTotal = 0, passoAtual = 0, respostasAtualizadas = emptyList())
                    },
                    c = c,
                )
            else -> {
                val perguntas =
                    remember(objetivoAtual, input?.connectionType) {
                        PerguntasDiagnosticoGuiado.perguntas(objetivoAtual, input?.connectionType)
                    }
                val pergunta = perguntas[passo]

                fun respostasComPasso(opcaoIndex: Int?): List<Int?> =
                    respostas.toMutableList().apply {
                        while (size <= passo) add(null)
                        this[passo] = opcaoIndex
                    }

                DiagnosticoGuiadoPerguntaFechadaSection(
                    modifier = Modifier.padding(padding),
                    pergunta = pergunta,
                    passo = passo,
                    total = perguntas.size,
                    respostaSelecionada = respostas.getOrNull(passo),
                    onEscolher = { opcaoIndex ->
                        avançarOuConcluir(perguntas.size, passo, respostasComPasso(opcaoIndex))
                    },
                    // Pula a pergunta desta etapa sem escolher opção — deixa `null` no índice do
                    // passo em vez de remover a posição, preservando o mesmo formato de
                    // `respostas` que uma resposta normal produz. `DiagnosticoGuiadoEngine` já
                    // tolerava resposta ausente antes desta pergunta existir.
                    onPular = { avançarOuConcluir(perguntas.size, passo, respostasComPasso(null)) },
                    c = c,
                )
            }
        }
    }
}

@Composable
internal fun ResultadoMetricaCard(
    value: String,
    label: String,
    c: LkTokens,
    modifier: Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(LkRadius.card))
            .background(c.bgSecondary)
            .padding(LkSpacing.md),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = c.textPrimary)
        Spacer(Modifier.height(LkSpacing.xs))
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
    }
}

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
