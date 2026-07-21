package io.signallq.app.feature.fibra

/**
 * GH#1213 item 12 — o reboot do Nokia G-1425G-B (`NokiaModemClient.reboot`, rota `reboot.cgi`)
 * nunca foi validado contra hardware físico: só o menu/rota foram confirmados por engenharia
 * reversa, não o payload exato do POST nem o comportamento real do equipamento ao reiniciar.
 * Até essa validação acontecer, a ação não pode ficar disponível em produção — critério de
 * aceite explícito da issue ("Reboot não validado fica indisponível em produção").
 *
 * Flag única e isolada (em vez de espalhar `if (BuildConfig.DEBUG)` pelo código) pra ser fácil
 * de encontrar e de flipar no dia em que a validação real acontecer — não remove a
 * implementação nem os testes, só desliga a exposição na UI/capabilities.
 */
object RebootLabFlags {
    const val HABILITADO_SEM_VALIDACAO_HARDWARE = false
}
