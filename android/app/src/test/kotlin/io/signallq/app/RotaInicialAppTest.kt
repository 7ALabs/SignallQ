package io.signallq.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes de caracterização da issue #1671 (Task 2.0.23, épico #1647) — cobrem os cenários do
 * critério de aceite "usuário existente não é re-onboarded indevidamente" e os estados listados
 * na issue: primeira abertura, retorno, aceite pendente, consentimento indisponível.
 */
class RotaInicialAppTest {
    @Test
    fun `primeira abertura, DataStore ainda nao respondeu, mostra Carregando`() {
        assertEquals(RotaInicialApp.Carregando, rotaInicialApp(onboardingConcluido = null, consentimentoLgpd = null))
    }

    @Test
    fun `Carregando independe do valor de consentimentoLgpd enquanto onboarding e null`() {
        assertEquals(RotaInicialApp.Carregando, rotaInicialApp(onboardingConcluido = null, consentimentoLgpd = true))
        assertEquals(RotaInicialApp.Carregando, rotaInicialApp(onboardingConcluido = null, consentimentoLgpd = false))
    }

    @Test
    fun `usuario novo, onboarding nao concluido, mostra Onboarding`() {
        assertEquals(RotaInicialApp.Onboarding, rotaInicialApp(onboardingConcluido = false, consentimentoLgpd = null))
    }

    @Test
    fun `onboarding concluido com aceite LGPD pendente mostra ConsentimentoLgpd`() {
        assertEquals(RotaInicialApp.ConsentimentoLgpd, rotaInicialApp(onboardingConcluido = true, consentimentoLgpd = null))
    }

    @Test
    fun `onboarding concluido e LGPD aceito mostra Home`() {
        assertEquals(RotaInicialApp.Home, rotaInicialApp(onboardingConcluido = true, consentimentoLgpd = true))
    }

    @Test
    fun `onboarding concluido e LGPD recusado tambem mostra Home (recusa e uma resposta valida)`() {
        assertEquals(RotaInicialApp.Home, rotaInicialApp(onboardingConcluido = true, consentimentoLgpd = false))
    }

    @Test
    fun `retorno de usuario existente nunca reexibe Onboarding, mesmo com LGPD ainda pendente`() {
        val rota = rotaInicialApp(onboardingConcluido = true, consentimentoLgpd = null)
        assert(rota != RotaInicialApp.Onboarding) { "usuário que já concluiu onboarding não pode ver Onboarding de novo" }
    }

    @Test
    fun `migracao de usuario existente com ambos os flags ja verdadeiros vai direto pra Home`() {
        assertEquals(RotaInicialApp.Home, rotaInicialApp(onboardingConcluido = true, consentimentoLgpd = true))
    }
}
