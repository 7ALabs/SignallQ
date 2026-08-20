package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.feature.devices.EstadoScanDispositivos
import io.signallq.app.feature.devices.ResultadoCorrelacaoTopologia
import io.signallq.app.feature.devices.SnapshotScanDispositivos
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.SignallQOfflineBanner

/**
 * Tela "Dispositivos conectados" — composição pura (Scaffold, TopBar, roteamento de estado).
 *
 * Issue #1663 (épico #1647, Task 2.0.15) extraiu a lista (ver [DispositivosLista.kt]) e as
 * sheets de detalhe (ver [DispositivoDetalheSheet.kt]) deste arquivo, que antes concentrava
 * 1381 linhas (dívida crítica, §7 da regra de higiene). Este arquivo mantém só a composição:
 * TopBar, delegação entre estado sem-Wi-Fi/vazio/lista, e passagem de parâmetros.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispositivosScreen(
    snapshotDevices: SnapshotScanDispositivos,
    snapshotRede: SnapshotRede,
    onRefresh: () -> Unit,
    apelidos: Map<String, String>,
    onSalvarApelido: (mac: String, apelido: String) -> Unit,
    onVoltar: (() -> Unit)? = null,
    // GH#531 — resumo "2,4G + 5G" das bandas Wi-Fi do gateway conectado, exibido
    // no subtítulo do GatewayItem na seção INFRAESTRUTURA. Null quando sem dado.
    bandasWifi: String? = null,
    /** Toggle remoto (Firebase Remote Config) + gate de consentimento UMP -- issue #555.
     *  Default `false`: nunca mostra anuncio sem sinal explicito de que pode. */
    adsEnabled: Boolean = false,
    /** #983 (Fase 4) — correlacao best-effort topologia/gateway, chaveada por id do dispositivo
     *  (ver MainViewModel.correlacoesTopologia). Mapa vazio (default) preserva o comportamento
     *  anterior a Fase 4 — nenhuma secao nova aparece no detalhe do dispositivo. */
    correlacoesTopologia: Map<String, ResultadoCorrelacaoTopologia> = emptyMap(),
) {
    val c = LocalLkTokens.current

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            // GH#1079: migrado de Column/Row cru para TopAppBar real do M3 -- o layout
            // manual nao aplicava inset de status bar/notch (`.statusBarsPadding()`),
            // diferente das outras 14 telas do app que ja usam TopAppBar/
            // CenterAlignedTopAppBar reais.
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(c.bgPrimary),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "Dispositivos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.W600,
                            color = c.textPrimary,
                        )
                    },
                    navigationIcon = {
                        if (onVoltar != null) {
                            IconButton(onClick = onVoltar) {
                                Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar", tint = c.textPrimary)
                            }
                        }
                    },
                    actions = {
                        val escaneando = snapshotDevices.estado == EstadoScanDispositivos.varrendo
                        IconButton(onClick = onRefresh, enabled = !escaneando) {
                            if (escaneando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = c.primary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Escanear rede",
                                    tint = c.textPrimary,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bgPrimary),
                )
                HorizontalDivider(color = c.outlineVariant, thickness = 1.dp)
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            if (!snapshotRede.conectado) {
                SignallQOfflineBanner()
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f),
            ) {
                if (snapshotRede.estadoConexao != EstadoConexao.wifi) {
                    SemWifiFallback(c = c, hasDadosMoveis = snapshotRede.estadoConexao == EstadoConexao.movel)
                    return@Box
                }

                val dispositivos = snapshotDevices.dispositivos
                val isLoading = snapshotDevices.estado == EstadoScanDispositivos.varrendo
                val erro = snapshotDevices.erroMensagem

                if (dispositivos.isEmpty()) {
                    EmptyStateDispositivos(
                        c = c,
                        isLoading = isLoading,
                        progresso = snapshotDevices.progressoPercentual,
                        estado = snapshotDevices.estado,
                        erro = erro,
                        onRefresh = onRefresh,
                    )
                } else {
                    DispositivosLista(
                        c = c,
                        dispositivos = dispositivos,
                        isLoading = isLoading,
                        resultadoParcial = snapshotDevices.estado == EstadoScanDispositivos.concluidoParcial,
                        erro = erro,
                        onRefresh = onRefresh,
                        apelidos = apelidos,
                        onSalvarApelido = onSalvarApelido,
                        bandasWifi = bandasWifi,
                        adsEnabled = adsEnabled,
                        correlacoesTopologia = correlacoesTopologia,
                    )
                }
            } // Box
        } // Column
    }
}
