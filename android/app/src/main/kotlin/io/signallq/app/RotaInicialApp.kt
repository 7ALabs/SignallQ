package io.signallq.app

// Issue #1671 (Task 2.0.23, épico #1647) — extrai a decisão de qual tela mostrar no cold
// start (Onboarding/LGPD/Home) do if/else embutido em MainActivity.onCreate para uma função
// pura testável. Mantém o comportamento existente (#895): `null` é um terceiro estado real
// ("DataStore ainda não respondeu"), distinto de `false` ("usuário novo").

/** Rotas possíveis na raiz do app, decididas a partir dos dois flags persistidos em DataStore. */
sealed class RotaInicialApp {
    /** DataStore ainda não emitiu o valor real de onboarding (nem `true` nem `false`). */
    data object Carregando : RotaInicialApp()

    /** Usuário novo ou que nunca concluiu o onboarding — nunca reexibido para quem já concluiu. */
    data object Onboarding : RotaInicialApp()

    /** Onboarding concluído, mas ainda sem escolha explícita de consentimento LGPD. */
    data object ConsentimentoLgpd : RotaInicialApp()

    /** Onboarding concluído e consentimento LGPD já respondido (aceito ou recusado). */
    data object Home : RotaInicialApp()
}

/**
 * Decide a rota inicial a partir dos flags persistidos.
 *
 * @param onboardingConcluido `null` enquanto o DataStore não respondeu; `false` para quem
 *   nunca concluiu o onboarding; `true` para quem já concluiu (nunca mais vê Onboarding).
 * @param consentimentoLgpd `null` enquanto a escolha de LGPD está pendente; `true`/`false`
 *   depois que o usuário aceitou ou recusou.
 */
fun rotaInicialApp(
    onboardingConcluido: Boolean?,
    consentimentoLgpd: Boolean?,
): RotaInicialApp =
    when {
        onboardingConcluido == null -> RotaInicialApp.Carregando
        !onboardingConcluido -> RotaInicialApp.Onboarding
        consentimentoLgpd == null -> RotaInicialApp.ConsentimentoLgpd
        else -> RotaInicialApp.Home
    }
