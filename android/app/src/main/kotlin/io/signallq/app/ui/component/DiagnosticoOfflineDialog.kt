package io.signallq.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.signallq.app.diagnosticooffline.DiagnosticoOfflineEstado
import io.signallq.app.diagnosticooffline.DiagnosticoOfflineViewModel
import io.signallq.app.diagnosticooffline.DiagnosticoOfflineViewModelFactory
import io.signallq.app.diagnosticooffline.EtapaDiagnosticoOffline
import io.signallq.app.diagnosticooffline.ResultadoEtapaDiagnosticoOffline
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens

/**
 * Tela real do fluxo de diagnóstico offline guiado (issue #1818) — substitui o antigo
 * `DiagnosticoOfflineStubDialog`. Instancia [DiagnosticoOfflineViewModel] via
 * [DiagnosticoOfflineViewModelFactory] (motor real, `DiagnosticoOfflineExecutorReal`) e dispara
 * a execução assim que o diálogo abre, refletindo as 4 etapas (gateway, DNS, rota externa,
 * hostname/captive portal) como um stepper Material 3 vertical.
 */
@Composable
fun DiagnosticoOfflineDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val viewModel: DiagnosticoOfflineViewModel =
        viewModel(factory = remember { DiagnosticoOfflineViewModelFactory(context.applicationContext) })
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.iniciar() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LocalLkTokens.current.surface,
        ) {
            DiagnosticoOfflineConteudo(
                estado = estado,
                onRetry = viewModel::retry,
                onDismiss = onDismiss,
            )
        }
    }
}

/**
 * `internal` (não `private`) para permitir teste de UI direto, sem passar pelo ViewModel/Factory
 * reais (que exigem Context/rede).
 *
 * Topo e rodapé ficam fora do `verticalScroll` de propósito, cada um com o inset correto
 * (`statusBarsPadding`/`navigationBarsPadding`) — achado de revisão do Caio na PR #1821: como
 * `usePlatformDefaultWidth = false` faz este ser o único diálogo full-screen do app sem
 * tratamento de inset (edge-to-edge, `targetSdk 36`), o botão de fechar caía sob a status bar e
 * o "Concluir" sob a nav bar. Fixar o rodapé fora do scroll também resolve o CTA ficar abaixo da
 * dobra em telas pequenas ou com fonte ampliada, sem depender de o usuário rolar até o fim.
 */
@Composable
internal fun DiagnosticoOfflineConteudo(
    estado: DiagnosticoOfflineEstado,
    onRetry: (EtapaDiagnosticoOffline?) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalLkTokens.current
    val concluido = estado as? DiagnosticoOfflineEstado.DiagnosticoConcluido
    Column(modifier = Modifier.fillMaxSize()) {
        DiagnosticoOfflineTopo(onDismiss)
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LkSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.lg),
        ) {
            Text(
                "Diagnóstico guiado",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "Testando gateway, DNS, rota externa e captive portal para entender o que está " +
                    "bloqueando sua conexão.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.onSurfaceVariant,
            )
            Spacer(Modifier.size(LkSpacing.sm))
            DiagnosticoOfflineStepper(estado = estado)
            if (concluido != null) {
                DiagnosticoOfflineResumoTexto(concluido = concluido)
            }
            Spacer(Modifier.size(LkSpacing.xl))
        }
        if (concluido != null) {
            DiagnosticoOfflineRodape(concluido = concluido, onRetry = onRetry, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun DiagnosticoOfflineTopo(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(LkSpacing.sm),
        horizontalArrangement = Arrangement.End,
    ) {
        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Fechar diagnóstico")
        }
    }
}

/** Status visual de uma etapa do stepper, derivado do [DiagnosticoOfflineEstado] atual. */
private enum class StatusEtapaVisual { PENDENTE, TESTANDO, OK, FALHOU }

private data class EtapaVisual(
    val etapa: EtapaDiagnosticoOffline,
    val status: StatusEtapaVisual,
    val motivo: String?,
)

