package io.signallq.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.signallq.app.R
import io.signallq.app.core.diagnostico.MetricStatus
import io.signallq.app.feature.dns.DetectorEnderecoIpPrivado
import io.signallq.app.feature.speedtest.PingExecutor
import io.signallq.app.feature.speedtest.PingResultado
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.SignallQScreenState
import io.signallq.app.ui.component.classificarLatenciaLocal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Host de referência exibido antes de a primeira medição terminar — é o mesmo host embutido
 * como default em [PingExecutor] (`speed.cloudflare.com`). Só rotula a UI; não decide o alvo
 * real da medição (isso continua sendo responsabilidade do [PingExecutor]).
 */
private const val PING_DESTINO_PADRAO_HOST = "speed.cloudflare.com"

/**
 * Resultado da validação de um endereço digitado na opção avançada (issue #1665). Reaproveita
 * [DetectorEnderecoIpPrivado] (já usado por `DnsScreen`/`feature/dns`) em vez de criar um
 * segundo validador — mesma regra de segurança contra destinos privados/locais, só decidindo
 * a apresentação (mensagem de erro) aqui.
 */
sealed interface ResultadoValidacaoDestinoPing {
    data class Valido(
        val hostNormalizado: String,
    ) : ResultadoValidacaoDestinoPing

    data class Invalido(
        val motivo: String,
    ) : ResultadoValidacaoDestinoPing
}

object ValidadorDestinoPing {
    private val regexHost =
        Regex("^[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$")

    /** Remove esquema, caminho e porta digitados por engano — só o host importa pro teste. */
    fun normalizar(entrada: String): String =
        entrada
            .trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore("/")
            .substringBefore(":")

    /**
     * `DetectorEnderecoIpPrivado.ehPrivadoOuLocal` resolve hostname via `InetAddress`, então
     * esta função faz I/O (DNS) — sempre chamar de uma coroutine, nunca da thread principal.
     */
    suspend fun validar(entrada: String): ResultadoValidacaoDestinoPing =
        withContext(Dispatchers.IO) {
            val host = normalizar(entrada)
            when {
                host.isBlank() ->
                    ResultadoValidacaoDestinoPing.Invalido("Digite um endereço para testar.")
                !regexHost.matches(host) ->
                    ResultadoValidacaoDestinoPing.Invalido("Endereço inválido. Use um domínio como exemplo.com.")
                DetectorEnderecoIpPrivado.ehPrivadoOuLocal(host) ->
                    ResultadoValidacaoDestinoPing.Invalido("Não é possível testar endereços privados ou locais.")
                else -> ResultadoValidacaoDestinoPing.Valido(host)
            }
        }
}

/**
 * Dado de UI do PingScreen.
 *
 * [Executando] carrega o progresso incremental durante a coleta de amostras.
 * [Concluido] carrega o resultado final do ping.
 *
 * Nota: PingScreenViewModel nao e @HiltViewModel — e criado via remember{} no Composable.
 * Isso e intencional para manter o escopo da tela. Nao refatorar aqui (Sub-task C).
 */
sealed interface PingUiData {
    data class Executando(
        val progresso: Int,
    ) : PingUiData

    data class Concluido(
        val resultado: PingResultado,
    ) : PingUiData
}

/**
 * @param criarExecutor fábrica do [PingExecutor] usado a cada execução — injetável só para
 * teste (issue #1665, caracterização de cancelamento); o comportamento padrão preserva o
 * `PingExecutor()` original quando [destino] é nulo/vazio, e usa o host informado na opção
 * avançada como novo alvo caso contrário. Motor de amostragem/mediana continua o mesmo
 * [PingExecutor] — não duplicado.
 *
 * Estado tipado como [SignallQScreenState] (issue #1779, decisão de arquitetura do Luiz
 * 2026-08-20) — antes usava o `UiState` genérico legado. [SignallQScreenState.Loading]
 * representa tanto o estado inicial (antes da primeira execução) quanto o progresso 0,
 * já que a UI sempre renderiza o bloco de execução para os dois — não havia distinção
 * visual entre eles no comportamento original.
 */
