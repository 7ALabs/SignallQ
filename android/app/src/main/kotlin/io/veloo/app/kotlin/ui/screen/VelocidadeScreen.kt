package io.signallq.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.signallq.app.R
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.FaseSpeedtest
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import io.signallq.app.ui.IspInfo
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.GaugeCircular
import io.signallq.app.ui.component.MiniGrafico
import kotlinx.coroutines.isActive
import androidx.compose.animation.core.tween as tweenSpec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VelocidadeScreen(
    snapshot: SnapshotExecucaoSpeedtest,
    localizacaoServidor: String?,
    ispInfo: IspInfo?,
    onCancelar: () -> Unit,
    onReiniciar: () -> Unit,
    onVoltar: () -> Unit = {},
) {
    val c = LocalLkTokens.current
    val haptic = LocalHapticFeedback.current
    var mostrarConfirmarCancelar by remember { mutableStateOf(false) }

    val fase = snapshot.faseAtual
    val corFase = corDaFase(fase)

    // Haptics nas transições de fase
    LaunchedEffect(fase) {
        when (fase) {
            FaseSpeedtest.download, FaseSpeedtest.upload ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            FaseSpeedtest.concluido ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            else -> {}
        }
    }
    LaunchedEffect(snapshot.estado) {
        if (snapshot.estado == EstadoExecucaoSpeedtest.erro) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (snapshot.estado == EstadoExecucaoSpeedtest.erro) "Erro" else "Medindo…",
                        style = MaterialTheme.typography.titleLarge,
                        color = c.textPrimary,
                    )
                },
                navigationIcon = {
                    AnimatedVisibility(
                        visible = snapshot.estado != EstadoExecucaoSpeedtest.executando,
                        enter = fadeIn(animationSpec = tweenSpec(200)),
                        exit = fadeOut(animationSpec = tweenSpec(200)),
                    ) {
                        IconButton(onClick = onVoltar) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = c.textPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        if (snapshot.estado == EstadoExecucaoSpeedtest.erro) {
            ErroContent(
                mensagem = snapshot.erroMensagem,
                onReiniciar = onReiniciar,
                onCancelar = onCancelar,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(LkSpacing.xl),
            )
            return@Scaffold
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Gauge centralizado com peso para ocupar espaço restante
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // Suavização do número: 0.25 * target + 0.75 * rendered por frame (~60fps)
                var renderedMbps by remember { mutableFloatStateOf(0f) }
                val snapshotState = rememberUpdatedState(snapshot)
                LaunchedEffect(Unit) {
                    while (isActive) {
                        withFrameMillis {
                            val target = snapshotState.value.velocidadeAtualMbps.toFloat()
                            renderedMbps = 0.25f * target + 0.75f * renderedMbps
                        }
                    }
                }

                val velocidadeExibida =
                    when (fase) {
                        FaseSpeedtest.download, FaseSpeedtest.upload -> renderedMbps
                        else -> 0f
                    }

                GaugeCircular(
                    progressoGlobal = snapshot.progressoGlobal,
                    rotulo = rotuloFase(fase),
                    velocidadeMbps = velocidadeExibida,
                    corFase = corFase,
                    unidade = "Mbps",
                    modifier =
                        Modifier.semantics {
                            contentDescription =
                                if (velocidadeExibida > 0f) {
                                    "Velocidade atual: ${"%.1f".format(velocidadeExibida)} Mbps"
                                } else {
                                    "Medindo velocidade"
                                }
                        },
                )
            }

            // Resultado de download durante upload
            AnimatedVisibility(
                visible = fase == FaseSpeedtest.upload,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                val pontosAoVivo = snapshot.pontosAoVivo
                val dlMedio =
                    remember(pontosAoVivo) {
                        pontosAoVivo.mapNotNull { it.dl }.average().takeIf { !it.isNaN() }
                    }
                if (dlMedio != null) {
                    Row(
                        modifier =
                            Modifier
                                .padding(bottom = LkSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "↓ ${"%.1f".format(dlMedio)} Mbps",
                            style = MaterialTheme.typography.titleSmall,
                            color = LkColors.phaseDownload,
                            fontWeight = FontWeight.W600,
                        )
                        Spacer(Modifier.width(LkSpacing.sm))
                        Text(
                            text = "download concluído",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.textTertiary,
                        )
                    }
                }
            }

            // Mini-gráfico ao vivo (apenas DL/UP)
            AnimatedVisibility(
                visible = fase == FaseSpeedtest.download || fase == FaseSpeedtest.upload,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LkSpacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    MiniGrafico(
                        pontos = snapshot.pontosAoVivo,
                        fase = fase,
                        corFase = corFase,
                    )
                }
            }

            Spacer(Modifier.height(LkSpacing.lg))

            // Pills de fase
            PillsFase(faseAtual = fase)

            Spacer(Modifier.height(LkSpacing.sm))

            // Frase narrativa
            Text(
                text = fraseFase(fase),
                style = MaterialTheme.typography.titleSmall,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = LkSpacing.xl),
            )

            Spacer(Modifier.height(LkSpacing.xl))

            // Botão Cancelar — pede confirmação, mesmo texto/fluxo do BackHandler (SpeedTestScreen)
            TextButton(onClick = { mostrarConfirmarCancelar = true }) {
                Text(
                    text = "Cancelar",
                    color = c.textTertiary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(LkSpacing.lg))

            // Linha de servidor — rodapé da tela de execução (Speed.jsx)
            LinhaServidor(
                localizacaoServidor = localizacaoServidor,
                ispInfo = ispInfo,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LkSpacing.xl),
            )

            Spacer(Modifier.height(LkSpacing.lg))
        }
    }

    if (mostrarConfirmarCancelar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmarCancelar = false },
            title = { Text("Interromper o teste?") },
            text = { Text("O teste em andamento será interrompido e o resultado descartado.") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmarCancelar = false
                        onCancelar()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LkColors.error),
                ) { Text("Interromper") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmarCancelar = false }) {
                    Text("Continuar testando")
                }
            },
        )
    }
}

