package io.signallq.app.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme
import io.signallq.app.ui.component.SignallQButton
import io.signallq.app.ui.component.SignallQFeedbackTone
import io.signallq.app.ui.component.SignallQTopAppBar

@Composable
internal fun Inicio2Screen(
    uiState: Inicio2UiState,
    onAnalisarConexao: () -> Long?,
    onAbrirPerfil: () -> Unit,
    onAlternarTema: () -> Unit = {},
    connectionTrail: Inicio2ConnectionTrailState? = null,
    onAbrirProblemas: () -> Unit = {},
    onAbrirVideos: () -> Unit = {},
) {
    val c = LocalLkTokens.current
    var geracaoSolicitada by remember { mutableStateOf<Long?>(null) }
    val iniciarDiagnostico = {
        if (geracaoSolicitada == null) {
            geracaoSolicitada = onAnalisarConexao()
        }
    }
    LaunchedEffect(uiState.geracaoDiagnostico, uiState.analise) {
        val geracao = geracaoSolicitada
        if (geracao != null && uiState.geracaoDiagnostico == geracao && uiState.analise !is Inicio2Analise.Carregando) {
            geracaoSolicitada = null
        }
    }
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            SignallQTopAppBar(
                title = "Início",
                actions = {
                    IconButton(onClick = onAbrirPerfil) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Abrir perfil e ajustes")
                    }
                    IconButton(onClick = onAlternarTema) {
                        Icon(Icons.Outlined.DarkMode, contentDescription = "Alternar tema")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.xxl),
        ) {
            Inicio2Hero(
                uiState = uiState,
                loading = uiState.analise is Inicio2Analise.Carregando || geracaoSolicitada != null,
                onIniciarDiagnostico = iniciarDiagnostico,
            )
            connectionTrail?.let {
                Inicio2ConnectionTrail(
                    state = it,
                    modifier = Modifier.padding(horizontal = LkSpacing.lg),
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = LkSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                Inicio2AtalhoProblema(
                    titulo = "Vídeos ou chamadas travam",
                    descricao = "Interrupções, áudio cortando ou imagem congelada",
                    icon = Icons.Outlined.Wifi,
                    onClick = onAbrirVideos,
                )
                HorizontalDivider(color = c.outlineVariant)
                Inicio2AtalhoProblema(
                    titulo = "Outro problema",
                    descricao = "Conte o que está acontecendo com sua conexão",
                    icon = Icons.Outlined.WarningAmber,
                    onClick = onAbrirProblemas,
                )
            }
        }
    }
}

@Composable
private fun Inicio2Hero(
    uiState: Inicio2UiState,
    loading: Boolean,
    onIniciarDiagnostico: () -> Unit,
) {
    val c = LocalLkTokens.current
    val (titulo, mensagem, tone) =
        when (val analise = uiState.analise) {
            is Inicio2Analise.EstadoConhecido ->
                Triple(
                    tituloConexao(analise.veredito),
                    "Este é o último diagnóstico conhecido. Faça uma nova análise se algo mudou.",
                    analise.veredito.feedbackTone(),
                )
            Inicio2Analise.SemAnalise ->
                Triple(
                    "Internet lenta",
                    "Vídeos em HD e chamadas podem travar agora.",
                    SignallQFeedbackTone.Neutral,
                )
            is Inicio2Analise.ResultadoAnterior ->
                Triple(
                    "Resultado anterior disponível",
                    "A medição salva pode não representar a conexão de agora.",
                    SignallQFeedbackTone.Neutral,
                )
            Inicio2Analise.Carregando ->
                Triple(
                    "Analisando sua conexão",
                    "Estamos reunindo evidências da rede.",
                    SignallQFeedbackTone.Neutral,
                )
            is Inicio2Analise.Interrompida ->
                Triple(
                    "Análise interrompida",
                    analise.mensagem,
                    SignallQFeedbackTone.Error,
                )
        }
    val (connectionLabel, connectionIcon) =
        when (uiState.conexao) {
            Inicio2Conexao.Wifi -> "Wi-Fi conectado" to Icons.Outlined.Wifi
            Inicio2Conexao.Movel -> "Dados móveis ativos" to Icons.Outlined.Wifi
            Inicio2Conexao.Ethernet -> "Rede cabeada conectada" to Icons.Outlined.Wifi
            Inicio2Conexao.Offline -> "Sem internet" to Icons.Outlined.WarningAmber
            Inicio2Conexao.Carregando -> "Verificando conexão" to Icons.Outlined.Wifi
        }
    val cor = tone.cor(c)
    Column(
        modifier = Modifier.padding(horizontal = LkSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(connectionIcon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(LkSpacing.sm))
            Text(connectionLabel, style = MaterialTheme.typography.labelLarge, color = c.textSecondary)
        }
        Box(
            modifier =
                Modifier
                    .size(112.dp)
                    .border(8.dp, cor.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("!", color = cor, fontSize = 52.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
        Text(
            text = titulo,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = c.textPrimary,
        )
        Text(
            text = mensagem,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = c.textSecondary,
        )
        SignallQButton(
            label = "Analisar minha conexão",
            onClick = onIniciarDiagnostico,
            modifier = Modifier.fillMaxWidth(),
            loading = loading,
        )
    }
}

@Composable
private fun Inicio2AtalhoProblema(
    titulo: String,
    descricao: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val c = LocalLkTokens.current
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = c.textSecondary)
            Spacer(Modifier.width(LkSpacing.md))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LkSpacing.xs)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, color = c.textPrimary, textAlign = TextAlign.Start)
                Text(descricao, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, textAlign = TextAlign.Start)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = c.textSecondary)
        }
    }
}