class PingScreenViewModel(
    private val criarExecutor: (destino: String?) -> PingExecutor = { destino ->
        if (destino.isNullOrBlank()) PingExecutor() else PingExecutor(targetUrl = "https://$destino/")
    },
) {
    private val mutableStateFlow = MutableStateFlow<SignallQScreenState<PingUiData>>(SignallQScreenState.Loading)
    val stateFlow: StateFlow<SignallQScreenState<PingUiData>> = mutableStateFlow.asStateFlow()

    suspend fun executarPing(destino: String? = null) {
        try {
            mutableStateFlow.value = SignallQScreenState.Content(PingUiData.Executando(0))
            val executor = criarExecutor(destino)
            val resultado =
                executor.executar(count = 20) { progresso ->
                    mutableStateFlow.value = SignallQScreenState.Content(PingUiData.Executando(progresso))
                }
            mutableStateFlow.value = SignallQScreenState.Content(PingUiData.Concluido(resultado))
        } catch (e: CancellationException) {
            // GH#1211 item 5 — nunca vira estado de erro na UI: fechar a tela/cancelar o
            // teste precisa propagar o cancelamento cooperativo, não ser tratado como falha.
            throw e
        } catch (e: Exception) {
            mutableStateFlow.value =
                SignallQScreenState.RecoverableError(
                    title = "Não foi possível medir",
                    message = e.message ?: "Erro ao executar ping",
                )
        }
    }

    fun resetar() {
        mutableStateFlow.value = SignallQScreenState.Loading
    }
}

// issue #1665 (épico #1647) — migrou de ModalBottomSheet para tela cheia roteada, mesmo
// padrão adotado por DnsScreen (GH#933 Fase 4). Motor ([PingExecutor]) e telemetria
// preservados; a novidade é a apresentação (título "Tempo de resposta" em vez de jargão de
// protocolo) e a opção avançada de destino customizado, escondida por padrão — decisão de
// produto do Luiz (2026-08-19): a tela sempre mostra um destino sugerido; editar exige abrir
// a opção avançada, nunca remove o campo por completo.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingScreen(
    destinoContextual: String? = null,
    onVoltar: () -> Unit,
) {
    val c = LocalLkTokens.current
    val scope = rememberCoroutineScope()
    val viewModel = remember { PingScreenViewModel() }
    val state by viewModel.stateFlow.collectAsState()

    var destinoAtivo by remember { mutableStateOf(destinoContextual?.let { ValidadorDestinoPing.normalizar(it) }) }
    var mostrarAvancado by remember { mutableStateOf(false) }
    var inputAvancado by remember { mutableStateOf(destinoAtivo.orEmpty()) }
    var erroValidacao by remember { mutableStateOf<String?>(null) }
    var validando by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (state == SignallQScreenState.Loading) {
            viewModel.executarPing(destinoAtivo)
        }
    }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ping_titulo),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Fechar",
                            tint = c.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.ping_descricao),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                modifier = Modifier.padding(top = LkSpacing.sm, bottom = LkSpacing.md),
            )

            val estadoAtualParaDestino = state
            val emExecucao =
                estadoAtualParaDestino is SignallQScreenState.Content && estadoAtualParaDestino.value is PingUiData.Executando

            PingDestinoBloco(
                c = c,
                destinoAtivo = destinoAtivo,
                mostrarAvancado = mostrarAvancado,
                inputAvancado = inputAvancado,
                erroValidacao = erroValidacao,
                validando = validando,
                emExecucao = emExecucao,
                onAlternarAvancado = {
                    erroValidacao = null
                    mostrarAvancado = !mostrarAvancado
                },
                onInputChange = {
                    inputAvancado = it
                    erroValidacao = null
                },
                onUsarSugerido = {
                    destinoAtivo = null
                    inputAvancado = ""
                    erroValidacao = null
                    mostrarAvancado = false
                    viewModel.resetar()
                    scope.launch { viewModel.executarPing(null) }
                },
                onAplicarAvancado = {
                    scope.launch {
                        validando = true
                        when (val resultado = ValidadorDestinoPing.validar(inputAvancado)) {
                            is ResultadoValidacaoDestinoPing.Invalido -> erroValidacao = resultado.motivo
                            is ResultadoValidacaoDestinoPing.Valido -> {
                                destinoAtivo = resultado.hostNormalizado
                                mostrarAvancado = false
                                viewModel.resetar()
                                viewModel.executarPing(resultado.hostNormalizado)
                            }
                        }
                        validando = false
                    }
                },
            )

            Spacer(Modifier.height(LkSpacing.md))

            when (val currentState = state) {
                SignallQScreenState.Loading -> {
                    // Estado inicial (antes da 1a execucao) e progresso 0 sao a mesma UI —
                    // nao havia distincao visual entre eles no comportamento original.
                    PingExecutandoBloco(c = c, progresso = 0)
                }

                is SignallQScreenState.Content -> {
                    when (val data = currentState.value) {
                        is PingUiData.Executando -> PingExecutandoBloco(c = c, progresso = data.progresso)

                        is PingUiData.Concluido -> {
                            PingResultadoBloco(c = c, resultado = data.resultado)
                            Spacer(Modifier.height(LkSpacing.md))
                            Button(
                                onClick = {
                                    viewModel.resetar()
                                    scope.launch { viewModel.executarPing(destinoAtivo) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.ping_btn_testar_novamente))
                            }
                        }
                    }
                }

                is SignallQScreenState.RecoverableError -> {
                    Text(
                        text = currentState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.error,
                        modifier = Modifier.padding(bottom = LkSpacing.md),
                    )
                    Button(
                        onClick = {
                            viewModel.resetar()
                            scope.launch { viewModel.executarPing(destinoAtivo) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.global_btn_tentar_novamente))
                    }
                }

                // PingScreenViewModel nunca produz estes estados — mantidos apenas para
                // exaustividade do sealed interface compartilhado (SignallQScreenState).
                is SignallQScreenState.Empty, is SignallQScreenState.Offline, is SignallQScreenState.PermissionRequired -> Unit
            }
        }
    }
}

