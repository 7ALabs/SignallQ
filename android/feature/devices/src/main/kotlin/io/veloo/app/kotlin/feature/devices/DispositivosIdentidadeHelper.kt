package io.signallq.app.feature.devices

/**
 * Logica pura de identificacao estavel de dispositivos entre scans.
 *
 * Extraido do DevicesViewModel para facilitar testes unitarios JVM puros,
 * sem necessidade de Hilt, Room ou Android runtime.
 */
object DispositivosIdentidadeHelper {

    /**
     * Retorna uma identidade estavel para rastrear dispositivos entre scans.
     *
     * - Com MAC disponivel: retorna null — o fluxo via Room (ApelidoDispositivoDao) cobre esse caso.
     * - Sem MAC: retorna "ipnome:<IP>:<nome normalizado>" como fallback baseado em IP + nome.
     *
     * LIMITACAO DOCUMENTADA: dispositivos sem MAC tem identidade derivada de ip+nome.
     * Se o IP mudar por DHCP ou o nome mudar (ex: reboot muda hostname), o dispositivo
     * pode ser notificado novamente como "novo". Comportamento aceitavel dado que MACs
     * randomizados no Android 10+ tornam a alternativa (so MAC) pior — detectaria nada.
     */
    fun identidadeEstavelDispositivo(dispositivo: DispositivoRede): String? {
        if (dispositivo.mac != null) return null
        val ip = dispositivo.ip ?: return null
        val nome = dispositivo.nomeExibicao.trim().lowercase()
        return "ipnome:$ip:$nome"
    }
}
