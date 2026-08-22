package io.signallq.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.component.ConfirmacaoDialog
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.SignallQButton
import io.signallq.app.ui.component.SignallQButtonStyle

// ─── Dados locais sheet ───────────────────────────────────────────────────────
// GH#936 — Fase 7 MD3 (6c Dados e privacidade): extraido de AjustesScreen.kt.
//
// Destino unico para as acoes de limpar/apagar/resetar dados -- consolidando o que
// antes eram 3 entradas espalhadas (Zona de risco, Historico e dados, Privacidade),
// cada uma com comportamento de confirmacao diferente. Escalonado por gravidade.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DadosLocaisSheet(
    c: LkTokens,
    onDismiss: () -> Unit,
    onLimparHistorico: () -> Unit,
    onApagarDadosLocais: () -> Unit,
    onResetarApp: () -> Unit,
    // Issue #1670 — estado observável da última ação disparada por este sheet, exposto por
    // MainViewModel.dadosLocaisAcaoEstado (ver AcaoDadosLocaisEstado.kt). Default Ocioso
    // mantém os outros call sites/previews compiláveis sem precisar de um ViewModel real.
    estado: AcaoDadosLocaisEstado = AcaoDadosLocaisEstado.Ocioso,
    onConsumirEstado: () -> Unit = {},
    quantidadeHistorico: Int? = null,
    quantidadeApelidos: Int? = null,
    onAbrirHistorico: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showConfirmLimpar by remember { mutableStateOf(false) }
    var showConfirmApagar by remember { mutableStateOf(false) }
    var showConfirmResetar by remember { mutableStateOf(false) }

    val emAndamento = estado as? AcaoDadosLocaisEstado.EmAndamento
    val falha = estado as? AcaoDadosLocaisEstado.Falha

    // Sucesso fecha o sheet (a confirmação já foi dada); falha mantém o sheet aberto e
    // mostra o erro (mesma convenção das demais sheets de Ajustes, #1252 item 8). Em
    // qualquer um dos dois casos finais, o estado é consumido pra não ser reexibido depois.
    LaunchedEffect(estado) {
        if (estado is AcaoDadosLocaisEstado.Sucesso) {
            onConsumirEstado()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (emAndamento == null) onDismiss() },
        sheetState = sheetState,
        dragHandle = {},
        containerColor = c.bgSecondary,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LkSpacing.lg)
                    .padding(top = LkSpacing.md, bottom = LkSpacing.xxl)
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            Box(
                modifier =
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(c.border)
                        .align(Alignment.CenterHorizontally)
                        .semantics { contentDescription = "Arrastar para fechar" },
            )
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                text = "Dados neste aparelho",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
            )
            Text(
                text = "Você pode apagar cada grupo sem excluir sua conta ou desinstalar o app.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                lineHeight = 20.sp,
            )
            DadosLocaisResumo(
                quantidadeHistorico = quantidadeHistorico,
                quantidadeApelidos = quantidadeApelidos,
                c = c,
            )
            // Decisão de produto (Luiz, 2026-08-19, issue #1670): a tela só explica a
            // diferença entre apagar localmente e apagar do servidor — sem oferecer um
            // segundo caminho de contato/suporte pra pedir remoção remota. Informa e para.
            Text(
                text =
                    "Isso não remove dados já enviados a servidores do SignallQ (por exemplo, " +
                        "ao usar o Diagnóstico por IA ou compartilhar um resultado). Esses dados " +
                        "seguem a retenção do que foi enviado, independente do que você apagar aqui.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary,
                lineHeight = 18.sp,
            )
            onAbrirHistorico?.let { abrirHistorico ->
                SignallQButton(
                    label = "Exportar histórico",
                    onClick = {
                        onDismiss()
                        abrirHistorico()
                    },
                    style = SignallQButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (falha != null) {
                Text(
                    text = falha.mensagem,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            OutlinedButton(
                onClick = { showConfirmLimpar = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = emAndamento == null,
                border = BorderStroke(1.dp, c.warning),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = c.warning),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                AcaoDadosLocaisIcone(emAndamento?.acao == TipoAcaoDadosLocais.LIMPAR_HISTORICO, Icons.Outlined.History)
                Spacer(Modifier.width(LkSpacing.xs))
                Text("Limpar histórico de testes")
            }
            OutlinedButton(
                onClick = { showConfirmApagar = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = emAndamento == null,
                border = BorderStroke(1.dp, c.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = c.error),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                AcaoDadosLocaisIcone(emAndamento?.acao == TipoAcaoDadosLocais.APAGAR_DADOS_LOCAIS, Icons.Outlined.Delete)
                Spacer(Modifier.width(LkSpacing.xs))
                Text("Apagar dados locais")
            }
            Button(
                onClick = { showConfirmResetar = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = emAndamento == null,
                // GH: faltava contentColor -- fallback do M3 nao batia com a cor fixa antiga
                // (LkColors.error), resultando em contraste ruim no dark. c.error/c.onError
                // sao o par tema-aware do design system (dark usa vermelho mais claro/saturado
                // pra manter contraste contra fundo escuro).
                colors = ButtonDefaults.buttonColors(containerColor = c.error, contentColor = c.onError),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                AcaoDadosLocaisIcone(emAndamento?.acao == TipoAcaoDadosLocais.RESETAR_APP, Icons.Outlined.RestartAlt)
                Spacer(Modifier.width(LkSpacing.xs))
                Text("Resetar app")
            }
        }
    }

    // A partir daqui a ação vira observável (EmAndamento/Sucesso/Falha) — o sheet NÃO fecha
    // mais no toque em "Confirmar" (ver GH#1670 acima): só fecha quando `estado` chega em
    // Sucesso (LaunchedEffect no topo). Falha mostra o texto de erro e mantém o sheet aberto.
    if (showConfirmLimpar) {
        ConfirmacaoDialog(
            titulo = "Limpar histórico?",
            mensagem = "Esta ação removerá todos os testes registrados. Não pode ser desfeita.",
            onConfirmar = {
                onLimparHistorico()
                showConfirmLimpar = false
            },
            onCancelar = { showConfirmLimpar = false },
        )
    }

    if (showConfirmApagar) {
        ConfirmacaoDialog(
            titulo = "Apagar dados locais?",
            mensagem = "Remove configurações salvas e preferências. Esta ação não pode ser desfeita.",
            onConfirmar = {
                onApagarDadosLocais()
                showConfirmApagar = false
            },
            onCancelar = { showConfirmApagar = false },
        )
    }

    if (showConfirmResetar) {
        ConfirmacaoDialog(
            titulo = "Redefinir o app?",
            mensagem =
                "Esta ação apagará todos os dados locais: histórico de testes, configurações salvas e preferências. " +
                    "O app voltará ao estado inicial. Esta ação não pode ser desfeita.",
            onConfirmar = {
                onResetarApp()
                showConfirmResetar = false
            },
            onCancelar = { showConfirmResetar = false },
        )
    }
}

@Composable
private fun DadosLocaisResumo(
    quantidadeHistorico: Int?,
    quantidadeApelidos: Int?,
    c: LkTokens,
) {
    LkSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
            DadoLocalRow(
                titulo = "Histórico de medições",
                valor = quantidadeHistorico?.let { "$it itens" } ?: "Carregando",
                c = c,
            )
            HorizontalDivider(color = c.outlineVariant)
            DadoLocalRow(
                titulo = "Apelidos de dispositivos",
                valor = quantidadeApelidos?.let { "$it itens" } ?: "Carregando",
                c = c,
            )
            HorizontalDivider(color = c.outlineVariant)
            DadoLocalRow(titulo = "Preferências", valor = "Gerenciadas pelo app", c = c)
            HorizontalDivider(color = c.outlineVariant)
            DadoLocalRow(titulo = "Credenciais", valor = "Protegidas neste aparelho", c = c)
        }
    }
}

@Composable
private fun DadoLocalRow(
    titulo: String,
    valor: String,
    c: LkTokens,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = titulo, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
        Text(text = valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600, color = c.textSecondary)
    }
}

// Cor do ícone/spinner segue LocalContentColor.current de propósito -- dentro do content
// lambda de Button/OutlinedButton o M3 já injeta o contentColor configurado em `colors`
// (warning/error/onError conforme o botão), então o spinner herda a mesma cor do ícone que
// substitui, sem precisar repassar qual botão é qual pra esta função auxiliar.
@Composable
private fun AcaoDadosLocaisIcone(
    emAndamento: Boolean,
    icone: androidx.compose.ui.graphics.vector.ImageVector,
) {
    if (emAndamento) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = LocalContentColor.current,
        )
    } else {
        Icon(icone, contentDescription = null, modifier = Modifier.size(16.dp))
    }
}
