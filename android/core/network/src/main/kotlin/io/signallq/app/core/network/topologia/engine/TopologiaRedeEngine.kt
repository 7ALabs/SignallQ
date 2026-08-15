package io.signallq.app.core.network.topologia.engine

import io.signallq.app.core.network.contracts.topologia.ClassificacaoTopologia
import io.signallq.app.core.network.contracts.topologia.ConflitoSinal
import io.signallq.app.core.network.contracts.topologia.Evidencia
import io.signallq.app.core.network.contracts.topologia.NivelConfianca
import io.signallq.app.core.network.contracts.topologia.OrigemDados
import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import io.signallq.app.core.network.contracts.topologia.PesoEvidencia
import io.signallq.app.core.network.contracts.topologia.TipoEvidencia
import io.signallq.app.core.network.contracts.wifi.RedeVizinha
import io.signallq.app.core.network.topologia.oui.OuiCatalog

/**
 * Motor de topologia unificado — Fase 2A do plano de unificação de topologia Wi-Fi
 * (issue #975/#979).
 *
 * Rodou em paralelo aos três motores antigos (`TopologiaWifiEngine`, `GatewayHeuristica`,
 * `ClassificadorDispositivoRede`) até a Fase 2B migrar Home/Sinal/Recomendação para este motor
 * único; `TopologiaWifiEngine` e `GatewayHeuristica` foram removidos na Fase 2C (#981). Absorve:
 * - Agrupamento por SSID + OUI + banda (mesma análise do antigo `TopologiaWifiEngine`), mas usando
 *   o catálogo único ([OuiCatalog]) e expressando cada sinal como [Evidencia] com peso.
 * - Palavra-chave de SSID (mesma lista do antigo `GatewayHeuristica`) como evidência adicional — nunca
 *   sobrescreve sozinha um sinal de OUI/banda mais forte; quando os dois concordam a confiança
 *   sobe, quando divergem vira [ConflitoSinal] com o OUI/banda priorizado (é sinal de hardware;
 *   SSID é configurável pelo usuário).
 * - Mesh sem 2ª rota IP: quando o grupo de BSSIDs do SSID tem evidência de múltiplos nós (mesmo
 *   OUI, banda repetida) mas não há confirmação de qual nó é o central (ver
 *   [temConfirmacaoRoteadorCentral]), o resultado é [PapelTopologia.SISTEMA_MESH_PROVAVEL] para
 *   todos os nós do grupo — nunca a afirmação de qual nó específico é o roteador (ajuste #1 da
 *   revisão do plano).
 */
object TopologiaRedeEngine {
    // Mesmas listas do antigo `GatewayHeuristica.kt` (app module) — duplicadas aqui porque
    // `:coreNetwork` não pode depender de `:app`. `GatewayHeuristica` foi removida na Fase 2C (#981).
    private val SSID_MESH_EXATOS = listOf("MESH", "DECO", "EERO", "VELOP")
    private val SSID_MESH_PREFIXO = listOf("ORBI")
    private val SSID_EXTENSOR_EXATOS = listOf("EXTENSOR", "REPEATER")
    private val SSID_EXTENSOR_TOKEN = listOf("EXT", "RANGE")

    private enum class SinalSsid { MESH, EXTENSOR, NENHUM }

    private fun tokenize(upper: String): Set<String> =
        upper.split(Regex("[^A-Z0-9]")).filter { it.isNotEmpty() }.toSet()

    private fun sinalSsid(ssid: String?): SinalSsid {
        if (ssid.isNullOrEmpty()) return SinalSsid.NENHUM
        val upper = ssid.uppercase()
        val tokens = tokenize(upper)
        if (SSID_MESH_EXATOS.any { upper.contains(it) }) return SinalSsid.MESH
        if (SSID_MESH_PREFIXO.any { tokens.contains(it) }) return SinalSsid.MESH
        if (SSID_EXTENSOR_EXATOS.any { upper.contains(it) }) return SinalSsid.EXTENSOR
        if (SSID_EXTENSOR_TOKEN.any { tokens.contains(it) }) return SinalSsid.EXTENSOR
        return SinalSsid.NENHUM
    }

    /**
     * Classifica cada rede vizinha, retornando o par (rede, classificação estruturada).
     *
     * @param temConfirmacaoRoteadorCentral evidência independente (ex.: `GatewayInfo` com uma
     * segunda rota IP visível) de que dá pra afirmar qual nó do grupo é o roteador central. Sem
     * essa confirmação, um grupo com evidência de múltiplos nós vira [PapelTopologia.SISTEMA_MESH_PROVAVEL]
     * pra todo mundo do grupo, nunca um veredito afirmativo de "roteador central" (ajuste #1).
     */
    fun classificar(
        redes: List<RedeVizinha>,
        connectedBssid: String?,
        temConfirmacaoRoteadorCentral: Boolean = false,
    ): List<Pair<RedeVizinha, ClassificacaoTopologia>> {
        if (redes.isEmpty()) return emptyList()

        val porSsid = redes.groupBy { it.ssid }
        return redes.map { rede ->
            val grupo = porSsid[rede.ssid] ?: listOf(rede)
            rede to classificarRede(rede, grupo, connectedBssid, temConfirmacaoRoteadorCentral)
        }
    }

