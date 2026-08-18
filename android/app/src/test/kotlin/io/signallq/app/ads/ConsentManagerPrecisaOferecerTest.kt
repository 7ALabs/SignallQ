package io.signallq.app.ads

import com.google.android.ump.ConsentInformation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Predicado de conformidade da UMP — issue #1703, bloqueio 2 do parecer de Caio na PR #1709.
 *
 * Enquanto a regra vivia embutida na chamada que acessa o SDK, ela **não era testada por nenhum
 * meio**: em máquina não, porque exigiria `Activity` e o SDK real; em aparelho tampouco, porque o
 * caminho `REQUIRED` só ocorre sob GDPR e o *debug geography* do AdMob não está configurado no
 * projeto. Duas mutações sobreviviam à suíte inteira do `:app` — e são exatamente as duas formas
 * de errar a obrigação regulatória:
 *
 * - fixar `true`: a entrada aparece no Brasil e abre um formulário vazio;
 * - comparar com `NOT_REQUIRED`: a entrada **some** sob GDPR, ou seja, o descumprimento que a
 *   issue existe para corrigir volta intacto, sem nada quebrar.
 *
 * Separado o predicado, mata-se as duas com uma tabela de três casos, sem `Activity`, sem SDK e
 * sem VPN.
 */
class ConsentManagerPrecisaOferecerTest {
    @Test
    fun `REQUIRED exige oferecer a entrada`() {
        assertTrue(ConsentManager.precisaOferecer(ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED))
    }

    @Test
    fun `NOT_REQUIRED nao oferece`() {
        // Fora de região GDPR o formulário da UMP não tem o que mostrar — item que abre tela
        // vazia é pior que item ausente.
        assertFalse(ConsentManager.precisaOferecer(ConsentInformation.PrivacyOptionsRequirementStatus.NOT_REQUIRED))
    }

    @Test
    fun `UNKNOWN nao oferece`() {
        // `UNKNOWN` é o estado ANTES do primeiro `requestConsentInfoUpdate` da sessão; ele não
        // persiste depois de um update bem-sucedido, que resolve para REQUIRED ou NOT_REQUIRED.
        // Esconder aqui é cautela correta, não bug — o risco de ler cedo demais é de quem chama,
        // não do predicado.
        assertFalse(ConsentManager.precisaOferecer(ConsentInformation.PrivacyOptionsRequirementStatus.UNKNOWN))
    }

    @Test
    fun `exatamente um dos estados possiveis exige a entrada`() {
        // Trava a regra contra o enum inteiro em vez de caso a caso: se o SDK ganhar um valor novo
        // numa atualização, este teste falha e obriga uma decisão explícita, em vez de o valor
        // novo cair silenciosamente no `else` e sumir com a entrada.
        val queExigem =
            ConsentInformation.PrivacyOptionsRequirementStatus.entries.filter {
                ConsentManager.precisaOferecer(it)
            }
        assertTrue(
            "esperado exatamente [REQUIRED], veio $queExigem",
            queExigem == listOf(ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED),
        )
    }

    // GH#1717 — o destino da entrada "Preferências de anúncios".
    //
    // Até esta issue a entrada só existia sob GDPR: quem está no Brasil recebia anúncio
    // personalizado e não tinha NENHUM controle dentro do app. Agora ela existe sempre, e é o
    // destino que muda — o que exige que a regra de destino seja total, não parcial.
    //
    // Mutante que estes testes matam: devolver `FORMULARIO_UMP` sempre. A pessoa fora do GDPR
    // abriria um formulário vazio, que é exatamente o que o KDoc de
    // `precisaOferecerOpcoesPrivacidade` diz ser pior que não abrir nada.
    @Test
    fun `sob GDPR o destino e o formulario da UMP`() {
        assertEquals(
            ConsentManager.DestinoOpcoesAnuncios.FORMULARIO_UMP,
            ConsentManager.destinoDasOpcoes(ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED),
        )
    }

    @Test
    fun `sem exigencia da UMP o destino sao as configuracoes do Android`() {
        listOf(
            ConsentInformation.PrivacyOptionsRequirementStatus.NOT_REQUIRED,
            ConsentInformation.PrivacyOptionsRequirementStatus.UNKNOWN,
        ).forEach { status ->
            assertEquals(
                "status $status nao pode abrir formulario vazio",
                ConsentManager.DestinoOpcoesAnuncios.CONFIGURACOES_DO_ANDROID,
                ConsentManager.destinoDasOpcoes(status),
            )
        }
    }

    // A regra tem que cobrir TODOS os status — um `when` que esquecesse um valor novo do SDK
    // deixaria a pessoa sem destino, e a entrada existe para todos desde a #1717.
    @Test
    fun `todo status tem destino`() {
        ConsentInformation.PrivacyOptionsRequirementStatus.entries.forEach { status ->
            assertNotNull("status $status ficou sem destino", ConsentManager.destinoDasOpcoes(status))
        }
    }
}
