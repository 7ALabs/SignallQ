package io.signallq.app.feature.devices

/**
 * GH#1217 item 1/RF (identidade e confiança) — antes a UI não tinha nenhum sinal de quão
 * confiável é a identidade de um [DispositivoRede] entre scans. Sem MAC, o app usa uma chave
 * sintética `ipnome:<ip>:<nome>` (ver [DispositivosIdentidadeHelper]) que quebra se o DHCP
 * trocar o IP ou o hostname mudar — mas isso nunca era comunicado, então um apelido podia
 * "desaparecer" ou um dispositivo reaparecer como "novo" sem nenhuma explicação visível.
 *
 * Ordem do mais pro menos confiável:
 * - [CONFIRMADA] — MAC real disponível. Estável entre scans (Android randomiza por rede, não
 *   por sessão — o mesmo MAC volta a aparecer na mesma rede).
 * - [PROVAVEL] — sem MAC, mas há correlação forte: IP presente + nome não-genérico (resolvido
 *   por SSDP/mDNS/reverse DNS/hostname do roteador) + fabricante identificado.
 * - [TEMPORARIA] — só IP e/ou nome genérico neste ciclo, sem nenhuma corroboração adicional.
 *   Corresponde exatamente à identidade "ipnome:" já documentada como limitação conhecida.
 * - [DESCONHECIDA] — evidência insuficiente pra identificar o dispositivo de qualquer forma
 *   (sem MAC, sem IP utilizável).
 */
enum class NivelConfiancaIdentidade {
    CONFIRMADA,
    PROVAVEL,
    TEMPORARIA,
    DESCONHECIDA,
}

object AvaliadorConfiancaIdentidade {
    private val NOMES_SEM_CORROBORACAO = setOf(
        "Dispositivo não identificado",
        "Host ativo",
        "Serviço mDNS",
        "Dispositivo SSDP",
        "Gateway",
    )

    fun avaliar(dispositivo: DispositivoRede): NivelConfiancaIdentidade {
        // "Este aparelho" é sempre confirmado -- o app sabe quem é via Build.MODEL/MANUFACTURER,
        // não depende de MAC/heurística de rede pra se identificar.
        if (dispositivo.esteDispositivo) return NivelConfiancaIdentidade.CONFIRMADA
        if (!dispositivo.mac.isNullOrBlank()) return NivelConfiancaIdentidade.CONFIRMADA

        val ip = dispositivo.ip
        if (ip.isNullOrBlank()) return NivelConfiancaIdentidade.DESCONHECIDA

        val nomeCorrobora =
            dispositivo.nomeExibicao !in NOMES_SEM_CORROBORACAO &&
                !dispositivo.nomeExibicao.startsWith("Dispositivo ") &&
                !dispositivo.nomeExibicao.startsWith("Roteador")
        val temFabricante = !dispositivo.fabricante.isNullOrBlank()

        return if (nomeCorrobora && temFabricante) {
            NivelConfiancaIdentidade.PROVAVEL
        } else {
            NivelConfiancaIdentidade.TEMPORARIA
        }
    }
}