@Composable
private fun LinhaServidor(
    localizacaoServidor: String?,
    ispInfo: IspInfo?,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val partes =
        listOfNotNull(
            localizacaoServidor?.takeIf { it.isNotBlank() && it != "—" },
            ispInfo?.isp?.takeIf { it.isNotBlank() && it != "—" },
        )
    if (partes.isEmpty()) {
        Spacer(Modifier.height(LkSpacing.sm))
        return
    }
    Row(
        modifier = modifier.padding(vertical = LkSpacing.sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Dns,
            contentDescription = null,
            tint = c.textTertiary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(LkSpacing.xs))
        Text(
            text = "Servidor: " + partes.joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = c.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private data class PillConfig(
    val fase: FaseSpeedtest,
    val label: String,
    val ordem: Int,
)

private val fasePills =
    listOf(
        PillConfig(FaseSpeedtest.ping, "LATÊNCIA", 0),
        PillConfig(FaseSpeedtest.download, "DOWNLOAD", 1),
        PillConfig(FaseSpeedtest.upload, "UPLOAD", 2),
        PillConfig(FaseSpeedtest.concluido, "CONCLUÍDO", 3),
    )

@Composable
private fun PillsFase(faseAtual: FaseSpeedtest) {
    val c = LocalLkTokens.current
    val ordemAtual = fasePills.firstOrNull { it.fase == faseAtual }?.ordem ?: -1

    Row(
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        fasePills.forEach { pill ->
            val ordemPill = pill.ordem
            val ativo = pill.fase == faseAtual
            val concluido = ordemPill < ordemAtual

            val corBorda =
                when {
                    concluido -> LkColors.success
                    ativo -> corDaFase(faseAtual)
                    else -> Color.Transparent
                }
            val corTexto =
                when {
                    concluido -> LkColors.success
                    ativo -> corDaFase(faseAtual)
                    else -> c.onSurfaceVariant
                }
            val bgColor =
                when {
                    concluido -> LkColors.success.copy(alpha = 0.16f)
                    ativo -> corDaFase(faseAtual).copy(alpha = 0.16f)
                    else -> c.surfaceContainer
                }

            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(LkRadius.pill))
                        .background(bgColor)
                        .then(
                            if (corBorda != Color.Transparent) {
                                Modifier.border(1.dp, corBorda, RoundedCornerShape(LkRadius.pill))
                            } else {
                                Modifier
                            },
                        ).padding(horizontal = LkSpacing.md, vertical = LkSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (concluido) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = LkColors.success,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(LkSpacing.xs))
                    }
                    Text(
                        text = pill.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = corTexto,
                    )
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun ErroContent(
    mensagem: String?,
    onReiniciar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = LkColors.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(LkSpacing.lg))
        Text(
            text = "Não foi possível completar o teste",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.W600,
            color = c.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(LkSpacing.sm))
        val textoExibido =
            when (mensagem) {
                "erroModemInacessivel" -> stringResource(R.string.fibra_erro_modem_inacessivel)
                "erroTimeout" -> stringResource(R.string.fibra_erro_timeout)
                "erroRespostaModemInvalida" -> stringResource(R.string.fibra_erro_resposta_invalida)
                "erroComunicacaoModem" -> stringResource(R.string.fibra_erro_comunicacao)
                "semRede" -> stringResource(R.string.fibra_erro_sem_rede)
                null -> "Verifique sua conexão e tente novamente."
                else -> stringResource(R.string.fibra_erro_generico)
            }
        Text(
            text = textoExibido,
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(LkSpacing.xl))
        Button(
            onClick = onReiniciar,
            colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
        ) {
            Text("Testar novamente")
        }
        Spacer(Modifier.height(LkSpacing.sm))
        TextButton(onClick = onCancelar) {
            Text("Cancelar", color = c.textTertiary)
        }
    }
}

fun corDaFase(fase: FaseSpeedtest): Color =
    when (fase) {
        FaseSpeedtest.ping -> LkColors.phaseLatencia
        FaseSpeedtest.download -> LkColors.phaseDownload
        FaseSpeedtest.upload -> LkColors.phaseUpload
        else -> LkColors.accent
    }

private fun rotuloFase(fase: FaseSpeedtest): String =
    when (fase) {
        FaseSpeedtest.ping -> "LATÊNCIA"
        FaseSpeedtest.download -> "DOWNLOAD"
        FaseSpeedtest.upload -> "UPLOAD"
        FaseSpeedtest.concluido -> "CONCLUÍDO"
        else -> "AGUARDANDO"
    }

private fun fraseFase(fase: FaseSpeedtest): String =
    when (fase) {
        FaseSpeedtest.ping -> "Verificando a resposta do servidor…"
        FaseSpeedtest.download -> "Medindo a velocidade de download…"
        FaseSpeedtest.upload -> "Medindo a velocidade de upload…"
        FaseSpeedtest.concluido -> "Quase pronto…"
        else -> "Preparando o teste…"
    }
