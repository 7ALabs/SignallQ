package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.signallq.app.core.network.contracts.gateway.GatewayConnectionResultado
import io.signallq.app.core.network.contracts.gateway.GatewayConnectionService
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.SheetDragHandle
import kotlinx.coroutines.launch

/**
 * Estado interno da sheet de conexao ao gateway (GH#526). Nao ha estado de
 * "sucesso" aqui: uma conexao bem-sucedida fecha a sheet via [onConectado].
 */
private sealed interface GatewayConnectionSheetState {
    data object Formulario : GatewayConnectionSheetState

    data object Conectando : GatewayConnectionSheetState

    data class Erro(
        val mensagem: String,
    ) : GatewayConnectionSheetState
}

private data class ModeloCompativelGateway(
    val marca: String,
    val modelo: String,
    val tipo: String,
)

private val modelosCompativeisGateway =
    listOf(
        ModeloCompativelGateway("Nokia", "G-1425", "Roteador Wi-Fi 5"),
        ModeloCompativelGateway("TP-Link", "Archer C6", "Roteador Wi-Fi 5 (AC1200)"),
    )

/**
 * Sheet de conexao ativa ao GPON/roteador (GH#526, epic #525).
 *
 * Escopo desta task: SOMENTE a UI (formulario + estados). A conexao real
 * (autenticacao no equipamento) e injetada via [conectar] — quando nenhuma
 * implementacao real existir ainda (antes de #527/#530), o caller deve
 * passar um [GatewayConnectionService] mockado.
 *
 * Persistencia de usuario/senha e responsabilidade do caller (via
 * [CredenciaisModemStore][io.signallq.app.core.datastore.CredenciaisModemStore],
 * ja usado no resto do app para essa finalidade) — esta sheet so avisa o
 * caller do resultado através de [onConectado], sem gravar nada sozinha.
 *
 * @param ipInicial IP do gateway pre-preenchido (do gateway ja detectado), editavel.
 * @param usuarioInicial usuario pre-preenchido (ex.: da ultima sessao salva).
 * @param senhaInicial senha pre-preenchida (ex.: da ultima sessao salva).
 * @param lembrarSenhaInicial estado inicial do toggle "Lembrar senha".
 * @param manterConectadoInicial estado inicial do toggle "Manter conectado" (implica lembrar senha).
 * @param conectar chamada assincrona que tenta autenticar no gateway.
 * @param onConectado disparado quando [conectar] retorna sucesso, com os dados
 *   digitados e o estado final dos dois toggles — o caller decide o que persistir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatewayConnectionSheet(
    ipInicial: String?,
    usuarioInicial: String = "",
    senhaInicial: String = "",
    lembrarSenhaInicial: Boolean = false,
    manterConectadoInicial: Boolean = false,
    onDismissRequest: () -> Unit,
    conectar: GatewayConnectionService,
    onConectado: (ip: String, usuario: String, senha: String, lembrarSenha: Boolean, manterConectado: Boolean) -> Unit = { _, _, _, _, _ -> },
) {
    val c = LocalLkTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = c.bgSecondary,
    ) {
        GatewayConnectionSheetContent(
            ipInicial = ipInicial,
            usuarioInicial = usuarioInicial,
            senhaInicial = senhaInicial,
            lembrarSenhaInicial = lembrarSenhaInicial,
            manterConectadoInicial = manterConectadoInicial,
            conectar = conectar,
            onConectado = { ip, usuario, senha, lembrarSenha, manterConectado ->
                onConectado(ip, usuario, senha, lembrarSenha, manterConectado)
                onDismissRequest()
            },
            c = c,
        )
    }
}

@Composable
internal fun GatewayConnectionSheetContent(
    ipInicial: String?,
    usuarioInicial: String,
    senhaInicial: String,
    lembrarSenhaInicial: Boolean,
    manterConectadoInicial: Boolean,
    conectar: GatewayConnectionService,
    onConectado: (ip: String, usuario: String, senha: String, lembrarSenha: Boolean, manterConectado: Boolean) -> Unit,
    c: LkTokens,
) {
    var ipInput by remember { mutableStateOf(ipInicial.orEmpty()) }
    var usuarioInput by remember { mutableStateOf(usuarioInicial) }
    var senhaInput by remember { mutableStateOf(senhaInicial) }
    var mostrarSenha by remember { mutableStateOf(false) }
    var lembrarSenha by remember { mutableStateOf(lembrarSenhaInicial || manterConectadoInicial) }
    var manterConectado by remember { mutableStateOf(manterConectadoInicial) }
    var estado by remember { mutableStateOf<GatewayConnectionSheetState>(GatewayConnectionSheetState.Formulario) }
    // GH#529: guia ilustrado de como obter usuario/senha, aberto sem sair da sheet de conexao.
    var mostrarGuiaCredenciais by remember { mutableStateOf(false) }
    var mostrarModelosCompativeis by remember { mutableStateOf(false) }

    val escopo = rememberCoroutineScope()
    val conectando = estado is GatewayConnectionSheetState.Conectando
    val podeConectar = ipInput.isNotBlank() && !conectando

    fun tentarConectar() {
        if (!podeConectar) return
        estado = GatewayConnectionSheetState.Conectando
        escopo.launch {
            when (val resultado = conectar.conectar(ipInput.trim(), usuarioInput, senhaInput)) {
                is GatewayConnectionResultado.Sucesso -> {
                    estado = GatewayConnectionSheetState.Formulario
                    onConectado(ipInput.trim(), usuarioInput, senhaInput, lembrarSenha, manterConectado)
                }
                is GatewayConnectionResultado.Falha -> {
                    estado = GatewayConnectionSheetState.Erro(resultado.mensagemUsuario)
                }
            }
        }
    }

    val fieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LkColors.accent,
            unfocusedBorderColor = c.border,
            focusedLabelColor = LkColors.accent,
            unfocusedLabelColor = c.textSecondary,
            cursorColor = LkColors.accent,
            focusedTextColor = c.textPrimary,
            unfocusedTextColor = c.textPrimary,
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.xl)
                .padding(bottom = LkSpacing.xxl)
                .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        SheetDragHandle()
        GatewayConnectionSegmentedState(
            estado = estado,
            c = c,
        )
        Text(
            text = "Conectar ao roteador",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.W700,
            color = c.textPrimary,
        )

        OutlinedTextField(
            value = ipInput,
            onValueChange = { ipInput = it },
            label = { Text("Endereço IP") },
            placeholder = { Text("192.168.1.1", color = c.textTertiary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !conectando,
            colors = fieldColors,
            shape = RoundedCornerShape(LkRadius.input),
        )

        OutlinedTextField(
            value = usuarioInput,
            onValueChange = { usuarioInput = it },
            label = { Text("Usuário") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !conectando,
            colors = fieldColors,
            shape = RoundedCornerShape(LkRadius.input),
        )

        OutlinedTextField(
            value = senhaInput,
            onValueChange = { senhaInput = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !conectando,
            visualTransformation = if (mostrarSenha) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { mostrarSenha = !mostrarSenha }) {
                    Icon(
                        imageVector = if (mostrarSenha) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (mostrarSenha) "Ocultar senha" else "Mostrar senha",
                        tint = c.textSecondary,
                    )
                }
            },
            colors = fieldColors,
            shape = RoundedCornerShape(LkRadius.input),
        )

        TextButton(
            onClick = { mostrarGuiaCredenciais = true },
            enabled = !conectando,
            modifier = Modifier.testTag("gateway_link_guia_credenciais"),
        ) {
            Text(
                text = "Não sabe o usuário e a senha?",
                style = MaterialTheme.typography.bodyMedium,
                color = LkColors.accent,
            )
        }

        TextButton(
            onClick = { mostrarModelosCompativeis = true },
            enabled = !conectando,
        ) {
            Text(
                text = "Ver modelos de roteador compatíveis",
                style = MaterialTheme.typography.bodyMedium,
                color = LkColors.accent,
            )
        }

        ToggleRow(
            titulo = "Lembrar senha",
            subtitulo = "Salvar usuário e senha neste aparelho",
            checked = lembrarSenha,
            // "Manter conectado" implica lembrar senha — nao da pra reconectar
            // sozinho sem credencial salva, entao trava o toggle nesse caso.
            enabled = !conectando && !manterConectado,
            onCheckedChange = { lembrarSenha = it },
            c = c,
            testTag = "gateway_toggle_lembrar_senha",
        )

        ToggleRow(
            titulo = "Manter conectado",
            subtitulo = "Reconectar automaticamente ao abrir o app",
            checked = manterConectado,
            enabled = !conectando,
            onCheckedChange = { checked ->
                manterConectado = checked
                if (checked) lembrarSenha = true
            },
            c = c,
            testTag = "gateway_toggle_manter_conectado",
        )

        val erroAtual = (estado as? GatewayConnectionSheetState.Erro)?.mensagem
        if (erroAtual != null) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.card))
                        .background(c.errorContainer)
                        .padding(LkSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = c.onErrorContainer,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(LkSpacing.sm))
                Text(
                    text = erroAtual,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onErrorContainer,
                    modifier = Modifier.testTag("gateway_error_message"),
                )
            }
        }

        Spacer(Modifier.height(LkSpacing.xs))

        Button(
            onClick = { tentarConectar() },
            enabled = podeConectar,
            modifier = Modifier.fillMaxWidth().testTag("gateway_connect_button"),
            shape = RoundedCornerShape(LkRadius.button),
            colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
        ) {
            if (conectando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(LkSpacing.sm))
                Text("Conectando…")
            } else {
                Text(if (erroAtual != null) "Tentar novamente" else "Conectar")
            }
        }
    }

    if (mostrarGuiaCredenciais) {
        GatewayCredentialsGuideSheet(onDismissRequest = { mostrarGuiaCredenciais = false })
    }

    if (mostrarModelosCompativeis) {
        GatewayCompatibleModelsSheet(onDismissRequest = { mostrarModelosCompativeis = false })
    }
}

@Composable
private fun GatewayConnectionSegmentedState(
    estado: GatewayConnectionSheetState,
    c: LkTokens,
) {
    val ativo =
        when (estado) {
            GatewayConnectionSheetState.Formulario -> "Formulário"
            GatewayConnectionSheetState.Conectando -> "Conectando"
            is GatewayConnectionSheetState.Erro -> "Erro"
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        listOf("Formulário", "Conectando", "Erro").forEach { label ->
            FilterChip(
                selected = ativo == label,
                onClick = {},
                enabled = false,
                modifier = Modifier.weight(1f),
                label = { Text(label) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = c.secondaryContainer,
                        selectedLabelColor = c.onSecondaryContainer,
                        disabledSelectedContainerColor = c.secondaryContainer,
                        disabledContainerColor = c.surfaceContainer,
                        disabledLabelColor = c.textTertiary,
                    ),
            )
        }
    }
}

@Composable
private fun ToggleRow(
    titulo: String,
    subtitulo: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    c: LkTokens,
    testTag: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .border(1.dp, c.outlineVariant, RoundedCornerShape(LkRadius.card))
                .background(c.surfaceContainer)
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
                color = c.textPrimary,
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.testTag(testTag),
            colors = toggleRowSwitchColors(c),
        )
    }
}

/**
 * Cores do [Switch] de [ToggleRow]. Extraida para ser testavel isoladamente
 * (GH#848): sem os overrides `disabled*`, o Switch cai nos tokens "disabled"
 * padrao do Material3 (cinza tanto para checked quanto unchecked), fazendo um
 * toggle true+disabled parecer desligado. Aqui a versao desabilitada mantem a
 * mesma aparencia do estado habilitado equivalente, so trocando a interatividade.
 */
