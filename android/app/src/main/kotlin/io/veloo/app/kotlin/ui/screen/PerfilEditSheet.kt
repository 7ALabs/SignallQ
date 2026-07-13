package io.signallq.app.ui.screen

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.ui.IspInfo
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens

// ─── Perfil edit sheet ────────────────────────────────────────────────────────
// GH#936 — Fase 7 MD3 (6a): extraido de AjustesScreen.kt. Entrada pela tela de
// Perfil (hero card) e pelo avatar no TopBar de qualquer tela (AppShell.kt).

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PerfilEditSheet(
    c: LkTokens,
    nomeAtual: String,
    fotoUriAtual: String?,
    deviceName: String,
    appVersion: String,
    ispInfo: IspInfo? = null,
    estadoConexao: EstadoConexao? = null,
    onDismiss: () -> Unit,
    onSalvar: (nome: String, fotoUri: String?) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nomeInput by remember { mutableStateOf(nomeAtual) }
    var fotoUriInput by remember { mutableStateOf(fotoUriAtual) }

    val pickerFoto =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                fotoUriInput = uri.toString()
            }
        }

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
            Text("Meu perfil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = c.textPrimary)

            // Avatar
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                UserAvatar(
                    fotoUri = fotoUriInput,
                    fallbackInitial = nomeInput.firstOrNull() ?: deviceName.firstOrNull(),
                    size = 80.dp,
                    onClick = { pickerFoto.launch("image/*") },
                )
            }
            Text(
                "Toque no avatar para alterar a foto",
                style = MaterialTheme.typography.labelMedium,
                color = c.textTertiary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

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

            OutlinedTextField(
                value = nomeInput,
                onValueChange = { nomeInput = it },
                label = { Text("Seu nome ou apelido") },
                placeholder = { Text(deviceName.ifBlank { "Ex: João" }, color = c.textTertiary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(8.dp),
            )

            HorizontalDivider(color = c.border)
            val tipoConexao =
                when (estadoConexao) {
                    EstadoConexao.wifi -> "Wi-Fi"
                    EstadoConexao.movel -> ispInfo?.isp?.takeIf { it.isNotEmpty() } ?: "Rede móvel"
                    EstadoConexao.ethernet -> "Ethernet"
                    else -> "Sem conexão"
                }
            val localizacao = listOfNotNull(ispInfo?.region, ispInfo?.country).joinToString(", ").ifBlank { null }

            ispInfo?.isp?.let { InfoRow(c, "Operadora / ISP", it) }
            if (ispInfo?.isp != null) HorizontalDivider(color = c.border)
            ispInfo?.ip?.let { InfoRow(c, "IP Público", it) }
            if (ispInfo?.ip != null) HorizontalDivider(color = c.border)
            InfoRow(c, "Conexão", tipoConexao)
            HorizontalDivider(color = c.border)
            localizacao?.let { InfoRow(c, "Localização", it) }
            if (localizacao != null) HorizontalDivider(color = c.border)
            InfoRow(c, "Versão", "v$appVersion")
            HorizontalDivider(color = c.border)

            Spacer(Modifier.height(LkSpacing.sm))
            Button(
                onClick = { onSalvar(nomeInput.trim(), fotoUriInput) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
            ) {
                Text("Salvar perfil")
            }
        }
    }
}

@Composable
internal fun UserAvatar(
    fotoUri: String?,
    fallbackInitial: Char?,
    size: Dp,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val bitmap =
        remember(fotoUri) {
            fotoUri?.let { uriStr ->
                runCatching {
                    context.contentResolver
                        .openInputStream(uriStr.toUri())
                        ?.use { stream -> BitmapFactory.decodeStream(stream)?.asImageBitmap() }
                }.getOrNull()
            }
        }
    Box(
        modifier =
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(LkColors.accent.copy(alpha = 0.12f))
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
            )
        } else {
            Text(
                text = fallbackInitial?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.W700,
                color = LkColors.accent,
            )
        }
    }
}
