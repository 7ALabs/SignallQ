package io.signallq.app.ui.screen

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.signallq.app.sinalwifi.SinalWifiUiState
import io.signallq.app.sinalwifi.SinalWifiViewModel
import io.signallq.app.sinalwifi.intentAcaoLigarWifi
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkLiveIndicator
import io.signallq.app.ui.component.LkPillBadge
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.SignalBars
import io.signallq.app.ui.component.SignallQScreenState
import io.signallq.app.ui.component.SignallQStatefulScreen
import io.signallq.app.ui.component.animacoesDoSistemaDesativadas
import io.signallq.app.ui.component.signalColor

/**
 * Ferramenta "Sinal WiFi" (GH#1201) do hub Ferramentas -- indicador dinâmico de sinal Wi-Fi
 * (RSSI/PHY rate) enquanto o usuário se movimenta pela casa, mais padrão Wi-Fi (4/5/6/6E/7) e
 * suporte a MU-MIMO.
 *
 * Migração pra ferramenta 2.0 (issue #1668, épico #1647): decisões de produto do Luiz
 * (2026-08-19) --
 * 1. Categoria simples (Excelente/Bom/Regular/Fraco, vocabulário já usado por
 *    [signalQuality]/`SinalMovelClassificacao.kt`) em destaque, dBm como detalhe secundário --
 *    não promete precisão de cômodo/planta que o celular não sustenta.
 * 2. Wi-Fi desligado e permissão negada resolvem com um botão direto, sem sair do app
 *    ([SignallQStatefulScreen] com `Offline`/`PermissionRequired`).
 * 3. Troca direta -- sem manter a versão antiga em paralelo/fallback por flag (mesmo padrão de
 *    #1661/#1662, exceção consciente à regra transversal do épico).
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
    val movimentoReduzido = animacoesDoSistemaDesativadas()

    // Mesmo padrão de SinalScreen.kt: sheet contextual de permissão de localização, dispensável
    // com "Agora não" (não trava a tela -- o corpo abaixo mostra o mesmo pedido de forma
    // persistente via SignallQStatefulScreen, então dispensar o sheet não deixa a pessoa sem
    // saída, ver GH#1668).
    var showLocalizacaoSheet by remember { mutableStateOf(false) }
    var localizacaoSheetDismissed by remember { mutableStateOf(false) }
    val locSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(temPermissaoLocalizacao, localizacaoSheetDismissed) {
        if (!temPermissaoLocalizacao && !localizacaoSheetDismissed) {
            showLocalizacaoSheet = true
        }
    }

    // Mesmo padrão de SinalScreen.kt (~408-416): amostragem só roda com a tela em foreground,
    // repeatOnLifecycle cancela sozinho ao sair de RESUMED (background ou saída do overlay) --
    // é o que garante que o scan Wi-Fi (caro em bateria) não vaza pra background.
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
        Box(modifier = Modifier.padding(padding)) {
            when {
                !uiState.wifiHabilitado ->
                    SignallQStatefulScreen(
                        state =
                            SignallQScreenState.Offline(
                                title = "Wi-Fi desligado",
                                message = "Ligue o Wi-Fi para acompanhar a intensidade do sinal enquanto você anda pela casa.",
                            ),
                        actionLabel = "Ligar Wi-Fi",
                        onAction = { ligarWifi(context, wifiManager) },
                        modifier = Modifier.fillMaxSize(),
                    ) {}
                !temPermissaoLocalizacao ->
                    SignallQStatefulScreen(
                        state =
                            SignallQScreenState.PermissionRequired(
                                title = if (localizacaoBloqueadaPermanentemente) "Permissão bloqueada" else "Permissão necessária",
                                message =
                                    if (localizacaoBloqueadaPermanentemente) {
                                        "A permissão de localização foi bloqueada nas configurações do Android. " +
                                            "Ela é exigida para ler a intensidade das redes Wi-Fi ao redor."
                                    } else {
                                        "O Android exige permissão de localização para medir a intensidade do sinal Wi-Fi."
                                    },
                            ),
                        actionLabel = if (localizacaoBloqueadaPermanentemente) "Abrir ajustes do Android" else "Conceder permissão",
                        onAction = onSolicitarPermissaoLocalizacao,
                        modifier = Modifier.fillMaxSize(),
                    ) {}
                !uiState.amostrado ->
                    SignallQStatefulScreen(
                        state = SignallQScreenState.Loading,
                        modifier = Modifier.fillMaxSize(),
                    ) {}
                !uiState.conectado ->
                    SignallQStatefulScreen(
                        state =
                            SignallQScreenState.Empty(
                                title = "Sem conexão Wi-Fi",
                                message = "Conecte-se a uma rede Wi-Fi para acompanhar a intensidade do sinal.",
                            ),
                        modifier = Modifier.fillMaxSize(),
                    ) {}
                else ->
                    SinalWifiConteudoAoVivo(
                        uiState = uiState,
                        movimentoReduzido = movimentoReduzido,
                        c = c,
                    )
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

@Composable
private fun SinalWifiConteudoAoVivo(
    uiState: SinalWifiUiState,
    movimentoReduzido: Boolean,
    c: LkTokens,
) {
    val rssi = uiState.rssiAtual ?: return
    val banda = uiState.banda
    val categoria = signalQuality(rssi, banda)
    val cor = signalColor(rssi, banda, c)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(LkSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(LkSpacing.lg))
        LkLiveIndicator(label = "Ao vivo", estatico = movimentoReduzido)
        Spacer(Modifier.height(LkSpacing.xl))

        // Indicador central -- SignalBars ampliado (graphicsLayer) sem alterar o componente
        // compartilhado, que precisa continuar idêntico em SinalScreen.kt.
        Box(
            modifier = Modifier.size(width = 96.dp, height = 72.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.graphicsLayer(scaleX = 4f, scaleY = 4f)) {
                SignalBars(rssiDbm = rssi, banda = banda)
            }
        }

        Spacer(Modifier.height(LkSpacing.md))

        // Categoria em destaque + dBm como detalhe técnico secundário -- decisão de produto
        // (Luiz, 2026-08-19): atende quem não entende de rede sem esconder o valor técnico de
        // quem entende. Mesmo padrão já usado em MobileDetailCard/SinalMovelSection (#1662).
        Text(
            text = categoria,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.W700,
            color = cor,
            modifier =
                Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "Sinal Wi-Fi $categoria, $rssi dBm"
                },
        )
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            text = "$rssi dBm",
            style = MaterialTheme.typography.bodyLarge,
            color = c.textSecondary,
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

/**
 * Issue #1668, decisão de produto (Luiz, 2026-08-19): botão direto que resolve Wi-Fi desligado
 * sem o usuário sair do app. Ver KDoc de [intentAcaoLigarWifi] para o porquê da divisão por SDK.
 */
private fun ligarWifi(
    context: Context,
    wifiManager: WifiManager,
) {
    val intent = intentAcaoLigarWifi(Build.VERSION.SDK_INT)
    if (intent == null) {
        @Suppress("DEPRECATION")
        wifiManager.setWifiEnabled(true)
        return
    }
    runCatching { context.startActivity(intent) }
        .onFailure {
            // OEM sem o painel do sistema disponível -- cai pra tela de Ajustes de Wi-Fi
            // completa, ainda dentro do fluxo padrão do Android (não é uma falha silenciosa).
            runCatching { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
        }
}
