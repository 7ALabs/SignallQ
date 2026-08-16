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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.NetworkWifi
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import io.signallq.app.R
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme
import io.signallq.app.ui.component.SignallQListRow
import io.signallq.app.ui.component.SignallQTopAppBar

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
            SignallQTopAppBar(
                title = "Ferramentas",
                navigationIcon = {
                    IconButton(onClick = onVoltar ?: onAbrirMenu) {
                        val guided = LocalAppShellMode.current == AppShellMode.Guided2
                        Icon(
                            imageVector =
                                when {
                                    onVoltar != null -> Icons.AutoMirrored.Filled.ArrowBack
                                    guided -> Icons.Filled.AccountCircle
                                    else -> Icons.Filled.Menu
                                },
                            contentDescription =
                                when {
                                    onVoltar != null -> "Voltar"
                                    guided -> stringResource(R.string.ajustes_cd_editar_perfil)
                                    else -> stringResource(R.string.appshell_cd_abrir_menu)
                                },
                        )
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
        TipoFerramenta.SINAL_CANAIS_MOVEL -> FerramentaVisual("Sinal e canais", "Wi-Fi, canais e rede móvel", Icons.Outlined.SignalCellularAlt)
        TipoFerramenta.SINAL_WIFI -> FerramentaVisual("Sinal Wi-Fi ao vivo", "Intensidade enquanto você anda pela casa", Icons.Outlined.NetworkWifi)
        TipoFerramenta.DISPOSITIVOS -> FerramentaVisual("Dispositivos", "Quem está na sua rede", Icons.Outlined.Devices)
        TipoFerramenta.EQUIPAMENTO_INTERNET -> FerramentaVisual("Equipamento de internet", "Status do modem ou ONT", Icons.Outlined.Router)
        TipoFerramenta.PING -> FerramentaVisual("Ping", "Tempo de resposta para um endereço", Icons.Outlined.NetworkCheck)
        TipoFerramenta.DNS -> FerramentaVisual("DNS", "Compare servidores", Icons.Outlined.Dns)
        TipoFerramenta.LAUDO -> FerramentaVisual("Laudo", "Resumo técnico completo", Icons.Outlined.Description)
        TipoFerramenta.MONITORAMENTO -> FerramentaVisual("Monitoramento", "Alertas em segundo plano", Icons.Outlined.MonitorHeart)
        TipoFerramenta.MODO_JOGOS -> FerramentaVisual("Modo gamer", "Teste para jogos específicos", Icons.Outlined.SportsEsports)
    }

@Preview(name = "Ferramentas claro", showBackground = true)
@Preview(name = "Ferramentas escuro 200%", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = 2f)
@Composable
private fun FerramentasScreenPreview() {
    SignallQTheme { FerramentasScreen(onAbrirMenu = {}) }
}
