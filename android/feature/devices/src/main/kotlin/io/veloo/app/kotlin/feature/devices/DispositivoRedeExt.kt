package io.signallq.app.feature.devices

/**
 * GH#531 — dispositivo-cliente "real" da rede: exclui o próprio gateway e nós
 * mesh/pontos de acesso, que são infraestrutura e não aparelhos do usuário.
 * Fonte única para qualquer contagem de "N dispositivos"/"N clientes" exibida
 * em Ajustes ou Dispositivos.
 */
fun DispositivoRede.ehClienteFinal(): Boolean =
    fonteNome != "gateway" && tipoDispositivo != TipoDispositivo.pontoAcesso

/**
 * #853 — chave estavel usada para persistir/consultar o apelido do dispositivo.
 *
 * MAC e a chave preferencial (estavel entre reinicios do roteador), mas em boa parte
 * dos aparelhos/versoes de Android o MAC de terceiros na rede nao e resolvivel via ARP
 * (mesma limitacao ja documentada em [DispositivosIdentidadeHelper] para a deteccao de
 * dispositivo novo). Sem esse fallback, o campo "Apelido" nunca aparecia para a maioria
 * dos dispositivos reais — era essa a causa raiz do bug: a secao inteira ficava oculta
 * porque a UI so a exibia quando `mac != null`.
 *
 * LIMITACAO DOCUMENTADA: quando nao ha MAC, a chave e derivada de ip+nome. Se o IP mudar
 * por DHCP ou o nome mudar, o apelido salvo deixa de ser encontrado — mesmo trade-off ja
 * aceito em [DispositivosIdentidadeHelper.identidadeEstavelDispositivo].
 */
fun DispositivoRede.chaveApelido(): String? = mac ?: DispositivosIdentidadeHelper.identidadeEstavelDispositivo(this)
