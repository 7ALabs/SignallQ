package io.signallq.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.signallq.app.BuildConfig
import io.signallq.app.R
import io.signallq.app.ads.AdSlot
import io.signallq.app.bssidElegivelParaAutoconexao
import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.core.diagnostico.topology.model.NatStatus
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.core.network.contracts.gateway.GatewayConnectionResultado
import io.signallq.app.core.network.contracts.gateway.GatewayConnectionService
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.core.telephony.MovelSimSnapshot
import io.signallq.app.core.telephony.MovelSnapshot
import io.signallq.app.feature.devices.ehClienteFinal
import io.signallq.app.feature.dns.SnapshotBenchmarkDns
import io.signallq.app.feature.fibra.SnapshotFibra
import io.signallq.app.feature.history.ResumoHistorico
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.ModoSpeedtest
import io.signallq.app.ui.FiltroConexaoHistorico
import io.signallq.app.ui.GatewayInfo
import io.signallq.app.ui.HistoryPoint
import io.signallq.app.ui.IspInfo
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.OperadoraSource
import io.signallq.app.ui.ResolvedOperadoraContact
import io.signallq.app.ui.ResolvedOperadoraIdentity
import io.signallq.app.ui.component.LkSymbol
import io.signallq.app.ui.resumoBandasWifi
import io.signallq.app.ui.state.UiState
import kotlinx.coroutines.delay

private enum class Overlay {
    Laudo,
    Ping,
    Privacidade,
    Novidades,
    ResultadoVelocidade,
    Fibra,
    Dispositivos,

    // GH#930 — Fase 1 MD3 (navegação). Estrutura preparada para as fases seguintes do plano
    // MD3 To-Be preencherem com telas reais — nenhuma delas ganha lógica final aqui.

    // GH#934 — Fase 5 MD3: EquipamentoInternetScreen real (composicao por capacidade),
    // substitui o antigo FibraModemScreen (Nokia-only) tambem no overlay Fibra acima.
    EquipamentoInternet,

    // GH#933 — Fase 4: hub real de atalhos (grid estático, sem chamada de rede própria).
    Ferramentas,

    // GH#933 — Fase 4: DNS saiu do ModalBottomSheet (showDnsSheet) e virou tela cheia
    // roteada, mesmo padrão dos demais overlays.
    Dns,

    // TODO(#935): Fase 6 — tela real de Jogos. Por ora um stub simples.
    Jogos,

    // GH#936 — Fase 7: reorganização 6a-6f concluída (ver AjustesScreen.kt).
    Perfil,

    // GH#1201 — nova ferramenta "Sinal WiFi", indicador dinâmico de RSSI/PHY/padrão via
    // polling manual (ver #1176 do Pro).
    SinalWifi,
}

/**
 * GH#1098 — zIndex real do overlay dentro da `Box`, baseado na posição em [overlayStack] (não na
 * ordem de declaração no arquivo). Sem isso, o Z-order de desenho seguia a ordem em que cada
 * bloco `AnimatedVisibility` aparece no código-fonte — um overlay empilhado por cima de outro
 * declarado DEPOIS dele desenhava por baixo (ex.: abrir Perfil e depois Novidades por cima
 * escondia a tela nova, mesmo com `overlayStack` correto).
 *
 * Guarda o último índice válido (>= 0) para não derrubar o zIndex no instante em que o overlay
 * é removido de [overlayStack] — sem isso, a animação de saída (`AnimatedVisibility` com
 * `exit = slideOutVertically`) cairia atrás do conteúdo principal no meio da transição, em vez
 * de continuar visível por cima até terminar.
 */
