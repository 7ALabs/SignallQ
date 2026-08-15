package io.signallq.app.feature.fibra

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * GH#1213 (dispatch final) — cenários mínimos de teste literalmente copiados da issue #1213
 * ("RX -18 dBm... dentro da faixa com margem", "RX -26 dBm: próximo ao limite inferior", etc.).
 */
class ClassificadorOpticoNokiaG1425GBTest {

    @Test
    fun `RX -18 dBm esta dentro da faixa com margem`() {
        assertEquals(NivelSinalOpticoRx.DENTRO_DA_FAIXA_COM_MARGEM, ClassificadorOpticoNokiaG1425GB.classificarRx(-18.0))
    }

    @Test
    fun `RX -26 dBm esta proximo ao limite inferior, sem afirmar falha imediata`() {
        assertEquals(NivelSinalOpticoRx.PROXIMO_AO_LIMITE, ClassificadorOpticoNokiaG1425GB.classificarRx(-26.0))
    }

    @Test
    fun `RX abaixo de -27 dBm esta fora da faixa da classe B+`() {
        assertEquals(NivelSinalOpticoRx.FORA_DE_ESPECIFICACAO, ClassificadorOpticoNokiaG1425GB.classificarRx(-27.1))
    }

    @Test
    fun `RX acima de -8 dBm (possivel sobrecarga) esta fora da faixa`() {
        assertEquals(NivelSinalOpticoRx.FORA_DE_ESPECIFICACAO, ClassificadorOpticoNokiaG1425GB.classificarRx(-7.9))
    }

    @Test
    fun `RX exatamente nas bordas normativas -27 e -8 ainda conta como proximo ao limite, nao fora`() {
        // A tabela da issue usa "< -27" e "> -8" (estritos) para "fora de especificacao" -- a
        // borda exata ja cai no bucket "proximo ao limite" ([-27,-25) / (-10,-8]).
        assertEquals(NivelSinalOpticoRx.PROXIMO_AO_LIMITE, ClassificadorOpticoNokiaG1425GB.classificarRx(-27.0))
        assertEquals(NivelSinalOpticoRx.PROXIMO_AO_LIMITE, ClassificadorOpticoNokiaG1425GB.classificarRx(-8.0))
    }

    @Test
    fun `RX exatamente nas bordas da margem -25 e -10 conta como dentro da faixa com margem`() {
        assertEquals(NivelSinalOpticoRx.DENTRO_DA_FAIXA_COM_MARGEM, ClassificadorOpticoNokiaG1425GB.classificarRx(-25.0))
        assertEquals(NivelSinalOpticoRx.DENTRO_DA_FAIXA_COM_MARGEM, ClassificadorOpticoNokiaG1425GB.classificarRx(-10.0))
    }

    @Test
    fun `RX ausente (null) nunca classifica como fora de especificacao`() {
        assertEquals(NivelSinalOpticoRx.NAO_INFORMADO, ClassificadorOpticoNokiaG1425GB.classificarRx(null))
    }

    @Test
    fun `TX nas bordas +0,5 e +5,0 esta dentro da faixa normativa`() {
        assertEquals(NivelSinalOpticoTx.DENTRO_DA_FAIXA_NORMATIVA, ClassificadorOpticoNokiaG1425GB.classificarTx(0.5))
        assertEquals(NivelSinalOpticoTx.DENTRO_DA_FAIXA_NORMATIVA, ClassificadorOpticoNokiaG1425GB.classificarTx(5.0))
    }

    @Test
    fun `TX abaixo ou acima das bordas esta fora da faixa normativa`() {
        assertEquals(NivelSinalOpticoTx.FORA_DE_ESPECIFICACAO, ClassificadorOpticoNokiaG1425GB.classificarTx(0.4))
        assertEquals(NivelSinalOpticoTx.FORA_DE_ESPECIFICACAO, ClassificadorOpticoNokiaG1425GB.classificarTx(5.1))
    }

    @Test
    fun `TX ausente (null) nunca classifica como fora de especificacao`() {
        assertEquals(NivelSinalOpticoTx.NAO_INFORMADO, ClassificadorOpticoNokiaG1425GB.classificarTx(null))
    }

    @Test
    fun `temperatura nula com RX-TX validos nao impede classificacao optica`() {
        // GH#1213 -- temperatura ausente nao pode contaminar RX/TX validos; este classificador
        // nem recebe temperatura como parametro (ela e tratada a parte, sem threshold ainda --
        // ver KDoc do perfil), entao RX/TX classificam normalmente mesmo sem nenhum dado de temp.
        assertEquals(NivelSinalOpticoRx.DENTRO_DA_FAIXA_COM_MARGEM, ClassificadorOpticoNokiaG1425GB.classificarRx(-18.0))
        assertEquals(NivelSinalOpticoTx.DENTRO_DA_FAIXA_NORMATIVA, ClassificadorOpticoNokiaG1425GB.classificarTx(2.1))
    }

    @Test
    fun `todos os campos opticos nulos classificam como nao informado, nunca como fora de especificacao`() {
        assertEquals(NivelSinalOpticoRx.NAO_INFORMADO, ClassificadorOpticoNokiaG1425GB.classificarRx(null))
        assertEquals(NivelSinalOpticoTx.NAO_INFORMADO, ClassificadorOpticoNokiaG1425GB.classificarTx(null))
    }
}
