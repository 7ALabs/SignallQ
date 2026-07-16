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
import androidx.compose.ui.unit.dp
import io.signallq.app.core.network.contracts.gateway.GatewayConnectionResultado
import io.signallq.app.core.network.contracts.gateway.GatewayConnectionService
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.SheetDragHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

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

internal const val MENSAGEM_ERRO_ALCANCABILIDADE = "Não foi possível alcançar esse endereço na rede."

private const val TIMEOUT_ALCANCABILIDADE_MS = 2000
private val PORTAS_ADMIN_ROTEADOR = listOf(80, 443)

/**
 * Alcancabilidade real do gateway (2b-i To-Be) — connect TCP com timeout curto
 * nas portas comuns de admin de roteador (80/443), em vez de
 * [java.net.InetAddress.isReachable] (ICMP, costuma vir bloqueado em rede
 * movel/Wi-Fi Android). Alcancavel se QUALQUER uma das portas aceitar conexao;
 * refused/timeout em ambas = inalcancavel.
 */
private suspend fun alcancavelViaSocket(ip: String): Boolean =
    withContext(Dispatchers.IO) {
        PORTAS_ADMIN_ROTEADOR.any { porta ->
            runCatching {
                Socket().use { it.connect(InetSocketAddress(ip, porta), TIMEOUT_ALCANCABILIDADE_MS) }
                true
            }.getOrDefault(false)
        }
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
    // 2b-i To-Be: alcancabilidade real via TCP connect (porta 80/443). Seam de
    // teste — producao usa o default (Socket real), testes injetam um stub pra
    // nao depender de rede real no Robolectric.
    verificarAlcancabilidade: suspend (String) -> Boolean = ::alcancavelViaSocket,
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
            val ipAlvo = ipInput.trim()
            // 2b-i To-Be: alcancabilidade antes de autenticar — diferencia "nao
            // alcancei o roteador" (rede errada, roteador desligado) de
            // "usuario/senha errados", que so faz sentido testar depois.
            if (!verificarAlcancabilidade(ipAlvo)) {
                estado = GatewayConnectionSheetState.Erro(MENSAGEM_ERRO_ALCANCABILIDADE)
                return@launch
            }
            when (val resultado = conectar.conectar(ipAlvo, usuarioInput, senhaInput)) {
                is GatewayConnectionResultado.Sucesso -> {
                    estado = GatewayConnectionSheetState.Formulario
                    onConectado(ipAlvo, usuarioInput, senhaInput, lembrarSenha, manterConectado)
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

/**
 * Host da sheet real de "Modelos compatíveis" (GH#539) — o conteudo (catalogo
 * [io.signallq.app.core.network.contracts.gateway.PublicCompatibilityCatalog],
 * separacao validado/experimental) vive em [GatewayCompatibleModelsSheetContent],
 * mesmo arquivo de tela dedicado `GatewayCompatibleModelsSheet.kt`. Aqui so o
 * boilerplate de hospedagem (ModalBottomSheet + estado), igual ao padrao usado
 * em [GatewayCredentialsGuideSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GatewayCompatibleModelsSheet(onDismissRequest: () -> Unit) {
    val c = LocalLkTokens.current
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.bgSecondary,
    ) {
        GatewayCompatibleModelsSheetContent(onBack = onDismissRequest, c = c)
    }
}
