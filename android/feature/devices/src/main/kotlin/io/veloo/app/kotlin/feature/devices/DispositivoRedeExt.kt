package io.signallq.app.feature.devices

/**
 * GH#531 — dispositivo-cliente "real" da rede: exclui o próprio gateway e nós
 * mesh/pontos de acesso, que são infraestrutura e não aparelhos do usuário.
 * Fonte única para qualquer contagem de "N dispositivos"/"N clientes" exibida
 * em Ajustes ou Dispositivos.
 */
fun DispositivoRede.ehClienteFinal(): Boolean =
    fonteNome != "gateway" && tipoDispositivo != TipoDispositivo.pontoAcesso