private fun historicoDoEstado(estado: DiagnosticoOfflineEstado): List<ResultadoEtapaDiagnosticoOffline> =
    when (estado) {
        DiagnosticoOfflineEstado.Idle -> emptyList()
        is DiagnosticoOfflineEstado.TestandoEtapa -> estado.historico
        is DiagnosticoOfflineEstado.EtapaOk -> estado.historico
        is DiagnosticoOfflineEstado.EtapaFalhou -> estado.historico
        is DiagnosticoOfflineEstado.RetryEmAndamento -> estado.historico
        is DiagnosticoOfflineEstado.DiagnosticoConcluido -> estado.historico
    }

private fun etapaEmTesteDoEstado(estado: DiagnosticoOfflineEstado): EtapaDiagnosticoOffline? =
    when (estado) {
        is DiagnosticoOfflineEstado.TestandoEtapa -> estado.etapa
        is DiagnosticoOfflineEstado.RetryEmAndamento -> estado.etapa
        else -> null
    }

private fun etapasVisuais(estado: DiagnosticoOfflineEstado): List<EtapaVisual> {
    val historico = historicoDoEstado(estado)
    val etapaEmTeste = etapaEmTesteDoEstado(estado)
    val resultadoPorEtapa = historico.associateBy { it.etapa }
    return EtapaDiagnosticoOffline.ORDEM.map { etapa ->
        val resultado = resultadoPorEtapa[etapa]
        when {
            resultado is ResultadoEtapaDiagnosticoOffline.Sucesso -> EtapaVisual(etapa, StatusEtapaVisual.OK, null)
            resultado is ResultadoEtapaDiagnosticoOffline.Falha -> EtapaVisual(etapa, StatusEtapaVisual.FALHOU, resultado.motivo)
            etapaEmTeste == etapa -> EtapaVisual(etapa, StatusEtapaVisual.TESTANDO, null)
            else -> EtapaVisual(etapa, StatusEtapaVisual.PENDENTE, null)
        }
    }
}

private fun rotuloEtapa(etapa: EtapaDiagnosticoOffline): String =
    when (etapa) {
        EtapaDiagnosticoOffline.GATEWAY -> "Gateway"
        EtapaDiagnosticoOffline.DNS -> "DNS"
        EtapaDiagnosticoOffline.ROTA_EXTERNA -> "Rota externa"
        EtapaDiagnosticoOffline.HOSTNAME_CAPTIVE_PORTAL -> "Hostname / captive portal"
    }

/**
 * Sem botão de retry por etapa: o fluxo real ([DiagnosticoOfflineViewModel.executarDesde]) para
 * na primeira falha e conclui imediatamente — nunca existe um estado onde uma etapa falhou e o
 * diagnóstico segue "em aberto" aguardando ação nela. O único retry oferecido ao usuário vive no
 * rodapé ([DiagnosticoOfflineRodape]), evitando dois botões "Tentar novamente" idênticos na tela
 * ao mesmo tempo (achado de revisão do Caio na PR #1821 — problema de acessibilidade, dois nós
 * com o mesmo rótulo e a mesma ação).
 */
@Composable
private fun DiagnosticoOfflineStepper(estado: DiagnosticoOfflineEstado) {
    val etapas = remember(estado) { etapasVisuais(estado) }
    Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.md)) {
        etapas.forEachIndexed { indice, etapaVisual ->
            EtapaStepperItem(etapaVisual = etapaVisual)
            if (indice < etapas.lastIndex) {
                HorizontalDivider(color = LocalLkTokens.current.outlineVariant)
            }
        }
    }
}

@Composable
private fun EtapaStepperItem(etapaVisual: EtapaVisual) {
    val c = LocalLkTokens.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = LkSpacing.sm),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        EtapaStatusIndicador(status = etapaVisual.status)
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LkSpacing.xs)) {
            Text(
                rotuloEtapa(etapaVisual.etapa),
                style = MaterialTheme.typography.titleMedium,
                color = c.onSurface,
            )
            Text(
                text = descricaoStatus(etapaVisual.status),
                style = MaterialTheme.typography.bodyMedium,
                color = if (etapaVisual.status == StatusEtapaVisual.FALHOU) c.error else c.onSurfaceVariant,
            )
            if (etapaVisual.status == StatusEtapaVisual.FALHOU && etapaVisual.motivo != null) {
                Text(
                    etapaVisual.motivo,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                )
            }
        }
    }
}

