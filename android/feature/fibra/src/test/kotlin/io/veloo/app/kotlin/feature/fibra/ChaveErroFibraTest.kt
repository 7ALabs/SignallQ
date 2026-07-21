package io.signallq.app.feature.fibra

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

/** GH#1213 (dispatch final) — diferenciação completa de estados de sessão/erro do Nokia G-1425G-B. */
class ChaveErroFibraTest {

    @Test
    fun `host invalido vira erroHostInvalido`() {
        assertEquals("erroHostInvalido", chaveErroFibra(IllegalArgumentException("host invalido")))
    }

    @Test
    fun `ConnectException vira erroModemInacessivel`() {
        assertEquals("erroModemInacessivel", chaveErroFibra(ConnectException("Connection refused")))
    }

    @Test
    fun `SocketTimeoutException vira erroTimeout`() {
        assertEquals("erroTimeout", chaveErroFibra(SocketTimeoutException("timed out")))
    }

    @Test
    fun `err_t=1 (credenciais invalidas) vira erroCredenciaisInvalidas`() {
        assertEquals("erroCredenciaisInvalidas", chaveErroFibra(IOException("login falhou: err_t=1")))
    }

    @Test
    fun `err_t=0 (sessao ocupada) vira erroSessaoOcupada, nao mais erroComunicacaoModem`() {
        assertEquals("erroSessaoOcupada", chaveErroFibra(IOException("sessao em uso por outro acesso (err_t=0)")))
    }

    @Test
    fun `err_t=2 (token expirado) vira erroTokenExpirado, nao mais erroComunicacaoModem`() {
        assertEquals("erroTokenExpirado", chaveErroFibra(IOException("token expirado — retry necessario (err_t=2)")))
    }

    @Test
    fun `pubkey ausente (driver incompativel) vira erroRespostaModemInvalida`() {
        assertEquals("erroRespostaModemInvalida", chaveErroFibra(IOException("pubkey nao encontrado na pagina de login")))
    }

    @Test
    fun `erro nao mapeado cai no bucket generico erroComunicacaoModem`() {
        assertEquals("erroComunicacaoModem", chaveErroFibra(IOException("resposta inesperada, status=500")))
    }
}
