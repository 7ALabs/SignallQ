package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.core.diagnostico.DeviceJogo
import io.signallq.app.feature.diagnostico.topology.lan.NatUdpResultado
import io.signallq.app.feature.diagnostico.topology.lan.NatUdpTipo
import io.signallq.app.feature.diagnostico.topology.lan.StunNatProbe
import io.signallq.app.feature.speedtest.PingExecutor
import io.signallq.app.modogamer.ModoGamerEtapa
import io.signallq.app.modogamer.SelecaoJogoModoGamer
import io.signallq.app.modogamer.icone
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.AcoesRecomendadasCard
import io.signallq.app.ui.component.AiVsMotorExplainer
import io.signallq.app.ui.component.DiagnosticoStatusBanner
import io.signallq.app.ui.component.LkSectionOverline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val AMOSTRAS_PING_ESPECIFICO = 24
private const val TIMEOUT_NAT_UDP_MS = 3_000L

/** Medição dedicada opcional (issue #1487, "Medir ping específico agora") — reaproveita
 *  [PingExecutor]/[StunNatProbe], mesmas classes do fluxo legado "Jogos" (GH#935), sem
 *  duplicar a lógica de amostragem. Roda em paralelo, mesmo padrão do antigo
 *  `JogosViewModel.iniciarTeste` — o NAT UDP nunca atrasa além do próprio ping (timeout
 *  próprio de 3s, igual ao legado). */
private data class MedicaoPingEspecifico(
    val latenciaMs: Double,
    val natUdp: NatUdpResultado,
)

private suspend fun medirPingEspecifico(probeUrl: String): MedicaoPingEspecifico =
    coroutineScope {
        val pingDeferred = async(Dispatchers.IO) { PingExecutor(targetUrl = probeUrl).executar(count = AMOSTRAS_PING_ESPECIFICO) }
        val natDeferred = async(Dispatchers.IO) { StunNatProbe().sondar() }
        val ping = pingDeferred.await()
        val nat =
            withTimeoutOrNull(TIMEOUT_NAT_UDP_MS) { natDeferred.await() }
                ?: NatUdpResultado(NatUdpTipo.NAO_VERIFICADO).also { natDeferred.cancel() }
        MedicaoPingEspecifico(latenciaMs = ping.latenciaMs, natUdp = nat)
    }

private sealed interface PingEspecificoState {
    data object Ocioso : PingEspecificoState

    data object Medindo : PingEspecificoState

    data class Concluido(
        val medicao: MedicaoPingEspecifico,
    ) : PingEspecificoState

    data object Erro : PingEspecificoState
}

/**
 * Etapa 3/3 — "Como quer usar essa combinação?" (protótipo #1474 `ModoGamerConfig`) + a
 * medição dedicada opcional da issue #1487 ("Medir ping específico agora"). Nenhuma das
 * opções é obrigatória — "Ver diagnóstico" funciona a qualquer momento, mesmo com a medição
 * ainda em andamento (nesse caso, [onConfirmar] recebe `pingEspecificoMs`/`natUdp` nulos e o
 * resultado usa só o [io.signallq.app.core.diagnostico.DiagnosticInput] já coletado).
 */
