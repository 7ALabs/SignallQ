package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.LkColors
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinhaConexaoScreen(
    operadora: String,
    estadoUf: String,
    cidadeNome: String,
    velocidadeContratadaDownMbps: Int,
    velocidadeContratadaUpMbps: Int,
    operadoraAutodetectada: String?,
    onSalvar: (operadora: String, estadoUf: String, cidadeNome: String, downMbps: Int, upMbps: Int) -> Unit,
    onVoltar: () -> Unit,
) {
    val c = LocalLkTokens.current
    var operadoraEdit by remember(operadora) { mutableStateOf(operadora) }
    var estadoUfEdit by remember(estadoUf) { mutableStateOf(estadoUf) }
    var cidadeEdit by remember(cidadeNome) { mutableStateOf(cidadeNome) }
    var downMbpsEdit by remember(velocidadeContratadaDownMbps) {
        mutableStateOf(if (velocidadeContratadaDownMbps > 0) velocidadeContratadaDownMbps.toString() else "")
    }
    var upMbpsEdit by remember(velocidadeContratadaUpMbps) {
        mutableStateOf(if (velocidadeContratadaUpMbps > 0) velocidadeContratadaUpMbps.toString() else "")
    }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Minha Conexão",
                        style = MaterialTheme.typography.titleLarge,
                        color = c.textPrimary,
                        fontWeight = FontWeight.W600,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Voltar",
                            tint = c.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(LkSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            // Seção: Operadora
            item {
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
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LkColors.accent,
                                focusedLabelColor = LkColors.accent,
                            ),
                        singleLine = true,
                    )
                }
            }

            // Seção: Velocidade Contratada
            item {
                MinhaConexaoSectionCard(c = c, title = "Velocidade Contratada") {
                    Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
                        OutlinedTextField(
                            value = downMbpsEdit,
                            onValueChange = { downMbpsEdit = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Download (Mbps)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LkColors.accent,
                                    focusedLabelColor = LkColors.accent,
                                ),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = upMbpsEdit,
                            onValueChange = { upMbpsEdit = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Upload (Mbps)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LkColors.accent,
                                    focusedLabelColor = LkColors.accent,
                                ),
                            singleLine = true,
                        )
                    }
                }
            }

            // Seção: Localização
            item {
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
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LkColors.accent,
                                focusedLabelColor = LkColors.accent,
                            ),
                        singleLine = true,
                    )
                }
            }

            // Botão Salvar
            item {
                Button(
                    onClick = {
                        onSalvar(
                            operadoraEdit.trim(),
                            estadoUfEdit,
                            cidadeEdit.trim(),
                            downMbpsEdit.toIntOrNull() ?: 0,
                            upMbpsEdit.toIntOrNull() ?: 0,
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
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
                .background(c.bgSecondary)
                .padding(LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.W600,
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
                .clip(RoundedCornerShape(8.dp))
                .background(LkColors.accent.copy(alpha = 0.10f))
                .border(1.dp, LkColors.accent.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                .semantics { role = Role.Button }
                .clickable(onClick = onClick)
                .padding(horizontal = LkSpacing.md, vertical = LkSpacing.xs),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.W500,
            color = LkColors.accent,
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
                    focusedBorderColor = LkColors.accent,
                    focusedLabelColor = LkColors.accent,
                ),
            singleLine = true,
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
                            color = if (uf == estadoUf) LkColors.accent else c.textPrimary,
                            fontWeight = if (uf == estadoUf) FontWeight.W600 else FontWeight.W400,
                        )
                    },
                    onClick = {
                        onChange(uf)
                        expanded = false
                    },
                    modifier =
                        Modifier.background(
                            if (uf == estadoUf) LkColors.accent.copy(alpha = 0.08f) else c.bgSecondary,
                        ),
                )
            }
        }
    }
}
