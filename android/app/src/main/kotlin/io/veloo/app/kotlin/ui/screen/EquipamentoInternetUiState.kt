package io.signallq.app.ui.screen

import io.signallq.app.core.network.contracts.gateway.AcessoEquipamento
import io.signallq.app.core.network.contracts.localdevice.DeviceType
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.core.network.contracts.localdevice.SupportLevel
import io.signallq.app.feature.diagnostico.topology.model.NatStatus
import io.signallq.app.feature.fibra.EstadoFibra
import io.signallq.app.feature.fibra.SnapshotFibra
import io.signallq.app.ui.component.dadosParciais

/**
 * Deriva o [AcessoEquipamento] (GH#934, Fase 5 MD3) a partir do [SnapshotFibra]
 * (único provider real hoje — Nokia G-1425G-B via [io.signallq.app.feature.fibra.ExecutorFibra])
 * e do contrato normalizado já mapeado ([LocalNetworkDeviceSnapshot]). Função
 * pura, testável sem Robolectric/Compose — mesmo padrão de
 * `mapLocalDeviceSectionUiState` em `LocalDeviceSection.kt`.
 *
 * `estado == idle/conectando` fica de fora de propósito — a tela trata esses
 * dois como "Carregando" ANTES de chamar esta função (loading não é um nível
 * de acesso, é ausência momentânea de resultado).
 *
 * ## Limitação conhecida (documentada em #934, não escondida)
 * Hoje só existe fingerprint passivo de outro fabricante via
 * `EquipmentClassifier`/`DeviceDriverCatalog` (core/network, testado), mas
 * esse pipeline **não está wireado** a nenhuma chamada de rede real neste
 * fluxo (`ExecutorFibra` só sabe falar o protocolo Nokia). O único sinal
 * disponível hoje de "isto provavelmente não é um Nokia" é a própria falha de
 * parsing da página de login (`erroRespostaModemInvalida` — pubkey/nonce/csrf
 * ausentes), usada abaixo como evidência de [AcessoEquipamento.SOMENTE_IDENTIFICACAO].
 * Fiar detecção passiva real (banner HTTP + IP canônico + rotas conhecidas,
 * via `EquipmentClassifier.classificar`) para determinar o fabricante ANTES
 * de tentar o login Nokia fica para uma fase futura — fora do escopo desta.
 */
fun mapAcessoEquipamento(
    snapshotFibra: SnapshotFibra,
    localDevice: LocalNetworkDeviceSnapshot?,
    modemHost: String?,
    modemUsername: String,
    modemPassword: String,
): AcessoEquipamento {
    val credenciaisConfiguradas =
        !modemHost.isNullOrBlank() || modemUsername.isNotBlank() || modemPassword.isNotBlank()
    if (!credenciaisConfiguradas) return AcessoEquipamento.CREDENCIAIS_NECESSARIAS

    if (snapshotFibra.estado == EstadoFibra.erro) {
        return when (snapshotFibra.erroMensagem) {
            // Pagina de login nao tem a cara de um Nokia — evidencia de equipamento
            // nao suportado, nao de sessao/senha (ver limitacao no KDoc acima).
            "erroRespostaModemInvalida" -> AcessoEquipamento.SOMENTE_IDENTIFICACAO
            "erroCredenciaisInvalidas" -> AcessoEquipamento.CREDENCIAIS_NECESSARIAS
            else -> AcessoEquipamento.SESSAO_EXPIRADA
        }
    }

    if (snapshotFibra.estado != EstadoFibra.concluido || localDevice == null) {
        // Estado inconsistente defensivo (concluido sem gpon/localDevice nulo por
        // alguma falha do mapper) — trata como sessao que precisa de novo retry,
        // nunca crasha nem finge dado.
        return AcessoEquipamento.SESSAO_EXPIRADA
    }

    val semDriverConhecido =
        localDevice.deviceType == DeviceType.UNKNOWN_SUPPORTED ||
            localDevice.deviceType == DeviceType.UNKNOWN_UNSUPPORTED ||
            localDevice.supportLevel == SupportLevel.UNKNOWN
    if (semDriverConhecido) return AcessoEquipamento.SOMENTE_IDENTIFICACAO

    if (dadosParciais(localDevice)) return AcessoEquipamento.LEITURA_PARCIAL

    return if (localDevice.capabilities.suportaGerenciamento) {
        AcessoEquipamento.GERENCIAMENTO_DISPONIVEL
    } else {
        AcessoEquipamento.LEITURA_COMPLETA
    }
}

/**
 * Suspeita de Double NAT (GH#934) — sinaliza só com evidência real, cruzando
 * DUAS fontes independentes já existentes no app (nenhuma nova aqui):
 *
 * 1. [NatStatus.DOUBLE_NAT_OR_CGNAT] — já calculado por `TopologyDiagnostic`
 *    (UPnP IGD + IP público, SIG-279), reaproveitado tal qual (não duplicado).
 *    Sozinho ele NÃO distingue Double NAT (roteador extra atrás da ONT) de
 *    CGNAT puro (o operador faz NAT do lado dele, nada a ver com o
 *    equipamento do cliente) — daí o segundo sinal.
 * 2. Modo reportado pela própria ONT (`GponStatus.mode`) indicando que ela
 *    ainda está roteando/fazendo NAT (não está em bridge) — se a ONT está em
 *    bridge, ela não é a segunda camada de NAT, então não há Double NAT para
 *    sinalizar aqui mesmo que o NAT status geral acuse algo.
 *
 * Modo desconhecido/vazio (`"—"` ou em branco) nunca sinaliza — falso
 * negativo é mais seguro que falso positivo neste alerta (defensivo por
 * decisão de produto, #934).
 */
fun suspeitaDoubleNat(
    natStatus: NatStatus?,
    gponMode: String?,
): Boolean {
    if (natStatus != NatStatus.DOUBLE_NAT_OR_CGNAT) return false
    val modo = gponMode?.trim().orEmpty()
    if (modo.isBlank() || modo == "—") return false
    val ontEmBridge = modo.contains("bridg", ignoreCase = true)
    return !ontEmBridge
}
