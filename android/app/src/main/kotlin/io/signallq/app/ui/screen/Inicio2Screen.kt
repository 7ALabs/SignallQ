package io.signallq.app.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme
import io.signallq.app.ui.component.SignallQBanner
import io.signallq.app.ui.component.SignallQButton
import io.signallq.app.ui.component.SignallQFeedbackTone
import io.signallq.app.ui.component.SignallQProgress
import io.signallq.app.ui.component.SignallQResultBlock
import io.signallq.app.ui.component.SignallQTopAppBar

@Composable
internal fun Inicio2Screen(
    uiState: Inicio2UiState,
    onAnalisarConexao: () -> Long?,
    onAbrirPerfil: () -> Unit,
    connectionTrail: Inicio2ConnectionTrailState? = null,
    onAbrirTrailRoute: (Inicio2TrailRoute) -> Unit = {},
) {
    val c = LocalLkTokens.current
    var geracaoSolicitada by remember { mutableStateOf<Long?>(null) }
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
            verticalArrangement = Arrangement.spacedBy(LkSpacing.xl),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = LkSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                Text(
                    text = "Sua conexão agora",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                )
                ConexaoAtual(uiState)
                connectionTrail?.let {
                    Inicio2ConnectionTrail(
                        state = it,
                        onOpenRoute = onAbrirTrailRoute,
                        modifier = Modifier.padding(top = LkSpacing.base),
                    )
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = LkSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.lg),
            ) {
                EstadoAnalise(uiState.analise)
                SignallQButton(
                    label = "Analisar minha conexão",
                    onClick = {
                        if (geracaoSolicitada == null) {
                            geracaoSolicitada = onAnalisarConexao()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    loading = uiState.analise is Inicio2Analise.Carregando || geracaoSolicitada != null,
                )
            }
        }
    }
}

@Composable
private fun ConexaoAtual(uiState: Inicio2UiState) {
    val (title, message) =
        when (uiState.conexao) {
            Inicio2Conexao.Wifi -> "Wi-Fi conectado" to (uiState.nomeConexao ?: "Rede Wi-Fi ativa")
            Inicio2Conexao.Movel -> "Dados móveis ativos" to "A análise evita consumo desnecessário"
            Inicio2Conexao.Ethernet -> "Rede cabeada conectada" to "Conexão Ethernet ativa"
            Inicio2Conexao.Offline -> "Sem internet" to "Ainda podemos verificar sinais da rede local"
            Inicio2Conexao.Carregando -> "Verificando conexão" to "Isso deve levar poucos segundos"
        }
    SignallQBanner(
        title = title,
        message = message,
        tone = if (uiState.conexao == Inicio2Conexao.Offline) SignallQFeedbackTone.Warning else SignallQFeedbackTone.Neutral,
    )
}

@Composable
private fun EstadoAnalise(analise: Inicio2Analise) {
    when (analise) {
        Inicio2Analise.SemAnalise -> {
            Text("Ainda não analisada", style = MaterialTheme.typography.titleLarge)
            Text("Faça uma análise para entender o que está acontecendo e o que fazer em seguida.")
        }
        is Inicio2Analise.EstadoConhecido ->
            SignallQResultBlock(
                conclusion = analise.veredito,
                explanation = "Este é o último estado conhecido da sua conexão.",
                tone = analise.veredito.feedbackTone(),
                nextStep = "Analise novamente se a rede ou o local mudaram.",
            )
        is Inicio2Analise.ResultadoAnterior ->
            SignallQResultBlock(
                conclusion = "Resultado anterior disponível",
                explanation = "A medição salva não representa necessariamente a conexão de agora.",
                tone = SignallQFeedbackTone.Neutral,
                nextStep = "Faça uma nova análise para atualizar o diagnóstico.",
            )
        Inicio2Analise.Carregando -> SignallQProgress("Analisando sua conexão")
        is Inicio2Analise.Interrompida ->
            SignallQBanner(
                title = "Análise interrompida",
                message = analise.mensagem,
                tone = SignallQFeedbackTone.Error,
            )
    }
}

internal fun String.feedbackTone(): SignallQFeedbackTone =
    when (this) {
        "Excelente", "Bom" -> SignallQFeedbackTone.Success
        "Regular" -> SignallQFeedbackTone.Warning
        "Fraco" -> SignallQFeedbackTone.Error
        else -> SignallQFeedbackTone.Neutral
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
                            Inicio2TrailNode("Equipamento", "Roteador ou modem", Inicio2TrailRoute.Equipamento),
                            Inicio2TrailNode("Wi-Fi", "Casa", Inicio2TrailRoute.Wifi),
                            Inicio2TrailNode("Este aparelho", "Dispositivo conectado", Inicio2TrailRoute.Dispositivos),
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
                            Inicio2TrailNode("Equipamento", "Roteador ou modem", Inicio2TrailRoute.Equipamento),
                            Inicio2TrailNode("Wi-Fi", "Rede local", Inicio2TrailRoute.Wifi),
                            Inicio2TrailNode("Este aparelho", "Dispositivo conectado", Inicio2TrailRoute.Dispositivos),
                        ),
                    supportingMessage = "A trilha mostra apenas o contexto local enquanto você está offline.",
                ),
        )
    }
}
