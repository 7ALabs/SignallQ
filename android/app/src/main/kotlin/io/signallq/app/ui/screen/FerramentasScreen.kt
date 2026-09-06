package io.signallq.app.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.NetworkWifi
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme
import io.signallq.app.ui.component.SignallQListRow

sealed interface FerramentaDisponibilidade {
    data object Disponivel : FerramentaDisponibilidade

    data class PermissaoNecessaria(
        val proximoPasso: String,
    ) : FerramentaDisponibilidade

    data class IndisponivelRemotamente(
        val proximoPasso: String,
    ) : FerramentaDisponibilidade

    data class Offline(
        val proximoPasso: String,
    ) : FerramentaDisponibilidade

    data class Oculta(
        val motivo: String,
    ) : FerramentaDisponibilidade
}

private data class FerramentaVisual(
    val titulo: String,
    val descricao: String,
    val icon: ImageVector,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FerramentasScreen(
    onAbrirMenu: () -> Unit,
    onAbrirSinalCanais: () -> Unit = {},
    onAbrirDispositivos: () -> Unit = {},
    onAbrirEquipamentoInternet: () -> Unit = {},
    onAbrirPing: () -> Unit = {},
    onAbrirDns: () -> Unit = {},
    onAbrirLaudo: () -> Unit = {},
    onAbrirMonitoramento: () -> Unit = {},
    onAbrirJogos: () -> Unit = {},
    onAbrirSinalWifi: () -> Unit = {},
    disponibilidade: (TipoFerramenta) -> FerramentaDisponibilidade = { FerramentaDisponibilidade.Disponivel },
    onRegistrarAbertura: (TipoFerramenta) -> Unit = {},
    ferramentaRecomendada: TipoFerramenta? = null,
    onVoltar: (() -> Unit)? = null,
) {
    val c = LocalLkTokens.current
    var feedback by remember { mutableStateOf<String?>(null) }
    val callbacks =
        mapOf(
            TipoFerramenta.SINAL_CANAIS_MOVEL to onAbrirSinalCanais,
            TipoFerramenta.SINAL_WIFI to onAbrirSinalWifi,
            TipoFerramenta.DISPOSITIVOS to onAbrirDispositivos,
            TipoFerramenta.EQUIPAMENTO_INTERNET to onAbrirEquipamentoInternet,
            TipoFerramenta.PING to onAbrirPing,
            TipoFerramenta.DNS to onAbrirDns,
            TipoFerramenta.LAUDO to onAbrirLaudo,
            TipoFerramenta.MONITORAMENTO to onAbrirMonitoramento,
            TipoFerramenta.MODO_JOGOS to onAbrirJogos,
        )
    val ferramentasVisiveis =
        CatalogoFerramentas.todos
            .map { tipo -> tipo to disponibilidade(tipo) }
            .filterNot { (_, estado) -> estado is FerramentaDisponibilidade.Oculta }
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            if (onVoltar == null) {
                TopAppBar(
                    title = { Text("Ferramentas") },
                    actions = {
                        IconButton(onClick = onAbrirMenu) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Abrir ajustes")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bgPrimary),
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text("Ferramentas") },
                    navigationIcon = {
                        IconButton(onClick = onVoltar) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    actions = {
                        IconButton(onClick = onAbrirMenu) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Abrir ajustes")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = LkSpacing.lg, vertical = LkSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            item {
                Text(
                    text = "Ferramentas de rede",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "Use quando precisar investigar um ponto específico.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.onSurfaceVariant,
                )
            }
            items(ferramentasVisiveis, key = { (tipo, _) -> tipo.name }) { (tipo, estado) ->
                val visual = tipo.visual()
                SignallQListRow(
                    title = visual.titulo,
                    subtitle = estado.subtitle(visual.descricao, tipo == ferramentaRecomendada),
                    icon = visual.icon,
                    onClick = {
                        when (estado) {
                            FerramentaDisponibilidade.Disponivel,
                            is FerramentaDisponibilidade.PermissaoNecessaria,
                            -> {
                                onRegistrarAbertura(tipo)
                                callbacks.getValue(tipo).invoke()
                            }
                            is FerramentaDisponibilidade.IndisponivelRemotamente -> {
                                callbacks.getValue(tipo).invoke()
                                feedback = estado.proximoPasso
                            }
                            is FerramentaDisponibilidade.Offline -> feedback = estado.proximoPasso
                            is FerramentaDisponibilidade.Oculta -> Unit
                        }
                    },
                )
            }
            feedback?.let { message ->
                item {
                    Text(
                        text = message,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun FerramentaDisponibilidade.subtitle(
    base: String,
    recomendado: Boolean,
): String {
    val prefix = if (recomendado) "Recomendado para você · " else ""
    val state =
        when (this) {
            FerramentaDisponibilidade.Disponivel -> base
            is FerramentaDisponibilidade.PermissaoNecessaria -> "Permissão necessária · $proximoPasso"
            is FerramentaDisponibilidade.IndisponivelRemotamente -> "Temporariamente indisponível · $proximoPasso"
            is FerramentaDisponibilidade.Offline -> "Sem conexão · $proximoPasso"
            is FerramentaDisponibilidade.Oculta -> motivo
        }
    return prefix + state
}

private fun TipoFerramenta.visual(): FerramentaVisual =
    when (this) {
        TipoFerramenta.SINAL_CANAIS_MOVEL -> FerramentaVisual("Wi-Fi e rede móvel", "Veja o sinal e os canais da sua rede", Icons.Outlined.SignalCellularAlt)
        TipoFerramenta.SINAL_WIFI -> FerramentaVisual("Encontrar um bom lugar", "Ande pela casa acompanhando o sinal Wi-Fi", Icons.Outlined.NetworkWifi)
        TipoFerramenta.DISPOSITIVOS -> FerramentaVisual("Quem está usando sua rede", "Veja os aparelhos conectados", Icons.Outlined.Devices)
        TipoFerramenta.EQUIPAMENTO_INTERNET -> FerramentaVisual("Seu equipamento", "Veja o estado do modem ou da ONT", Icons.Outlined.Router)
        TipoFerramenta.PING -> FerramentaVisual("Tempo de resposta", "Veja se há atraso até um endereço", Icons.Outlined.NetworkCheck)
        TipoFerramenta.DNS -> FerramentaVisual("Abertura de sites", "Compare servidores que ajudam a encontrar sites", Icons.Outlined.Dns)
        TipoFerramenta.LAUDO -> FerramentaVisual("Relatório para sua operadora", "Gere um resumo completo da conexão", Icons.Outlined.Description)
        TipoFerramenta.MONITORAMENTO -> FerramentaVisual("Acompanhar conexão", "Receba alertas quando algo mudar", Icons.Outlined.MonitorHeart)
        TipoFerramenta.MODO_JOGOS -> FerramentaVisual("Jogos online", "Veja se sua conexão pode causar atrasos", Icons.Outlined.SportsEsports)
    }

@Preview(name = "Ferramentas claro", showBackground = true)
@Preview(name = "Ferramentas escuro 200%", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 2f)
@Composable
private fun FerramentasScreenPreview() {
    SignallQTheme { FerramentasScreen(onAbrirMenu = {}) }
}
