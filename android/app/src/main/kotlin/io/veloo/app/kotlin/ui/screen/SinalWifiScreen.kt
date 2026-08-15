package io.signallq.app.ui.screen

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.WifiFind
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.signallq.app.sinalwifi.SinalWifiViewModel
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkPillBadge
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.SignalBars

/**
 * Ferramenta "Sinal WiFi" (GH#1201) do hub Ferramentas -- indicador dinâmico de sinal Wi-Fi
 * (RSSI/PHY rate) enquanto o usuário se movimenta pela casa, mais padrão Wi-Fi (4/5/6/6E/7) e
 * suporte a MU-MIMO.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SinalWifiScreen(
    temPermissaoLocalizacao: Boolean,
    localizacaoBloqueadaPermanentemente: Boolean,
    onSolicitarPermissaoLocalizacao: () -> Unit,
    onVoltar: () -> Unit,
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current
    val wifiManager =
        remember(context) {
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        }
    val viewModel =
        remember(wifiManager) {
            SinalWifiViewModel(
                wifiManager = wifiManager,
                permissaoConcedida = { temPermissaoLocalizacao },
            )
        }
    val uiState by viewModel.uiState.collectAsState()

    // Mesmo padrão de SinalScreen.kt: sheet contextual de permissão de localização, dispensável
    // com "Agora não" (não trava a tela, só a leitura de RSSI fica sem dado até conceder).
    var showLocalizacaoSheet by remember { mutableStateOf(false) }
    var localizacaoSheetDismissed by remember { mutableStateOf(false) }
    val locSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(temPermissaoLocalizacao, localizacaoSheetDismissed) {
        if (!temPermissaoLocalizacao && !localizacaoSheetDismissed) {
            showLocalizacaoSheet = true
        }
    }

    // Mesmo padrão de SinalScreen.kt (~408-416): amostragem só roda com a tela em foreground,
    // repeatOnLifecycle cancela sozinho ao sair de RESUMED (background ou saída do overlay).
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.iniciarAmostragem()
        }
    }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Sinal WiFi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Voltar",
                            tint = c.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(LkSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(LkSpacing.xxl))

            // Indicador central -- SignalBars ampliado (graphicsLayer) sem alterar o componente
            // compartilhado, que precisa continuar idêntico em SinalScreen.kt.
            Box(
                modifier = Modifier.size(width = 96.dp, height = 72.dp),
                contentAlignment = Alignment.Center,
            ) {
                val rssi = uiState.rssiAtual
                if (rssi != null) {
                    Box(modifier = Modifier.graphicsLayer(scaleX = 4f, scaleY = 4f)) {
                        SignalBars(rssiDbm = rssi)
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.WifiFind,
                        contentDescription = null,
                        tint = c.textTertiary,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }

            Spacer(Modifier.height(LkSpacing.md))

            Text(
                text = uiState.rssiAtual?.let { "$it dBm" } ?: "Aguardando leitura de sinal…",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )

            Spacer(Modifier.height(LkSpacing.xs))

            Text(
                text = uiState.linkSpeedMbps?.let { "$it Mbps" } ?: "—",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textSecondary,
            )

            Spacer(Modifier.height(LkSpacing.xxl))

            LkSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Padrão Wi-Fi",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.textSecondary,
                    )
                    Spacer(Modifier.height(LkSpacing.xs))
                    Text(
                        text = uiState.padraoWifi ?: "Não identificado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                    if (uiState.suportaMuMimo == true) {
                        Spacer(Modifier.height(LkSpacing.sm))
                        LkPillBadge(
                            text = "Suporta MU-MIMO",
                            containerColor = c.successContainer,
                            contentColor = c.onSuccessContainer,
                        )
                    }
                }
            }
        }
    }

    if (showLocalizacaoSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showLocalizacaoSheet = false
                localizacaoSheetDismissed = true
            },
            sheetState = locSheetState,
        ) {
            PermissaoLocalizacaoContextoSheet(
                bloqueadaPermanentemente = localizacaoBloqueadaPermanentemente,
                onConceder = {
                    showLocalizacaoSheet = false
                    onSolicitarPermissaoLocalizacao()
                },
                onAgoraNao = {
                    showLocalizacaoSheet = false
                    localizacaoSheetDismissed = true
                },
            )
        }
    }
}
