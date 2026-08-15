package io.signallq.app.feature.devices

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes de CARACTERIZAÇÃO de [ClassificadorDispositivoRede] — issue #976 (fase Preparação
 * do épico #975).
 *
 * Trava o comportamento ATUAL do classificador de dispositivos como baseline, focado no
 * cenário "dispositivo cliente comum do mesmo fabricante do roteador" listado na issue —
 * risco de falso positivo que a Fase 4 (correlação best-effort) do #975 precisa respeitar
 * quando for implementada ("um smartphone Intelbras não vira 'possível roteador' só por
 * compartilhar fabricante com o gateway Intelbras da casa").
 */
class ClassificadorDispositivoCaracterizacaoTest {

    private fun dispositivo(
        nome: String = "Dispositivo não identificado",
        fonteNome: String = "arp",
        tiposServico: Set<String> = emptySet(),
        portas: Set<Int> = emptySet(),
        mac: String? = null,
    ) = DispositivoRede(
        id = "test:$nome",
        ip = "192.168.1.100",
        mac = mac,
        nomeExibicao = nome,
        fonteNome = fonteNome,
        tiposServicoMdns = tiposServico,
        portasAbertas = portas,
    )

    // ─── Dispositivo cliente comum do mesmo fabricante do roteador ─────────────────

    @Test
    fun `BUG CONHECIDO - dispositivo cliente comum sem outros sinais e fabricante TP-Link e classificado como roteador so pelo fabricante`() {
        // "TP-Link" está em fabricantesRoteador. Um dispositivo cliente qualquer (ex.: um
        // acessório/smart plug TP-Link, ou qualquer aparelho cujo OUI resolva pro
        // fabricante do roteador) sem nome/porta/mDNS que o identifique melhor cai no
        // fallback de fabricante e é classificado como roteador — mesmo não sendo o
        // gateway. Esse é exatamente o risco "mesmo fabricante != é o roteador" citado na
        // issue #976. Comportamento atual, não corrigir aqui — vira requisito explícito
        // da Fase 4 (correlação) do épico #975: correlação só por fabricante/OUI deve ser
        // evidência fraca, nunca veredito.
        val clienteComum = dispositivo(nome = "Dispositivo não identificado", fonteNome = "arp")
        val tipo = ClassificadorDispositivoRede.classificar(clienteComum, "TP-Link")
        assertEquals(TipoDispositivo.roteador, tipo)
    }

    @Test
    fun `dispositivo cliente com nome de smartphone Apple nao e afetado por fabricante do roteador ser outro`() {
        // Caso de controle: quando há sinal de nome mais forte (Apple/iPhone), o fabricante
        // do roteador (mesmo que fosse compartilhado) não deveria mudar o resultado —
        // confirma que o problema acima é específico do fallback "sem outros hints".
        val iphone = dispositivo(nome = "iPhone de Maria", fonteNome = "arp")
        val tipo = ClassificadorDispositivoRede.classificar(iphone, "TP-Link")
        assertEquals(TipoDispositivo.smartphone, tipo)
    }

    @Test
    fun `gateway real fabricante Intelbras continua roteador (nao e o cenario de falso positivo)`() {
        // Contraste com o cenário acima: o próprio gateway (fonteNome == "gateway") deve
        // continuar sendo roteador — o risco documentado é só para dispositivos que NÃO
        // são o gateway mas compartilham fabricante com ele.
        val gateway = dispositivo(nome = "Roteador Intelbras", fonteNome = "gateway")
        val tipo = ClassificadorDispositivoRede.classificar(gateway, "Intelbras")
        assertEquals(TipoDispositivo.roteador, tipo)
    }
}
