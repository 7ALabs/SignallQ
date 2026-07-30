package io.signallq.app.ui.screen

import io.signallq.app.core.network.contracts.gateway.GatewayConnectionService
import io.signallq.app.core.network.contracts.gateway.GatewayConnectionServiceIndisponivelPadrao

data class AjustesPerfilState(
    val nomeUsuario: String,
    val fotoUriUsuario: String?,
    val deviceName: String,
    val appVersion: String,
    val onSalvarPerfil: (nome: String, fotoUri: String?) -> Unit,
)

/**
 * GH#1249 (recorte de #1227) — "Minha conexão" passa a ser espelho do
 * [io.signallq.app.core.datastore.ConnectionProfilePersistido] da rede atual ([minhaConexao]),
 * não mais de chaves DataStore globais. `planoInternet`/`regiao` seguem como fallback de
 * exibição legado — nunca tiveram campo editável nesta tela (só eram reescritos, sem mudança,
 * a cada "Salvar" do fluxo antigo); mantidos só como leitura, sem novo ponto de escrita.
 */
data class AjustesProvedorState(
    val planoInternet: String,
    val regiao: String,
    val minhaConexao: MinhaConexaoUiState,
    val onSalvarConnectionProfile: (
        providerFixed: String?,
        downloadMbps: Int?,
        uploadMbps: Int?,
        cidade: String?,
        uf: String?,
        userConfirmed: Boolean,
    ) -> Unit,
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
    // BUG#1511 (P0) — default nunca pode simular sucesso: sem implementação real conectada
    // pelo caller, o resultado honesto é "indisponível", nunca "conectado".
    val conectarGateway: GatewayConnectionService = GatewayConnectionServiceIndisponivelPadrao,
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
