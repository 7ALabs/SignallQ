package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.feature.settings.ValidadorCidadeUf
import io.signallq.app.feature.settings.ValidadorVelocidadeContratada
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens

private val ESTADOS_BRASILEIROS =
    listOf(
        "AC",
        "AL",
        "AP",
        "AM",
        "BA",
        "CE",
        "DF",
        "ES",
        "GO",
        "MA",
        "MT",
        "MS",
        "MG",
        "PA",
        "PB",
        "PR",
        "PE",
        "PI",
        "RJ",
        "RN",
        "RS",
        "RO",
        "RR",
        "SC",
        "SP",
        "SE",
        "TO",
    )

/**
 * GH#1249 (recorte de #1227, item C) — banner mostrado quando o provedor detectado nesta rede
 * diverge do salvo E o usuário já confirmou o valor salvo explicitamente antes (senão a
 * sobrescrita é silenciosa, sem banner nenhum — ver [io.signallq.app.feature.settings.DetectorDivergenciaPerfilConexao]).
 */
@Composable
fun MinhaConexaoDivergenciaBanner(
    c: LkTokens,
    provedorDetectado: String,
    onUsarDetectado: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.primary.copy(alpha = 0.08f))
                .padding(LkSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Detectamos $provedorDetectado nesta rede.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Text(
                "Usar este provedor?",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
        TextButton(onClick = onUsarDetectado) {
            Text("Usar", color = c.primary, fontWeight = FontWeight.W600)
        }
    }
}

