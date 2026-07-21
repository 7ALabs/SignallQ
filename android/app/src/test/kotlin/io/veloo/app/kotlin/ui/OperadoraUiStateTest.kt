package io.signallq.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/** GH#1226 item 6/RF-A — identidade sem contato não pode virar "não identificada". */
class OperadoraUiStateTest {
    private fun identidade(source: OperadoraSource = OperadoraSource.REMOTE) =
        ResolvedOperadoraIdentity(
            displayName = "ISP Regional",
            monograma = "I",
            corMarca = null,
            logoRes = null,
            logoUrl = null,
            source = source,
        )

    private fun contato(
        source: OperadoraSource = OperadoraSource.REMOTE,
        site: String? = null,
        whatsapp: String? = null,
        sacPhone: String? = null,
    ) = ResolvedOperadoraContact(
        displayName = "ISP Regional",
        sacPhone = sacPhone,
        whatsapp = whatsapp,
        site = site,
        source = source,
    )

    @Test
    fun `carregando tem prioridade sobre qualquer dado parcial`() {
        val estado = resolverOperadoraUiState(carregando = true, identidade = identidade(), contato = contato(site = "https://x.com"))
        assertTrue(estado is OperadoraUiState.Loading)
    }

    @Test
    fun `identidade com contato acionavel e IdentifiedWithContacts`() {
        val estado = resolverOperadoraUiState(false, identidade(), contato(whatsapp = "11999999999"))
        assertTrue(estado is OperadoraUiState.IdentifiedWithContacts)
    }

    @Test
    fun `identidade sem NENHUM canal e IdentifiedWithoutContacts, nunca NotIdentified`() {
        // GH#1226 item 6 -- este e o bug real que a issue descreve: antes isso virava
        // "Não foi possível identificar sua operadora automaticamente", uma mentira.
        val estado = resolverOperadoraUiState(false, identidade(), contato())
        assertTrue(estado is OperadoraUiState.IdentifiedWithoutContacts)
    }

    @Test
    fun `identidade com apenas site ja conta como contato disponivel`() {
        val estado = resolverOperadoraUiState(false, identidade(), contato(site = "https://ispregional.com.br"))
        assertTrue(estado is OperadoraUiState.IdentifiedWithContacts)
    }

    @Test
    fun `sem identidade real (fallback ou nulo) e NotIdentified`() {
        assertTrue(resolverOperadoraUiState(false, null, null) is OperadoraUiState.NotIdentified)
        assertTrue(
            resolverOperadoraUiState(false, identidade(OperadoraSource.FALLBACK), null) is OperadoraUiState.NotIdentified,
        )
    }
}
