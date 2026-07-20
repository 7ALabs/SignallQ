package io.signallq.app.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Router
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.signallq.app.core.diagnostico.BandaWifi
import io.signallq.app.core.diagnostico.NivelCongestionamento
import io.signallq.app.core.network.contracts.topologia.NivelConfianca
import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import io.signallq.app.core.network.contracts.wifi.RedeVizinha
import io.signallq.app.core.network.contracts.wifi.SegurancaWifi
import io.signallq.app.core.network.topologia.engine.TopologiaRedeEngine
import io.signallq.app.feature.devices.DispositivoRede
import io.signallq.app.feature.devices.encontrarDispositivoPorBssid
import io.signallq.app.feature.wifi.ConfiancaTopologia
import io.signallq.app.feature.wifi.RedeClassificada
import io.signallq.app.feature.wifi.TipoTopologia
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkTokens

/**
 * GH#1207/#1209 (higiene §4.8b) — classificação de topologia/sinal/canal extraída de
 * `SinalScreen.kt` para um arquivo próprio, sem lógica de tela nenhuma (só funções puras). A
 * regra de higiene do repo pede que motores/classificadores não fiquem dentro da Composable —
 * este é o primeiro passo dessa separação (as seções Composable de cada aba — Móvel/Canal/Wi-Fi
 * — continuam em `SinalScreen.kt` por ora; extraí-las é passo seguinte, de risco maior por
 * mexer em estado/composição visual).
 */
internal data class TopologiaIconData(
    val icon: ImageVector,
    val cor: Color,
)

internal fun TipoTopologia.toIconData(c: LkTokens): TopologiaIconData? =
    when (this) {
        TipoTopologia.ROTEADOR -> TopologiaIconData(Icons.Outlined.Router, c.primary)
        TipoTopologia.ROTEADOR_MESH -> TopologiaIconData(Icons.Outlined.Hub, c.primary)
        TipoTopologia.NO_MESH -> TopologiaIconData(Icons.Outlined.Hub, c.primary)
        TipoTopologia.REPETIDOR -> TopologiaIconData(Icons.Outlined.CellTower, c.warning)
        TipoTopologia.PONTO_DE_ACESSO -> TopologiaIconData(Icons.Outlined.Lan, LkColors.signallQTextSecondaryOnDark)
        TipoTopologia.DESCONHECIDO -> null
    }

// #980 (Fase 2B) — adapta o resultado do motor unificado (TopologiaRedeEngine, Fase 2A/#979)
// pro shape legado (RedeClassificada/TipoTopologia/ConfiancaTopologia) que esta tela ja consome,
// pra nao precisar reescrever icone/agrupamento/renderizacao — so troca a fonte da classificacao.
// SISTEMA_MESH_PROVAVEL vira NO_MESH (mesmo icone de hoje; a incerteza ja e comunicada pelo aviso
// de GrupoRedeTree quando a confianca nao e ALTA — GH#1209 item 8).
internal fun PapelTopologia.paraTipoTopologiaLegado(): TipoTopologia =
    when (this) {
        PapelTopologia.ROTEADOR -> TipoTopologia.ROTEADOR
        PapelTopologia.NO_MESH -> TipoTopologia.NO_MESH
        PapelTopologia.SISTEMA_MESH_PROVAVEL -> TipoTopologia.NO_MESH
        PapelTopologia.REPETIDOR -> TipoTopologia.REPETIDOR
        PapelTopologia.PONTO_DE_ACESSO -> TipoTopologia.PONTO_DE_ACESSO
        PapelTopologia.DESCONHECIDO -> TipoTopologia.DESCONHECIDO
    }

internal fun NivelConfianca.paraConfiancaTopologiaLegado(): ConfiancaTopologia =
    when (this) {
        NivelConfianca.ALTA -> ConfiancaTopologia.ALTA
        NivelConfianca.MEDIA -> ConfiancaTopologia.MEDIA
        NivelConfianca.BAIXA -> ConfiancaTopologia.BAIXA
    }

// GH#1025 (3c) — tipos de nó que o protótipo trata como "ponto de acesso/mesh": abrem
// MeshApSheet (dado real do DispositivoRede correlacionado) em vez de NetworkDetailSheet.
// ROTEADOR fica de fora de propósito — é o próprio gateway conectado, tratado como "sua conexão"
// (NetworkDetailSheet já é a sheet certa pra ele); DESCONHECIDO também fica de fora (rede vizinha
// comum, sem classificação de topologia).
private val TIPOS_TOPOLOGIA_AP_MESH =
    setOf(
        TipoTopologia.ROTEADOR_MESH,
        TipoTopologia.NO_MESH,
        TipoTopologia.REPETIDOR,
        TipoTopologia.PONTO_DE_ACESSO,
    )

