package io.linka.app.kotlin.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LkTokens
import io.linka.app.kotlin.ui.LocalLkTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

private data class ChangelogEntry(
    val versao: String,
    val data: String,
    val titulo: String,
    val itens: List<String>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovidadesScreen(
    appVersion: String,
    onVoltar: () -> Unit,
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current
    var entradas by remember { mutableStateOf<List<ChangelogEntry>>(emptyList()) }
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
                    val result = mutableListOf<ChangelogEntry>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val itensArr = obj.getJSONArray("itens")
                        result.add(
                            ChangelogEntry(
                                versao = obj.getString("versao"),
                                data = obj.getString("data"),
                                titulo = obj.getString("titulo"),
                                itens = (0 until itensArr.length()).map { itensArr.getString(it) },
                            ),
                        )
                    }
                    result
                }
            }
        resultado.fold(
            onSuccess = { entradas = it },
            onFailure = { erro = true },
        )
        carregando = false
    }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("O que há de novo", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = c.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        when {
            carregando -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Carregando…", style = MaterialTheme.typography.bodyMedium, color = c.textTertiary)
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
                            color = c.textTertiary,
                        )
                        Spacer(Modifier.height(LkSpacing.md))
                        TextButton(onClick = { tentativa++ }) {
                            Text("Tentar novamente", color = LkColors.accent)
                        }
                    }
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    items(entradas) { entrada ->
                        ChangelogCard(entrada = entrada, c = c)
                    }
                    item { Spacer(Modifier.navigationBarsPadding().height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ChangelogCard(
    entrada: ChangelogEntry,
    c: LkTokens,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm)
                .clip(RoundedCornerShape(16.dp))
                .background(c.bgSecondary)
                .border(1.dp, c.border, RoundedCornerShape(16.dp))
                .padding(LkSpacing.lg),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LkColors.accent.copy(alpha = 0.10f))
                            .padding(horizontal = LkSpacing.sm, vertical = 2.dp),
                ) {
                    Text(
                        text = entrada.versao,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.W600,
                        color = LkColors.accent,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(text = entrada.data, style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            }
            Spacer(Modifier.height(LkSpacing.xs))
            Text(
                text = entrada.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(LkSpacing.sm))
            entrada.itens.forEach { item ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("•", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                    Spacer(Modifier.width(4.dp))
                    Text(text = item, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                }
            }
        }
    }
}
