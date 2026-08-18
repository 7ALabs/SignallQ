package io.signallq.app.ads

import com.google.android.ump.ConsentInformation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // Ressalva R5 de Caio: a versão anterior deste teste usava `assertNotNull` sobre um retorno
    // Kotlin não-nulo — tautologia que nenhuma mutação mata. O que importa é que só `REQUIRED`
    // leve ao formulário; qualquer status novo do SDK cai no destino que funciona em toda região.
    @Test
    fun `so REQUIRED leva ao formulario, todo o resto vai para o sistema`() {
        ConsentInformation.PrivacyOptionsRequirementStatus.entries.forEach { status ->
            val esperado =
                if (status == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) {
                    ConsentManager.DestinoOpcoesAnuncios.FORMULARIO_UMP
                } else {
                    ConsentManager.DestinoOpcoesAnuncios.CONFIGURACOES_DO_ANDROID
                }
            assertEquals("destino errado para $status", esperado, ConsentManager.destinoDasOpcoes(status))
        }
    }

    // BLOQUEIO B3 de Caio na PR #1717. O roteamento e a mudanca central da issue e nao tinha um
    // unico teste: trocar o `if` do overlay por `if (true)` passava na suite inteira do :app, e
    // todo mundo fora do GDPR cairia no formulario vazio da UMP.
    @Test
    fun `sob GDPR abre o formulario e nao mexe no sistema`() {
        var formulario = 0
        var sistema = 0

        val abriu =
            ConsentManager.executarOpcoesDeAnuncios(
                destino = ConsentManager.DestinoOpcoesAnuncios.FORMULARIO_UMP,
                abrirFormulario = { formulario += 1 },
                abrirSistema = {
                    sistema += 1
                    true
                },
            )

        assertTrue(abriu)
        assertEquals(1, formulario)
        assertEquals("nao pode abrir a tela do sistema sob GDPR", 0, sistema)
    }

    @Test
    fun `fora do GDPR abre o sistema e nao o formulario vazio`() {
        var formulario = 0
        var sistema = 0

        val abriu =
            ConsentManager.executarOpcoesDeAnuncios(
                destino = ConsentManager.DestinoOpcoesAnuncios.CONFIGURACOES_DO_ANDROID,
                abrirFormulario = { formulario += 1 },
                abrirSistema = {
                    sistema += 1
                    true
                },
            )

        assertTrue(abriu)
        assertEquals("nao pode abrir formulario vazio fora do GDPR", 0, formulario)
        assertEquals(1, sistema)
    }

    // A mensagem de erro tambem nao tinha teste (mutante M2 do parecer): fazer
    // `abrirConfiguracoesDeAnuncios` devolver `true` sempre matava o aviso sem nada reclamar.
    @Test
    fun `aparelho sem tela de anuncios devolve falso para o chamador avisar`() {
        val abriu =
            ConsentManager.executarOpcoesDeAnuncios(
                destino = ConsentManager.DestinoOpcoesAnuncios.CONFIGURACOES_DO_ANDROID,
                abrirFormulario = { error("nao deve abrir formulario") },
                abrirSistema = { false },
            )

        assertFalse("toque mudo e pior que aviso", abriu)
    }

    // RESSALVA R8 de Caio na PR #1717. A lista de acoes nao tinha teste: apagar a acao especifica
    // de anuncios e deixar so a tela generica de Privacidade passava na suite inteira. Sob esse
    // mutante, todo mundo fora do GDPR aterrissa na Privacidade generica e precisa achar
    // "Anuncios" sozinho -- e a §4 da politica publicada afirma que o item "leva as configuracoes
    // de anuncios do Android, onde e possivel limitar a personalizacao". A frase viraria falsa
    // sem nada reclamar.
    @Test
    fun `a primeira acao e a tela de anuncios, nao a de privacidade generica`() {
        assertEquals(
            "a acao especifica precisa vir primeiro, senao a §4 da politica deixa de ser verdade",
            "com.google.android.gms.settings.ADS_PRIVACY",
            ConsentManager.ACOES_ANUNCIOS_DO_SISTEMA.first(),
        )
        assertTrue(
            "precisa haver um fallback depois da acao especifica",
            ConsentManager.ACOES_ANUNCIOS_DO_SISTEMA.size >= 2,
        )
    }
}