    private fun classificarRede(
        rede: RedeVizinha,
        grupo: List<RedeVizinha>,
        connectedBssid: String?,
        temConfirmacaoRoteadorCentral: Boolean,
    ): ClassificacaoTopologia {
        val oui = rede.oui.uppercase()
        val entry = OuiCatalog.lookup(oui)
        val papeis = entry?.papeisPossiveis ?: emptySet()

        val isUnico = grupo.size == 1
        val ouisNoGrupo = grupo.map { it.oui.uppercase() }.toSet()
        val todosOuiIguais = ouisNoGrupo.size == 1
        val bandas = grupo.map { it.banda }
        val bandaRepetida = grupo.size > 1 && bandas.size != bandas.toSet().size

        val evidencias = mutableListOf<Evidencia>()
        if (oui.isNotEmpty()) evidencias += Evidencia(TipoEvidencia.OUI, oui, PesoEvidencia.FORTE)
        if (grupo.size > 1 && todosOuiIguais) {
            evidencias += Evidencia(TipoEvidencia.BANDA, bandas.joinToString(","), PesoEvidencia.MEDIO)
        }
        val sinalSsid = sinalSsid(rede.ssid)
        if (sinalSsid != SinalSsid.NENHUM) {
            evidencias += Evidencia(TipoEvidencia.SSID, rede.ssid.orEmpty(), PesoEvidencia.MEDIO)
        }

        var (papel, confianca) =
            decidirPapelPorOuiEBanda(
                rede = rede,
                grupo = grupo,
                connectedBssid = connectedBssid,
                temConfirmacaoRoteadorCentral = temConfirmacaoRoteadorCentral,
                papeis = papeis,
                isUnico = isUnico,
                todosOuiIguais = todosOuiIguais,
                bandaRepetida = bandaRepetida,
            )

        val conflitos = mutableListOf<ConflitoSinal>()
        if (papeis.size > 1) {
            // OUI ambíguo isolado (ex.: Intelbras C46E1F/6C5AB0) sem grupo que desempate —
            // decidirPapelPorOuiEBanda já classificou como DESCONHECIDO/BAIXA; registra o
            // conflito entre as duas leituras possíveis do mesmo OUI para auditoria.
            if (papel == PapelTopologia.DESCONHECIDO) {
                val evidenciaA = Evidencia(TipoEvidencia.OUI, oui, PesoEvidencia.MEDIO)
                val evidenciaB = Evidencia(TipoEvidencia.OUI, oui, PesoEvidencia.MEDIO)
                conflitos += ConflitoSinal(
                    evidenciaA,
                    evidenciaB,
                    "OUI $oui consta como nó mesh e como gateway ISP — sem outro nó no grupo pra desempatar",
                )
            }
        }

        val papelIndicaMesh = papel == PapelTopologia.NO_MESH || papel == PapelTopologia.SISTEMA_MESH_PROVAVEL
        val papelIndicaExtensor = papel == PapelTopologia.REPETIDOR

        when (sinalSsid) {
            SinalSsid.MESH -> {
                if (papelIndicaMesh) {
                    confianca = subirConfianca(confianca)
                } else if (papel == PapelTopologia.ROTEADOR || papel == PapelTopologia.REPETIDOR) {
                    conflitos += conflitoSsidVsHardware(oui, rede.ssid, papel, evidencias)
                }
            }
            SinalSsid.EXTENSOR -> {
                if (papelIndicaExtensor) {
                    confianca = subirConfianca(confianca)
                } else if (papel == PapelTopologia.ROTEADOR || papelIndicaMesh) {
                    conflitos += conflitoSsidVsHardware(oui, rede.ssid, papel, evidencias)
                }
            }
            SinalSsid.NENHUM -> Unit
        }

        return ClassificacaoTopologia(
            papelProvavel = papel,
            confianca = confianca,
            evidencias = evidencias,
            origemDados = OrigemDados.SCAN_WIFI_PASSIVO,
            conflitos = conflitos,
        )
    }

