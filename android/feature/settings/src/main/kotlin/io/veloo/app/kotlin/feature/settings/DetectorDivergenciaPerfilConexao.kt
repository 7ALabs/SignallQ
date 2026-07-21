package io.signallq.app.feature.settings

/**
 * GH#1227 item 2/RF-B — resultado de comparar o [ConnectionProfile] salvo pra uma rede contra o
 * provedor atualmente detectado (Wi-Fi/topologia/operadora). Tipado em vez de `Boolean` porque as
 * três situações pedem tratamento diferente pelo chamador (silencioso vs. alerta vs. nada a fazer).
 */
sealed interface ResultadoDivergenciaPerfilConexao {
    /** Não há base de comparação: sem perfil salvo, sem provedor salvo nele, ou nada detectado. */
    data object SemBaseParaComparar : ResultadoDivergenciaPerfilConexao

    /** Provedor salvo e detectado coincidem (case-insensitive) — nada a fazer. */
    data object PerfilCoincide : ResultadoDivergenciaPerfilConexao

    /**
     * Diverge, mas o usuário nunca confirmou explicitamente o valor salvo
     * ([ConnectionProfile.userConfirmed] `false`) — o chamador deve sobrescrever o perfil com o
     * valor detectado silenciosamente, sem perguntar (ver KDoc de [ConnectionProfile]).
     */
    data class AtualizavelSilenciosamente(val salvo: String, val detectado: String) : ResultadoDivergenciaPerfilConexao

    /**
     * Diverge e o usuário confirmou o valor salvo explicitamente — sobrescrever sem avisar
     * jogaria fora uma escolha deliberada. O chamador deve sinalizar a divergência na UI e deixar
     * o usuário decidir qual valor manter.
     */
    data class DivergenciaConfirmadaPeloUsuario(val salvo: String, val detectado: String) : ResultadoDivergenciaPerfilConexao
}

/** Função pura, sem dependência de Android/Hilt/DataStore — testável isoladamente. */
object DetectorDivergenciaPerfilConexao {
    fun avaliar(
        perfilSalvo: ConnectionProfile?,
        providerDetectado: String?,
    ): ResultadoDivergenciaPerfilConexao {
        val salvo = perfilSalvo?.providerFixed?.trim()?.takeIf { it.isNotBlank() }
        val detectado = providerDetectado?.trim()?.takeIf { it.isNotBlank() }
        if (salvo == null || detectado == null) return ResultadoDivergenciaPerfilConexao.SemBaseParaComparar
        if (salvo.equals(detectado, ignoreCase = true)) return ResultadoDivergenciaPerfilConexao.PerfilCoincide
        return if (perfilSalvo.userConfirmed) {
            ResultadoDivergenciaPerfilConexao.DivergenciaConfirmadaPeloUsuario(salvo, detectado)
        } else {
            ResultadoDivergenciaPerfilConexao.AtualizavelSilenciosamente(salvo, detectado)
        }
    }
}
