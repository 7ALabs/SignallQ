package io.signallq.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes de caracterização da issue #1671 (Task 2.0.23, épico #1647) — cobrem "permissão
 * aceita/negada em contexto": localização, telefonia e notificação passaram a ser solicitadas
 * no ponto de uso (nunca em lote no onboarding), então cada uma decide entre pedir o diálogo
 * real do SO ou mandar o usuário para Ajustes usando esta mesma função pura.
 */
class PermissaoContextualTest {
    @Test
    fun `permissao ja concedida nao solicita nada`() {
        assertEquals(
            DecisaoPermissaoContextual.JA_CONCEDIDA,
            decidirPermissaoContextual(concedida = true, bloqueadaPermanentemente = false),
        )
    }

    @Test
    fun `permissao ja concedida ignora bloqueio permanente (nao deveria acontecer, mas concedida vence)`() {
        assertEquals(
            DecisaoPermissaoContextual.JA_CONCEDIDA,
            decidirPermissaoContextual(concedida = true, bloqueadaPermanentemente = true),
        )
    }

    @Test
    fun `primeira vez, nao concedida e nao bloqueada, pede o dialogo real do sistema`() {
        assertEquals(
            DecisaoPermissaoContextual.SOLICITAR,
            decidirPermissaoContextual(concedida = false, bloqueadaPermanentemente = false),
        )
    }

    @Test
    fun `negada permanentemente manda para Ajustes do app em vez de reperguntar`() {
        assertEquals(
            DecisaoPermissaoContextual.ABRIR_AJUSTES,
            decidirPermissaoContextual(concedida = false, bloqueadaPermanentemente = true),
        )
    }
}
