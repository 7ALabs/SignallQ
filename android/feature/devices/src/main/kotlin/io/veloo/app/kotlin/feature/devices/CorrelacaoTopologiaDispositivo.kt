package io.signallq.app.feature.devices

import io.signallq.app.core.network.contracts.localdevice.ClientSnapshot
import io.signallq.app.core.network.contracts.localdevice.TipoConexaoFisica
import io.signallq.app.core.network.contracts.topologia.ClassificacaoTopologia
import io.signallq.app.core.network.contracts.topologia.Evidencia
import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import io.signallq.app.core.network.contracts.topologia.PesoEvidencia
import io.signallq.app.core.network.contracts.topologia.TipoEvidencia
import io.signallq.app.core.network.contracts.wifi.RedeVizinha
import io.signallq.app.core.network.topologia.oui.OuiCatalog

/**
 * Nível de força da correlação encontrada entre um [DispositivoRede] (scan LAN ativo) e um sinal
 * de topologia Wi-Fi/gateway — do mais forte pro mais fraco. GH#983 Fase 4.
 */
enum class NivelCorrelacao {
    CLIENT_SNAPSHOT_EXATO,
    MAC_EXATO,
    OUI_FRACO,
    SEM_MATCH,
}

/**
 * Resultado estruturado da correlação best-effort entre scan LAN ([DispositivoRede]) e topologia
 * Wi-Fi/gateway — GH#983 Fase 4.
 *
 * [papelTopologiaHerdado] só é preenchido pras correlações fortes ([NivelCorrelacao.CLIENT_SNAPSHOT_EXATO]
 * e [NivelCorrelacao.MAC_EXATO]) — a função nunca preenche esse campo pra [NivelCorrelacao.OUI_FRACO].
 * Correlação fraca (só OUI) sempre chega no chamador só como [evidenciaAuxiliar]: quem decide se e
 * como usar essa evidência (nunca reclassificando sozinha) é o consumidor desta função, nunca ela
 * mesma — ver [correlacionarDispositivoComTopologia].
 */
data class ResultadoCorrelacaoTopologia(
    val dispositivo: DispositivoRede,
    val nivel: NivelCorrelacao,
    val papelTopologiaHerdado: PapelTopologia? = null,
    val tipoConexaoFisicaConfirmada: TipoConexaoFisica? = null,
    val evidenciaAuxiliar: Evidencia? = null,
)

/**
 * Correlaciona um [DispositivoRede] (scan LAN ativo, feature/devices) com sinais de topologia
 * Wi-Fi ([RedeVizinha] + [ClassificacaoTopologia] já produzidos pelo `TopologiaRedeEngine`,
 * coreNetwork) e/ou leitura direta do gateway ([ClientSnapshot]) — GH#983 Fase 4.
 *
 * Função pura e best-effort, sem dependência de Android/Hilt — pode ser chamada de qualquer
 * ViewModel/consumidor que já tenha os três sinais em mãos, sem exigir os três simultaneamente:
 * chamar sem [clientesGateway] e/ou sem [redesWifiClassificadas] (ou com [DispositivoRede.mac]
 * nulo) nunca lança exceção, sempre cai em [NivelCorrelacao.SEM_MATCH] e devolve o [dispositivo]
 * inalterado — comportamento estritamente aditivo, idêntico ao de antes da Fase 4 quando não há
 * scan Wi-Fi ou credencial de gateway disponível.
 *
 * Tenta 4 níveis, do mais forte pro mais fraco:
 * 1. [NivelCorrelacao.CLIENT_SNAPSHOT_EXATO] — o mac do [dispositivo] bate com o mac de um
 *    [ClientSnapshot] reportado pelo próprio gateway (leitura direta, não heurística — a
 *    evidência mais forte dos quatro níveis). Quando o mesmo mac também bate com o bssid de um
 *    nó Wi-Fi já classificado, o resultado herda o papel desse nó em [ResultadoCorrelacaoTopologia.papelTopologiaHerdado];
 *    sempre carrega o [ClientSnapshot.tipoConexaoFisica] confirmado pelo gateway em
 *    [ResultadoCorrelacaoTopologia.tipoConexaoFisicaConfirmada], que ajuda o chamador a decidir
 *    com-fio vs Wi-Fi mesmo sem correspondência de bssid.
 * 2. [NivelCorrelacao.MAC_EXATO] — sem [ClientSnapshot] correspondente, mas o mac do [dispositivo]
 *    bate exatamente com o bssid de um nó Wi-Fi já classificado (normalizando maiúsculas/separadores
 *    antes de comparar). Herda o papel desse nó.
 * 3. [NivelCorrelacao.OUI_FRACO] — só o prefixo OUI bate (mesmo fabricante do nó Wi-Fi), sem mac
 *    exato em nenhum dos dois níveis acima. Evidência fraca/auxiliar: **nunca** preenche
 *    [ResultadoCorrelacaoTopologia.papelTopologiaHerdado] — um smartphone com o mesmo prefixo de
 *    fabricante do roteador não vira "possível roteador" só por isso. Só popula
 *    [ResultadoCorrelacaoTopologia.evidenciaAuxiliar] com peso [PesoEvidencia.FRACO], deixando a
 *    decisão de reclassificar (ou não) inteiramente com o chamador.
 * 4. [NivelCorrelacao.SEM_MATCH] — nenhum sinal bateu (ou não havia sinal disponível); devolve o
 *    [dispositivo] inalterado, mesmo comportamento de antes da Fase 4.
 */
