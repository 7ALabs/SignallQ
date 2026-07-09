package io.signallq.app.ui.screen

import io.signallq.app.core.network.contracts.gateway.GatewayConnectionResultado
import io.signallq.app.core.network.contracts.gateway.GatewayConnectionService

data class AjustesPerfilState(
    val nomeUsuario: String,
    val fotoUriUsuario: String?,
    val deviceName: String,
    val appVersion: String,
    val onSalvarPerfil: (nome: String, fotoUri: String?) -> Unit,
)

data class AjustesProvedorState(
    val operadora: String,
    val planoInternet: String,
    val regiao: String,
    val estadoUf: String,
    val cidadeNome: String,
    val ispDetectado: String?,
    val ispConfirmado: Boolean,
    val velocidadeContratadaDownMbps: Int = 0,
    val velocidadeContratadaUpMbps: Int = 0,
    val operadoraAutodetectada: String? = null,
    val onSalvarDadosProvedor: (operadora: String, plano: String, regiao: String) -> Unit,
    val onSalvarEstadoCidade: (estadoUf: String, cidadeNome: String) -> Unit,
    val onConfirmarIsp: (operadora: String) -> Unit,
    val onDispensarBannerIsp: () -> Unit,
    val onSalvarVelocidadeContratada: (downMbps: Int, upMbps: Int) -> Unit = { _, _ -> },
)

data class AjustesMonitoramentoState(
    val monitoramentoAtivo: Boolean,
    val analiseAvancada: Boolean,
    val notificacaoLatenciaAtiva: Boolean,
    val notificacaoDnsAtiva: Boolean,
    val notificacaoRssiAtiva: Boolean,
    val notificacaoSemInternetAtiva: Boolean,
    val onAtivarMonitoramento: (Boolean) -> Unit,
    val onDefinirAnaliseAvancada: (Boolean) -> Unit,
    val onDefinirNotificacaoLatenciaAtiva: (Boolean) -> Unit,
    val onDefinirNotificacaoDnsAtiva: (Boolean) -> Unit,
    val onDefinirNotificacaoRssiAtiva: (Boolean) -> Unit,
    val onDefinirNotificacaoSemInternetAtiva: (Boolean) -> Unit,
)

data class AjustesModemState(
    val modemHost: String?,
    val modemUsername: String,
    val modemPassword: String,
    val modemPermanecerConectado: Boolean,
    val gatewayIpDetectado: String?,
    val onSalvarConfiguracaoModem: (host: String, username: String, password: String, permanecer: Boolean) -> Unit,
    val onConectarFibra: (host: String, username: String, password: String) -> Unit,
    // GH#530 — reuso da GatewayConnectionSheet na linha do roteador. Sessão válida
    // (BSSID atual == BSSID salvo com "manter conectado") pula a sheet.
    val gatewaySessaoValida: Boolean = false,
    val conectarGateway: GatewayConnectionService = GatewayConnectionService { _, _, _ -> GatewayConnectionResultado.Sucesso },
    val onGatewayConectado: (ip: String, usuario: String, senha: String, lembrarSenha: Boolean, manterConectado: Boolean) -> Unit =
        { _, _, _, _, _ -> },
    // GH#531 — resumo de bandas Wi-Fi ("2,4G + 5G") e contagem de dispositivos
    // na rede, exibidos no subtítulo da linha "Roteador e rede". Null/0 quando
    // ainda não há dado suficiente (mantém subtítulo genérico "Conectado").
    val bandasWifi: String? = null,
    val dispositivosNaRede: Int = 0,
)

data class AjustesDadosMoveisState(
    val speedtestPermiteHeavyMovel: Boolean,
    val speedtestMbConsumidosMes: Long,
    val onSetSpeedtestPermiteHeavyMovel: (Boolean) -> Unit,
)
