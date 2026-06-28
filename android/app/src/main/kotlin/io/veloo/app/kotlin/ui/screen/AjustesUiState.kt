package io.signallq.app.ui.screen

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
    val onSalvarDadosProvedor: (operadora: String, plano: String, regiao: String) -> Unit,
    val onSalvarEstadoCidade: (estadoUf: String, cidadeNome: String) -> Unit,
    val onConfirmarIsp: (operadora: String) -> Unit,
    val onDispensarBannerIsp: () -> Unit,
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
)

data class AjustesDadosMoveisState(
    val speedtestPermiteHeavyMovel: Boolean,
    val speedtestMbConsumidosMes: Long,
    val onSetSpeedtestPermiteHeavyMovel: (Boolean) -> Unit,
)