private fun SignallQFeedbackTone.cor(c: io.signallq.app.ui.LkTokens): Color =
    when (this) {
        SignallQFeedbackTone.Success -> c.success
        SignallQFeedbackTone.Warning -> c.warning
        SignallQFeedbackTone.Error -> c.error
        SignallQFeedbackTone.Neutral -> c.primary
    }

internal fun String.feedbackTone(): SignallQFeedbackTone =
    when (this) {
        "Excelente", "Bom" -> SignallQFeedbackTone.Success
        "Regular" -> SignallQFeedbackTone.Warning
        "Fraco" -> SignallQFeedbackTone.Error
        else -> SignallQFeedbackTone.Neutral
    }

internal fun tituloConexao(veredito: String): String =
    when (veredito) {
        "Excelente" -> "Conexão excelente"
        "Bom" -> "Conexão boa"
        "Regular" -> "Conexão regular"
        "Fraco" -> "Conexão fraca"
        else -> "Conexão ${veredito.lowercase()}"
    }

@Preview(name = "Início 2 claro", showBackground = true)
@Composable
private fun Inicio2ScreenPreview() {
    SignallQTheme {
        Inicio2Screen(
            uiState = Inicio2UiState(Inicio2Conexao.Wifi, "Casa", Inicio2Analise.SemAnalise),
            onAnalisarConexao = { null },
            onAbrirPerfil = {},
            connectionTrail =
                Inicio2ConnectionTrailState(
                    nodes =
                        listOf(
                            Inicio2TrailNode("Internet", "Conectada"),
                            Inicio2TrailNode("Equipamento", "Roteador ou modem"),
                            Inicio2TrailNode("Wi-Fi", "Casa"),
                            Inicio2TrailNode("Este aparelho", "Conectado por Wi-Fi"),
                        ),
                    supportingMessage = null,
                ),
        )
    }
}

@Preview(name = "Início 2 escuro 200%", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 2f)
@Composable
private fun Inicio2ScreenDarkPreview() {
    SignallQTheme {
        Inicio2Screen(
            uiState = Inicio2UiState(Inicio2Conexao.Offline, null, Inicio2Analise.Interrompida("Seu contexto foi preservado.")),
            onAnalisarConexao = { null },
            onAbrirPerfil = {},
            connectionTrail =
                Inicio2ConnectionTrailState(
                    nodes =
                        listOf(
                            Inicio2TrailNode("Internet", "Sem acesso"),
                            Inicio2TrailNode("Este aparelho", "Sem conexão ativa"),
                        ),
                    supportingMessage = "Conecte-se a uma rede para completar a trilha.",
                ),
        )
    }
}
