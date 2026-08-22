package io.signallq.app.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import io.signallq.app.feature.settings.ThemePreference
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme
import io.signallq.app.ui.component.SignallQListRow
import io.signallq.app.ui.component.SignallQTopAppBar

/** Centro administrativo da jornada única. Não representa conta ou identidade remota. */
@Composable
fun PerfilScreen(
    appVersion: String,
    temaSelecionado: String,
    onVoltar: () -> Unit,
    onDefinirTemaSelecionado: (String) -> Unit,
    onAbrirAjustes: () -> Unit,
    onAbrirPrivacidade: () -> Unit,
    onAbrirNovidades: () -> Unit,
    onAbrirAjuda: () -> Unit,
    onAbrirTermos: () -> Unit,
    onAbrirSobre: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val temaEscuroAtivo =
        when (ThemePreference.parse(temaSelecionado)) {
            ThemePreference.DARK -> true
            ThemePreference.LIGHT -> false
            ThemePreference.SYSTEM -> isSystemInDarkTheme()
        }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = c.bgPrimary,
        topBar = {
            SignallQTopAppBar(
                title = "Perfil",
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = LkSpacing.lg, vertical = LkSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.xs)) {
                    Text(
                        text = "Perfil e aplicativo",
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "Preferências, privacidade e informações do SignallQ.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.onSurfaceVariant,
                    )
                }
            }
            item {
                TemaEscuroRow(
                    ativo = temaEscuroAtivo,
                    onAtivoChange = { ativo ->
                        onDefinirTemaSelecionado(
                            if (ativo) ThemePreference.DARK.chaveDataStore else ThemePreference.LIGHT.chaveDataStore,
                        )
                    },
                )
            }
            item {
                SignallQListRow(
                    title = "Ajustes",
                    subtitle = "Conexão, monitoramento e dados locais",
                    icon = Icons.Outlined.Settings,
                    onClick = onAbrirAjustes,
                )
            }
            item {
                SignallQListRow(
                    title = "Privacidade",
                    subtitle = "Consentimento e uso de dados",
                    icon = Icons.Outlined.PrivacyTip,
                    onClick = onAbrirPrivacidade,
                )
            }
            item {
                SignallQListRow(title = "Novidades", icon = Icons.Outlined.NewReleases, onClick = onAbrirNovidades)
            }
            item {
                SignallQListRow(
                    title = "Ajuda e suporte",
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    onClick = onAbrirAjuda,
                )
            }
            item {
                SignallQListRow(title = "Termos de uso", icon = Icons.Outlined.Description, onClick = onAbrirTermos)
            }
            item {
                SignallQListRow(title = "Sobre o SignallQ", icon = Icons.Outlined.Info, onClick = onAbrirSobre)
            }
            item {
                Text(
                    text = "Versão $appVersion",
                    modifier = Modifier.padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TemaEscuroRow(
    ativo: Boolean,
    onAtivoChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = LkSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "Tema escuro", style = MaterialTheme.typography.bodyLarge)
        Switch(checked = ativo, onCheckedChange = onAtivoChange)
    }
}

@Preview(name = "Perfil claro", showBackground = true)
@Preview(name = "Perfil escuro 200%", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 2f)
@Composable
private fun PerfilScreenPreview() {
    SignallQTheme {
        PerfilScreen(
            appVersion = "2.0.0",
            temaSelecionado = ThemePreference.SYSTEM.chaveDataStore,
            onVoltar = {},
            onDefinirTemaSelecionado = {},
            onAbrirAjustes = {},
            onAbrirPrivacidade = {},
            onAbrirNovidades = {},
            onAbrirAjuda = {},
            onAbrirTermos = {},
            onAbrirSobre = {},
        )
    }
}