@Composable
private fun rememberOverlayZIndex(
    overlay: Overlay,
    overlayStack: List<Overlay>,
): Float {
    val indiceAtual = overlayStack.indexOf(overlay)
    val ultimoIndiceValido = remember { mutableIntStateOf(0) }
    if (indiceAtual >= 0) ultimoIndiceValido.intValue = indiceAtual
    return (ultimoIndiceValido.intValue + 1).toFloat()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    snapshotRede: SnapshotRede,
    speedtest: AppShellSpeedtestState,
    wifi: AppShellWifiState,
    diagnostico: AppShellDiagnosticoState,
    signallQ: AppShellSignallQState,
    ads: AppShellAdsState = AppShellAdsState(),
    snapshotDns: SnapshotBenchmarkDns,
    history: List<HistoryPoint>,
    localIp: UiState<String>,
    publicIp: UiState<String>,
    ispInfo: UiState<IspInfo>,
    gateways: List<GatewayInfo>,
    deviceName: String,
    dnsResolverIp: String?,
    historico: List<MedicaoEntity>,
    resumoHistorico: ResumoHistorico? = null,
    snapshotFibra: SnapshotFibra,
    // GH#865 Fase 1 — snapshot normalizado do equipamento local (ONT Nokia),
    // null ate a primeira leitura de fibra concluir com sucesso.
    localDevice: LocalNetworkDeviceSnapshot? = null,
    // GH#934 — Fase 5: sinal ja existente de NAT/CGNAT (SIG-279, TopologyDiagnostic),
    // reaproveitado pela EquipamentoInternetScreen para alerta de Double NAT.
    natStatus: NatStatus? = null,
    modemHost: String?,
    modemUsername: String,
    modemPassword: String,
    modemPermanecerConectado: Boolean,
    // GH#530 — BSSID em que a sessao "manter conectado" do gateway foi estabelecida.
    gatewaySessionBssid: String?,
    gatewayIpDetectado: String?,
    localizacaoServidor: UiState<String>,
    temaSelecionado: String,
    analiseAvancada: Boolean,
    onDispararBenchmarkDns: () -> Unit,
    onReconectarFibra: (host: String, username: String, password: String) -> Unit,
    // GH#934 — solicita reboot do equipamento (so relevante quando AcessoEquipamento.GERENCIAMENTO_DISPONIVEL).
    onReiniciarEquipamento: () -> Unit = {},
    onSalvarConfiguracaoModem: (host: String, username: String, password: String, permanecer: Boolean) -> Unit,
    // GH#530 — persiste o resultado da GatewayConnectionSheet (fonte unica dos dois entry points).
    onRegistrarConexaoGateway: (
        ip: String,
        usuario: String,
        senha: String,
        lembrarSenha: Boolean,
        manterConectado: Boolean,
        bssidAtual: String?,
    ) -> Unit = { _, _, _, _, _, _ -> },
    onDefinirTemaSelecionado: (String) -> Unit,
    onDefinirAnaliseAvancada: (Boolean) -> Unit,
    nomeUsuario: String,
    fotoUriUsuario: String?,
    operadora: String,
    planoInternet: String,
    regiao: String,
    estadoUf: String,
    cidadeNome: String,
    ispConfirmado: Boolean,
    limiteAlertaMbps: Int,
    onLimparHistorico: () -> Unit,
    onApagarDadosLocais: () -> Unit,
    onResetarApp: () -> Unit,
    monitoramentoAtivo: Boolean,
    onAtivarMonitoramento: (Boolean) -> Unit,
    notificacaoLatenciaAtiva: Boolean,
    notificacaoDnsAtiva: Boolean,
    notificacaoRssiAtiva: Boolean,
    notificacaoSemInternetAtiva: Boolean,
    onDefinirNotificacaoLatenciaAtiva: (Boolean) -> Unit,
    onDefinirNotificacaoDnsAtiva: (Boolean) -> Unit,
    onDefinirNotificacaoRssiAtiva: (Boolean) -> Unit,
    onDefinirNotificacaoSemInternetAtiva: (Boolean) -> Unit,
    onSalvarPerfil: (nome: String, fotoUri: String?) -> Unit,
    onSalvarDadosProvedor: (operadora: String, plano: String, regiao: String) -> Unit,
    onSalvarEstadoCidade: (estadoUf: String, cidadeNome: String) -> Unit,
    onConfirmarIsp: (operadora: String) -> Unit,
    onDispensarBannerIsp: () -> Unit,
    onSalvarLimiteAlerta: (Int) -> Unit,
    movelSnapshot: MovelSnapshot?,
    simsAtivos: List<MovelSimSnapshot> = emptyList(),
    temPermissaoTelefonia: Boolean = false,
    onSolicitarPermissaoTelefonia: () -> Unit = {},
    temPermissaoLocalizacao: Boolean = true,
    localizacaoBloqueadaPermanentemente: Boolean = false,
    onSolicitarPermissaoLocalizacao: () -> Unit = {},
    // Issue #85 — Minha Conexão
    velocidadeContratadaDownMbps: Int = 0,
    velocidadeContratadaUpMbps: Int = 0,
    onSalvarVelocidadeContratada: (downMbps: Int, upMbps: Int) -> Unit = { _, _ -> },
    onSalvarConexaoDadosCompletos: (
        operadora: String,
        estadoUf: String,
        cidadeNome: String,
        downMbps: Int,
        upMbps: Int,
    ) -> Unit = { _, _, _, _, _ -> },
    // #82 — Banner Anatel dismissível
    anatelBannerDismissed: Boolean = false,
    onDispensarBannerAnatel: () -> Unit = {},
    // #95 — Filtros do Historico
    historicoFiltrado: List<MedicaoEntity> = emptyList(),
    filtroConexaoHistorico: FiltroConexaoHistorico = FiltroConexaoHistorico.TODOS,
    onFiltroConexaoHistoricoChange: (FiltroConexaoHistorico) -> Unit = {},
    filtroOperadoraHistorico: String? = null,
    onFiltroOperadoraHistoricoChange: (String?) -> Unit = {},
    operadorasDisponiveisHistorico: List<String> = emptyList(),
    onScreenView: (screenName: String) -> Unit = {},
    // GH#784 — etapa "compartilhou" do funil do teste de velocidade.
    onCompartilharResultadoVelocidade: () -> Unit = {},
    // GH#970 — resolucao de identidade/contato de operadora: nivel 1 (catalogo local,
    // sincrono, sem I/O) + cadeia completa (local -> diretorio remoto do worker
    // signallq-diagnostic -> fallback generico). Injetado a partir da MainActivity
    // (OperadoraDirectoryResolver via Hilt) — AppShell so repassa, nao resolve nada.
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity? =
        { _, _ -> null },
    resolveOperadoraContatoLocal: (String?, Boolean) -> ResolvedOperadoraContact? =
        { _, _ -> null },
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity =
        { nome, _ ->
            ResolvedOperadoraIdentity(
                displayName = nome ?: "Operadora",
                monograma = nome?.firstOrNull()?.uppercase() ?: "?",
                corMarca = null,
                logoRes = null,
                logoUrl = null,
                source = OperadoraSource.FALLBACK,
            )
        },
    resolveOperadoraContatoRemoto: suspend (String?, Boolean) -> ResolvedOperadoraContact =
        { nome, _ ->
            ResolvedOperadoraContact(
                displayName = nome ?: "Operadora",
                sacPhone = null,
                whatsapp = null,
                site = null,
                source = OperadoraSource.FALLBACK,
            )
        },
) {
    // Desempacota os grupos de estado para variaveis locais — mantém compatibilidade com
    // o corpo interno sem precisar propagar o prefixo `speedtest.x` por toda a funcao.
    val snapshotSpeedtest = speedtest.snapshotSpeedtest
    val speedtestPendenteModoMovel = speedtest.speedtestPendenteModoMovel
    val speedtestPermiteHeavyMovel = speedtest.speedtestPermiteHeavyMovel
    val speedtestMbConsumidosMes = speedtest.speedtestMbConsumidosMes
    val onNovoTeste = speedtest.onNovoTeste
    val onNovoTesteJaConfirmadoMovel = speedtest.onNovoTesteJaConfirmadoMovel
    val onCancelarTeste = speedtest.onCancelarTeste
    val onConfirmarSpeedtestMovel = speedtest.onConfirmarSpeedtestMovel
    val onCancelarSpeedtestMovel = speedtest.onCancelarSpeedtestMovel
    val onSetSpeedtestPermiteHeavyMovel = speedtest.onSetSpeedtestPermiteHeavyMovel

    val snapshotWifi = wifi.snapshotWifi
    val connectedNetwork = wifi.connectedNetwork
    val snapshotDevices = wifi.snapshotDevices
    val apelidos = wifi.apelidos
    val onRefreshDispositivos = wifi.onRefreshDispositivos
    val onRefreshSinal = wifi.onRefreshSinal
    val onSalvarApelido = wifi.onSalvarApelido

    // GH#531 — resumo de bandas Wi-Fi + contagem de clientes do gateway, reusado
    // no subtítulo de Ajustes ("Roteador e rede") e no GatewayItem de Dispositivos.
    val bandasWifiGateway = resumoBandasWifi(snapshotWifi.redes, connectedNetwork?.ssid)
    val clientesNaRedeGateway = snapshotDevices.dispositivos.count { it.ehClienteFinal() }

    val snapshotDiagnostico = diagnostico.snapshotDiagnostico
    val onIniciarDiagnostico = diagnostico.onIniciarDiagnostico
    val analisadorState = diagnostico.analisadorState
    val onAnalisarProblema = diagnostico.onAnalisarProblema
    val onResetarAnalisador = diagnostico.onResetarAnalisador
    val onLaudoFechado = diagnostico.onLaudoFechado
    val recommendationDecision = diagnostico.recommendationDecision
    val recommendationFeedback = diagnostico.recommendationFeedback
    val onRecommendationShown = diagnostico.onRecommendationShown
    val onRecommendationClicked = diagnostico.onRecommendationClicked
    val onRecommendationFeedback = diagnostico.onRecommendationFeedback
    val onRecommendationDismissed = diagnostico.onRecommendationDismissed

    val operadoraMovel = signallQ.operadoraMovel
    val onVerificarGemma = signallQ.onVerificarGemma

    // Monetizacao nativa (issue #555) -- resolvido uma vez aqui, repassado como
    // booleano simples "adsEnabled" por tela para nao acoplar as 4 telas ao tipo AdsFlags.
    val adsFlags = ads.flags
    val podeRequisitarAnuncio = ads.podeRequisitarAnuncio

    val c = LocalLkTokens.current
    // Desempacota UiState<T> → tipos opcionais para as telas filhas que ainda recebem primitivos.
    // Loading e Error resultam em null — as telas exibem fallback textual próprio.
    val localizacaoServidorStr: String? = (localizacaoServidor as? UiState.Success)?.data
    val localIpStr: String? = (localIp as? UiState.Success)?.data
    val publicIpStr: String? = (publicIp as? UiState.Success)?.data
    val ispInfoData: IspInfo? = (ispInfo as? UiState.Success)?.data
    val isIspInfoLoading = publicIp is UiState.Loading
    // #381/#376: cold start sempre abre na aba Velocidade (indice 1), nunca em Home
    // e nunca restaurando a ultima tela — decisao de produto que substitui o
    // comportamento anterior (abria em Home, indice 0).
    var selectedTab by remember { mutableIntStateOf(1) }
    var modoSelecionado by remember { mutableStateOf(ModoSpeedtest.complete) }
    val overlayStack = remember { mutableStateListOf<Overlay>() }

    // Implementacao mock (GH#526/#530) — autenticacao real no equipamento (HTTP/TR-064 por
    // fabricante/firmware) segue fora deste escopo. Existe so para a sheet e a autoconexao
    // funcionarem hoje, sem acoplar a UI a uma implementacao concreta.
    val gatewayConnectionServiceMock =
        remember {
            GatewayConnectionService { _, _, _ ->
                delay(900)
                GatewayConnectionResultado.Sucesso
            }
        }

    // GH#527 — sessao "manter conectado" do gateway, fonte unica compartilhada pelos dois
    // entry points (Home e Ajustes). Elegivel quando o toggle esta ativo E o BSSID atual bate
    // com o BSSID vinculado a credencial — rede diferente (mesmo com o mesmo SSID) invalida.
    // Elegibilidade sozinha nao basta: dispara uma tentativa real de autenticacao silenciosa
    // ao entrar na rede vinculada, sem pedir a senha de novo. Falha nao mostra erro intrusivo —
    // so mantem a trilha no estado "desconectado" (nó do gateway volta a abrir a sheet manual).
    val bssidAtual = snapshotRede.wifiLinkSnapshot?.bssid
    val elegivelParaAutoconexao =
        bssidElegivelParaAutoconexao(modemPermanecerConectado, gatewaySessionBssid, bssidAtual)
    var gatewaySessaoValida by remember { mutableStateOf(false) }
    LaunchedEffect(elegivelParaAutoconexao, modemHost, modemUsername, modemPassword) {
        gatewaySessaoValida =
            if (elegivelParaAutoconexao && !modemHost.isNullOrBlank()) {
                val resultado =
                    runCatching { gatewayConnectionServiceMock.conectar(modemHost, modemUsername, modemPassword) }
                        .getOrNull()
                resultado is GatewayConnectionResultado.Sucesso
            } else {
                false
            }
    }

    // GH#930 — Fase 1 MD3: Ajustes saiu da tab bar (virou "Perfil", 5a tab agora e Ferramentas).
    // Unico ponto de entrada agora e o avatar no TopBar das outras telas, empilhado como overlay.
    val onAbrirPerfilOverlay: () -> Unit = {
        if (Overlay.Perfil !in overlayStack) overlayStack.add(Overlay.Perfil)
    }

    // GH#936 — Fase 7 MD3 (5f): "Monitoramento" agora é sheet dedicado (MonitoramentoSheet.kt),
    // hoisted aqui pra ser destino único do atalho no hub Ferramentas e da linha equivalente
    // dentro de Perfil/Ajustes — nenhum dos dois reimplementa os toggles.
    var showMonitoramentoSheet by remember { mutableStateOf(false) }
    val onAbrirMonitoramentoOverlay: () -> Unit = { showMonitoramentoSheet = true }

    // Destino provisorio da conexao ao gateway — FibraModemScreen ja le sinal do modem, mas
    // NAO e a tela de detalhe definitiva do GPON/Roteador (isso e SIG-357, ainda nao existe).
    // Reusada por ambos entry points (nó do gateway na Home e linha do roteador em Ajustes).
    val onAbrirGatewayDetalhe: () -> Unit = {
        onReconectarFibra(modemHost ?: "", modemUsername, modemPassword)
        if (Overlay.Fibra !in overlayStack) overlayStack.add(Overlay.Fibra)
    }

    // GH#1099 — CTA "Configure o acesso ao equipamento" (EquipamentoInternetScreen, estado
    // AcessoEquipamento.CREDENCIAIS_NECESSARIAS) abria Ajustes genérico via onAbrirPerfilOverlay
    // em vez do formulário real de credenciais — mesma GatewayConnectionSheet já usada pelo nó
    // do gateway na Home (ver HomeScreen.kt). Como o usuário já está dentro do overlay de
    // equipamento, aqui não empilha Overlay.Fibra de novo — só persiste a credencial e
    // reconecta com o dado novo, deixando a tela por trás atualizar sozinha.
    var showEquipamentoCredenciaisSheet by remember { mutableStateOf(false) }
    val onAbrirCredenciaisEquipamento: () -> Unit = { showEquipamentoCredenciaisSheet = true }

    // GH#933 — Fase 4: callbacks de overlay compartilhados entre os entry points antigos
    // (Home, SpeedTest, Ajustes) e os novos atalhos do hub Ferramentas — evita duplicar a
    // mesma lógica de push/pop em dois lugares.
    val onAbrirDispositivosOverlay: () -> Unit = {
        if (Overlay.Dispositivos !in overlayStack) overlayStack.add(Overlay.Dispositivos)
    }
    val onAbrirPingOverlay: () -> Unit = {
        if (Overlay.Ping !in overlayStack) overlayStack.add(Overlay.Ping)
    }
    val onAbrirLaudoOverlay: () -> Unit = {
        if (Overlay.Laudo !in overlayStack) overlayStack.add(Overlay.Laudo)
    }
    val onAbrirJogosOverlay: () -> Unit = {
        if (Overlay.Jogos !in overlayStack) overlayStack.add(Overlay.Jogos)
    }
    // GH#1201 — nova ferramenta "Sinal WiFi" no hub Ferramentas.
    val onAbrirSinalWifiOverlay: () -> Unit = {
        if (Overlay.SinalWifi !in overlayStack) overlayStack.add(Overlay.SinalWifi)
    }
    val onAbrirDnsOverlay: () -> Unit = {
        if (Overlay.Dns !in overlayStack) overlayStack.add(Overlay.Dns)
    }
    // Stub Fase 1 (#930) reaproveitado pela Fase 4 — mesma engine/estado do "Fibra" já usado
    // pelo nó do gateway na Home, so que empilhando Overlay.EquipamentoInternet (nome
    // definitivo, ver TODO da Fase 5/#934 mais abaixo).
    val onAbrirEquipamentoInternetOverlay: () -> Unit = {
        onReconectarFibra(modemHost ?: "", modemUsername, modemPassword)
        if (Overlay.EquipamentoInternet !in overlayStack) overlayStack.add(Overlay.EquipamentoInternet)
    }
    // GH#1031 — "Ver detalhes do Wi-Fi" na EquipamentoInternetScreen: aba Sinal é o único
    // lugar do app com detalhe real de Wi-Fi (RSSI, banda, varredura) — fecha o(s) overlay(s)
    // de equipamento e troca de aba em vez de empilhar mais um overlay.
    val onVerDetalhesWifiDoEquipamento: () -> Unit = {
        overlayStack.remove(Overlay.Fibra)
        overlayStack.remove(Overlay.EquipamentoInternet)
        selectedTab = 2
    }

    // Callback unico chamado quando a GatewayConnectionSheet conecta com sucesso, em qualquer
    // um dos dois entry points — persiste a sessao e navega ao destino provisorio.
    val onGatewayConectado: (
        ip: String,
        usuario: String,
        senha: String,
        lembrarSenha: Boolean,
        manterConectado: Boolean,
    ) -> Unit = { ip, usuario, senha, lembrarSenha, manterConectado ->
        onRegistrarConexaoGateway(ip, usuario, senha, lembrarSenha, manterConectado, bssidAtual)
        onAbrirGatewayDetalhe()
    }

    var showForaDoWifiDialog by remember { mutableStateOf(false) }
    var showPerfilSheet by remember { mutableStateOf(false) }
    var showGerenciarDadosSheet by remember { mutableStateOf(false) }
    var testeAtivo by remember { mutableStateOf(false) }
    var mostrarConcluido by remember { mutableStateOf(false) }
    val primeiraHistoria = remember(historico) { historico.firstOrNull() }
    val tabScreenNames = listOf("home", "speedtest", "sinal_wifi", "historico", "ferramentas")

    // NAV-D: verifica IA ao entrar na tab Velocidade (índice 1)
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) onVerificarGemma()
        tabScreenNames.getOrNull(selectedTab)?.let { onScreenView(it) }
    }

    LaunchedEffect(snapshotSpeedtest.estado) {
        when (snapshotSpeedtest.estado) {
            EstadoExecucaoSpeedtest.executando -> testeAtivo = true
            EstadoExecucaoSpeedtest.concluido -> {
                if (testeAtivo) {
                    onIniciarDiagnostico()
                    mostrarConcluido = true
                    delay(400)
                    mostrarConcluido = false
                    if (Overlay.ResultadoVelocidade !in overlayStack) overlayStack.add(Overlay.ResultadoVelocidade)
                    testeAtivo = false
                }
            }
            else -> {}
        }
    }

    // Back em overlay: desfaz último overlay empilhado.
    BackHandler(enabled = overlayStack.isNotEmpty()) {
        val removido = overlayStack.removeLastOrNull()
        // SIG-173/#664 — back fisico tambem conta como "fechou o Laudo" para fins
        // de elegibilidade do prompt de avaliacao, mesmo caminho do botao voltar da tela.
        if (removido == Overlay.Laudo) onLaudoFechado()
    }

    // Back na tab Histórico (índice 3): volta para Home em vez de sair do app.
    // Sem este handler, o back gesture do sistema fecha o app enquanto o usuário está
    // navegando pelo histórico — comportamento confuso reportado como "trava".
    BackHandler(enabled = overlayStack.isEmpty() && selectedTab == 3) {
        selectedTab = 0
    }

    // #374: tela de erro do speedtest (overlay VelocidadeScreen) não tinha BackHandler
    // próprio — o back físico do sistema saía direto do app em vez de descartar o erro.
    BackHandler(enabled = snapshotSpeedtest.estado == EstadoExecucaoSpeedtest.erro) {
        onCancelarTeste()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = c.bgPrimary,
            bottomBar = {
                if (snapshotSpeedtest.estado != EstadoExecucaoSpeedtest.executando) {
                    AppBottomNavBar(
                        c = c,
                        selectedTab = selectedTab,
                        testeAtivo = testeAtivo,
                        onTabSelected = { selectedTab = it },
                    )
                }
            },
        ) { padding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
            ) {
                when (selectedTab) {
                    // NAV-E: Tab 0 — Home
                    0 ->
                        HomeScreen(
                            snapshotRede = snapshotRede,
                            snapshotSpeedtest = snapshotSpeedtest,
                            history = history,
                            ultimaMedicao = primeiraHistoria,
                            localIp = localIpStr,
                            publicIp = publicIpStr,
                            ispInfo = ispInfoData,
                            isIspInfoLoading = isIspInfoLoading,
                            gateways = gateways,
                            deviceName = deviceName,
                            nomeUsuario = nomeUsuario,
                            fotoUriUsuario = fotoUriUsuario,
                            connectedNetwork = connectedNetwork,
                            movelSnapshot = movelSnapshot,
                            simsAtivos = simsAtivos,
                            // GH#530 — nó do gateway na trilha: sessão válida pula a sheet,
                            // sem sessão abre a GatewayConnectionSheet (mesmo componente do Ajustes).
                            gatewaySessaoValida = gatewaySessaoValida,
                            conectarGateway = gatewayConnectionServiceMock,
                            modemUsername = modemUsername,
                            modemPassword = modemPassword,
                            modemPermanecerConectado = modemPermanecerConectado,
                            onAbrirGatewayDetalhe = onAbrirGatewayDetalhe,
                            onGatewayConectado = onGatewayConectado,
                            onIniciarTeste = { modo ->
                                if (snapshotRede.estadoConexao == EstadoConexao.movel) {
                                    // AppShell decide: em rede móvel mostra ForaDoWifiDialog
                                    // O modo fica registrado no modoSelecionado para uso posterior
                                    modoSelecionado = modo
                                    showForaDoWifiDialog = true
                                } else {
                                    modoSelecionado = modo
                                    onNovoTeste(modo)
                                }
                            },
                            onAbrirHistorico = { selectedTab = 3 },
                            onAbrirPerfil = onAbrirPerfilOverlay,
                            // NAV-B: Sinal agora é tab 2 — navega por tab em vez de overlay
                            onAbrirRedes = { selectedTab = 2 },
                            anatelBannerDismissed = anatelBannerDismissed,
                            onDismissAnatelBanner = onDispensarBannerAnatel,
                            resolveOperadoraIdentidadeLocal = resolveOperadoraIdentidadeLocal,
                            resolveOperadoraIdentidadeRemota = resolveOperadoraIdentidadeRemota,
                        )
                    // NAV-E: Tab 1 — Velocidade (SpeedTestScreen como tab fixa)
                    1 ->
                        SpeedTestScreen(
                            snapshotSpeedtest = snapshotSpeedtest,
                            snapshotRede = snapshotRede,
                            ispInfo = ispInfoData,
                            localizacaoServidor = localizacaoServidorStr,
                            modoSelecionado = modoSelecionado,
                            onModoSelecionado = { modoSelecionado = it },
                            onIniciarTeste = { onNovoTeste(modoSelecionado) },
                            onCancelarTeste = onCancelarTeste,
                            onAbrirDnsBenchmark = onAbrirDnsOverlay,
                            onAbrirPing = onAbrirPingOverlay,
                            onVerResultado = {
                                if (Overlay.ResultadoVelocidade !in
                                    overlayStack
                                ) {
                                    overlayStack.add(Overlay.ResultadoVelocidade)
                                }
                            },
                            onAbrirHistorico = { selectedTab = 3 },
                            onAbrirAjustes = onAbrirPerfilOverlay,
                            nomeUsuario = nomeUsuario,
                            fotoUri = fotoUriUsuario,
                            onAbrirPerfil = onAbrirPerfilOverlay,
                            planoInternet = planoInternet,
                            speedtestPendenteModoMovel = speedtestPendenteModoMovel,
                            onConfirmarSpeedtestMovel = onConfirmarSpeedtestMovel,
                            onCancelarSpeedtestMovel = onCancelarSpeedtestMovel,
                            movelSnapshot = movelSnapshot,
                            adsEnabled = podeRequisitarAnuncio && adsFlags.habilitadoPara(AdSlot.VELOCIDADE),
                        )
                    // NAV-B: Tab 2 — Sinal (SinalScreen como tab fixa, sem botão voltar)
                    2 ->
                        SinalScreen(
                            snapshotWifi = snapshotWifi,
                            connectedNetwork = connectedNetwork,
                            estadoConexao = snapshotRede.estadoConexao,
                            conectado = snapshotRede.conectado,
                            movelSnapshot = movelSnapshot,
                            simsAtivos = simsAtivos,
                            localIp = localIpStr,
                            temPermissaoTelefonia = temPermissaoTelefonia,
                            onSolicitarPermissaoTelefonia = onSolicitarPermissaoTelefonia,
                            temPermissaoLocalizacao = temPermissaoLocalizacao,
                            localizacaoBloqueadaPermanentemente = localizacaoBloqueadaPermanentemente,
                            onSolicitarPermissaoLocalizacao = onSolicitarPermissaoLocalizacao,
                            onRefresh = onRefreshSinal,
                            onVoltar = { selectedTab = 0 },
                            nomeUsuario = nomeUsuario,
                            fotoUri = fotoUriUsuario,
                            onAbrirPerfil = onAbrirPerfilOverlay,
                            wifiLinkSnapshot = snapshotRede.wifiLinkSnapshot,
                            dispositivosRede = snapshotDevices.dispositivos,
                            apelidos = apelidos,
                            onSalvarApelido = onSalvarApelido,
                        )
                    // Tab 3 — Historico (indice mantido conforme spec)
                    3 ->
                        HistoricoScreen(
                            historico = historicoFiltrado,
                            resumoHistorico = resumoHistorico,
                            nomeUsuario = nomeUsuario,
                            fotoUri = fotoUriUsuario,
                            onAbrirPerfil = onAbrirPerfilOverlay,
                            onIniciarTeste = { selectedTab = 1 },
                            filtroConexao = filtroConexaoHistorico,
                            onFiltroConexaoChange = onFiltroConexaoHistoricoChange,
                            filtroOperadora = filtroOperadoraHistorico,
                            onFiltroOperadoraChange = onFiltroOperadoraHistoricoChange,
                            operadorasDisponiveis = operadorasDisponiveisHistorico,
                            adsEnabled = podeRequisitarAnuncio && adsFlags.habilitadoPara(AdSlot.HISTORICO),
                        )
                    // Tab 4 — Ferramentas (GH#930: substitui Ajustes; Ajustes virou overlay
                    // "Perfil", acessado pelo avatar no TopBar — ver Overlay.Perfil abaixo)
                    else ->
                        FerramentasScreen(
                            nomeUsuario = nomeUsuario,
                            fotoUri = fotoUriUsuario,
                            onAbrirPerfil = onAbrirPerfilOverlay,
                            onAbrirDispositivos = onAbrirDispositivosOverlay,
                            onAbrirEquipamentoInternet = onAbrirEquipamentoInternetOverlay,
                            onAbrirPing = onAbrirPingOverlay,
                            onAbrirDns = onAbrirDnsOverlay,
                            onAbrirLaudo = onAbrirLaudoOverlay,
                            onAbrirMonitoramento = onAbrirMonitoramentoOverlay,
                            onAbrirJogos = onAbrirJogosOverlay,
                            onAbrirSinalWifi = onAbrirSinalWifiOverlay,
                        )
                }
            }
        }

        // Overlay de execução do speedtest — cobre toda a tela durante o teste
        AnimatedVisibility(
            visible =
                snapshotSpeedtest.estado == EstadoExecucaoSpeedtest.executando ||
                    snapshotSpeedtest.estado == EstadoExecucaoSpeedtest.erro,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            VelocidadeScreen(
                snapshot = snapshotSpeedtest,
                localizacaoServidor = localizacaoServidorStr,
                ispInfo = ispInfoData,
                onCancelar = onCancelarTeste,
                onReiniciar = { onNovoTeste(modoSelecionado) },
                onVoltar = onCancelarTeste,
            )
        }

        AnimatedVisibility(
            visible = mostrarConcluido,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            val cLocal = LocalLkTokens.current
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(cLocal.bgPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(R.string.appshell_cd_concluido),
                        tint = cLocal.success,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.appshell_concluido),
                        style = MaterialTheme.typography.titleLarge,
                        color = cLocal.success,
                        fontWeight = FontWeight.W600,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = Overlay.ResultadoVelocidade in overlayStack && snapshotSpeedtest.resultado != null,
            modifier = Modifier.zIndex(rememberOverlayZIndex(Overlay.ResultadoVelocidade, overlayStack)),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            snapshotSpeedtest.resultado?.let { resultado ->
                ResultadoVelocidadeScreen(
                    resultado = resultado,
                    snapshotDiagnostico = snapshotDiagnostico,
                    onTestarNovamente = { overlayStack.remove(Overlay.ResultadoVelocidade) },
                    onIrParaHome = {
                        overlayStack.remove(Overlay.ResultadoVelocidade)
                        selectedTab = 0
                    },
                    onVoltar = { overlayStack.remove(Overlay.ResultadoVelocidade) },
                    onCompartilhar = onCompartilharResultadoVelocidade,
                    localizacaoServidor = localizacaoServidorStr,
                    ispInfo = ispInfoData,
                    operadoraMovel = operadoraMovel,
                    analisadorState = analisadorState,
                    onAnalisarProblema = onAnalisarProblema,
                    onResetarAnalisador = onResetarAnalisador,
                    recommendationDecision = recommendationDecision,
                    recommendationFeedback = recommendationFeedback,
                    onRecommendationShown = onRecommendationShown,
                    onRecommendationClicked = onRecommendationClicked,
                    onRecommendationFeedback = onRecommendationFeedback,
                    onRecommendationDismissed = onRecommendationDismissed,
                    localDevice = localDevice,
                    adsEnabled = podeRequisitarAnuncio && adsFlags.habilitadoPara(AdSlot.RESULTADO),
                    resolveOperadoraIdentidadeLocal = resolveOperadoraIdentidadeLocal,
                    resolveOperadoraContatoLocal = resolveOperadoraContatoLocal,
                    resolveOperadoraIdentidadeRemota = resolveOperadoraIdentidadeRemota,
                    resolveOperadoraContatoRemoto = resolveOperadoraContatoRemoto,
                )
            }
        }

        AnimatedVisibility(
            visible = Overlay.Laudo in overlayStack,
            modifier = Modifier.zIndex(rememberOverlayZIndex(Overlay.Laudo, overlayStack)),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            LaudoScreen(
                snapshotDiagnostico = snapshotDiagnostico,
                ultimaMedicao = primeiraHistoria,
                nomeUsuario = nomeUsuario,
                operadora = operadora,
                ssid = connectedNetwork?.ssid,
                ipLocal = localIpStr,
                ipPublico = publicIpStr,
                onVoltar = {
                    overlayStack.remove(Overlay.Laudo)
                    onLaudoFechado()
                },
                velocidadeContratadaMbps = planoInternet.filter { it.isDigit() }.toIntOrNull(),
                conectado = snapshotRede.conectado,
            )
        }

        AnimatedVisibility(
            visible = Overlay.Privacidade in overlayStack,
            modifier = Modifier.zIndex(rememberOverlayZIndex(Overlay.Privacidade, overlayStack)),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            PrivacidadeScreen(
                onVoltar = { overlayStack.remove(Overlay.Privacidade) },
                onAbrirGerenciarDados = {
                    overlayStack.remove(Overlay.Privacidade)
                    showGerenciarDadosSheet = true
                },
            )
        }

        AnimatedVisibility(
            visible = Overlay.Novidades in overlayStack,
            modifier = Modifier.zIndex(rememberOverlayZIndex(Overlay.Novidades, overlayStack)),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            NovidadesScreen(
                appVersion = BuildConfig.VERSION_NAME,
                onVoltar = { overlayStack.remove(Overlay.Novidades) },
            )
        }

        if (Overlay.Ping in overlayStack) {
            Box(modifier = Modifier.zIndex(rememberOverlayZIndex(Overlay.Ping, overlayStack))) {
                PingScreen(onDismiss = { overlayStack.remove(Overlay.Ping) })
            }
        }

        AnimatedVisibility(
            visible = Overlay.Fibra in overlayStack,
            modifier = Modifier.zIndex(rememberOverlayZIndex(Overlay.Fibra, overlayStack)),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            EquipamentoInternetScreen(
                snapshotFibra = snapshotFibra,
                localDevice = localDevice,
                natStatus = natStatus,
                modemHost = modemHost,
                modemUsername = modemUsername,
                modemPassword = modemPassword,
                onVoltar = { overlayStack.remove(Overlay.Fibra) },
                onRetentar = { onReconectarFibra(modemHost ?: "", modemUsername, modemPassword) },
                onAbrirAjustes = onAbrirCredenciaisEquipamento,
                onReiniciarEquipamento = onReiniciarEquipamento,
                onVerDispositivos = onAbrirDispositivosOverlay,
                onExecutarDiagnostico = onAbrirLaudoOverlay,
                onVerDetalhesWifi = onVerDetalhesWifiDoEquipamento,
            )
        }

        AnimatedVisibility(
            visible = Overlay.Dispositivos in overlayStack,
            modifier = Modifier.zIndex(rememberOverlayZIndex(Overlay.Dispositivos, overlayStack)),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            DispositivosScreen(
                snapshotDevices = snapshotDevices,
                snapshotRede = snapshotRede,
                onRefresh = {
                    onRefreshDispositivos()
                },
                apelidos = apelidos,
                onSalvarApelido = onSalvarApelido,
                onVoltar = { overlayStack.remove(Overlay.Dispositivos) },
                bandasWifi = bandasWifiGateway,
                adsEnabled = podeRequisitarAnuncio && adsFlags.habilitadoPara(AdSlot.DISPOSITIVOS),
                correlacoesTopologia = wifi.correlacoesTopologia,
            )
        }

        // GH#934 — Fase 5 MD3: EquipamentoInternetScreen real, composta por capacidade
        // (engine plugável Nokia, unico provider real hoje — ver decisao #1 do plano).
        AnimatedVisibility(
            visible = Overlay.EquipamentoInternet in overlayStack,
            modifier = Modifier.zIndex(rememberOverlayZIndex(Overlay.EquipamentoInternet, overlayStack)),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            EquipamentoInternetScreen(
                snapshotFibra = snapshotFibra,
                localDevice = localDevice,
                natStatus = natStatus,
                modemHost = modemHost,
                modemUsername = modemUsername,
                modemPassword = modemPassword,
                onVoltar = { overlayStack.remove(Overlay.EquipamentoInternet) },
                onRetentar = { onReconectarFibra(modemHost ?: "", modemUsername, modemPassword) },
                onAbrirAjustes = onAbrirCredenciaisEquipamento,
                onReiniciarEquipamento = onReiniciarEquipamento,
                onVerDispositivos = onAbrirDispositivosOverlay,
                onExecutarDiagnostico = onAbrirLaudoOverlay,
                onVerDetalhesWifi = onVerDetalhesWifiDoEquipamento,
            )
        }

        // GH#933 — Fase 4: hub real de atalhos (5a-5g). Overlay.Ferramentas fica disponível
        // como ponto de entrada fora da tab bar (ex.: atalho futuro na Home) — hoje só a tab
        // 4 usa FerramentasScreen diretamente, sem passar por este overlay.
        AnimatedVisibility(
            visible = Overlay.Ferramentas in overlayStack,
            modifier = Modifier.zIndex(rememberOverlayZIndex(Overlay.Ferramentas, overlayStack)),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            FerramentasScreen(
                nomeUsuario = nomeUsuario,
                fotoUri = fotoUriUsuario,
                onAbrirPerfil = onAbrirPerfilOverlay,
                onAbrirDispositivos = onAbrirDispositivosOverlay,
                onAbrirEquipamentoInternet = onAbrirEquipamentoInternetOverlay,
                onAbrirPing = onAbrirPingOverlay,
                onAbrirDns = onAbrirDnsOverlay,
                onAbrirLaudo = onAbrirLaudoOverlay,
                onAbrirMonitoramento = onAbrirMonitoramentoOverlay,
                onAbrirJogos = onAbrirJogosOverlay,
                onAbrirSinalWifi = onAbrirSinalWifiOverlay,
            )
        }

        // GH#933 — Fase 4: DNS migrou de ModalBottomSheet (showDnsSheet) para tela cheia
        // roteada — lógica de benchmark preservada, ver DnsScreen.kt.
        AnimatedVisibility(
            visible = Overlay.Dns in overlayStack,
            modifier = Modifier.zIndex(rememberOverlayZIndex(Overlay.Dns, overlayStack)),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            DnsScreen(
                snapshotDns = snapshotDns,
                dnsResolverIp = dnsResolverIp,
                snapshotRede = snapshotRede,
                onIniciarBenchmark = onDispararBenchmarkDns,
                onVoltar = { overlayStack.remove(Overlay.Dns) },
            )
        }

        // GH#935 — Fase 6: tela real de Jogos (fluxo de 5 etapas).
        AnimatedVisibility(
            visible = Overlay.Jogos in overlayStack,
            modifier = Modifier.zIndex(rememberOverlayZIndex(Overlay.Jogos, overlayStack)),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            JogosScreen(
                tipoConexaoAtual = snapshotRede.estadoConexao,
                wifiLinkSnapshot = snapshotRede.wifiLinkSnapshot,
                onVoltar = { overlayStack.remove(Overlay.Jogos) },
                adsEnabled = podeRequisitarAnuncio && adsFlags.habilitadoPara(AdSlot.JOGOS),
            )
        }

        // GH#1201 — nova ferramenta "Sinal WiFi" (indicador dinâmico de RSSI/PHY/padrão).
        AnimatedVisibility(
            visible = Overlay.SinalWifi in overlayStack,
            modifier = Modifier.zIndex(rememberOverlayZIndex(Overlay.SinalWifi, overlayStack)),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            SinalWifiScreen(
                temPermissaoLocalizacao = temPermissaoLocalizacao,
                localizacaoBloqueadaPermanentemente = localizacaoBloqueadaPermanentemente,
                onSolicitarPermissaoLocalizacao = onSolicitarPermissaoLocalizacao,
                onVoltar = { overlayStack.remove(Overlay.SinalWifi) },
            )
        }

        // GH#936 — Fase 7: AjustesScreen.kt virou lista de entradas pras 6 sub-telas
        // (6a PerfilEditSheet, 6b MinhaConexaoSheet, 6c DadosLocaisSheet, 6d Privacidade,
        // 6e Novidades, 6f SobreSheet) em vez de formulário monolítico — alcançada pelo
        // avatar no TopBar em vez da antiga tab 4.
        AnimatedVisibility(
            visible = Overlay.Perfil in overlayStack,
            modifier = Modifier.zIndex(rememberOverlayZIndex(Overlay.Perfil, overlayStack)),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            AjustesScreen(
                perfil =
                    AjustesPerfilState(
                        nomeUsuario = nomeUsuario,
                        fotoUriUsuario = fotoUriUsuario,
                        deviceName = deviceName,
                        appVersion = BuildConfig.VERSION_NAME,
                        onSalvarPerfil = onSalvarPerfil,
                    ),
                provedor =
                    AjustesProvedorState(
                        operadora = operadora,
                        planoInternet = planoInternet,
                        regiao = regiao,
                        estadoUf = estadoUf,
                        cidadeNome = cidadeNome,
                        ispDetectado = ispInfoData?.isp,
                        ispConfirmado = ispConfirmado,
                        velocidadeContratadaDownMbps = velocidadeContratadaDownMbps,
                        velocidadeContratadaUpMbps = velocidadeContratadaUpMbps,
                        operadoraAutodetectada = movelSnapshot?.operadora,
                        onSalvarDadosProvedor = onSalvarDadosProvedor,
                        onSalvarEstadoCidade = onSalvarEstadoCidade,
                        onConfirmarIsp = onConfirmarIsp,
                        onDispensarBannerIsp = onDispensarBannerIsp,
                        onSalvarVelocidadeContratada = onSalvarVelocidadeContratada,
                    ),
                monitoramento =
                    AjustesMonitoramentoState(
                        monitoramentoAtivo = monitoramentoAtivo,
                        analiseAvancada = analiseAvancada,
                        notificacaoLatenciaAtiva = notificacaoLatenciaAtiva,
                        notificacaoDnsAtiva = notificacaoDnsAtiva,
                        notificacaoRssiAtiva = notificacaoRssiAtiva,
                        notificacaoSemInternetAtiva = notificacaoSemInternetAtiva,
                        onAtivarMonitoramento = onAtivarMonitoramento,
                        onDefinirAnaliseAvancada = onDefinirAnaliseAvancada,
                        onDefinirNotificacaoLatenciaAtiva = onDefinirNotificacaoLatenciaAtiva,
                        onDefinirNotificacaoDnsAtiva = onDefinirNotificacaoDnsAtiva,
                        onDefinirNotificacaoRssiAtiva = onDefinirNotificacaoRssiAtiva,
                        onDefinirNotificacaoSemInternetAtiva = onDefinirNotificacaoSemInternetAtiva,
                    ),
                modem =
                    AjustesModemState(
                        modemHost = modemHost,
                        modemUsername = modemUsername,
                        modemPassword = modemPassword,
                        modemPermanecerConectado = modemPermanecerConectado,
                        gatewayIpDetectado = gatewayIpDetectado,
                        onSalvarConfiguracaoModem = onSalvarConfiguracaoModem,
                        onConectarFibra = { host, user, pass -> onReconectarFibra(host, user, pass) },
                        gatewaySessaoValida = gatewaySessaoValida,
                        conectarGateway = gatewayConnectionServiceMock,
                        onGatewayConectado = onGatewayConectado,
                        bandasWifi = bandasWifiGateway,
                        dispositivosNaRede = clientesNaRedeGateway,
                    ),
                temaSelecionado = temaSelecionado,
                onDefinirTemaSelecionado = onDefinirTemaSelecionado,
                limiteAlertaMbps = limiteAlertaMbps,
                onSalvarLimiteAlerta = onSalvarLimiteAlerta,
                onLimparHistorico = onLimparHistorico,
                onApagarDadosLocais = onApagarDadosLocais,
                onResetarApp = onResetarApp,
                onAbrirHistorico = {
                    overlayStack.remove(Overlay.Perfil)
                    selectedTab = 3
                },
                onAbrirLaudo = onAbrirLaudoOverlay,
                onAbrirPerfil = { showPerfilSheet = true },
                onAbrirMonitoramento = onAbrirMonitoramentoOverlay,
                onAbrirPrivacidade = { if (Overlay.Privacidade !in overlayStack) overlayStack.add(Overlay.Privacidade) },
                onAbrirNovidades = { if (Overlay.Novidades !in overlayStack) overlayStack.add(Overlay.Novidades) },
                // GH#530 — mesmo destino provisório usado pelo nó do gateway na Home.
                onAbrirFibra = onAbrirGatewayDetalhe,
                dadosMoveis =
                    AjustesDadosMoveisState(
                        speedtestPermiteHeavyMovel = speedtestPermiteHeavyMovel,
                        speedtestMbConsumidosMes = speedtestMbConsumidosMes,
                        onSetSpeedtestPermiteHeavyMovel = onSetSpeedtestPermiteHeavyMovel,
                    ),
                onVoltar = { overlayStack.remove(Overlay.Perfil) },
            )
        }

        if (showPerfilSheet) {
            PerfilEditSheet(
                c = c,
                nomeAtual = nomeUsuario,
                fotoUriAtual = fotoUriUsuario,
                deviceName = deviceName,
                appVersion = BuildConfig.VERSION_NAME,
                ispInfo = ispInfoData,
                estadoConexao = snapshotRede.estadoConexao,
                onDismiss = { showPerfilSheet = false },
                onSalvar = { nome, fotoUri ->
                    onSalvarPerfil(nome, fotoUri)
                    showPerfilSheet = false
                },
            )
        }

        if (showForaDoWifiDialog) {
            ForaDoWifiDialog(
                onContinuar = {
                    showForaDoWifiDialog = false
                    // Usuario ja confirmou o aviso de dados moveis aqui — pula o segundo
                    // gate de confirmacao em rede medida (#516).
                    onNovoTesteJaConfirmadoMovel(modoSelecionado)
                },
                onCancelar = { showForaDoWifiDialog = false },
            )
        }

        if (showGerenciarDadosSheet) {
            DadosLocaisSheet(
                c = c,
                onDismiss = { showGerenciarDadosSheet = false },
                onLimparHistorico = onLimparHistorico,
                onApagarDadosLocais = onApagarDadosLocais,
                onResetarApp = onResetarApp,
            )
        }

        if (showMonitoramentoSheet) {
            MonitoramentoSheet(
                c = c,
                analiseAvancada = analiseAvancada,
                monitoramentoAtivo = monitoramentoAtivo,
                notificacaoLatenciaAtiva = notificacaoLatenciaAtiva,
                notificacaoDnsAtiva = notificacaoDnsAtiva,
                notificacaoRssiAtiva = notificacaoRssiAtiva,
                notificacaoSemInternetAtiva = notificacaoSemInternetAtiva,
                onDismiss = { showMonitoramentoSheet = false },
                onDefinirAnaliseAvancada = onDefinirAnaliseAvancada,
                onAtivarMonitoramento = onAtivarMonitoramento,
                onDefinirNotificacaoLatenciaAtiva = onDefinirNotificacaoLatenciaAtiva,
                onDefinirNotificacaoDnsAtiva = onDefinirNotificacaoDnsAtiva,
                onDefinirNotificacaoRssiAtiva = onDefinirNotificacaoRssiAtiva,
                onDefinirNotificacaoSemInternetAtiva = onDefinirNotificacaoSemInternetAtiva,
            )
        }

        // GH#1099 — formulário real de credenciais do equipamento, aberto pelo CTA "Revisar
        // configurações"/"Configure o acesso" dentro do overlay de Fibra/EquipamentoInternet.
        // Mesmo componente e mecânica do nó do gateway na Home (GatewayConnectionSheet).
        if (showEquipamentoCredenciaisSheet) {
            GatewayConnectionSheet(
                ipInicial = modemHost,
                usuarioInicial = modemUsername,
                senhaInicial = modemPassword,
                lembrarSenhaInicial = modemUsername.isNotBlank() || modemPassword.isNotBlank(),
                manterConectadoInicial = modemPermanecerConectado,
                onDismissRequest = { showEquipamentoCredenciaisSheet = false },
                conectar = gatewayConnectionServiceMock,
                onConectado = { ip, usuario, senha, lembrarSenha, manterConectado ->
                    onRegistrarConexaoGateway(ip, usuario, senha, lembrarSenha, manterConectado, bssidAtual)
                    onReconectarFibra(ip, usuario, senha)
                },
            )
        }
    }
}