/**
 * Decide qual [DispositivoRede] (se algum) deve abrir MeshApSheet pra um nó [rede] da árvore de
 * topologia da tela Sinal — GH#1025. Só tenta correlacionar quando o nó já foi classificado como
 * AP/mesh ([TIPOS_TOPOLOGIA_AP_MESH]); fora disso devolve null sempre, mesmo que por acaso exista
 * um [DispositivoRede] com MAC batendo (evita abrir a sheet errada pra uma rede vizinha comum).
 *
 * Quando o nó é AP/mesh mas nenhum [DispositivoRede] correlaciona (scan LAN ainda não descobriu
 * esse equipamento, ou MAC não resolvível via ARP) devolve null — o chamador mantém o fallback
 * padrão (NetworkDetailSheet), nunca fabrica um [DispositivoRede] parcial.
 */
internal fun resolverDispositivoParaNoTopologia(
    rede: RedeVizinha,
    tipoTopologia: TipoTopologia?,
    dispositivosRede: List<DispositivoRede>,
): DispositivoRede? {
    if (tipoTopologia !in TIPOS_TOPOLOGIA_AP_MESH) return null
    return encontrarDispositivoPorBssid(dispositivosRede, rede.bssid)
}

internal fun classificarComMotorUnificado(
    redes: List<RedeVizinha>,
    connectedBssid: String?,
): List<RedeClassificada> =
    TopologiaRedeEngine
        .classificar(redes = redes, connectedBssid = connectedBssid)
        .map { (rede, classificacao) ->
            RedeClassificada(
                rede = rede,
                tipo = classificacao.papelProvavel.paraTipoTopologiaLegado(),
                confianca = classificacao.confianca.paraConfiancaTopologiaLegado(),
                motivo = "",
            )
        }

/**
 * GH#1209 item 1 — um BSSID só é considerado parte da MESMA estrutura de rede do usuário
 * quando o motor de topologia unificado já encontrou evidência suficiente pra classificá-lo
 * (OUI, banda, padrão de SSID) — nunca só por compartilhar o SSID com a rede conectada.
 * [TipoTopologia.DESCONHECIDO] ou ausência de classificação (BSSID fora do scan classificado)
 * significa "sem evidência", então o nó fica em "outras redes", mesmo com nome idêntico.
 */
internal fun ehMembroDaEstruturaPropria(tipo: TipoTopologia?): Boolean =
    tipo != null && tipo != TipoTopologia.DESCONHECIDO

/** GH#1209 item 6 — deriva [BandaWifi] (2 valores) a partir de [RedeVizinha.banda] (3 valores:
 *  "2.4GHz"/"5GHz"/"6GHz"), não mais de frequência bruta com 2 ramos que colapsava 6GHz
 *  silenciosamente dentro do "else". 6GHz reaproveita a régua de 5GHz de propósito
 *  (propagação equivalente) — não existe `BandaWifi.ghz6` hoje; adicioná-lo tem blast radius
 *  maior (~10 arquivos com `when` exaustivo) e fica para issue própria se o produto quiser régua
 *  dedicada de 6GHz. */
internal fun RedeVizinha.paraBandaWifi(): BandaWifi =
    when (banda) {
        "2.4GHz" -> BandaWifi.ghz24
        else -> BandaWifi.ghz5
    }

/** GH#1209 item 7 — rótulo de banda combinado quando um grupo tem BSSIDs em mais de uma
 *  banda (ex.: roteador dual/tri-band exibido como grupo). Antes mostrava só a banda de UM
 *  BSSID (o de 2,4GHz se existisse, senão a de qualquer outro) mesmo quando o grupo tinha
 *  duas ou três bandas presentes. */
internal fun List<RedeVizinha>.bandaCombinadaLabel(): String {
    val bandas = map { it.banda }.toSet()
    if (bandas.isEmpty()) return "—"
    if (bandas.size == 1) return bandas.first()
    if (bandas.size >= 3) return "Multibanda"
    val ordem = listOf("2.4GHz", "5GHz", "6GHz")
    val numeros = mapOf("2.4GHz" to "2,4", "5GHz" to "5", "6GHz" to "6")
    return ordem.filter { it in bandas }.mapNotNull { numeros[it] }.joinToString(" + ") + " GHz"
}

internal fun signalQuality(
    rssiDbm: Int,
    banda: BandaWifi = BandaWifi.desconhecida,
): String =
    when (banda) {
        BandaWifi.ghz5 ->
            when {
                rssiDbm >= -55 -> "Excelente"
                rssiDbm >= -65 -> "Bom"
                rssiDbm >= -75 -> "Regular"
                else -> "Fraco"
            }
        else ->
            when {
                rssiDbm >= -50 -> "Excelente"
                rssiDbm >= -60 -> "Bom"
                rssiDbm >= -70 -> "Regular"
                else -> "Fraco"
            }
    }

internal fun congestionColor(
    nivel: NivelCongestionamento,
    c: LkTokens,
): Color =
    when (nivel) {
        NivelCongestionamento.livre -> c.success
        NivelCongestionamento.moderado -> c.warning
        NivelCongestionamento.congestionado -> c.error
    }

internal fun securityLabel(s: SegurancaWifi): String =
    when (s) {
        SegurancaWifi.aberta -> "Aberta"
        SegurancaWifi.wep -> "WEP"
        SegurancaWifi.wpa -> "WPA"
        SegurancaWifi.wpa2 -> "WPA2"
        SegurancaWifi.wpa3 -> "WPA3"
        SegurancaWifi.desconhecida -> "Desconhecida"
    }
