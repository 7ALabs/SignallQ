package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.signallq.app.R
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

private data class NovidadeItem(
    val tipo: String,
    val titulo: String,
    val descricao: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovidadesScreen(
    appVersion: String,
    onVoltar: () -> Unit,
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current
    var itens by remember { mutableStateOf<List<NovidadeItem>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf(false) }
    var tentativa by remember { mutableStateOf(0) }

    LaunchedEffect(tentativa) {
        carregando = true
        erro = false
        val resultado =
            withContext(Dispatchers.IO) {
                runCatching {
                    val text =
                        context.assets
                            .open("changelog.json")
                            .bufferedReader()
                            .use { it.readText() }
                    val arr = JSONArray(text)
                    (0 until arr.length()).map { i ->
                        val obj = arr.getJSONObject(i)
                        NovidadeItem(
                            tipo = obj.optString("tipo", "novo"),
                            titulo = obj.getString("titulo"),
                            descricao = obj.optString("descricao", ""),
                        )
                    }
                }
            }
        resultado.fold(
            onSuccess = { itens = it },
            onFailure = { erro = true },
        )
        carregando = false
    }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "Novidades",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.W600,
                                color = c.textPrimary,
                            )
                            Text(
                                "v$appVersion",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.textSecondary,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onVoltar) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.global_cd_voltar),
                                tint = c.textPrimary,
                            )
                        }
                    },
                    actions = {
                        Spacer(Modifier.width(40.dp))
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
                )
                HorizontalDivider(color = c.border, thickness = 1.dp)
            }
        },
    ) { padding ->
        when {
            carregando -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.novidades_carregando), style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                }
            }
            erro -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Não foi possível carregar as novidades.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textSecondary,
                        )
                        Spacer(Modifier.height(LkSpacing.md))
                        TextButton(onClick = { tentativa++ }) {
                            Text(stringResource(R.string.global_btn_tentar_novamente), color = LkColors.accent)
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(itens) { item ->
                        NovidadeRow(item = item, c = c)
                        HorizontalDivider(
                            color = c.border,
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = LkSpacing.lg),
                        )
                    }
                    item { Spacer(Modifier.navigationBarsPadding().height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun NovidadeRow(
    item: NovidadeItem,
    c: LkTokens,
) {
    val (badgeLabel, badgeCor) =
        when (item.tipo) {
            "novo" -> "NOVO" to LkColors.success
            "melhoria" -> "MELHORIA" to LkColors.accent
            "correcao" -> "CORREÇÃO" to LkColors.error
            else -> item.tipo.uppercase() to c.textSecondary
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(LkRadius.pill))
                    .background(badgeCor.copy(alpha = 0.14f))
                    .padding(horizontal = LkSpacing.sm, vertical = 2.dp),
        ) {
            Text(
                text = badgeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.W700,
                color = badgeCor,
            )
        }
        Spacer(Modifier.width(LkSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.descricao,
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                textAlign = TextAlign.Start,
            )
        }
    }
}