@Composable
private fun AppBottomNavBar(
    c: LkTokens,
    selectedTab: Int,
    testeAtivo: Boolean = false,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        HorizontalDivider(color = c.outlineVariant, thickness = 1.dp)
        NavigationBar(
            containerColor = c.surfaceContainer,
            tonalElevation = 0.dp,
        ) {
            AppNavItem(c, selectedTab, 0, "Início", "home", onTabSelected)
            AppNavItem(c, selectedTab, 1, "Velocidade", "speed", onTabSelected, showBadge = testeAtivo)
            AppNavItem(c, selectedTab, 2, "Sinal", "wifi", onTabSelected)
            AppNavItem(c, selectedTab, 3, "Histórico", "history", onTabSelected)
            AppNavItem(c, selectedTab, 4, "Ferramentas", "build", onTabSelected)
        }
    }
}

@Composable
private fun RowScope.AppNavItem(
    c: LkTokens,
    selectedTab: Int,
    index: Int,
    label: String,
    symbolName: String,
    onTabSelected: (Int) -> Unit,
    showBadge: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val badgePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(700, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "badgeAlpha",
    )
    NavigationBarItem(
        selected = selectedTab == index,
        onClick = { onTabSelected(index) },
        icon = {
            BadgedBox(badge = {
                if (showBadge) Badge(modifier = Modifier.graphicsLayer { alpha = badgePulseAlpha })
            }) {
                LkSymbol(
                    name = symbolName,
                    filled = selectedTab == index,
                )
            }
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Medium,
            )
        },
        colors =
            NavigationBarItemDefaults.colors(
                selectedIconColor = c.onPrimaryContainer,
                unselectedIconColor = c.onSurfaceVariant,
                selectedTextColor = c.onSurface,
                unselectedTextColor = c.onSurfaceVariant,
                indicatorColor = c.primaryContainer,
            ),
    )
}

// ─── Dialog: fora do Wi-Fi ────────────────────────────────────────────────────

@Composable
private fun ForaDoWifiDialog(
    onContinuar: () -> Unit,
    onCancelar: () -> Unit,
) {
    val c = LocalLkTokens.current
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.appshell_sem_wifi), fontWeight = FontWeight.W600) },
        text = {
            Text(
                "Você está usando dados móveis. Fazer um teste de velocidade pode consumir uma quantidade significativa do seu plano de dados.\n\nDeseja continuar mesmo assim?",
                fontSize = 14.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onContinuar) {
                Text(stringResource(R.string.appshell_continuar_mesmo_assim), color = c.warning)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.global_btn_cancelar)) }
        },
    )
}
