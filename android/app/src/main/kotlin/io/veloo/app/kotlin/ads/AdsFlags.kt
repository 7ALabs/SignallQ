package io.signallq.app.ads

/**
 * Estado resolvido do toggle remoto de anuncios nativos (issue #555, Firebase Remote Config).
 *
 * [masterEnabled] liga/desliga tudo de uma vez; as 4 flags por tela permitem desligar
 * pontualmente uma tela sem afetar as outras. Default de todos os campos e `false` --
 * fetch falhou ou ainda nao completou nunca deve mostrar anuncio (regra do plano de
 * implementacao da #555: "nunca travar a tela esperando config" + fail-safe seguro).
 */
data class AdsFlags(
    val masterEnabled: Boolean = false,
    val velocidade: Boolean = false,
    val resultado: Boolean = false,
    val dispositivos: Boolean = false,
    val historico: Boolean = false,
) {
    fun habilitadoPara(slot: AdSlot): Boolean {
        if (!masterEnabled) return false
        return when (slot) {
            AdSlot.VELOCIDADE -> velocidade
            AdSlot.RESULTADO -> resultado
            AdSlot.DISPOSITIVOS -> dispositivos
            AdSlot.HISTORICO -> historico
        }
    }

    companion object {
        /** Fallback local seguro -- usado antes do primeiro fetch e se o fetch falhar. */
        val DESLIGADO = AdsFlags()
    }
}