@Composable
internal fun toggleRowSwitchColors(c: LkTokens): SwitchColors =
    SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
        checkedTrackColor = LkColors.accent,
        checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = c.textTertiary,
        uncheckedTrackColor = c.border,
        disabledCheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
        disabledCheckedTrackColor = LkColors.accent,
        disabledCheckedBorderColor = Color.Transparent,
        disabledUncheckedThumbColor = c.textTertiary,
        disabledUncheckedTrackColor = c.border,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GatewayCompatibleModelsSheet(onDismissRequest: () -> Unit) {
    val c = LocalLkTokens.current
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.bgSecondary,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LkSpacing.xl)
                    .padding(top = LkSpacing.sm, bottom = LkSpacing.xxl)
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Voltar",
                        tint = c.textSecondary,
                    )
                }
                Text(
                    text = "Modelos compatíveis",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.W700,
                    color = c.textPrimary,
                )
            }
            Text(
                text = "O SignallQ já testou a conexão automática com estes roteadores. Outros modelos também podem funcionar via usuário e senha manuais.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
            modelosCompativeisGateway.forEach { modelo ->
                GatewayModelRow(
                    marcaModelo = "${modelo.marca} ${modelo.modelo}",
                    tipo = modelo.tipo,
                    c = c,
                )
            }
        }
    }
}

@Composable
private fun GatewayModelRow(
    marcaModelo: String,
    tipo: String,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.surfaceContainer)
                .padding(LkSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GatewayModelIcon(c = c)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = marcaModelo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Text(
                text = tipo,
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
        Text(
            text = "Compatível",
            style = MaterialTheme.typography.labelMedium,
            color = LkColors.success,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(LkColors.success.copy(alpha = 0.14f))
                    .padding(horizontal = LkSpacing.sm, vertical = 4.dp),
        )
    }
}

@Composable
private fun GatewayModelIcon(c: LkTokens) {
    Row(
        modifier =
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LkColors.accent.copy(alpha = 0.14f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Router,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = LkColors.accent,
        )
    }
}