@Composable
internal fun ModoGamerConfigConteudo(
    modifier: Modifier = Modifier,
    selecaoJogo: SelecaoJogoModoGamer,
    device: DeviceJogo,
    probeUrl: String,
    onConfirmar: (salvarComoPadrao: Boolean, pingEspecificoMs: Double?, natUdp: NatUdpResultado?) -> Unit,
) {
    val c = LocalLkTokens.current
    val scope = rememberCoroutineScope()
    var salvarComoPadrao by remember { mutableStateOf(true) }
    var pingState by remember { mutableStateOf<PingEspecificoState>(PingEspecificoState.Ocioso) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
    ) {
        ModoGamerListItem(titulo = selecaoJogo.nomeExibido, subtitulo = device.label, icone = selecaoJogo.categoria.icone(), onClick = {})
        Spacer(Modifier.height(LkSpacing.lg))
        LkSectionOverline(text = "Como quer usar essa combinação?")
        Spacer(Modifier.height(LkSpacing.sm))
        ModoGamerOpcaoConfig(
            titulo = "Salvar como padrão",
            descricao = "Próxima vez, o Modo gamer já abre direto com ${selecaoJogo.nomeExibido} + ${device.label}",
            selecionada = salvarComoPadrao,
            onClick = { salvarComoPadrao = true },
        )
        Spacer(Modifier.height(LkSpacing.sm))
        ModoGamerOpcaoConfig(
            titulo = "Usar só desta vez",
            descricao = "Não muda o padrão do Modo gamer",
            selecionada = !salvarComoPadrao,
            onClick = { salvarComoPadrao = false },
        )
        Spacer(Modifier.height(LkSpacing.lg))
        LkSectionOverline(text = "Refinar com uma medição dedicada (opcional)")
        Spacer(Modifier.height(LkSpacing.sm))
        ModoGamerPingEspecificoRow(
            state = pingState,
            onMedir = {
                pingState = PingEspecificoState.Medindo
                scope.launch {
                    pingState =
                        runCatching { medirPingEspecifico(probeUrl) }
                            .fold(
                                onSuccess = { PingEspecificoState.Concluido(it) },
                                onFailure = { PingEspecificoState.Erro },
                            )
                }
            },
        )
        Spacer(Modifier.height(LkSpacing.lg))
        Button(
            onClick = {
                val medicao = (pingState as? PingEspecificoState.Concluido)?.medicao
                onConfirmar(salvarComoPadrao, medicao?.latenciaMs, medicao?.natUdp)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
            colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
        ) {
            Text(text = "Ver diagnóstico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ModoGamerPingEspecificoRow(
    state: PingEspecificoState,
    onMedir: () -> Unit,
) {
    val c = LocalLkTokens.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.input))
                .background(c.bgSecondary)
                .padding(LkSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Outlined.NetworkCheck, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(LkSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Medir ping específico agora", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = c.textPrimary)
            Text(
                text =
                    when (state) {
                        PingEspecificoState.Ocioso -> "Leva alguns segundos — refina a latência com uma sonda dedicada"
                        PingEspecificoState.Medindo -> "Medindo…"
                        is PingEspecificoState.Concluido -> "Ping medido: %.0f ms".format(state.medicao.latenciaMs)
                        PingEspecificoState.Erro -> "Não foi possível medir agora — o resultado usa a medição já coletada"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
        Spacer(Modifier.width(LkSpacing.sm))
        if (state == PingEspecificoState.Medindo) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = c.primary, strokeWidth = 2.dp)
        } else {
            TextButton(onClick = onMedir) {
                Text(text = if (state is PingEspecificoState.Concluido) "Medir de novo" else "Medir", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ModoGamerOpcaoConfig(
    titulo: String,
    descricao: String,
    selecionada: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalLkTokens.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.input))
                .background(if (selecionada) c.primary.copy(alpha = 0.08f) else c.bgPrimary)
                .clickable(onClick = onClick)
                .padding(LkSpacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = if (selecionada) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selecionada) c.primary else c.textTertiary,
        )
        Spacer(Modifier.width(LkSpacing.sm))
        Column {
            Text(text = titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = c.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(text = descricao, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
    }
}

/**
 * Resultado do Modo gamer — reaproveita [DiagnosticoStatusBanner]/[AiVsMotorExplainer]/
 * [AcoesRecomendadasCard] (mesmos componentes de [DiagnosticoGuiadoScreen], issue #1476
 * critério "reaproveita motor de decisão de #1475, sem lógica duplicada").
 */
@Composable
internal fun ModoGamerResultadoConteudo(
    modifier: Modifier = Modifier,
    etapa: ModoGamerEtapa.Resultado,
    analisadorState: AnalisadorState,
    onTrocarJogoOuDevice: () -> Unit,
    onIrParaHome: () -> Unit,
) {
    val c = LocalLkTokens.current
    val resultado = etapa.resultado
    val fallback = etapa.selecaoJogo is SelecaoJogoModoGamer.ForaDoCatalogo

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(c.bgPrimary)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(c.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Outlined.SportsEsports, contentDescription = null, tint = c.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(LkSpacing.sm))
            Column {
                Text(text = etapa.selecaoJogo.nomeExibido, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W600, color = c.textPrimary)
                Text(text = etapa.device.label, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        }

        if (fallback) {
            Spacer(Modifier.height(LkSpacing.sm))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.input))
                        .background(c.warningContainer)
                        .padding(LkSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Outlined.Info, contentDescription = null, tint = c.onWarningContainer, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(LkSpacing.sm))
                Text(
                    text = "Este jogo não está no catálogo — usando o perfil de referência de ${etapa.selecaoJogo.categoria.label.lowercase()}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onWarningContainer,
                )
            }
        }

        Spacer(Modifier.height(LkSpacing.lg))
        DiagnosticoStatusBanner(status = resultado.status, mensagem = resultado.mensagemMotor, c = c)

        Spacer(Modifier.height(LkSpacing.lg))
        AiVsMotorExplainer(evidencias = resultado.evidencias, analisadorState = analisadorState, c = c)

        if (resultado.acoes.isNotEmpty()) {
            Spacer(Modifier.height(LkSpacing.lg))
            LkSectionOverline(text = "Ações recomendadas")
            Spacer(Modifier.height(LkSpacing.sm))
            AcoesRecomendadasCard(acoes = resultado.acoes, c = c)
        }

        // Issue #1487 — resultado do StunNatProbe reaproveitado do legado (GH#935/#1200),
        // só quando o usuário pediu a medição dedicada. Puramente informativo, mesmo
        // princípio "NAT nunca rebaixa o veredito sozinho" do legado — nunca aparece como
        // Dimensao/EvidenciaDiagnostico do motor.
        if (etapa.natUdp != null) {
            Spacer(Modifier.height(LkSpacing.sm))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.input))
                        .background(c.bgSecondary)
                        .padding(LkSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Outlined.Info, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(LkSpacing.sm))
                Text(
                    text = "Conexão para jogos peer-to-peer (NAT UDP): ${etapa.natUdp.tipo.rotulo()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(LkSpacing.lg))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.input))
                    .background(c.bgSecondary)
                    .padding(LkSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (etapa.salvoComoPadrao) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(LkSpacing.sm))
            Text(
                text =
                    if (etapa.salvoComoPadrao) {
                        "Salvo como padrão do Modo gamer (${etapa.selecaoJogo.nomeExibido} + ${etapa.device.label})"
                    } else {
                        "Usado só desta vez — o padrão do Modo gamer não mudou"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }

        Spacer(Modifier.height(LkSpacing.lg))
        OutlinedButton(
            onClick = onTrocarJogoOuDevice,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
        ) {
            Text(text = "Trocar jogo ou aparelho", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        }
        Spacer(Modifier.height(LkSpacing.sm))
        TextButton(onClick = onIrParaHome, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Ir para o início", style = MaterialTheme.typography.bodyMedium, color = c.primary)
        }
        Spacer(Modifier.height(LkSpacing.xl))
    }
}

/** Rótulo humano do NAT UDP (issue #1487) — versão compacta do texto do legado
 *  (`JogosScreen.textoNatUdp`), sem o card colorido dedicado (fora do escopo desta fusão). */
private fun NatUdpTipo.rotulo(): String =
    when (this) {
        NatUdpTipo.ABERTO -> "Aberta"
        NatUdpTipo.MODERADO -> "Moderada"
        NatUdpTipo.RESTRITO -> "Restrita"
        NatUdpTipo.BLOQUEADO -> "Bloqueada"
        NatUdpTipo.NAO_VERIFICADO -> "Não verificada"
    }