/**
 * Sheet de edicao de "Minha conexao" -- unificado com o mesmo padrao de bottom sheet
 * usado por PerfilEditSheet (drag handle + Column com scroll), em vez de tela cheia
 * separada. Poucos campos (operadora, velocidade, localizacao) cabem bem em sheet.
 *
 * GH#1249 (recorte de #1227) — `estadoUf`/`cidadeNome`/velocidades agora nullable (campo vazio
 * é estado explícito de "sem dado", nunca mais `0`/string vazia tratados como preenchidos), com
 * validação inline via [ValidadorVelocidadeContratada]/[ValidadorCidadeUf] — "Salvar" fica
 * desabilitado enquanto houver erro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinhaConexaoSheet(
    operadora: String?,
    estadoUf: String?,
    cidadeNome: String?,
    velocidadeContratadaDownMbps: Int?,
    velocidadeContratadaUpMbps: Int?,
    operadoraAutodetectada: String?,
    onSalvar: (operadora: String?, estadoUf: String?, cidadeNome: String?, downMbps: Int?, upMbps: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalLkTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var operadoraEdit by remember(operadora) { mutableStateOf(operadora.orEmpty()) }
    var estadoUfEdit by remember(estadoUf) { mutableStateOf(estadoUf.orEmpty()) }
    var cidadeEdit by remember(cidadeNome) { mutableStateOf(cidadeNome.orEmpty()) }
    var downMbpsEdit by remember(velocidadeContratadaDownMbps) {
        mutableStateOf(velocidadeContratadaDownMbps?.toString().orEmpty())
    }
    var upMbpsEdit by remember(velocidadeContratadaUpMbps) {
        mutableStateOf(velocidadeContratadaUpMbps?.toString().orEmpty())
    }

    // GH#1249 -- validação inline. Campo vazio é sempre válido (null); só valida quando há valor.
    val downMbpsValor = downMbpsEdit.toIntOrNull()
    val upMbpsValor = upMbpsEdit.toIntOrNull()
    val downMbpsErro = !ValidadorVelocidadeContratada.ehValida(downMbpsValor)
    val upMbpsErro = !ValidadorVelocidadeContratada.ehValida(upMbpsValor)
    val cidadeUfErro = !ValidadorCidadeUf.ehCombinacaoValida(cidadeEdit, estadoUfEdit)
    val formularioValido = !downMbpsErro && !upMbpsErro && !cidadeUfErro

    val fieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.primary,
            unfocusedBorderColor = c.border,
            focusedLabelColor = c.primary,
            unfocusedLabelColor = c.textSecondary,
            cursorColor = c.primary,
            focusedTextColor = c.textPrimary,
            unfocusedTextColor = c.textPrimary,
        )
    val fieldColorsErro =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.error,
            unfocusedBorderColor = c.error,
            focusedLabelColor = c.error,
            unfocusedLabelColor = c.error,
            cursorColor = c.error,
            focusedTextColor = c.textPrimary,
            unfocusedTextColor = c.textPrimary,
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            Text("Minha conexão", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = c.textPrimary)

            // Seção: Operadora
            MinhaConexaoSectionCard(c = c, title = "Operadora") {
                if (!operadoraAutodetectada.isNullOrBlank() && operadoraEdit.isBlank()) {
                    MinhaConexaoSugestaoChip(
                        label = "Detectada: $operadoraAutodetectada",
                        onClick = { operadoraEdit = operadoraAutodetectada },
                        c = c,
                    )
                    Spacer(Modifier.height(LkSpacing.sm))
                }
                OutlinedTextField(
                    value = operadoraEdit,
                    onValueChange = { operadoraEdit = it },
                    label = { Text("Operadora / ISP") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    singleLine = true,
                    shape = RoundedCornerShape(LkRadius.input),
                )
            }

            // Seção: Velocidade contratada
            MinhaConexaoSectionCard(c = c, title = "Velocidade contratada") {
                Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = downMbpsEdit,
                            onValueChange = { downMbpsEdit = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Download (Mbps)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = if (downMbpsErro) fieldColorsErro else fieldColors,
                            isError = downMbpsErro,
                            singleLine = true,
                            shape = RoundedCornerShape(LkRadius.input),
                        )
                        if (downMbpsErro) {
                            Text(
                                "Informe um valor entre 1 e ${ValidadorVelocidadeContratada.LIMITE_SUPERIOR_MBPS} Mbps",
                                color = c.error,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp, start = 4.dp),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = upMbpsEdit,
                            onValueChange = { upMbpsEdit = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Upload (Mbps)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = if (upMbpsErro) fieldColorsErro else fieldColors,
                            isError = upMbpsErro,
                            singleLine = true,
                            shape = RoundedCornerShape(LkRadius.input),
                        )
                        if (upMbpsErro) {
                            Text(
                                "Informe um valor entre 1 e ${ValidadorVelocidadeContratada.LIMITE_SUPERIOR_MBPS} Mbps",
                                color = c.error,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp, start = 4.dp),
                            )
                        }
                    }
                }
            }

            // Seção: Localização
            MinhaConexaoSectionCard(c = c, title = "Localização") {
                EstadoUfDropdown(
                    estadoUf = estadoUfEdit,
                    onChange = { estadoUfEdit = it },
                    c = c,
                )
                Spacer(Modifier.height(LkSpacing.sm))
                OutlinedTextField(
                    value = cidadeEdit,
                    onValueChange = { cidadeEdit = it },
                    label = { Text("Cidade") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (cidadeUfErro) fieldColorsErro else fieldColors,
                    isError = cidadeUfErro,
                    singleLine = true,
                    shape = RoundedCornerShape(LkRadius.input),
                )
                if (cidadeUfErro) {
                    Text(
                        "Informe cidade e UF juntas, ou deixe as duas em branco",
                        color = c.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, start = 4.dp),
                    )
                }
            }

            // Botão Salvar
            Button(
                enabled = formularioValido,
                onClick = {
                    onSalvar(
                        operadoraEdit.trim().takeIf { it.isNotBlank() },
                        estadoUfEdit.takeIf { it.isNotBlank() },
                        cidadeEdit.trim().takeIf { it.isNotBlank() },
                        downMbpsValor,
                        upMbpsValor,
                    )
                    onDismiss()
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.primary),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Text(
                    "Salvar",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                )
            }
        }
    }
}

// ─── Helpers privados ────────────────────────────────────────────────────────

@Composable
private fun MinhaConexaoSectionCard(
    c: LkTokens,
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.surfaceContainer)
                .padding(LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = c.textSecondary,
            modifier = Modifier.padding(bottom = LkSpacing.xs),
        )
        content()
    }
}

@Composable
private fun MinhaConexaoSugestaoChip(
    label: String,
    onClick: () -> Unit,
    c: LkTokens,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(LkRadius.pill))
                .background(c.primary.copy(alpha = 0.14f))
                .border(1.dp, c.primary.copy(alpha = 0.30f), RoundedCornerShape(LkRadius.pill))
                .semantics { role = Role.Button }
                .clickable(onClick = onClick)
                .padding(horizontal = LkSpacing.md, vertical = LkSpacing.xs),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.W500,
            color = c.primary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EstadoUfDropdown(
    estadoUf: String,
    onChange: (String) -> Unit,
    c: LkTokens,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = estadoUf,
            onValueChange = {},
            readOnly = true,
            label = { Text("Estado (UF)") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
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
            singleLine = true,
            shape = RoundedCornerShape(LkRadius.input),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(c.bgSecondary),
        ) {
            ESTADOS_BRASILEIROS.forEach { uf ->
                DropdownMenuItem(
                    text = {
                        Text(
                            uf,
                            color = if (uf == estadoUf) c.primary else c.textPrimary,
                            fontWeight = if (uf == estadoUf) FontWeight.W600 else FontWeight.W400,
                        )
                    },
                    onClick = {
                        onChange(uf)
                        expanded = false
                    },
                    modifier =
                        Modifier.background(
                            if (uf == estadoUf) c.primary.copy(alpha = 0.08f) else c.bgSecondary,
                        ),
                )
            }
        }
    }
}