// ─── Bloco de destino (sugerido + opção avançada) ──────────────────────────────

@Composable
private fun PingDestinoBloco(
    c: LkTokens,
    destinoAtivo: String?,
    mostrarAvancado: Boolean,
    inputAvancado: String,
    erroValidacao: String?,
    validando: Boolean,
    emExecucao: Boolean,
    onAlternarAvancado: () -> Unit,
    onInputChange: (String) -> Unit,
    onUsarSugerido: () -> Unit,
    onAplicarAvancado: () -> Unit,
) {
    val rotuloDestino = destinoAtivo ?: PING_DESTINO_PADRAO_HOST
    val ehSugerido = destinoAtivo == null

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .border(1.dp, c.outlineVariant, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Destino testado",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                )
                Text(
                    text = if (ehSugerido) "$rotuloDestino (sugerido)" else rotuloDestino,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
            }
            TextButton(onClick = onAlternarAvancado, enabled = !emExecucao) {
                Text(if (mostrarAvancado) "Fechar" else "Testar outro endereço")
            }
        }

        AnimatedVisibility(
            visible = mostrarAvancado,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(modifier = Modifier.padding(top = LkSpacing.sm)) {
                OutlinedTextField(
                    value = inputAvancado,
                    onValueChange = onInputChange,
                    label = { Text("Endereço") },
                    placeholder = { Text("exemplo.com", color = c.textTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = erroValidacao != null,
                    enabled = !validando,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.primary,
                            unfocusedBorderColor = c.border,
                            focusedLabelColor = c.primary,
                            unfocusedLabelColor = c.textSecondary,
                            cursorColor = c.primary,
                            focusedTextColor = c.textPrimary,
                            unfocusedTextColor = c.textPrimary,
                        ),
                    shape = RoundedCornerShape(LkRadius.input),
                )
                if (erroValidacao != null) {
                    Text(
                        text = erroValidacao,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.height(LkSpacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
                    Button(onClick = onAplicarAvancado, enabled = !validando) {
                        Text(if (validando) "Verificando…" else "Usar este endereço")
                    }
                    if (!ehSugerido) {
                        TextButton(onClick = onUsarSugerido, enabled = !validando) {
                            Text("Usar destino sugerido")
                        }
                    }
                }
            }
        }
    }
}

// ─── Bloco de execução ─────────────────────────────────────────────────────────

@Composable
private fun PingExecutandoBloco(
    c: LkTokens,
    progresso: Int,
) {
    Text(
        text = stringResource(R.string.ping_coletando_amostras, progresso),
        style = MaterialTheme.typography.bodyLarge,
        color = c.textPrimary,
        modifier = Modifier.padding(bottom = LkSpacing.md),
    )
    LinearProgressIndicator(
        progress = { (progresso / 20f).coerceIn(0f, 1f) },
        modifier =
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .semantics {
                    contentDescription =
                        "Medindo tempo de resposta — $progresso de 20 amostras coletadas"
                },
        color = c.primary,
        trackColor = c.surfaceContainerHighest,
    )
}

// ─── Bloco de resultado ─────────────────────────────────────────────────────────

@Composable
private fun PingResultadoBloco(
    c: LkTokens,
    resultado: PingResultado,
) {
    // #380: 100% de perda não é uma medição válida — é falha de teste.
    // "0,0 ms" sem destaque parecia o melhor resultado possível.
    val falhaTotal = resultado.perdaPercentual >= 100.0

    // GH#1211: item 6/7 — rede caída (abortado cedo) e execução
    // parcial por timeout global são estados diferentes de "falha
    // total" (destino respondeu ruim) e diferentes entre si.
    when {
        resultado.abortadoPorRede -> {
            Text(
                text = stringResource(R.string.ping_rede_indisponivel),
                style = MaterialTheme.typography.bodyMedium,
                color = c.error,
                modifier = Modifier.padding(bottom = LkSpacing.sm),
            )
        }
        falhaTotal -> {
            Text(
                text = stringResource(R.string.ping_falha_perda_total),
                style = MaterialTheme.typography.bodyMedium,
                color = c.error,
                modifier = Modifier.padding(bottom = LkSpacing.sm),
            )
        }
        else -> {
            Text(
                text = stringResource(R.string.ping_resultados_titulo),
                style = MaterialTheme.typography.labelMedium,
                color = c.textSecondary,
                modifier = Modifier.padding(bottom = LkSpacing.sm),
            )
        }
    }

    if (resultado.execucaoParcial && !resultado.abortadoPorRede) {
        Text(
            text = stringResource(R.string.ping_execucao_parcial),
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.padding(bottom = LkSpacing.sm),
        )
    }

    // GH#1211 item 1/7 — disclosure explícito de método e destino:
    // isso não é ICMP, é latência HTTPS até o host informado.
    Text(
        text = stringResource(R.string.ping_metodo_destino, resultado.destino),
        style = MaterialTheme.typography.bodySmall,
        color = c.textSecondary,
        modifier = Modifier.padding(bottom = LkSpacing.md),
    )

    if (!falhaTotal && !resultado.abortadoPorRede) {
        PingResultadoHero(resultado = resultado, c = c)
        Spacer(Modifier.height(LkSpacing.xl))
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = LkSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        PingMetricCard(
            c = c,
            label = stringResource(R.string.ping_metrica_latencia),
            valor = if (falhaTotal) "—" else "%.1f ms".format(resultado.latenciaMs),
            destacarErro = falhaTotal,
            modifier = Modifier.weight(1f),
        )
        PingMetricCard(
            c = c,
            label = stringResource(R.string.ping_metrica_jitter),
            valor = if (falhaTotal) "—" else "%.1f ms".format(resultado.jitterMs),
            destacarErro = falhaTotal,
            modifier = Modifier.weight(1f),
        )
        PingMetricCard(
            c = c,
            label = stringResource(R.string.ping_metrica_perda),
            valor = "%.0f%%".format(resultado.perdaPercentual),
            destacarErro = falhaTotal,
            modifier = Modifier.weight(1f),
        )
    }

    if (!falhaTotal) {
        // GH#1211 item 10/12 — quantidade de tentativas/amostras
        // válidas e picos não desaparecem só porque a mediana os
        // filtrou; ficam visíveis como informação de estabilidade.
        Text(
            text =
                stringResource(
                    R.string.ping_amostras_info,
                    resultado.amostrasValidas,
                    resultado.amostras,
                ),
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        Text(
            text =
                stringResource(
                    R.string.ping_picos_info,
                    resultado.picos,
                    resultado.maxMs,
                    resultado.p95Ms,
                ),
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.padding(bottom = LkSpacing.md),
        )
    }
}

@Composable
private fun PingResultadoHero(
    resultado: PingResultado,
    c: LkTokens,
) {
    val veredito = vereditoPing(resultado.latenciaMs, resultado.perdaPercentual)
    val cor =
        when (veredito) {
            "Excelente", "Bom" -> c.success
            "Regular" -> c.warning
            else -> c.error
        }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
    ) {
        Text(
            text = "%.0f ms".format(resultado.latenciaMs),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.W600,
            color = cor,
        )
        Text(
            text = "$veredito · %.0f%% sem resposta".format(resultado.perdaPercentual),
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )
    }
}

internal fun vereditoPing(
    latenciaMs: Double,
    perdaPercentual: Double,
): String =
    when {
        perdaPercentual > 0.0 -> "Com perda de dados"
        else ->
            when (classificarLatenciaLocal(latenciaMs)) {
                MetricStatus.excelente -> "Excelente"
                MetricStatus.bom -> "Bom"
                MetricStatus.regular -> "Regular"
                MetricStatus.ruim, MetricStatus.critico -> "Ruim"
                MetricStatus.inconclusivo -> "Inconclusivo"
            }
    }

@Composable
private fun PingMetricCard(
    c: LkTokens,
    label: String,
    valor: String,
    destacarErro: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(LkRadius.card))
                .border(1.dp, if (destacarErro) c.error.copy(alpha = 0.30f) else c.outlineVariant, RoundedCornerShape(LkRadius.card))
                .background(if (destacarErro) c.error.copy(alpha = 0.08f) else c.surfaceContainer)
                .padding(LkSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = c.textSecondary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (destacarErro) c.error else c.textPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