    private fun conflitoSsidVsHardware(
        oui: String,
        ssid: String?,
        papel: PapelTopologia,
        evidencias: List<Evidencia>,
    ): ConflitoSinal {
        val evidenciaOui = evidencias.firstOrNull { it.tipo == TipoEvidencia.OUI }
            ?: Evidencia(TipoEvidencia.OUI, oui, PesoEvidencia.FORTE)
        val evidenciaSsid = evidencias.firstOrNull { it.tipo == TipoEvidencia.SSID }
            ?: Evidencia(TipoEvidencia.SSID, ssid.orEmpty(), PesoEvidencia.MEDIO)
        return ConflitoSinal(
            evidenciaA = evidenciaOui,
            evidenciaB = evidenciaSsid,
            descricao = "SSID '$ssid' diverge do papel $papel indicado por OUI/banda — OUI priorizado",
        )
    }

    private fun subirConfianca(atual: NivelConfianca): NivelConfianca = when (atual) {
        NivelConfianca.BAIXA -> NivelConfianca.MEDIA
        NivelConfianca.MEDIA -> NivelConfianca.ALTA
        NivelConfianca.ALTA -> NivelConfianca.ALTA
    }

    private fun decidirPapelPorOuiEBanda(
        rede: RedeVizinha,
        grupo: List<RedeVizinha>,
        connectedBssid: String?,
        temConfirmacaoRoteadorCentral: Boolean,
        papeis: Set<PapelTopologia>,
        isUnico: Boolean,
        todosOuiIguais: Boolean,
        bandaRepetida: Boolean,
    ): Pair<PapelTopologia, NivelConfianca> {
        if (papeis.contains(PapelTopologia.NO_MESH)) {
            if (isUnico || todosOuiIguais) {
                return when {
                    !isUnico && bandaRepetida -> {
                        // Evidência real de múltiplos nós físicos (mesmo OUI, banda repetida).
                        if (temConfirmacaoRoteadorCentral) {
                            if (rede.bssid == connectedBssid) {
                                PapelTopologia.ROTEADOR to NivelConfianca.ALTA
                            } else {
                                PapelTopologia.NO_MESH to NivelConfianca.ALTA
                            }
                        } else {
                            // Sem 2ª rota IP: não afirma qual nó é o central (ajuste #1).
                            PapelTopologia.SISTEMA_MESH_PROVAVEL to NivelConfianca.MEDIA
                        }
                    }
                    isUnico && papeis.contains(PapelTopologia.ROTEADOR) -> {
                        // OUI ambíguo (ex.: Intelbras) isolado, sem grupo que desempate.
                        PapelTopologia.DESCONHECIDO to NivelConfianca.BAIXA
                    }
                    isUnico -> {
                        // OUI de linha mesh, sem ambiguidade de papel, sem grupo — sinal forte e não-contestado.
                        PapelTopologia.NO_MESH to NivelConfianca.ALTA
                    }
                    else -> {
                        // Mesmo OUI, grupo > 1, banda NÃO repetida — um AP físico único
                        // expondo bandas distintas. Banda tem prioridade sobre a ambiguidade
                        // de papel do OUI.
                        PapelTopologia.ROTEADOR to NivelConfianca.MEDIA
                    }
                }
            }
            // OUI de linha mesh, mas grupo com OUIs diferentes (ex.: roteador + extensor de
            // outro fabricante) — não é o sinal de mesh homogêneo; cai na lógica de grupo misto.
            return decidirGrupoMisto(rede, grupo)
        }

        if (papeis.contains(PapelTopologia.ROTEADOR)) {
            return PapelTopologia.ROTEADOR to NivelConfianca.ALTA
        }

        // OUI desconhecido/genérico (sem papel de topologia declarado no catálogo).
        if (grupo.size > 1) {
            return if (todosOuiIguais) {
                if (bandaRepetida) {
                    if (temConfirmacaoRoteadorCentral) {
                        if (rede.bssid == connectedBssid) {
                            PapelTopologia.ROTEADOR to NivelConfianca.MEDIA
                        } else {
                            PapelTopologia.NO_MESH to NivelConfianca.MEDIA
                        }
                    } else {
                        PapelTopologia.SISTEMA_MESH_PROVAVEL to NivelConfianca.BAIXA
                    }
                } else {
                    PapelTopologia.ROTEADOR to NivelConfianca.MEDIA
                }
            } else {
                decidirGrupoMisto(rede, grupo)
            }
        }

        return PapelTopologia.ROTEADOR to NivelConfianca.BAIXA
    }

    private fun decidirGrupoMisto(
        rede: RedeVizinha,
        grupo: List<RedeVizinha>,
    ): Pair<PapelTopologia, NivelConfianca> {
        val maisForte = grupo.maxByOrNull { it.rssiDbm }
        return if (rede.bssid == maisForte?.bssid) {
            PapelTopologia.ROTEADOR to NivelConfianca.MEDIA
        } else {
            PapelTopologia.REPETIDOR to NivelConfianca.MEDIA
        }
    }
}