fun correlacionarDispositivoComTopologia(
    dispositivo: DispositivoRede,
    clientesGateway: List<ClientSnapshot> = emptyList(),
    redesWifiClassificadas: List<Pair<RedeVizinha, ClassificacaoTopologia>> = emptyList(),
): ResultadoCorrelacaoTopologia {
    val macDispositivo = OuiCatalog.normalizarMacCompleto(dispositivo.mac)
        ?: return ResultadoCorrelacaoTopologia(dispositivo = dispositivo, nivel = NivelCorrelacao.SEM_MATCH)

    val nodeWifiPorMacExato = redesWifiClassificadas.firstOrNull { (rede, _) ->
        OuiCatalog.normalizarMacCompleto(rede.bssid) == macDispositivo
    }

    val clientSnapshotExato = clientesGateway.firstOrNull { cliente ->
        OuiCatalog.normalizarMacCompleto(cliente.mac) == macDispositivo
    }

    if (clientSnapshotExato != null) {
        return ResultadoCorrelacaoTopologia(
            dispositivo = dispositivo,
            nivel = NivelCorrelacao.CLIENT_SNAPSHOT_EXATO,
            papelTopologiaHerdado = nodeWifiPorMacExato?.second?.papelProvavel,
            tipoConexaoFisicaConfirmada = clientSnapshotExato.tipoConexaoFisica,
        )
    }

    if (nodeWifiPorMacExato != null) {
        return ResultadoCorrelacaoTopologia(
            dispositivo = dispositivo,
            nivel = NivelCorrelacao.MAC_EXATO,
            papelTopologiaHerdado = nodeWifiPorMacExato.second.papelProvavel,
        )
    }

    val prefixoDispositivo = OuiCatalog.normalizarPrefixo(dispositivo.mac)
    val bateOuiFraco = prefixoDispositivo != null &&
        redesWifiClassificadas.any { (rede, _) -> OuiCatalog.normalizarPrefixo(rede.bssid) == prefixoDispositivo }

    if (bateOuiFraco && prefixoDispositivo != null) {
        return ResultadoCorrelacaoTopologia(
            dispositivo = dispositivo,
            nivel = NivelCorrelacao.OUI_FRACO,
            evidenciaAuxiliar = Evidencia(
                tipo = TipoEvidencia.OUI,
                valorBruto = prefixoDispositivo,
                peso = PesoEvidencia.FRACO,
            ),
        )
    }

    return ResultadoCorrelacaoTopologia(dispositivo = dispositivo, nivel = NivelCorrelacao.SEM_MATCH)
}

/**
 * Localiza, dentre os dispositivos do scan LAN ([dispositivos], feature/devices), aquele cujo
 * MAC bate exatamente com o [bssid] de um nó Wi-Fi — usado pra abrir a sheet de detalhe de
 * dispositivo (ex.: `MeshApSheet`) a partir de um nó da árvore de topologia da tela Sinal,
 * reaproveitando o mesmo dado já carregado em Dispositivos (5a). GH#1025.
 *
 * Best-effort e unidirecional (nó Wi-Fi → dispositivo, sentido inverso de
 * [correlacionarDispositivoComTopologia]): quando nenhum dispositivo tem MAC resolvido ou
 * nenhum bate com o [bssid] (ex.: o AP ainda não foi descoberto pelo scan LAN), devolve null —
 * quem chama decide o fallback, nunca fabrica um [DispositivoRede] parcial.
 */
fun encontrarDispositivoPorBssid(
    dispositivos: List<DispositivoRede>,
    bssid: String,
): DispositivoRede? {
    val bssidNormalizado = OuiCatalog.normalizarMacCompleto(bssid) ?: return null
    return dispositivos.firstOrNull { OuiCatalog.normalizarMacCompleto(it.mac) == bssidNormalizado }
}
