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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextAlign
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
    // 2b-iii To-Be: lista de modelos compativeis, aberta sem sair da sheet de conexao
    // (mesmo padrao do guia de credenciais acima).
    var mostrarModelosCompativeis by remember { mutableStateOf(false) }

    val escopo = rememberCoroutineScope()
    val conectando = estado is GatewayConnectionSheetState.Conectando
    // 2b-i To-Be: valida formato IPv4 antes de habilitar "Conectar" — diferencia
    // erro de credencial de erro de formato de IP antes mesmo de tentar autenticar.
    val ipValido = isIpv4Valido(ipInput)
    val podeConectar = ipValido && !conectando

    if (mostrarModelosCompativeis) {
        GatewayCompatibleModelsSheetContent(onBack = { mostrarModelosCompativeis = false }, c = c)
        return
    }

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
        Spacer(Modifier.height(LkSpacing.sm))
        EstadoConexaoSegmented(estado = estado, c = c)
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            text = "Conectar ao roteador",
            style = MaterialTheme.typography.headlineSmall,
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
            isError = ipInput.isNotBlank() && !ipValido,
            colors = fieldColors,
            shape = RoundedCornerShape(LkRadius.input),
        )
        if (ipInput.isNotBlank() && !ipValido) {
            Text(
                text = "Endereço IP inválido — use o formato 192.168.1.1",
                style = MaterialTheme.typography.labelSmall,
                color = LkColors.error,
            )
        }

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
            modifier = Modifier.testTag("gateway_link_modelos_compativeis"),
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
                        .background(LkColors.error.copy(alpha = 0.08f))
                        .padding(LkSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = LkColors.error,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(LkSpacing.sm))
                Text(
                    text = erroAtual,
                    style = MaterialTheme.typography.bodySmall,
                    color = LkColors.error,
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
}

/**
 * Formato de IPv4 (2b-i To-Be) — valida cada octeto no intervalo 0-255 antes de
 * habilitar "Conectar". Real (regex de faixa), não decorativo: "999.999.999.999"
 * ou "192.168.1" não passam. Alcançabilidade de rede (ping/reachability) fica
 * fora deste escopo — a validação aqui é só de formato.
 */
private val ipv4Regex =
    Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")

private fun isIpv4Valido(ip: String): Boolean = ipv4Regex.matches(ip.trim())

/**
 * Indicador visual do estado atual da sheet (2b-i To-Be) — Formulário/Conectando/Erro.
 * Só reflete [estado], não é selecionável: o estado real é resultado da tentativa
 * de conexão, nunca escolhido manualmente pelo usuário.
 */
@Composable
private fun EstadoConexaoSegmented(
    estado: GatewayConnectionSheetState,
    c: LkTokens,
) {
    val opcoes =
        listOf(
            "Formulário" to (estado is GatewayConnectionSheetState.Formulario),
            "Conectando" to (estado is GatewayConnectionSheetState.Conectando),
            "Erro" to (estado is GatewayConnectionSheetState.Erro),
        )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, c.border, RoundedCornerShape(20.dp))
                .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        opcoes.forEach { (label, ativo) ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (ativo) c.textPrimary else c.textSecondary,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (ativo) LkColors.accent.copy(alpha = 0.14f) else Color.Transparent)
                        .padding(vertical = 9.dp),
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
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
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
