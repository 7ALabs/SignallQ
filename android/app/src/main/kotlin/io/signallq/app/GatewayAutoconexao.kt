package io.signallq.app

/**
 * Determina se a sessao "manter conectado" do gateway (GH#527, epic #525) e elegivel
 * para tentativa de autoconexao silenciosa: o toggle precisa estar ativo, precisa existir
 * um BSSID vinculado a credencial salva, e o BSSID atual do link Wi-Fi precisa bater
 * exatamente com o vinculado.
 *
 * Compara por BSSID (endereco do radio do gateway) e nunca por SSID (nome da rede) porque
 * o SSID pode se repetir entre redes fisicamente diferentes — autoconectar so pelo nome
 * abriria a porta para autenticar num equipamento errado (ex.: rede clonada/vizinha com o
 * mesmo nome).
 */
fun bssidElegivelParaAutoconexao(
    permanecerConectado: Boolean,
    bssidVinculado: String?,
    bssidAtual: String?,
): Boolean =
    permanecerConectado &&
        bssidVinculado != null &&
        bssidAtual != null &&
        bssidVinculado == bssidAtual