private fun descricaoStatus(status: StatusEtapaVisual): String =
    when (status) {
        StatusEtapaVisual.PENDENTE -> "Aguardando"
        StatusEtapaVisual.TESTANDO -> "Testando…"
        StatusEtapaVisual.OK -> "Concluído com sucesso"
        StatusEtapaVisual.FALHOU -> "Falhou"
    }

@Composable
private fun EtapaStatusIndicador(status: StatusEtapaVisual) {
    val c = LocalLkTokens.current
    Box(
        modifier = Modifier.size(LkSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        when (status) {
            StatusEtapaVisual.PENDENTE ->
                Surface(
                    modifier = Modifier.size(LkSpacing.base).clip(CircleShape),
                    shape = CircleShape,
                    color = c.surfaceContainerHigh,
                    content = {},
                )
            StatusEtapaVisual.TESTANDO ->
                CircularProgressIndicator(
                    modifier = Modifier.size(LkSpacing.base),
                    strokeWidth = 2.dp,
                    color = c.primary,
                )
            StatusEtapaVisual.OK -> IconeStatus(Icons.Filled.CheckCircle, c.success)
            StatusEtapaVisual.FALHOU -> IconeStatus(Icons.Filled.Error, c.error)
        }
    }
}

@Composable
private fun IconeStatus(
    icon: ImageVector,
    tint: Color,
) {
    Icon(icon, contentDescription = null, tint = tint)
}

/** Só o cartão de resumo (texto), sem ação — os botões vivem no rodapé fixo ([DiagnosticoOfflineRodape]). */
@Composable
private fun DiagnosticoOfflineResumoTexto(concluido: DiagnosticoOfflineEstado.DiagnosticoConcluido) {
    val c = LocalLkTokens.current
    val etapaComFalha = concluido.etapaComFalha
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (etapaComFalha == null) c.successContainer else c.errorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(LkSpacing.base),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            val titulo = if (etapaComFalha == null) "Diagnóstico concluído" else "Diagnóstico concluído com falha"
            val tituloCor = if (etapaComFalha == null) c.onSuccessContainer else c.onErrorContainer
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = tituloCor)
            val mensagem =
                if (etapaComFalha == null) {
                    "Todas as etapas confirmaram sucesso — gateway, DNS, rota externa e captive portal " +
                        "estão respondendo normalmente."
                } else {
                    val motivo =
                        concluido.historico
                            .lastOrNull { it.etapa == etapaComFalha }
                            ?.let { it as? ResultadoEtapaDiagnosticoOffline.Falha }
                            ?.motivo
                    "A etapa \"${rotuloEtapa(etapaComFalha)}\" falhou" +
                        (if (motivo != null) ": $motivo." else ".")
                }
            Text(mensagem, style = MaterialTheme.typography.bodyMedium, color = tituloCor)
        }
    }
}

/**
 * Rodapé fixo (fora do `verticalScroll`) com os botões de ação — único lugar da tela que oferece
 * "Tentar novamente", e sempre visível independente de altura de conteúdo, tamanho de tela ou
 * escala de fonte, com o inset de navegação aplicado (`navigationBarsPadding`).
 */
@Composable
private fun DiagnosticoOfflineRodape(
    concluido: DiagnosticoOfflineEstado.DiagnosticoConcluido,
    onRetry: (EtapaDiagnosticoOffline?) -> Unit,
    onDismiss: () -> Unit,
) {
    val etapaComFalha = concluido.etapaComFalha
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = LkSpacing.xl, vertical = LkSpacing.base),
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        if (etapaComFalha != null) {
            SignallQButton(
                label = "Tentar novamente",
                onClick = { onRetry(null) },
                style = SignallQButtonStyle.Primary,
            )
        }
        SignallQButton(
            label = "Concluir",
            onClick = onDismiss,
            style = if (etapaComFalha == null) SignallQButtonStyle.Primary else SignallQButtonStyle.Text,
        )
    }
}
